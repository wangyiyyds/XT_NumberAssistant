CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id       VARCHAR(64)  NOT NULL UNIQUE,
    brand           VARCHAR(32)  NOT NULL DEFAULT 'xianting',
    email           VARCHAR(128) NOT NULL UNIQUE,
    password        VARCHAR(256) NOT NULL,
    name            VARCHAR(64),
    avatar          VARCHAR(256),
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    disabled        BOOLEAN      NOT NULL DEFAULT FALSE,
    contract_read_at TIMESTAMP   NULL DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_balance (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id              BIGINT  NOT NULL UNIQUE,
    token_credits        BIGINT  NOT NULL DEFAULT 0,
    auto_refill_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
    expire_at            TIMESTAMP NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_department (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id   VARCHAR(64) NOT NULL UNIQUE,
    name        VARCHAR(64) NOT NULL,
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    brand_only  BOOLEAN     NOT NULL DEFAULT FALSE,
    sort_order  INT         NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_agent (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id             VARCHAR(64) NOT NULL UNIQUE,
    department_id         BIGINT,
    name                  VARCHAR(128) NOT NULL,
    description           VARCHAR(1024),
    avatar                VARCHAR(512),
    category              VARCHAR(64),
    is_preset             BOOLEAN NOT NULL DEFAULT TRUE,
    conversation_starters VARCHAR(2048),
    instructions          TEXT,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_conversation (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id     VARCHAR(64) NOT NULL UNIQUE,
    user_id       BIGINT      NOT NULL,
    agent_id      BIGINT      NOT NULL,
    title         VARCHAR(256),
    endpoint      VARCHAR(64) NOT NULL DEFAULT 'agents',
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_user (user_id, agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_message (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id         VARCHAR(64) NOT NULL UNIQUE,
    conversation_id   BIGINT      NOT NULL,
    parent_message_id VARCHAR(64),
    sender            VARCHAR(16) NOT NULL,
    text              TEXT,
    error             BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_message_conversation (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_inspiration_keyword (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    keyword          VARCHAR(128) NOT NULL,
    sort_order       INT          NOT NULL DEFAULT 0,
    cache_expires_at TIMESTAMP    NULL DEFAULT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_inspiration_video (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword_id    BIGINT       NOT NULL,
    author        VARCHAR(128),
    fans          VARCHAR(32),
    duration      VARCHAR(16),
    publish_date  VARCHAR(16),
    title         VARCHAR(1024),
    likes         VARCHAR(32),
    cover_url     VARCHAR(512),
    video_url     VARCHAR(512),
    low_fans_high_likes BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_video_keyword (keyword_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
