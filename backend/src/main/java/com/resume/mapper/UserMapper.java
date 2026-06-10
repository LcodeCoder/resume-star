package com.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.entity.User;

/**
 * 用户 Mapper 接口
 * 功能：预留用户表 CRUD 数据访问能力，接入 MySQL 后由 MyBatis Plus 自动实现
 * @author 开发人员
 * @date 2026-06-10
 */
public interface UserMapper extends BaseMapper<User> {
}
