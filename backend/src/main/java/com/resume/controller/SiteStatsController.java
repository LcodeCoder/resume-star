package com.resume.controller;

import com.resume.common.Result;
import com.resume.repository.InMemoryDataRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 站点统计控制器
 * 功能：提供公开的平台统计数据（模板数、行业分类数、累计 AI 优化次数等），供首页展示
 * @author 开发人员
 * @date 2026-06-13
 */
@RestController
@RequestMapping("/stats")
public class SiteStatsController {
    /** 内存数据仓库 */
    private final InMemoryDataRepository repository;

    /** 构造站点统计控制器 */
    public SiteStatsController(InMemoryDataRepository repository) {
        this.repository = repository;
    }

    /**
     * 查询站点公开统计
     * @return templateCount-模板数 categoryCount-行业分类数 aiCallCount-累计 AI 优化次数
     */
    @GetMapping("/site")
    public Result<Map<String, Object>> site() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("templateCount", repository.listTemplates(null, null).size());
        data.put("categoryCount", repository.listCategories().size());
        data.put("aiCallCount", repository.getAiCallCount());
        return Result.success(data);
    }
}
