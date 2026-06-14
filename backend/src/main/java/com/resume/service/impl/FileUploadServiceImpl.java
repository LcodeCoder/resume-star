package com.resume.service.impl;

import com.resume.service.FileUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务实现类
 * 功能：处理用户头像上传，支持本地存储，校验文件格式与大小
 * @author 开发人员
 * @date 2026-06-10
 */
@Service
public class FileUploadServiceImpl implements FileUploadService {
    /** 文件上传根目录 */
    private static final String UPLOAD_DIR = "uploads/avatars/";
    /** 允许的图片格式 */
    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/gif");
    /** 文件大小限制（5MB） */
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    /**
     * 上传用户头像
     * @param file 上传的图片文件
     * @param userId 用户ID
     * @return 头像访问URL
     */
    @Override
    public String uploadAvatar(MultipartFile file, Long userId) throws Exception {
        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 2. 校验文件格式
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("仅支持 JPG、PNG、GIF 格式的图片");
        }

        // 3. 校验文件大小
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 5MB");
        }

        // 4. 创建上传目录
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 5. 生成唯一文件名：扩展名由「已校验的 contentType」推导，绝不取用户文件名后缀，
        //    避免 image/* 内容配 evil.html / evil.jsp 后缀落地成可被解析执行的文件（存储型 XSS / 脚本执行）
        String extension = extensionForType(contentType);
        String filename = userId + "_" + UUID.randomUUID().toString() + extension;

        // 6. 保存文件到本地
        Path filePath = Paths.get(UPLOAD_DIR + filename);
        try {
            Files.write(filePath, file.getBytes());
        } catch (IOException e) {
            throw new Exception("文件保存失败：" + e.getMessage());
        }

        // 7. 返回文件访问URL（相对路径，前端需配置静态资源访问）
        return "/uploads/avatars/" + filename;
    }

    /** 由已通过白名单校验的 contentType 推导安全扩展名（不信任用户上传的文件名后缀） */
    private String extensionForType(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            default -> ".jpg"; // image/jpeg、image/jpg
        };
    }
}
