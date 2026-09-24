-- 智能简历独立按自然日计数；与普通 AI 调用额度分开。
CREATE TABLE rl_smart_resume_usage (
  user_id BIGINT NOT NULL,
  usage_day DATE NOT NULL,
  used INT NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id, usage_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
