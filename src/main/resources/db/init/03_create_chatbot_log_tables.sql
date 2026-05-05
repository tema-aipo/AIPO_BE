-- =============================================
-- 챗봇 로그 테이블
-- 사용자와 챗봇 간의 대화 메시지를 기록
-- message_role: USER | ASSISTANT | SYSTEM
-- =============================================
CREATE TABLE chatbot_log (
    log_id          BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id         BIGINT,
    session_id      VARCHAR(100) NOT NULL,
    message_role    VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    token_count     INT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    CONSTRAINT fk_chatbot_log_user
        FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE SET NULL
);

CREATE INDEX idx_chatbot_log_user_id    ON chatbot_log(user_id);
CREATE INDEX idx_chatbot_log_session_id ON chatbot_log(session_id);
CREATE INDEX idx_chatbot_log_created_at ON chatbot_log(created_at DESC);
