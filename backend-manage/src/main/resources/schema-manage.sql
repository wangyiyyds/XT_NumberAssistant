-- ============================================================================
-- 管理端结构脚本：与客户端 schema.sql 在同一库（video_assistant）共存，全部幂等，
-- 由 spring.sql.init 每次启动重跑。本文件是管理端相关结构的唯一事实源。
--
-- 结构变更记录（一旦改变 SQL 结构，必须在此登记）：
--   2026-11-24  初始版本：新建 t_admin（管理员）、t_plan（套餐）、t_plan_department（套餐-部门关系）
--   2026-11-24  t_user 增加 plan_id 列（BIGINT NULL）+ idx_user_plan 索引：用户所属套餐，
--               客户端 User 实体不映射该列，客户端读写不受影响
--   2026-11-24  新建 t_menu（管理端左侧导航菜单）
-- ============================================================================

CREATE TABLE IF NOT EXISTS t_admin (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id   VARCHAR(64)  NOT NULL UNIQUE,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(256) NOT NULL,
    name        VARCHAR(64),
    disabled    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_plan (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id   VARCHAR(64) NOT NULL UNIQUE,
    name        VARCHAR(64) NOT NULL,
    -- 套餐等级，数字越大功能越全；level 唯一，作为套餐排序与高低比较依据
    level       INT         NOT NULL,
    description VARCHAR(512),
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_plan_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 套餐-部门(左侧导航)多对多关系：决定每个套餐开放哪些部门给用户。
-- 不建外键：t_department 由客户端维护，避免两端启动顺序耦合；关系清理由 Service 层负责。
CREATE TABLE IF NOT EXISTS t_plan_department (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id       BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    UNIQUE KEY uk_plan_department (plan_id, department_id),
    INDEX idx_plan_department_dept (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 管理端左侧导航菜单：前端登录后经 /api/menus 读取渲染，可在「菜单管理」页维护。
-- icon 存 @element-plus/icons-vue 的导出名（如 Cpu），由前端映射成组件。
CREATE TABLE IF NOT EXISTS t_menu (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id   VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(64)  NOT NULL,
    -- 前端路由路径，全表唯一
    path        VARCHAR(128) NOT NULL,
    icon        VARCHAR(64),
    sort_order  INT          NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_menu_path (path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 变更：t_user 增加 plan_id 列 + idx_user_plan 索引（2026-11-24）
-- MySQL 不支持 ALTER TABLE ... ADD COLUMN IF NOT EXISTS，而本脚本每次启动重跑，
-- 故用 information_schema 判断 + PREPARE 动态 SQL 实现幂等；
-- t_user 不存在（全新库未启动过客户端 backend）时跳过。
-- ----------------------------------------------------------------------------
SET @t_user_exists = (SELECT COUNT(*) FROM information_schema.TABLES
                      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user');

SET @plan_id_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'plan_id');
SET @ddl = IF(@t_user_exists > 0 AND @plan_id_exists = 0,
              'ALTER TABLE t_user ADD COLUMN plan_id BIGINT NULL DEFAULT NULL',
              'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_plan_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
                        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND INDEX_NAME = 'idx_user_plan');
SET @ddl = IF(@t_user_exists > 0 AND @idx_plan_exists = 0,
              'CREATE INDEX idx_user_plan ON t_user (plan_id)',
              'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
