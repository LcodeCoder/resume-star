-- ============================================================================
-- resume-lcode 生产库基线 schema (MySQL 8, InnoDB, utf8mb4)
-- 由 SQLite 版 PersistenceStore 的表结构移植而来：
--   * 每类数据一张表，data 列（LONGTEXT）存整对象 JSON 快照，另冗余少量可查询列
--   * 日志/流水类表主键 AUTO_INCREMENT，其余主键由应用层显式指定（沿用原 id 生成器）
-- 字符集统一 utf8mb4，索引前缀列长度控制在 190 以内以兼容 utf8mb4 索引上限
-- ============================================================================

-- 用户
CREATE TABLE rl_user (
  id        BIGINT       NOT NULL PRIMARY KEY,
  username  VARCHAR(64),
  nickname  VARCHAR(128),
  email     VARCHAR(190),
  vip_level VARCHAR(32),
  banned    TINYINT      DEFAULT 0,
  password  VARCHAR(100),
  token     VARCHAR(128),
  data      LONGTEXT,
  KEY idx_user_username (username),
  KEY idx_user_token (token),
  KEY idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 管理员
CREATE TABLE rl_admin (
  id       BIGINT NOT NULL PRIMARY KEY,
  username VARCHAR(64),
  data     LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 会员套餐
CREATE TABLE rl_member_package (
  id         BIGINT NOT NULL PRIMARY KEY,
  name       VARCHAR(128),
  level_code VARCHAR(64),
  price      DECIMAL(10,2),
  data       LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 会员兑换码
CREATE TABLE rl_redeem_code (
  id           BIGINT NOT NULL PRIMARY KEY,
  code         VARCHAR(64),
  package_id   BIGINT,
  package_name VARCHAR(128),
  price        DECIMAL(10,2),
  used         TINYINT DEFAULT 0,
  data         LONGTEXT,
  KEY idx_redeem_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 额度套餐
CREATE TABLE rl_quota_package (
  id           BIGINT NOT NULL PRIMARY KEY,
  name         VARCHAR(128),
  ai_count     INT,
  export_count INT,
  price        DECIMAL(10,2),
  data         LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 额度兑换码
CREATE TABLE rl_quota_code (
  id           BIGINT NOT NULL PRIMARY KEY,
  code         VARCHAR(64),
  package_id   BIGINT,
  package_name VARCHAR(128),
  price        DECIMAL(10,2),
  used         TINYINT DEFAULT 0,
  data         LONGTEXT,
  KEY idx_quota_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 简历（owner_id 冗余便于按归属分页）
CREATE TABLE rl_resume (
  id       BIGINT NOT NULL PRIMARY KEY,
  title    VARCHAR(255),
  owner_id BIGINT,
  data     LONGTEXT,
  KEY idx_resume_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 模板分类
CREATE TABLE rl_template_category (
  id   BIGINT NOT NULL PRIMARY KEY,
  name VARCHAR(128),
  code VARCHAR(64),
  data LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 简历模板
CREATE TABLE rl_resume_template (
  id           BIGINT NOT NULL PRIMARY KEY,
  name         VARCHAR(128),
  vip_template TINYINT DEFAULT 0,
  data         LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 后台审计日志（纯 DB，自增）
CREATE TABLE rl_audit_log (
  id       BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  operator VARCHAR(64),
  action   VARCHAR(128),
  data     LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 简历历史版本
CREATE TABLE rl_resume_version (
  id        BIGINT NOT NULL PRIMARY KEY,
  resume_id BIGINT,
  data      LONGTEXT,
  KEY idx_resume_version_rid (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 简历分享
CREATE TABLE rl_resume_share (
  token     VARCHAR(64) NOT NULL PRIMARY KEY,
  resume_id BIGINT,
  data      LONGTEXT,
  KEY idx_resume_share_rid (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户操作记录（纯 DB，自增）
CREATE TABLE rl_user_activity (
  id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  type    VARCHAR(64),
  data    LONGTEXT,
  KEY idx_user_activity_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 充值余额流水（纯 DB，自增）
CREATE TABLE rl_quota_ledger (
  id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  type    VARCHAR(64),
  data    LONGTEXT,
  KEY idx_quota_ledger_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户收藏模板（联合主键）
CREATE TABLE rl_user_favorite (
  user_id     BIGINT NOT NULL,
  template_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 会员组件分组 / 单 key
CREATE TABLE rl_vip_component_group (
  group_key VARCHAR(128) NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rl_vip_component_key (
  component_key VARCHAR(190) NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 站内公告
CREATE TABLE rl_announcement (
  id      BIGINT NOT NULL PRIMARY KEY,
  title   VARCHAR(255),
  enabled TINYINT DEFAULT 0,
  data    LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 社区案例
CREATE TABLE rl_community_case (
  id        BIGINT NOT NULL PRIMARY KEY,
  title     VARCHAR(255),
  author_id BIGINT,
  featured  TINYINT DEFAULT 0,
  data      LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 社区文章
CREATE TABLE rl_community_article (
  id        BIGINT NOT NULL PRIMARY KEY,
  title     VARCHAR(255),
  category  VARCHAR(64),
  published TINYINT DEFAULT 0,
  data      LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 社区点赞（k = userId:targetType:targetId）
CREATE TABLE rl_community_like (
  k VARCHAR(190) NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 系统配置（单行 id=1）
CREATE TABLE rl_system_config (
  id   BIGINT NOT NULL PRIMARY KEY,
  data LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI 配置
CREATE TABLE rl_ai_config (
  id   BIGINT NOT NULL PRIMARY KEY,
  data LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通用 KV（计数器等）
CREATE TABLE rl_kv (
  k VARCHAR(64) NOT NULL PRIMARY KEY,
  v TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 每日额度使用计数（k = yyyy-MM-dd:userId）
CREATE TABLE rl_daily_usage (
  k      VARCHAR(64) NOT NULL PRIMARY KEY,
  ai     INT DEFAULT 0,
  export INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 面试分类
CREATE TABLE rl_interview_category (
  id      BIGINT NOT NULL PRIMARY KEY,
  name    VARCHAR(128),
  code    VARCHAR(64),
  enabled TINYINT DEFAULT 0,
  data    LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 面试记录
CREATE TABLE rl_interview_record (
  id          BIGINT NOT NULL PRIMARY KEY,
  user_id     BIGINT,
  resume_id   BIGINT,
  total_score INT,
  data        LONGTEXT,
  KEY idx_interview_record_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI 调用日志（纯 DB，自增）
CREATE TABLE rl_ai_call_log (
  id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT,
  username      VARCHAR(64),
  feature_type  VARCHAR(64),
  vip_level     VARCHAR(32),
  quota_cost    INT,
  status        VARCHAR(32),
  error_message TEXT,
  create_time   VARCHAR(32),
  KEY idx_ai_log_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 导出记录（纯 DB，自增）
CREATE TABLE rl_export_record (
  id              BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT,
  username        VARCHAR(64),
  resume_id       BIGINT,
  resume_title    VARCHAR(255),
  export_type     VARCHAR(32),
  high_definition TINYINT,
  watermark       TINYINT,
  create_time     VARCHAR(32),
  KEY idx_export_rec_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
