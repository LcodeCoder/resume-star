package com.resume.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.util.RateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitInterceptorTest {
    private final RateLimitInterceptor interceptor = new RateLimitInterceptor(new RateLimiter(), new ObjectMapper());

    private MockHttpServletRequest request(String path, long userId) {
        var request = new MockHttpServletRequest("POST", path);
        request.getSession().setAttribute("userId", userId);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    @Test void allPaidAiEndpointsSharePerUserBurstLimit() throws Exception {
        for (int i = 0; i < 20; i++) {
            String path = i % 3 == 0 ? "/ai/optimize" :
                    i % 3 == 1 ? "/career-lab/assist" : "/smart-resume/generate";
            assertTrue(interceptor.preHandle(request(path, 7L), new MockHttpServletResponse(), new Object()));
        }
        var blocked = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request("/smart-resume/generate", 7L), blocked, new Object()));
        assertEquals(429, blocked.getStatus());
        // 同 IP 上的另一会员仍可生成，说明没有进入登录接口的 IP 桶。
        assertTrue(interceptor.preHandle(request("/smart-resume/generate", 8L), new MockHttpServletResponse(), new Object()));
    }
}
