-- =============================================
-- 로그인 이력 테이블
-- 사용자(USER, ADMIN) 로그인 발생 시 기록
-- =============================================
CREATE TABLE user_login_log (
    log_id          BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id         BIGINT NOT NULL,
    login_id        VARCHAR(50) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    logged_in_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    CONSTRAINT fk_login_log_user
        FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_login_log_user_id ON user_login_log(user_id);
CREATE INDEX idx_login_log_logged_in_at ON user_login_log(logged_in_at DESC);
