package com.resume.controller;

import com.resume.common.Result;
import com.resume.service.FileUploadService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 * 功能：处理用户头像、简历附件等文件上传
 * @author 开发人员
 * @date 2026-06-10
 */
@RestController
@RequestMapping("/upload")
public class FileUploadController {
    /** 文件上传服务 */
    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * 上传用户头像
     * @param file 头像图片文件（仅支持 jpg、jpeg、png、gif 格式，大小不超过 5MB）
     * @param session 当前用户会话
     * @return 上传成功返回头像 URL 地址
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpSession session) {
        // 校验用户登录状态
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 调用文件上传服务处理头像上传
        try {
            String avatarUrl = fileUploadService.uploadAvatar(file, (Long) userId);
            return Result.success(avatarUrl);
        } catch (IllegalArgumentException e) {
            // 捕获业务校验异常（文件格式、大小不符等）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 捕获其他异常
            return Result.fail("上传失败：" + e.getMessage());
        }
    }
}
