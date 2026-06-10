package com.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.entity.AiConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 配置数据访问层
 * 功能：管理 AI 接口配置的增删改查
 * @author 开发人员
 * @date 2026-06-10
 */
@Mapper
public interface AiConfigMapper extends BaseMapper<AiConfig> {
}
