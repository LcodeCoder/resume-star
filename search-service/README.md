# 学习笔记：证据混合检索

本服务被 Java 后端在 Docker 内网调用，浏览器只访问受登录态保护的 `/career-lab/evidence/analyze`。Java 根据 Session 的 `userId` 从 MySQL 加载经历，避免浏览器传入别人的 ID 或伪造检索材料。

1. `documents()` 把每份经历的本人行动、材料来源和核验记录拆成**连续的原文片段**，保留标题、来源字段及经历 ID。较长材料采用轮流选片段，确保后面的项目也有机会进入索引。每次分析依据 MySQL 与 Qdrant 对照，只嵌入变化的片段并删除过时的片段。点 ID 由用户、项目、标题、字段与原句稳定生成。
2. `bm25_scores()` 用 jieba 搜索分词和英文精确词做词项检索。BM25 大致等于 `IDF × 词频饱和 × 文档长度修正`；Java/Vue 等技术名词能精确检出。
3. `model.embed()` 用 **BAAI/bge-small-zh-v1.5** 输出 512 维中文向量。Qdrant 使用余弦相似度并强制 `user_id` payload 过滤。例如岗位写「高并发接口」，材料写「缓存、限流、压测」时，可从语义排名找到候选原句。
4. `rrf()` 对两个排名分别按 `1 / (60 + 名次)` 计分后相加。BM25 原始分与余弦值单位不同，直接相加并不合理；RRF 利用名次融合。界面同时展示两路原始排名和引用原句。
5. `supported` 是**保守的候选材料门槛**：中文语义首名需要显著领先且达到下限；出现 Java/Vue 等技术专名时必须在材料原句中逐字出现。门槛不代表已经证实个人能力。误检与漏检都可能发生，生产使用前应收集人工标注的 JD/经历样本调参。
6. `recommend()` 是最多选三项的**加权集合覆盖贪心算法**：每轮选能覆盖最多「此前还未覆盖」岗位要求权重的项目。含「必须/精通」的要求权重 1.5，「优先/加分」权重 0.7，其余 1.0。同一项目多个片段仍只占一个推荐名额；剩余要求列在 `uncovered`。

本地验证（需要 Python 环境安装 `requirements.txt`）：

```bash
PYTHONPATH=search-service python -m unittest discover -s search-service/tests -v
```

Docker 中 `qdrant-data` 和 `embedding-models` 分别保存向量与模型权重，Qdrant 无对外端口。第一次分析要下载模型，失败会明确告知，不会把关键词线索伪装成语义结果。会员每日检索次数由后台会员套餐调整；请求失败会退还次数。

代理部署：在部署机器先确认容器可访问 Mihomo 的 HTTP 入口，然后设置 `MIHOMO_PROXY_URL=http://<宿主机内网地址>:<端口>` 再运行 `docker compose up -d --build`；该地址同时用于 pip 构建依赖与首次下载中文模型。不要填仅绑定宿主机回环的地址；`NO_PROXY` 保证 Qdrant 内网直连。
