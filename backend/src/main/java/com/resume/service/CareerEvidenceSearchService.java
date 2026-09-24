package com.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 检索代理：服务端根据登录态读取用户自己的 MySQL 工作区，再交给内网检索服务。
 * 不接受浏览器提交的 userId 或经历材料，避免跨用户检索与任意材料伪造。
 */
@Service
public class CareerEvidenceSearchService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final RestClient client;
    private final String endpoint;
    private final CareerEvidenceQuotaService quota;

    public CareerEvidenceSearchService(JdbcTemplate jdbc, ObjectMapper mapper,
                                       CareerEvidenceQuotaService quota,
                                       @Value("${resume.evidence-search.url:}") String endpoint) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.endpoint = endpoint;
        this.quota = quota;
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(5_000);
        requests.setReadTimeout(150_000); // 首次加载/下载中文模型可能耗时，前端超时略长于此值。
        this.client = RestClient.builder().requestFactory(requests).build();
    }

    public JsonNode analyze(Long userId, String jd) {
        if (jd == null || jd.isBlank() || jd.length() > 6000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写岗位要求（最多 6000 字）");
        }
        if (endpoint.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "检索服务未配置，请先启动 Qdrant 与中文向量服务");
        }
        int remaining = quota.reserve(userId);
        try {
            List<String> rows = jdbc.queryForList("SELECT data FROM rl_career_lab WHERE user_id = ?", String.class, userId);
            JsonNode experiences = rows.isEmpty() ? mapper.createArrayNode() : mapper.readTree(rows.get(0)).path("experiences");
            if (!experiences.isArray()) experiences = mapper.createArrayNode();
            // 服务端身份、服务端经历、用户当前 JD；工作区尚未保存时前端需先等待自动保存完成。
            JsonNode result = client.post().uri(endpoint + "/analyze")
                    .body(Map.of("userId", userId, "jd", jd, "experiences", experiences))
                    .retrieve().body(JsonNode.class);
            if (!(result instanceof com.fasterxml.jackson.databind.node.ObjectNode object))
                throw new IllegalStateException("Invalid evidence search response");
            object.put("quotaRemaining", remaining);
            return object;
        } catch (Exception ex) {
            try { quota.refund(userId); }
            catch (Exception refundError) { org.slf4j.LoggerFactory.getLogger(getClass()).error("Evidence quota refund failed userId={}", userId, refundError); }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "语义检索暂不可用，请检查 Qdrant、模型下载与检索服务", ex);
        }
    }
}
