-- 异步生成仅保存状态与最终简历。原始用户材料只驻留本次工作进程内。
CREATE TABLE rl_smart_resume_job (
  id CHAR(36) NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  usage_day DATE NOT NULL,
  status VARCHAR(16) NOT NULL,
  resume_json MEDIUMTEXT NULL,
  message VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_smart_resume_user_status (user_id, status, created_at),
  KEY idx_smart_resume_user_recent (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
