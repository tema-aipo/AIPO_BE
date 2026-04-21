CREATE TABLE IF NOT EXISTS app_user (
    user_id BIGINT GENERATED ALWAYS AS IDENTITY,
    login_id VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_name VARCHAR(30) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    user_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP NULL,
    password_updated_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_app_user_login_id UNIQUE (login_id),
    CONSTRAINT uk_app_user_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS user_investment_type (
    investment_type_id BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    investment_type VARCHAR(30) NOT NULL,
    score INT NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    source_type VARCHAR(20) NOT NULL DEFAULT 'TEST',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (investment_type_id),
    CONSTRAINT fk_user_investment_type_user
        FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_investment_current
ON user_investment_type(user_id)
WHERE is_current = TRUE;

CREATE TABLE IF NOT EXISTS user_refresh_token (
    refresh_token_id BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    refresh_token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (refresh_token_id),
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ipo_stock (
    stock_id BIGINT GENERATED ALWAYS AS IDENTITY,
    stock_name VARCHAR(100) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    stock_code VARCHAR(20),
    market_type VARCHAR(20),
    one_line_description VARCHAR(255),
    confirmed_offer_price NUMERIC(15,2),
    subscription_start_date DATE,
    subscription_end_date DATE,
    listing_date DATE,
    recent_growth_score INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (stock_id)
);

CREATE TABLE IF NOT EXISTS ipo_lead_manager (
    lead_manager_id BIGINT GENERATED ALWAYS AS IDENTITY,
    stock_id BIGINT NOT NULL,
    manager_name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (lead_manager_id),
    CONSTRAINT uk_ipo_lead_manager_stock_order UNIQUE (stock_id, display_order),
    CONSTRAINT fk_ipo_lead_manager_stock
        FOREIGN KEY (stock_id) REFERENCES ipo_stock(stock_id)
);

CREATE TABLE IF NOT EXISTS ipo_attraction_score (
    score_id BIGINT GENERATED ALWAYS AS IDENTITY,
    stock_id BIGINT NOT NULL,
    total_score INT NOT NULL,
    financial_score INT,
    demand_score INT,
    market_score INT,
    score_comment TEXT,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (score_id),
    CONSTRAINT fk_ipo_attraction_score_stock
        FOREIGN KEY (stock_id) REFERENCES ipo_stock(stock_id)
);

CREATE TABLE IF NOT EXISTS ipo_attraction_reason (
    reason_id BIGINT GENERATED ALWAYS AS IDENTITY,
    stock_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    display_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reason_id),
    CONSTRAINT uk_ipo_attraction_reason_stock_order UNIQUE (stock_id, display_order),
    CONSTRAINT fk_ipo_attraction_reason_stock
        FOREIGN KEY (stock_id) REFERENCES ipo_stock(stock_id)
);

CREATE TABLE IF NOT EXISTS ipo_demand_forecast (
    forecast_id BIGINT GENERATED ALWAYS AS IDENTITY,
    stock_id BIGINT NOT NULL,
    institutional_competition_rate NUMERIC(10,2),
    participating_institution_count INT,
    above_upper_price_competition_rate NUMERIC(10,2),
    above_upper_price_institution_count INT,
    lockup_competition_rate NUMERIC(10,2),
    lockup_institution_count INT,
    lockup_rate NUMERIC(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (forecast_id),
    CONSTRAINT uk_ipo_demand_forecast_stock UNIQUE (stock_id),
    CONSTRAINT fk_ipo_demand_forecast_stock
        FOREIGN KEY (stock_id) REFERENCES ipo_stock(stock_id)
);

CREATE TABLE IF NOT EXISTS ipo_subscription_competition (
    competition_id BIGINT GENERATED ALWAYS AS IDENTITY,
    stock_id BIGINT NOT NULL,
    default_tab VARCHAR(20),
    equal_expected_allocation_quantity NUMERIC(10,2),
    equal_competition_rate NUMERIC(10,2),
    proportional_expected_allocation_quantity NUMERIC(10,2),
    proportional_competition_rate NUMERIC(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (competition_id),
    CONSTRAINT uk_ipo_subscription_competition_stock UNIQUE (stock_id),
    CONSTRAINT fk_ipo_subscription_competition_stock
        FOREIGN KEY (stock_id) REFERENCES ipo_stock(stock_id)
);

CREATE TABLE IF NOT EXISTS ipo_schedule (
    schedule_id BIGINT GENERATED ALWAYS AS IDENTITY,
    stock_id BIGINT NOT NULL,
    schedule_type VARCHAR(30) NOT NULL,
    schedule_date DATE NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (schedule_id),
    CONSTRAINT uk_ipo_schedule_stock_type UNIQUE (stock_id, schedule_type),
    CONSTRAINT fk_ipo_schedule_stock
        FOREIGN KEY (stock_id) REFERENCES ipo_stock(stock_id)
);

CREATE TABLE IF NOT EXISTS ipo_deposit_info (
    deposit_info_id BIGINT GENERATED ALWAYS AS IDENTITY,
    stock_id BIGINT NOT NULL,
    securities_company_name VARCHAR(100) NOT NULL,
    amount_for_ten_shares NUMERIC(15,2),
    display_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (deposit_info_id),
    CONSTRAINT uk_ipo_deposit_info_stock_company UNIQUE (stock_id, securities_company_name),
    CONSTRAINT uk_ipo_deposit_info_stock_order UNIQUE (stock_id, display_order),
    CONSTRAINT fk_ipo_deposit_info_stock
        FOREIGN KEY (stock_id) REFERENCES ipo_stock(stock_id)
);

CREATE TABLE IF NOT EXISTS ipo_offering_info (
    offering_info_id BIGINT GENERATED ALWAYS AS IDENTITY,
    stock_id BIGINT NOT NULL,
    market_cap NUMERIC(20,2),
    equal_allocation_ratio NUMERIC(5,2),
    circulating_ratio NUMERIC(5,2),
    old_share_sale_ratio NUMERIC(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (offering_info_id),
    CONSTRAINT uk_ipo_offering_info_stock UNIQUE (stock_id),
    CONSTRAINT fk_ipo_offering_info_stock
        FOREIGN KEY (stock_id) REFERENCES ipo_stock(stock_id)
);

CREATE TABLE IF NOT EXISTS user_favorite_stock (
    favorite_id BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    display_order INT NULL,
    alert_priority INT NULL,
    alert_yn CHAR(1) NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (favorite_id),
    CONSTRAINT uk_user_favorite_stock UNIQUE (user_id, stock_id),
    CONSTRAINT fk_user_favorite_stock_user
        FOREIGN KEY (user_id) REFERENCES app_user(user_id),
    CONSTRAINT fk_user_favorite_stock_stock
        FOREIGN KEY (stock_id) REFERENCES ipo_stock(stock_id)
);

CREATE TABLE IF NOT EXISTS ipo_view_log (
    view_log_id BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT,
    stock_id BIGINT NOT NULL,
    viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(30),
    PRIMARY KEY (view_log_id),
    CONSTRAINT fk_ipo_view_log_user
        FOREIGN KEY (user_id) REFERENCES app_user(user_id),
    CONSTRAINT fk_ipo_view_log_stock
        FOREIGN KEY (stock_id) REFERENCES ipo_stock(stock_id)
);

DELETE FROM ipo_view_log;
DELETE FROM user_favorite_stock;
DELETE FROM ipo_deposit_info;
DELETE FROM ipo_schedule;
DELETE FROM ipo_subscription_competition;
DELETE FROM ipo_demand_forecast;
DELETE FROM ipo_attraction_reason;
DELETE FROM ipo_attraction_score;
DELETE FROM ipo_lead_manager;
DELETE FROM ipo_offering_info;
DELETE FROM user_refresh_token;
DELETE FROM user_investment_type;
DELETE FROM ipo_stock;
DELETE FROM app_user;

INSERT INTO app_user (
    user_id, login_id, password_hash, user_name, email, role, user_status,
    created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
    (
        1, 'demo-user', '$2a$10$5lbUnT8wA2VerP5Zc4Yffe8bYnSYM5jVhU4dgjYZBU46F4xVvtPbi',
        '데모사용자', 'demo-user@aipo.test', 'USER', 'ACTIVE',
        '2026-04-01 09:00:00', '2026-04-21 09:00:00'
    ),
    (
        2, 'watcher-user', '$2a$10$5lbUnT8wA2VerP5Zc4Yffe8bYnSYM5jVhU4dgjYZBU46F4xVvtPbi',
        '관심사용자', 'watcher-user@aipo.test', 'USER', 'ACTIVE',
        '2026-04-02 09:00:00', '2026-04-21 09:00:00'
    );

INSERT INTO user_investment_type (
    investment_type_id, user_id, investment_type, score, is_current, source_type, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
    (1, 1, 'BALANCED', 67, TRUE, 'TEST', '2026-04-01 09:10:00', '2026-04-01 09:10:00'),
    (2, 2, 'AGGRESSIVE', 82, TRUE, 'TEST', '2026-04-02 09:10:00', '2026-04-02 09:10:00');

INSERT INTO ipo_stock (
    stock_id, stock_name, company_name, stock_code, market_type, one_line_description,
    confirmed_offer_price, subscription_start_date, subscription_end_date, listing_date,
    recent_growth_score, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
    (
        101, '에이포테크', '에이포테크', '101001', 'KOSDAQ', 'RAG 기반 금융 AI 솔루션 기업',
        15000.00, '2026-04-28', '2026-04-29', '2026-05-08',
        78, '2026-04-01 08:00:00', '2026-04-21 08:00:00'
    ),
    (
        102, '비전바이오', '비전바이오', '102002', 'KOSDAQ', '차세대 진단 플랫폼 바이오 기업',
        22000.00, '2026-05-06', '2026-05-07', '2026-05-16',
        72, '2026-04-01 08:05:00', '2026-04-21 08:05:00'
    ),
    (
        103, '클라우드페이', '클라우드페이', '103003', 'KOSPI', '클라우드 결제 인프라 SaaS 기업',
        18500.00, '2026-05-12', '2026-05-13', '2026-05-22',
        81, '2026-04-01 08:10:00', '2026-04-21 08:10:00'
    );

INSERT INTO ipo_lead_manager (
    lead_manager_id, stock_id, manager_name, display_order, created_at
) OVERRIDING SYSTEM VALUE VALUES
    (1001, 101, '미래투자증권', 1, '2026-04-01 09:20:00'),
    (1002, 101, '한국투자증권', 2, '2026-04-01 09:21:00'),
    (1003, 102, 'NH투자증권', 1, '2026-04-01 09:22:00'),
    (1004, 102, '신한투자증권', 2, '2026-04-01 09:23:00'),
    (1005, 103, 'KB증권', 1, '2026-04-01 09:24:00'),
    (1006, 103, '삼성증권', 2, '2026-04-01 09:25:00');

INSERT INTO ipo_attraction_score (
    score_id, stock_id, total_score, financial_score, demand_score, market_score, score_comment, calculated_at
) OVERRIDING SYSTEM VALUE VALUES
    (2001, 101, 82, 80, 83, 81, '초기 산출 점수', '2026-04-10 09:00:00'),
    (2002, 101, 88, 86, 90, 88, '수요예측 반영 최신 점수', '2026-04-20 18:00:00'),
    (2003, 102, 76, 74, 78, 75, '기관 수요예측 반영 점수', '2026-04-22 18:00:00'),
    (2004, 103, 74, 72, 75, 73, '1차 계산 점수', '2026-04-15 18:00:00'),
    (2005, 103, 79, 77, 81, 79, '시장 반응 반영 최신 점수', '2026-04-27 18:00:00');

INSERT INTO ipo_attraction_reason (
    reason_id, stock_id, title, description, display_order, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
    (3001, 101, '수요예측 강세', '기관 경쟁률과 상단 초과 비중이 높아 긍정적입니다.', 1, '2026-04-20 18:10:00', '2026-04-20 18:10:00'),
    (3002, 101, '유통 물량 부담 제한', '상장 직후 유통 가능 물량 비율이 낮은 편입니다.', 2, '2026-04-20 18:11:00', '2026-04-20 18:11:00'),
    (3003, 102, '진단 시장 성장성', '주요 사업 시장 성장률이 높아 성장 기대가 있습니다.', 1, '2026-04-22 18:10:00', '2026-04-22 18:10:00'),
    (3004, 102, '기술 특례 관심', '기술 기반 상장 케이스로 관심도가 높습니다.', 2, '2026-04-22 18:11:00', '2026-04-22 18:11:00'),
    (3005, 103, '결제 SaaS 반복 매출', '구독형 매출 구조가 안정적입니다.', 1, '2026-04-27 18:10:00', '2026-04-27 18:10:00'),
    (3006, 103, '클라우드 확장성', '대형 고객사 확장이 기대됩니다.', 2, '2026-04-27 18:11:00', '2026-04-27 18:11:00');

INSERT INTO ipo_demand_forecast (
    forecast_id, stock_id, institutional_competition_rate, participating_institution_count,
    above_upper_price_competition_rate, above_upper_price_institution_count,
    lockup_competition_rate, lockup_institution_count, lockup_rate,
    created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
    (4001, 101, 1234.56, 2450, 92.10, 2300, 80.50, 1800, 15.30, '2026-04-20 18:20:00', '2026-04-20 18:20:00'),
    (4002, 102, 845.32, 1875, 76.40, 1602, 61.20, 1204, 11.80, '2026-04-25 18:20:00', '2026-04-25 18:20:00'),
    (4003, 103, 978.44, 2102, 84.25, 1903, 70.10, 1420, 13.60, '2026-04-29 18:20:00', '2026-04-29 18:20:00');

INSERT INTO ipo_subscription_competition (
    competition_id, stock_id, default_tab,
    equal_expected_allocation_quantity, equal_competition_rate,
    proportional_expected_allocation_quantity, proportional_competition_rate,
    created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
    (5001, 101, 'EQUAL', 2.50, 150.25, 1.25, 320.75, '2026-04-21 10:00:00', '2026-04-21 10:00:00'),
    (5002, 102, 'PROPORTIONAL', 1.80, 112.40, 0.92, 278.10, '2026-04-26 10:00:00', '2026-04-26 10:00:00'),
    (5003, 103, 'EQUAL', 2.10, 134.80, 1.05, 301.45, '2026-04-29 10:00:00', '2026-04-29 10:00:00');

INSERT INTO ipo_schedule (
    schedule_id, stock_id, schedule_type, schedule_date, note, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
    (6001, 101, 'DEMAND_FORECAST_START', '2026-04-20', '에이포테크 수요예측 시작', '2026-04-01 10:00:00', '2026-04-01 10:00:00'),
    (6002, 101, 'DEMAND_FORECAST_END',   '2026-04-21', '에이포테크 수요예측 종료', '2026-04-01 10:01:00', '2026-04-01 10:01:00'),
    (6003, 101, 'REFUND',                '2026-05-02', '에이포테크 환불일',      '2026-04-01 10:02:00', '2026-04-01 10:02:00'),
    (6004, 101, 'LISTING',               '2026-05-08', '에이포테크 상장일',      '2026-04-01 10:03:00', '2026-04-01 10:03:00'),
    (6005, 102, 'DEMAND_FORECAST_START', '2026-04-24', '비전바이오 수요예측 시작', '2026-04-01 10:04:00', '2026-04-01 10:04:00'),
    (6006, 102, 'DEMAND_FORECAST_END',   '2026-04-25', '비전바이오 수요예측 종료', '2026-04-01 10:05:00', '2026-04-01 10:05:00'),
    (6007, 102, 'REFUND',                '2026-05-09', '비전바이오 환불일',       '2026-04-01 10:06:00', '2026-04-01 10:06:00'),
    (6008, 102, 'LISTING',               '2026-05-16', '비전바이오 상장일',       '2026-04-01 10:07:00', '2026-04-01 10:07:00'),
    (6009, 103, 'DEMAND_FORECAST_START', '2026-04-28', '클라우드페이 수요예측 시작', '2026-04-01 10:08:00', '2026-04-01 10:08:00'),
    (6010, 103, 'DEMAND_FORECAST_END',   '2026-04-29', '클라우드페이 수요예측 종료', '2026-04-01 10:09:00', '2026-04-01 10:09:00'),
    (6011, 103, 'REFUND',                '2026-05-15', '클라우드페이 환불일',       '2026-04-01 10:10:00', '2026-04-01 10:10:00'),
    (6012, 103, 'LISTING',               '2026-05-22', '클라우드페이 상장일',       '2026-04-01 10:11:00', '2026-04-01 10:11:00');

INSERT INTO ipo_deposit_info (
    deposit_info_id, stock_id, securities_company_name, amount_for_ten_shares,
    display_order, created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
    (7001, 101, '미래투자증권', 75000.00, 1, '2026-04-21 10:10:00', '2026-04-21 10:10:00'),
    (7002, 101, '한국투자증권', 80000.00, 2, '2026-04-21 10:11:00', '2026-04-21 10:11:00'),
    (7003, 102, 'NH투자증권',   110000.00, 1, '2026-04-26 10:10:00', '2026-04-26 10:10:00'),
    (7004, 102, '신한투자증권', 115000.00, 2, '2026-04-26 10:11:00', '2026-04-26 10:11:00'),
    (7005, 103, 'KB증권',        92500.00, 1, '2026-04-29 10:10:00', '2026-04-29 10:10:00'),
    (7006, 103, '삼성증권',      94000.00, 2, '2026-04-29 10:11:00', '2026-04-29 10:11:00');

INSERT INTO ipo_offering_info (
    offering_info_id, stock_id, market_cap, equal_allocation_ratio, circulating_ratio, old_share_sale_ratio,
    created_at, updated_at
) OVERRIDING SYSTEM VALUE VALUES
    (8001, 101, 250000000000.00, 50.00, 18.40, 5.10, '2026-04-21 10:20:00', '2026-04-21 10:20:00'),
    (8002, 102, 340000000000.00, 48.00, 22.10, 7.20, '2026-04-26 10:20:00', '2026-04-26 10:20:00'),
    (8003, 103, 410000000000.00, 50.00, 19.80, 4.50, '2026-04-29 10:20:00', '2026-04-29 10:20:00');

INSERT INTO user_favorite_stock (
    favorite_id, user_id, stock_id, display_order, alert_priority, alert_yn, created_at
) OVERRIDING SYSTEM VALUE VALUES
    (9001, 1, 101, 1, 1, 'Y', '2026-04-21 11:00:00'),
    (9002, 1, 102, 2, 2, 'Y', '2026-04-21 11:05:00'),
    (9003, 2, 103, 1, 1, 'Y', '2026-04-21 11:10:00');
