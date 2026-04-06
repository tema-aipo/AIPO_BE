-- =============================================
-- 사용자 테이블
-- role: USER | ADMIN
-- user_status: ACTIVE | INACTIVE | WITHDRAWN | SUSPENDED
-- =============================================
CREATE TABLE app_user (
    user_id             BIGINT GENERATED ALWAYS AS IDENTITY,
    login_id            VARCHAR(50) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    user_name           VARCHAR(30) NOT NULL,
    email               VARCHAR(100),
    role                VARCHAR(20) NOT NULL DEFAULT 'USER',
    user_status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at       TIMESTAMP NULL,
    password_updated_at TIMESTAMP NULL,
    deleted_at          TIMESTAMP NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_app_user_login_id UNIQUE (login_id),
    CONSTRAINT uk_app_user_email    UNIQUE (email)
);

-- =============================================
-- 투자 성향 테이블
-- 사용자별 투자 성향 테스트 결과 기록
-- is_current=TRUE 인 행이 현재 유효한 성향
-- source_type: TEST | ADMIN
-- =============================================
CREATE TABLE user_investment_type (
    investment_type_id  BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id             BIGINT NOT NULL,
    investment_type     VARCHAR(30) NOT NULL,
    score               INT NOT NULL,
    is_current          BOOLEAN NOT NULL DEFAULT TRUE,
    source_type         VARCHAR(20) NOT NULL DEFAULT 'TEST',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (investment_type_id),
    CONSTRAINT fk_user_investment_type_user
        FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE
);

-- 사용자당 현재 성향은 1건만 허용
CREATE UNIQUE INDEX uk_user_investment_current
    ON user_investment_type(user_id)
    WHERE is_current = TRUE;

-- =============================================
-- Refresh Token 테이블
-- 사용자당 최신 토큰 1건만 유지 (기기별 미지원)
-- =============================================
CREATE TABLE user_refresh_token (
    refresh_token_id    BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id             BIGINT NOT NULL,
    refresh_token       VARCHAR(500) NOT NULL,
    expires_at          TIMESTAMP NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (refresh_token_id),
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE
);
