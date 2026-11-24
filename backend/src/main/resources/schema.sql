CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id       VARCHAR(64)  NOT NULL UNIQUE,
    --   brand  默认值为xianting
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
    milvus_collection     VARCHAR(255),
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_milvus_collection (milvus_collection)
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

--  新建t_prompt表，每个智能体都有专属的prompt(可更新每个智能体的prompt版本)
CREATE TABLE IF NOT EXISTS `t_prompt`  (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `public_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                             `agent_id` bigint NOT NULL,
                             `version` int NOT NULL,
                             `version_tag` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                             `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                             `description` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                             `prompt_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SYSTEM',
                             `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                             `variables` json NULL,
                             `model_config` json NULL,
                             `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT',
                             `parent_prompt_id` bigint NULL DEFAULT NULL,
                             `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                             `updated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                             `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             `published_at` timestamp NULL DEFAULT NULL,
                             `archived_at` timestamp NULL DEFAULT NULL,
                             `active_agent_id` bigint GENERATED ALWAYS AS ((case when (`status` = _utf8mb4'ACTIVE') then `agent_id` else NULL end)) VIRTUAL NULL,
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `uk_public_id`(`public_id` ASC) USING BTREE,
                             UNIQUE INDEX `uk_agent_version`(`agent_id` ASC, `version` ASC) USING BTREE,
                             UNIQUE INDEX `uk_agent_active`(`active_agent_id` ASC) USING BTREE,
                             INDEX `idx_agent_status`(`agent_id` ASC, `status` ASC) USING BTREE,
                             INDEX `idx_agent_created`(`agent_id` ASC, `created_at` ASC) USING BTREE,
                             INDEX `idx_parent`(`parent_prompt_id` ASC) USING BTREE,
                             CONSTRAINT `fk_prompt_agent` FOREIGN KEY (`agent_id`) REFERENCES `t_agent` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                             CONSTRAINT `fk_prompt_parent` FOREIGN KEY (`parent_prompt_id`) REFERENCES `t_prompt` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1030 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Agent Prompt 版本管理表' ROW_FORMAT = Dynamic;
