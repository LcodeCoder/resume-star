"""职业实验室检索服务：中文 BGE 语义向量 + BM25 + RRF + 加权集合覆盖。

只在 Docker 内网接受后端转发的请求；身份来自 Java 登录态，绝不能接受浏览器自报 userId。
"""
import os
import re
import uuid
from collections import Counter
from math import log
from typing import Any, Dict, List

import jieba
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from qdrant_client import QdrantClient, models
from fastembed import TextEmbedding

MODEL = "BAAI/bge-small-zh-v1.5"
DIMENSION = 512
COLLECTION = "career_evidence_bge_zh_v1"  # 换模型/维度须新建 collection，避免混用不同向量空间。
QDRANT_URL = os.getenv("QDRANT_URL", "http://qdrant:6333")
app = FastAPI(title="职业实验室证据检索", docs_url=None, redoc_url=None)
_model = None
_client = None


class AnalyzeRequest(BaseModel):
    userId: int = Field(gt=0)
    jd: str = Field(max_length=6000)
    experiences: List[Dict[str, Any]] = Field(max_length=120)


def resources():
    global _model, _client
    # 延迟加载允许容器先启动；第一次请求会下载模型并生成本地缓存。
    if _model is None:
        _model = TextEmbedding(model_name=MODEL, cache_dir=os.getenv("FASTEMBED_CACHE_PATH", "/models"))
    if _client is None:
        _client = QdrantClient(url=QDRANT_URL, timeout=30)
    if not _client.collection_exists(COLLECTION):
        _client.create_collection(COLLECTION, vectors_config=models.VectorParams(size=DIMENSION, distance=models.Distance.COSINE))
        _client.create_payload_index(COLLECTION, field_name="user_id", field_schema=models.PayloadSchemaType.INTEGER)
    return _model, _client


def requirements(jd: str) -> List[str]:
    """与前端图谱相同的拆句规则和上限，保证推荐能准确点回该条 JD。"""
    return list(dict.fromkeys(p for p in (
        re.sub(r"^[\s\d、.（）()\-]+", "", s).strip()
        for s in re.split(r"[\n；;。•]", jd)
    ) if len(p) >= 3))[:16]


def tokenize(text: str) -> List[str]:
    """jieba 切中文词，同时保留 Java/Vue 3/C++ 等英文技术词的精确匹配。"""
    text = text.lower()
    pieces = re.findall(r"[a-z][a-z0-9+.#-]*|[\u4e00-\u9fff]+", text)
    result = []
    stopwords = {"熟悉", "掌握", "能够", "具备", "要求", "相关", "项目", "工作", "经验", "负责", "优先", "加分"}
    for piece in pieces:
        if re.search(r"[\u4e00-\u9fff]", piece):
            result.extend(t for t in jieba.cut_for_search(piece) if len(t) >= 2)
        else:
            result.append(piece)
    return [word for word in result if word not in stopwords]


def snippets(text: str, limit: int = 360) -> List[str]:
    """按原句切片，过长的材料再按原始位置分块，返回值始终是原文的连续子串。"""
    result = []
    for part in re.findall(r"[^。；;\n]+[。；;\n]?", text[:4500]):
        part = part.strip()
        if len(part) < 3:
            continue
        for start in range(0, len(part), limit):
            quote = part[start:start + limit].strip()
            if len(quote) >= 3:
                result.append(quote)
    return result[:24]


def documents(experiences: List[Dict[str, Any]]) -> List[Dict[str, str]]:
    buckets = []
    for exp in experiences[:120]:
        exp_id = str(exp.get("id") or "")[:80]
        if not exp_id:
            continue
        title = str(exp.get("title") or "项目经历")[:120]
        group = []
        for field, label in (("description", "本人行动"), ("source", "材料来源"), ("evidence", "核验记录")):
            raw = str(exp.get(field) or "")[:4500]
            for quote in snippets(raw):
                group.append({"experienceId": exp_id, "title": title, "field": field,
                             "source": label, "quote": quote})
        buckets.append(group[:10])
    # 轮流选每个项目的原句，避免一个超长 PDF 挤掉后面项目的所有证据。
    return [doc for i in range(10) for group in buckets for doc in group[i:i + 1]][:600]


