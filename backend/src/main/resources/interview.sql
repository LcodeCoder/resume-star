-- 面试分类表
CREATE TABLE IF NOT EXISTS `rl_interview_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `code` VARCHAR(50) NOT NULL COMMENT '分类编码',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
  `sort` INT DEFAULT 0 COMMENT '展示排序',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试分类表';

-- 面试记录表
CREATE TABLE IF NOT EXISTS `rl_interview_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `resume_id` BIGINT DEFAULT NULL COMMENT '简历ID',
  `resume_title` VARCHAR(100) DEFAULT NULL COMMENT '简历标题',
  `category_code` VARCHAR(50) DEFAULT NULL COMMENT '面试分类编码',
  `start_time` DATETIME DEFAULT NULL COMMENT '面试开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '面试结束时间',
  `duration_seconds` INT DEFAULT 0 COMMENT '面试总时长（秒）',
  `total_score` INT DEFAULT 0 COMMENT '综合得分',
  `answered_count` INT DEFAULT 0 COMMENT '回答题目数',
  `qa_detail` TEXT COMMENT '问答详情JSON',
  `ability_distribution` TEXT COMMENT '能力分布JSON',
  `summary` TEXT COMMENT '总体评价',
  `encouragement` VARCHAR(500) DEFAULT NULL COMMENT '鼓励语',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试记录表';

-- 插入初始面试分类数据
INSERT INTO `rl_interview_category` (`name`, `code`, `description`, `sort`, `enabled`) VALUES
('Java后端开发', 'java_backend', '面向Java后端开发岗位的面试题目', 1, 1),
('前端开发', 'frontend', '面向前端开发岗位的面试题目', 2, 1),
('产品经理', 'product', '面向产品经理岗位的面试题目', 3, 1),
('UI/UX设计', 'design', '面向UI/UX设计岗位的面试题目', 4, 1),
('数据分析', 'data_analysis', '面向数据分析岗位的面试题目', 5, 1),
('通用面试', 'general', '适用于各类岗位的通用面试题目', 6, 1);
