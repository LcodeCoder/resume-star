package com.resume.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 * 功能：处理用户头像、简历附件等文件的上传、校验、存储
 * @author 开发人员
 * @date 2026-06-10
 */
public interface FileUploadService {
    /**
     * 上传用户头像
     * @param file 上传的图片文件
     * @param userId 用户ID
     * @return 上传成功后的文件访问 URL
     * @throws IllegalArgumentException 文件格式不支持、文件过大等校验异常
     * @throws Exception 文件存储失败等其他异常
     */
    String uploadAvatar(MultipartFile file, Long userId) throws Exception;
}
