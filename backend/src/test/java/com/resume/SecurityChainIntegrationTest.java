package com.resume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 鉴权 / 越权 / 限流「拦截器 + 控制器」真实链路集成测试。
 *
 * 与服务层单测互补：单测验证业务逻辑，本测试验证 {@link com.resume.config.WebMvcConfig} 里
 * 拦截器路径与控制器映射是否真正对齐 —— 正是这条链路上的「{@code /resume/**} 写错、与 {@code /resumes}
 * 不匹配导致鉴权形同虚设」漏洞，单测无法发现，必须用 MockMvc 跑通真实请求才能守住回归。
 *
 * 数据库指向临时目录，每次运行使用全新 SQLite，不污染开发库。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityChainIntegrationTest {

    /** 把 SQLite 数据目录指向一次性临时目录，保证测试隔离、注册账号每轮都是干净的 */
    @DynamicPropertySource
    static void isolateDatabase(DynamicPropertyRegistry registry) throws IOException {
        Path tempDir = Files.createTempDirectory("resume-itest-");
        tempDir.toFile().deleteOnExit();
        registry.add("resume.data-dir", tempDir::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---- ① 受保护接口：未登录必须 401（路径鉴权 bug 的回归守卫）----

    @Test
    void protectedEndpoints_requireLogin() throws Exception {
        // 这三个端点正是被拦截器保护的核心资源；未登录访问必须 401
        mockMvc.perform(get("/resumes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/user/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/ai/optimize")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- ② 公开读接口：未登录也应可达（验证 exclude 配置未误伤）----

    @Test
    void publicEndpoints_accessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/interview/categories")).andExpect(status().isOk());
    }

    // ---- ③ 登录后凭会话可正常访问自己的资源 ----

    @Test
    void afterRegister_canAccessOwnResumes() throws Exception {
        MockHttpSession session = registerAndLogin("itest_owner", "10.0.0.1");
        mockMvc.perform(get("/resumes").session(session))
                .andExpect(status().isOk());
    }

    // ---- ④ 水平越权（IDOR）：B 不能读到 A 的简历 ----

    @Test
    void userCannotReadAnothersResume() throws Exception {
        MockHttpSession sessionA = registerAndLogin("itest_alice", "10.0.0.2");
        // A 创建一份空白简历，拿到其 id
        MvcResult created = mockMvc.perform(post("/resumes/blank").session(sessionA))
                .andExpect(status().isOk())
                .andReturn();
        long resumeIdOfA = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        assertThat(resumeIdOfA).isPositive();

        // B 登录后，拿 A 的简历 id 去查 —— 必须查不到（归属隔离），返回业务失败而非泄露数据
        MockHttpSession sessionB = registerAndLogin("itest_bob", "10.0.0.3");
        MvcResult asB = mockMvc.perform(get("/resumes/" + resumeIdOfA).session(sessionB))
                .andExpect(status().isOk()) // 业务失败仍是 HTTP 200，靠 body.success 区分
                .andReturn();
        JsonNode body = objectMapper.readTree(asB.getResponse().getContentAsString());
        assertThat(body.path("success").asBoolean()).isFalse();
    }

    // ---- ⑤ 登录接口限流：同一 IP 短时间内超过阈值返回 429 ----

    @Test
    void loginEndpoint_isRateLimitedPerIp() throws Exception {
        String ip = "10.9.9.9"; // 独立 IP 桶，避免与其它用例相互干扰
        String body = "{\"username\":\"nobody_itest\",\"password\":\"wrong\"}";
        // 前 10 次（阈值内）不应触发 429
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/user/login")
                    .header("X-Forwarded-For", ip)
                    .contentType("application/json").content(body));
        }
        // 第 11 次超过每分钟上限，限流拦截器应直接返回 429
        mockMvc.perform(post("/user/login")
                        .header("X-Forwarded-For", ip)
                        .contentType("application/json").content(body))
                .andExpect(status().isTooManyRequests());
    }

    /** 注册一个新用户并返回已写入登录态的会话（emailVerify 默认关闭，无需验证码）。 */
    private MockHttpSession registerAndLogin(String username, String ip) throws Exception {
        MockHttpSession session = new MockHttpSession();
        String json = "{\"username\":\"" + username + "\",\"password\":\"Passw0rd!\",\"nickname\":\"" + username + "\"}";
        mockMvc.perform(post("/user/register")
                        .session(session)
                        .header("X-Forwarded-For", ip)
                        .contentType("application/json").content(json))
                .andExpect(status().isOk());
        return session;
    }
}
