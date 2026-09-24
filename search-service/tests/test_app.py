import unittest
from types import SimpleNamespace
from unittest.mock import patch

from qdrant_client import models
from app import AnalyzeRequest, analyze, bm25_scores, documents, recommend, requirements, rrf


class FakeModel:
    def __init__(self):
        self.calls = []

    def embed(self, texts):
        self.calls.append(list(texts))
        for text in texts:
            yield SimpleNamespace(tolist=lambda text=text: [0.7] * 512)


class FakeClient:
    def __init__(self, scores=None):
        self.points = {}
        self.filters = []
        self.scores = scores

    def scroll(self, collection_name, scroll_filter, limit, offset, with_payload, with_vectors):
        self.filters.append(scroll_filter)
        user = scroll_filter.must[0].match.value
        points = [SimpleNamespace(id=p.id) for p in self.points.values() if p.payload['user_id'] == user]
        return points, None

    def delete(self, collection_name, points_selector, wait):
        self.points = {key: p for key, p in self.points.items() if key not in points_selector.points}

    def upsert(self, collection_name, points, wait):
        self.points.update((p.id, p) for p in points)

    def query_points(self, collection_name, query, query_filter, limit, with_payload):
        self.filters.append(query_filter)
        user = query_filter.must[0].match.value
        user_points = [p for p in self.points.values() if p.payload['user_id'] == user][:limit]
        return SimpleNamespace(points=[SimpleNamespace(payload=p.payload,
                                     score=self.scores[i] if self.scores else 0.7)
                                       for i, p in enumerate(user_points)])


class EvidenceSearchTests(unittest.TestCase):
    def test_chinese_bm25_and_rrf_keep_exact_technology(self):
        docs = documents([{'id': 'p1', 'title': '接口优化', 'description': '用缓存、限流降低高并发接口延迟。'},
                          {'id': 'p2', 'title': '网站', 'description': '使用 Vue 实现网页。'}])
        scores = bm25_scores('高并发接口，熟悉 Java', docs)
        self.assertGreater(scores[0], scores[1])
        self.assertEqual(rrf([0, 1], [1, 0]), [(0, 1/61 + 1/62), (1, 1/61 + 1/62)])
        self.assertEqual(requirements('精通 Vue；精通 Vue；熟悉 Java'), ['精通 Vue', '熟悉 Java'])

    def test_semantic_bridge_is_candidate_but_missing_java_remains_gap(self):
        client = FakeClient(scores=[0.43, 0.34])
        response = analyze(AnalyzeRequest(userId=42, jd='高并发接口；熟悉 Java', experiences=[
            {'id': 'a', 'title': '校园服务性能调优', 'description': '完成缓存、限流、压测，优化响应时间。'},
            {'id': 'b', 'title': '网站开发', 'description': '用 Vue 实现页面。'}
        ]), FakeModel(), client)
        self.assertEqual(response['recommendation']['projects'][0]['experienceId'], 'a')
        self.assertEqual(response['recommendation']['uncovered'], ['熟悉 Java'])

    def test_weighted_cover_prefers_new_requirements_and_shows_gaps(self):
        rows = [{'requirement': r, 'matches': matches} for r, matches in [
            ('必须熟悉 Vue', [{'experienceId': 'a', 'title': '前端', 'quote': 'Vue 页面', 'supported': True}]),
            ('接口设计', [{'experienceId': 'b', 'title': '服务端', 'quote': '设计接口', 'supported': True}]),
            ('团队协作', [{'experienceId': 'a', 'title': '前端', 'quote': '项目协作', 'supported': True},
                       {'experienceId': 'b', 'title': '服务端', 'quote': '对接成员', 'supported': True}]),
            ('安全审计', [{'experienceId': 'b', 'title': '服务端', 'quote': '无关材料', 'supported': False}])]]
        result = recommend(rows, 2)
        self.assertEqual([p['experienceId'] for p in result['projects']], ['a', 'b'])
        self.assertEqual(result['uncovered'], ['安全审计'])
        self.assertEqual(result['projects'][1]['covers'][0]['requirement'], '接口设计')

    def test_qdrant_incremental_sync_on_delete_and_user_isolation(self):
        client = FakeClient()
        model = FakeModel()
        a = AnalyzeRequest(userId=42, jd='高并发接口', experiences=[{'id': 'a', 'title': '接口', 'description': '缓存、限流优化高并发接口。'}])
        b = AnalyzeRequest(userId=43, jd='Vue 项目', experiences=[{'id': 'b', 'title': 'Vue 项目', 'description': '搭建 Vue 项目。'}])
        analyze(a, model, client)
        first_calls = len(model.calls)
        analyze(a, model, client)
        self.assertEqual(len(model.calls), first_calls + 1)  # Only the query; unchanged document vector is reused.
        analyze(b, model, client)
        result = analyze(AnalyzeRequest(userId=42, jd='高并发接口', experiences=[]), model, client)
        self.assertEqual(result['recommendation']['uncovered'], ['高并发接口'])
        self.assertEqual({p.payload['user_id'] for p in client.points.values()}, {43})
        self.assertTrue(all(isinstance(f, models.Filter) for f in client.filters))
        self.assertEqual([f.must[0].match.value for f in client.filters], [42, 42, 42, 42, 43, 43, 42])


if __name__ == '__main__':
    unittest.main()