def bm25_scores(query: str, docs: List[Dict[str, str]]) -> List[float]:
    """BM25：中文搜索分词 + 逆文档频率；每次只计算当前登录用户的材料。"""
    tokens = [tokenize(d["title"] + " " + d["quote"]) for d in docs]
    if not tokens:
        return []
    lengths = [len(t) for t in tokens]
    avg = max(1, sum(lengths) / len(lengths))
    freq = Counter(word for words in tokens for word in set(words))
    scores = []
    for words, size in zip(tokens, lengths):
        counts = Counter(words)
        score = 0.0
        for term in set(tokenize(query)):
            tf = counts[term]
            if tf:
                idf = log(1 + (len(tokens) - freq[term] + 0.5) / (freq[term] + 0.5))
                score += idf * (tf * 2.2) / (tf + 1.2 * (0.25 + 0.75 * size / avg))
        scores.append(score)
    return scores


def rrf(lexical: List[int], semantic: List[int], k: int = 60) -> List[tuple]:
    """Reciprocal Rank Fusion：只合并两个排序的名次，不直接相加不同量纲的分数。"""
    scores = Counter()
    for ranking in (lexical, semantic):
        for rank, index in enumerate(ranking, 1):
            scores[index] += 1 / (k + rank)
    return sorted(scores.items(), key=lambda item: (-item[1], item[0]))


def weight(requirement: str) -> float:
    if any(word in requirement for word in ("必须", "精通", "核心", "必备")):
        return 1.5
    if any(word in requirement for word in ("加分", "优先", "更佳")):
        return 0.7
    return 1.0


def recommend(rows: List[Dict[str, Any]], top_n: int = 3) -> Dict[str, Any]:
    """加权集合覆盖的贪心近似：每轮挑对 *尚未覆盖* JD 要求增益最大的项目。

    同一个项目可有多段材料，但只占一个名额；不因重复命中而刷高总覆盖率。
    """
    project_hits: Dict[str, Dict[str, Any]] = {}
    all_reqs = [row["requirement"] for row in rows]
    for row in rows:
        for hit in row["matches"]:
            if not hit["supported"]:
                continue
            key = hit["experienceId"]
            entry = project_hits.setdefault(key, {"experienceId": key, "title": hit["title"], "evidence": {}})
            entry["evidence"].setdefault(row["requirement"], hit["quote"])
    selected = []
    remaining = set(all_reqs)
    while len(selected) < top_n:
        candidates = [(sum(weight(r) for r in remaining & set(p["evidence"])), key)
                      for key, p in project_hits.items() if key not in {s["experienceId"] for s in selected}]
        if not candidates:
            break
        gain, key = sorted(candidates, key=lambda x: (-x[0], x[1]))[0]
        if gain <= 0:
            break
        p = project_hits[key]
        new = [r for r in all_reqs if r in remaining and r in p["evidence"]]
        selected.append({"experienceId": key, "title": p["title"], "gain": round(gain, 2),
                         "covers": [{"requirement": r, "quote": p["evidence"][r]} for r in new]})
        remaining.difference_update(new)
    return {"projects": selected, "uncovered": [r for r in all_reqs if r in remaining],
            "coveredWeight": round(sum(weight(r) for r in all_reqs if r not in remaining), 2),
            "totalWeight": round(sum(weight(r) for r in all_reqs), 2)}


