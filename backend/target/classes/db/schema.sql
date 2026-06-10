-- resume-lcode 数据库建表脚本
-- 功能：定义用户、简历、模板、AI、导出、会员预留等核心表结构
-- 说明：当前应用默认以内存数据启动；接入 MySQL 后执行本脚本并启用数据源配置

CREATE TABLE IF NOT EXISTS rl_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  username VARCHAR(64) NOT NULL COMMENT '登录账号',
  nickname VARCHAR(64) COMMENT '用户昵称',
  phone VARCHAR(32) COMMENT '手机号',
  email VARCHAR(128) COMMENT '邮箱',
  avatar VARCHAR(512) COMMENT '头像地址',
  vip_level VARCHAR(32) DEFAULT 'FREE' COMMENT '会员等级编码【会员体系扩展字段】',
  vip_expire_time DATETIME NULL COMMENT '会员到期时间【会员体系扩展字段】',
  daily_ai_used INT DEFAULT 0 COMMENT '当日 AI 已使用次数【会员体系扩展字段】',
  total_ai_quota INT DEFAULT 5 COMMENT '总 AI 额度【会员体系扩展字段】',
  daily_export_used INT DEFAULT 0 COMMENT '当日导出已使用次数【会员体系扩展字段】',
  total_export_quota INT DEFAULT 3 COMMENT '总导出额度【会员体系扩展字段】',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='用户信息表';

CREATE TABLE IF NOT EXISTS rl_resume (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  title VARCHAR(128) NOT NULL COMMENT '简历标题',
  target_job VARCHAR(128) COMMENT '目标岗位',
  content_json LONGTEXT COMMENT '简历组件 JSON 数据',
  template_id BIGINT COMMENT '来源模板 ID',
  draft TINYINT DEFAULT 1 COMMENT '是否草稿：1-草稿 0-正式',
  vip_component_limit INT DEFAULT 0 COMMENT '高级组件数量上限【会员体系扩展字段】',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='简历数据表';

CREATE TABLE IF NOT EXISTS rl_template_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  name VARCHAR(64) NOT NULL COMMENT '分类名称',
  code VARCHAR(64) NOT NULL COMMENT '分类编码',
  sort INT DEFAULT 0 COMMENT '排序值'
) COMMENT='模板分类表';

CREATE TABLE IF NOT EXISTS rl_resume_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  category_id BIGINT COMMENT '模板分类 ID',
  name VARCHAR(128) NOT NULL COMMENT '模板名称',
  industry VARCHAR(64) COMMENT '行业名称',
  style_tag VARCHAR(64) COMMENT '风格标签',
  cover_url VARCHAR(512) COMMENT '模板封面图',
  schema_json LONGTEXT COMMENT '模板组件 JSON',
  vip_template TINYINT DEFAULT 0 COMMENT '是否会员专属：1-是 0-否【会员体系扩展字段】',
  favorite_count INT DEFAULT 0 COMMENT '收藏数',
  view_count INT DEFAULT 0 COMMENT '浏览数',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='简历模板表';

CREATE TABLE IF NOT EXISTS rl_ai_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  name VARCHAR(64) COMMENT '配置名称',
  endpoint VARCHAR(512) COMMENT 'AI 接口地址，仅后端保存',
  api_key VARCHAR(512) COMMENT 'AI API Key，仅后端保存',
  model VARCHAR(128) COMMENT '模型名称',
  timeout_millis INT DEFAULT 15000 COMMENT '超时时间毫秒',
  enabled TINYINT DEFAULT 1 COMMENT '是否启用',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='AI 接口配置表';

CREATE TABLE IF NOT EXISTS rl_ai_call_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  user_id BIGINT COMMENT '用户 ID',
  feature_type VARCHAR(64) COMMENT 'AI 功能类型',
  vip_level VARCHAR(32) COMMENT '用户会员等级【会员体系扩展字段】',
  quota_cost INT DEFAULT 1 COMMENT '本次消耗额度【会员体系扩展字段】',
  status VARCHAR(32) COMMENT '调用状态：SUCCESS/FAIL',
  error_message VARCHAR(512) COMMENT '错误信息',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='AI 调用日志表';

CREATE TABLE IF NOT EXISTS rl_export_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  user_id BIGINT COMMENT '用户 ID',
  resume_id BIGINT COMMENT '简历 ID',
  export_type VARCHAR(32) COMMENT '导出类型：PDF/IMAGE',
  high_definition TINYINT DEFAULT 0 COMMENT '是否高清导出【会员体系扩展字段】',
  watermark TINYINT DEFAULT 0 COMMENT '是否带水印【会员体系扩展字段】',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='导出记录表';

CREATE TABLE IF NOT EXISTS rl_member_level (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  name VARCHAR(64) NOT NULL COMMENT '等级名称',
  code VARCHAR(32) NOT NULL COMMENT '等级编码：FREE/BASIC/PRO/ENTERPRISE',
  daily_ai_quota INT DEFAULT 5 COMMENT '每日 AI 额度【会员体系扩展字段】',
  daily_export_quota INT DEFAULT 3 COMMENT '每日导出额度【会员体系扩展字段】',
  allow_vip_template TINYINT DEFAULT 0 COMMENT '是否允许会员模板【会员体系扩展字段】',
  sort INT DEFAULT 0 COMMENT '排序值'
) COMMENT='会员等级表【预留扩展】';

CREATE TABLE IF NOT EXISTS rl_member_package (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  name VARCHAR(64) NOT NULL COMMENT '套餐名称',
  level_code VARCHAR(32) COMMENT '对应会员等级编码',
  price DECIMAL(10,2) DEFAULT 0 COMMENT '套餐价格',
  valid_days INT DEFAULT 30 COMMENT '有效天数',
  benefits TEXT COMMENT '权益说明',
  enabled TINYINT DEFAULT 1 COMMENT '是否启用'
) COMMENT='会员套餐表【预留扩展】';

CREATE TABLE IF NOT EXISTS rl_payment_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  package_id BIGINT NOT NULL COMMENT '会员套餐 ID',
  package_name VARCHAR(64) COMMENT '套餐名称快照',
  level_code VARCHAR(32) COMMENT '会员等级快照',
  amount DECIMAL(10,2) DEFAULT 0 COMMENT '订单金额',
  pay_channel VARCHAR(32) DEFAULT 'MOCK' COMMENT '支付方式：MOCK/ALIPAY/WECHAT',
  status VARCHAR(32) DEFAULT 'PENDING' COMMENT '订单状态：PENDING/PAID/CLOSED',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  paid_time DATETIME NULL COMMENT '支付完成时间'
) COMMENT='会员支付订单表【当前支持模拟支付】';

CREATE TABLE IF NOT EXISTS rl_feature_quota_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
  user_id BIGINT COMMENT '用户 ID',
  feature_type VARCHAR(64) COMMENT '功能类型：AI/EXPORT/TEMPLATE',
  cost INT DEFAULT 1 COMMENT '消耗额度【会员体系扩展字段】',
  business_id BIGINT COMMENT '关联业务 ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='功能额度记录表【预留扩展】';
