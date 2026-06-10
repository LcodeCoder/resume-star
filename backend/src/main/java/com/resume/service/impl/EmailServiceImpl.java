package com.resume.service.impl;

import com.resume.entity.EmailVerifyCode;
import com.resume.entity.SystemConfig;
import com.resume.service.EmailService;
import com.resume.service.SystemConfigService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱服务实现类
 * 功能：发送验证码邮件、验证验证码有效性
 * 说明：当前演示版本使用内存存储验证码，生产环境建议使用Redis
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class EmailServiceImpl implements EmailService {
    /** QQ邮箱 SMTP 服务器地址 */
    private static final String QQ_SMTP_HOST = "smtp.qq.com";
    /** QQ邮箱 SMTP SSL 端口 */
    private static final int QQ_SMTP_SSL_PORT = 465;

    /** 系统配置服务 */
    private final SystemConfigService configService;

    /** 验证码存储（演示环境内存存储，key为邮箱地址） */
    private final Map<String, EmailVerifyCode> codeStore = new ConcurrentHashMap<>();

    /** 验证码有效期（分钟） */
    private static final int CODE_EXPIRE_MINUTES = 5;

    public EmailServiceImpl(SystemConfigService configService) {
        this.configService = configService;
    }

    /**
     * 发送注册验证码
     * @param email 邮箱地址
     * @return 是否发送成功
     */
    @Override
    public boolean sendRegisterCode(String email) {
        try {
            // 1. 生成6位随机验证码
            String code = generateCode();

            // 2. 获取邮箱配置
            SystemConfig config = configService.getConfig();
            if (config.getEmailUsername() == null || config.getEmailUsername().isBlank()
                    || config.getEmailPassword() == null || config.getEmailPassword().isBlank()) {
                System.err.println("QQ邮箱未配置发送账号或授权码");
                return false;
            }

            // 3. 使用 QQ 邮箱 SMTP 发送验证码
            sendMail(email, code, config);

            // 4. 发送成功后保存验证码到内存
            EmailVerifyCode verifyCode = EmailVerifyCode.builder()
                    .email(email)
                    .code(code)
                    .expireTime(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES))
                    .used(false)
                    .createTime(LocalDateTime.now())
                    .build();
            codeStore.put(email, verifyCode);
            return true;

        } catch (Exception e) {
            System.err.println("发送验证码失败：" + e.getMessage());
            return false;
        }
    }

    /**
     * 验证邮箱验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @return 是否验证通过
     */
    @Override
    public boolean verifyCode(String email, String code) {
        // 1. 获取存储的验证码
        EmailVerifyCode stored = codeStore.get(email);
        if (stored == null) {
            return false;
        }

        // 2. 检查是否已使用
        if (stored.getUsed()) {
            return false;
        }

        // 3. 检查是否过期
        if (LocalDateTime.now().isAfter(stored.getExpireTime())) {
            codeStore.remove(email);
            return false;
        }

        // 4. 验证验证码是否匹配
        if (!code.equals(stored.getCode())) {
            return false;
        }

        // 5. 标记为已使用
        stored.setUsed(true);
        codeStore.put(email, stored);

        return true;
    }

    /**
     * 发送 QQ 邮箱验证码邮件
     * @param email 收件邮箱
     * @param code 验证码
     * @param config 系统配置
     */
    private void sendMail(String email, String code, SystemConfig config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(QQ_SMTP_HOST);
        mailSender.setPort(QQ_SMTP_SSL_PORT);
        mailSender.setUsername(config.getEmailUsername());
        mailSender.setPassword(config.getEmailPassword());
        mailSender.setDefaultEncoding("UTF-8");

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.ssl.required", "true");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(config.getEmailUsername());
        message.setTo(email);
        message.setSubject("resume-lcode 邮箱验证码");
        message.setText(buildEmailContent(code));
        mailSender.send(message);
    }

    /**
     * 构建验证码邮件正文
     * @param code 验证码
     * @return 邮件正文
     */
    private String buildEmailContent(String code) {
        return "【resume-lcode】\n\n"
                + "您好！\n"
                + "您正在进行账号注册邮箱验证。\n\n"
                + "本次验证码：" + code + "\n\n"
                + "有效期：" + CODE_EXPIRE_MINUTES + " 分钟\n"
                + "请勿将验证码泄露给他人，如非本人操作请忽略本邮件。";
    }

    /**
     * 生成6位随机数字验证码
     * @return 验证码字符串
     */
    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}
