-- 每日签到后台 MySQL 建表脚本（MySQL 5.7+ / 8.0）
-- 执行：mysql -uroot -p < db/schema.sql
-- 建表语句已包含签到积分字段；当前库没有历史数据，重复执行会先删表重建。

CREATE DATABASE IF NOT EXISTS daily_checkin
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE daily_checkin;

DROP TABLE IF EXISTS t_checkin_record;
DROP TABLE IF EXISTS t_checkin_task;
DROP TABLE IF EXISTS t_user;

-- 用户表：openId、昵称、头像 + 登录凭证
CREATE TABLE t_user (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    openid          VARCHAR(64)  NOT NULL COMMENT '微信 openId',
    nickname        VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '昵称，首次登录由服务端词库随机生成',
    avatar          VARCHAR(32)  NOT NULL DEFAULT 'sprout' COMMENT '预设头像标识',
    token           VARCHAR(64)  DEFAULT NULL COMMENT '登录凭证',
    token_expire_at DATETIME     DEFAULT NULL COMMENT '凭证过期时间',
    last_login_at   DATETIME     DEFAULT NULL,
    -- 签到积分：按「用户 + 自然日」计，一天内多个任务只有首次签到计分。
    total_points      INT        NOT NULL DEFAULT 0  COMMENT '累计签到积分',
    point_streak      INT        NOT NULL DEFAULT 0  COMMENT '按自然日计的连续签到天数（决定积分档位）',
    last_point_date   DATE       DEFAULT NULL COMMENT '最近一次计分的自然日',
    last_point_points INT        NOT NULL DEFAULT 0  COMMENT '最近一次计分当天获得的积分',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_openid (openid),
    KEY idx_token (token)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户信息';

-- 签到任务表：按用户隔离，deleted 做逻辑删除
CREATE TABLE t_checkin_task (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL,
    title      VARCHAR(32) NOT NULL COMMENT '任务名称',
    icon       VARCHAR(16) NOT NULL DEFAULT '🌱' COMMENT '任务图标(emoji)',
    color      VARCHAR(16) NOT NULL DEFAULT '#4C6FFF' COMMENT '主题色',
    note       VARCHAR(64) NOT NULL DEFAULT '' COMMENT '备注',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '0-正常 1-已删除',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_deleted (user_id, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '签到任务';

-- 签到记录表：唯一索引保证「用户 + 任务 + 日期」一天只能签一次
CREATE TABLE t_checkin_record (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id      BIGINT UNSIGNED NOT NULL,
    task_id      BIGINT UNSIGNED NOT NULL,
    checkin_date DATE         NOT NULL COMMENT '签到日期(服务端当天)',
    checkin_time VARCHAR(5)   NOT NULL DEFAULT '' COMMENT '签到时间 HH:mm',
    points       INT          NOT NULL DEFAULT 0  COMMENT '本次签到获得的积分；当天非首次签到为 0',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_task_date (user_id, task_id, checkin_date),
    KEY idx_user_task (user_id, task_id, checkin_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '签到记录';
