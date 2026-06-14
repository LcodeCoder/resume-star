package com.resume.service.impl;

import com.resume.entity.AuthLoginVO;
import com.resume.entity.LoginRequest;
import com.resume.entity.RegisterRequest;
import com.resume.entity.SystemConfig;
import com.resume.repository.InMemoryDataRepository;
import com.resume.service.EmailService;
import com.resume.service.SystemConfigService;
import com.resume.support.TestRepos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 关键路径测试：注册 / 登录 / token / 改密 鉴权流程。
 * 直接构造 {@link UserServiceImpl} + 隔离内存仓库，邮箱与系统配置用 Mockito 桩。
 */
class AuthServiceImplTest {

    private InMemoryDataRepository repo;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        repo = TestRepos.freshRepo();
        EmailService emailService = Mockito.mock(EmailService.class);
        SystemConfigService configService = Mockito.mock(SystemConfigService.class);
        SystemConfig cfg = new SystemConfig();
        cfg.setEmailVerifyEnabled(false); // 关闭邮箱验证，专注鉴权主流程
        Mockito.when(configService.getConfig()).thenReturn(cfg);
        userService = new UserServiceImpl(repo, emailService, configService);
    }

    private RegisterRequest reg(String username, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username);
        r.setPassword(password);
        r.setNickname(username);
        return r;
    }

    private LoginRequest login(String username, String password) {
        LoginRequest r = new LoginRequest();
        r.setUsername(username);
        r.setPassword(password);
        return r;
    }

    @Test
    @DisplayName("注册成功返回 token 与用户资料")
    void register_returnsTokenAndProfile() {
        AuthLoginVO vo = userService.register(reg("alice", "secret123"));
        assertNotNull(vo);
        assertNotNull(vo.getToken());
        assertEquals("alice", vo.getProfile().getUsername());
    }

    @Test
    @DisplayName("重复用户名注册被拒（返回 null）")
    void register_duplicateRejected() {
        userService.register(reg("bob", "secret123"));
        assertNull(userService.register(reg("bob", "another1")));
    }

    @Test
    @DisplayName("密码以 BCrypt 存储，正确密码校验通过、错误密码失败")
    void password_isHashedAndVerifiable() {
        AuthLoginVO vo = userService.register(reg("carol", "secret123"));
        Long id = vo.getProfile().getId();
        assertTrue(repo.checkUserPassword(id, "secret123"));
        assertFalse(repo.checkUserPassword(id, "wrongpass"));
    }

    @Test
    @DisplayName("登录：正确凭据返回可用 token；用户不存在或密码错误返回 null")
    void login_flow() {
        userService.register(reg("dave", "secret123"));
        AuthLoginVO ok = userService.login(login("dave", "secret123"));
        assertNotNull(ok);
        assertNotNull(ok.getToken());
        assertEquals("dave", repo.findUserByToken(ok.getToken()).getUsername());
        assertNull(userService.login(login("dave", "badpass")));
        assertNull(userService.login(login("ghost", "secret123")));
    }

    @Test
    @DisplayName("登出后 token 立即失效")
    void logout_invalidatesToken() {
        AuthLoginVO vo = userService.register(reg("erin", "secret123"));
        String token = vo.getToken();
        assertNotNull(repo.findUserByToken(token));
        userService.logoutByToken(token);
        assertNull(repo.findUserByToken(token));
    }

    @Test
    @DisplayName("改密：旧密码错误失败；成功后新密码可登录、旧密码失效")
    void changePassword_flow() {
        AuthLoginVO vo = userService.register(reg("frank", "oldpass1"));
        Long id = vo.getProfile().getId();
        assertFalse(userService.changePassword(id, "wrongold", "newpass1"));
        assertTrue(userService.changePassword(id, "oldpass1", "newpass1"));
        assertNull(userService.login(login("frank", "oldpass1")));
        assertNotNull(userService.login(login("frank", "newpass1")));
    }
}
