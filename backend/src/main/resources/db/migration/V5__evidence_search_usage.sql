-- 证据混合检索独立的会员自然日额度，不占用智能简历/普通 AI 额度。
CREATE TABLE rl_evidence_search_usage (
  user_id BIGINT NOT NULL,
  usage_day DATE NOT NULL,
  used INT NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id, usage_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
