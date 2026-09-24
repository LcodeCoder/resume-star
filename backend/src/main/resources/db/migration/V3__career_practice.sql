-- 练习记录独立于工作区保存，防止面试页与实验室同时写入时互相覆盖。
CREATE TABLE rl_career_practice (
  user_id BIGINT NOT NULL,
  practice_id VARCHAR(64) NOT NULL,
  payload TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, practice_id),
  INDEX idx_career_practice_recent (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
