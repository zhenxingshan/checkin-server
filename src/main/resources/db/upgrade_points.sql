-- 签到积分字段迁移：给「已经建过表」的线上库补齐字段。
-- 全新库直接执行 schema.sql 即可；本脚本幂等，重复执行不会报错。
-- 执行：mysql -uroot -p < db/upgrade_points.sql

USE daily_checkin;

DELIMITER $$
DROP PROCEDURE IF EXISTS upgrade_checkin_points $$
CREATE PROCEDURE upgrade_checkin_points()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'total_points'
    ) THEN
        ALTER TABLE t_user ADD COLUMN total_points INT NOT NULL DEFAULT 0 COMMENT '累计签到积分';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'point_streak'
    ) THEN
        ALTER TABLE t_user ADD COLUMN point_streak INT NOT NULL DEFAULT 0 COMMENT '按自然日计的连续签到天数';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'last_point_date'
    ) THEN
        ALTER TABLE t_user ADD COLUMN last_point_date DATE DEFAULT NULL COMMENT '最近一次计分的自然日';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'last_point_points'
    ) THEN
        ALTER TABLE t_user ADD COLUMN last_point_points INT NOT NULL DEFAULT 0 COMMENT '最近一次计分当天获得的积分';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_checkin_record' AND COLUMN_NAME = 'points'
    ) THEN
        ALTER TABLE t_checkin_record ADD COLUMN points INT NOT NULL DEFAULT 0 COMMENT '本次签到获得的积分';
    END IF;
END $$
DELIMITER ;

CALL upgrade_checkin_points();
DROP PROCEDURE IF EXISTS upgrade_checkin_points;
