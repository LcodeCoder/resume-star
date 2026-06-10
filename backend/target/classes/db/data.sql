-- resume-lcode 初始化数据脚本
-- 功能：提供最小可演示数据，便于生产接入 MySQL 后快速验证基础流程

INSERT INTO rl_user (id, username, nickname, vip_level, daily_ai_used, total_ai_quota, daily_export_used, total_export_quota)
VALUES (1, 'demo', '求职者 Demo', 'FREE', 0, 5, 0, 3);

INSERT INTO rl_template_category (id, name, code, sort) VALUES
(1, '互联网技术', 'tech', 1),
(2, '产品运营', 'product', 2),
(3, '应届校园', 'campus', 3),
(4, '设计创意', 'design', 4),
(5, '通用商务', 'business', 5);

-- 模板组件数据（schema_json）由应用层生成，此处保留空数组占位；cover 字段保留兼容，前端按组件数据渲染缩略图
INSERT INTO rl_resume_template (id, category_id, name, industry, style_tag, cover_url, schema_json, vip_template, favorite_count, view_count)
VALUES
(1, 1, '经典蓝·后端工程师', 'tech', '经典专业', '', '[]', 0, 128, 2048),
(2, 1, '深色商务·架构师', 'tech', '深色稳重', '', '[]', 1, 96, 1620),
(3, 1, '极简灰·开发通用', 'tech', '极简轻量', '', '[]', 0, 64, 980),
(4, 2, '紫罗兰·产品经理', 'product', '清爽商务', '', '[]', 1, 89, 1320),
(5, 2, '墨青·运营增长', 'product', '沉稳干练', '', '[]', 0, 73, 1105),
(6, 3, '清新绿·应届通用', 'campus', '清新明快', '', '[]', 0, 152, 2380),
(7, 3, '暖橙·校园求职', 'campus', '活力醒目', '', '[]', 0, 58, 860),
(8, 4, '玫红·视觉设计', 'design', '个性鲜明', '', '[]', 1, 67, 1024),
(9, 4, '靛蓝·交互设计', 'design', '理性克制', '', '[]', 0, 49, 720),
(10, 5, '藏青·市场管理', 'business', '大气商务', '', '[]', 0, 81, 1188),
(11, 5, '黑白·通用经典', 'business', '黑白经典', '', '[]', 0, 110, 1675);

INSERT INTO rl_member_level (id, name, code, daily_ai_quota, daily_export_quota, allow_vip_template, sort) VALUES
(1, '免费用户', 'FREE', 5, 3, 0, 1),
(2, '基础会员', 'BASIC', 20, 10, 1, 2),
(3, '专业会员', 'PRO', 100, 50, 1, 3),
(4, '企业会员', 'ENTERPRISE', 999, 999, 1, 4);

INSERT INTO rl_member_package (id, name, level_code, price, valid_days, benefits, enabled) VALUES
(1, '基础会员', 'BASIC', 19.90, 30, '每日 AI 20 次；高清导出预留；会员模板标识展示', 1),
(2, '专业会员', 'PRO', 49.90, 30, '每日 AI 100 次；高级组件预留；岗位适配优先级预留', 1),
(3, '企业会员', 'ENTERPRISE', 199.00, 365, '团队模板库预留；企业 API 配额预留；专属模型配置预留', 1);