def analyze(request: AnalyzeRequest, model, client) -> Dict[str, Any]:
    reqs = requirements(request.jd)
    docs = documents(request.experiences)
    user_filter = models.Filter(must=[models.FieldCondition(key="user_id", match=models.MatchValue(value=request.userId))])
    # MySQL 工作区是真实来源：稳定 ID 由用户、项目、标题和原句决定。
    # JD 改动只重新计算查询向量；材料新增/修改才重新嵌入，删除时移除旧点。
    desired = {}
    for doc in docs:
        identity = f'{request.userId}/{doc["experienceId"]}/{doc["title"]}/{doc["field"]}/{doc["quote"]}'
        point_id = str(uuid.uuid5(uuid.NAMESPACE_URL, identity))
        desired[point_id] = doc
    existing = set()
    offset = None
    while True:
        points, offset = client.scroll(collection_name=COLLECTION, scroll_filter=user_filter,
                                       limit=256, offset=offset, with_payload=False, with_vectors=False)
        existing.update(str(point.id) for point in points)
        if offset is None:
            break
    new_ids = [point_id for point_id in desired if point_id not in existing]
    if new_ids:
        vectors = list(model.embed([desired[point_id]["title"] + "。" + desired[point_id]["quote"] for point_id in new_ids]))
        client.upsert(collection_name=COLLECTION, points=[
            models.PointStruct(id=point_id, vector=vector.tolist(), payload={**desired[point_id], "user_id": request.userId})
            for point_id, vector in zip(new_ids, vectors)
        ], wait=True)
    removed = list(existing - desired.keys())
    if removed:
        client.delete(collection_name=COLLECTION, points_selector=models.PointIdsList(points=removed), wait=True)
    rows = []
    if reqs and docs:
        query_vectors = list(model.embed(reqs))
        for req, vector in zip(reqs, query_vectors):
            lexical_scores = bm25_scores(req, docs)
            lexical = sorted((i for i, value in enumerate(lexical_scores) if value > 0), key=lambda i: -lexical_scores[i])[:20]
            neighbors = client.query_points(collection_name=COLLECTION, query=vector.tolist(), query_filter=user_filter,
                                            limit=min(20, len(docs)), with_payload=True).points
            key_to_index = {(d["experienceId"], d["field"], d["quote"]): i for i, d in enumerate(docs)}
            semantic = []
            similarities = {}
            for point in neighbors:
                payload = point.payload or {}
                i = key_to_index.get((payload.get("experienceId"), payload.get("field"), payload.get("quote")))
                if i is not None:
                    semantic.append(i)
                    similarities[i] = float(point.score)
            matches = []
            query_terms = set(tokenize(req))
            # Java、Vue 等明确技术词必须在引用原句中出现；不能用近似语义把别的语言当成它。
            exact_tech = {term for term in query_terms if re.match(r"^[a-z]", term)}
            top_semantic = similarities.get(semantic[0], 0) if semantic else 0
            runner_up = similarities.get(semantic[1], 0) if len(semantic) > 1 else 0
            for i, fused in rrf(lexical, semantic)[:8]:
                quote_terms = set(tokenize(docs[i]["quote"]))
                overlap = query_terms & quote_terms
                lexical_support = bool(query_terms) and len(overlap) >= max(1, (len(query_terms) + 1) // 2)
                # 中文语义弱分只可作为“唯一显著领先”线索；技术专名仍须逐字出现。
                semantic_support = (i == (semantic[0] if semantic else None) and top_semantic >= 0.40
                                    and (top_semantic - runner_up >= 0.07 or top_semantic >= 0.62))
                supported = exact_tech.issubset(quote_terms) and (lexical_support or semantic_support)
                matches.append({**docs[i], "rrfScore": round(fused, 5), "bm25Score": round(lexical_scores[i], 3),
                                "vectorScore": round(similarities.get(i, 0), 3), "supported": supported,
                                "bm25Rank": lexical.index(i) + 1 if i in lexical else None,
                                "vectorRank": semantic.index(i) + 1 if i in semantic else None})
            rows.append({"requirement": req, "weight": weight(req), "matches": matches})
    else:
        rows = [{"requirement": req, "weight": weight(req), "matches": []} for req in reqs]
    return {"model": MODEL, "ranking": "BM25 + Qdrant cosine + RRF(k=60)",
            "requirements": rows, "recommendation": recommend(rows)}


@app.post("/analyze")
def analyze_endpoint(request: AnalyzeRequest):
    try:
        model, client = resources()
        return analyze(request, model, client)
    except Exception as exc:
        # 不要静默退化为关键词检索却仍展示“语义检索已完成”。
        raise HTTPException(status_code=503, detail="语义检索不可用；请检查模型缓存与 Qdrant 服务") from exc
