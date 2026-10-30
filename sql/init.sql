-- =====================================================================
-- WangOJ 数据库初始化脚本（MySQL 8.0+）
--
-- 适用：全新部署
-- 用法：
--   mysql -h127.0.0.1 -uroot -p < sql/init.sql
--   （docker compose 会自动挂载本目录到 /docker-entrypoint-initdb.d/）
--
-- 说明：
--   - 表名保持与实体 @TableName 一致（camelCase），Linux 下 MySQL 默认区分大小写；
--   - 字符集统一 utf8mb4，避免 emoji / 生僻字写入失败。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `wangoj`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `wangoj`;

-- ---------------------------------------------------------------------
-- 用户
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user`
(
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `userAccount`   VARCHAR(64)  NOT NULL COMMENT '登录账号',
    `userPassword`  VARCHAR(255) NOT NULL COMMENT '密码哈希（PBKDF2 或历史 MD5）',
    `userRole`      VARCHAR(32)  NOT NULL DEFAULT 'user' COMMENT 'user / admin',
    `lastLoginTime` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间',
    `createdTime`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `isDeleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否，1 是',
    UNIQUE KEY `uk_user_account` (`userAccount`),
    KEY `idx_user_created` (`createdTime`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户';

-- ---------------------------------------------------------------------
-- 题目
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `problem`
(
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title`       VARCHAR(255) NOT NULL COMMENT '标题',
    `content`     MEDIUMTEXT   NOT NULL COMMENT '题面（Markdown）',
    `tags`        VARCHAR(512) NOT NULL DEFAULT '[]' COMMENT '标签 JSON 数组',
    `testCase`    MEDIUMTEXT COMMENT '测试用例 JSON：[{input,output}]',
    `limitOption` VARCHAR(255) COMMENT '限制 JSON：{timeoutLimit(ms),memoryLimit(MB)}',
    `commitNum`   INT          NOT NULL DEFAULT 0 COMMENT '提交次数',
    `acceptNum`   INT          NOT NULL DEFAULT 0 COMMENT '通过次数',
    `createdTime` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `isDeleted`   TINYINT      NOT NULL DEFAULT 0,
    KEY `idx_problem_title` (`title`),
    KEY `idx_problem_created` (`createdTime`),
    -- 热门题目按 acceptNum / commitNum 排序
    KEY `idx_problem_hot` (`acceptNum`, `commitNum`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='题目';

-- ---------------------------------------------------------------------
-- 提交记录
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `commitLog`
(
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `userId`        BIGINT       NOT NULL,
    `problemId`     BIGINT       NOT NULL,
    `code`          MEDIUMTEXT   NOT NULL COMMENT '用户代码',
    `lang`          VARCHAR(32)  NOT NULL DEFAULT 'c++' COMMENT 'c/c++/java/python/javascript/golang',
    `status`        VARCHAR(32)  NOT NULL DEFAULT 'Waiting' COMMENT '判题状态',
    `limitOption`   VARCHAR(255) COMMENT '判题耗时/内存 JSON',
    `errorMessage`  TEXT         COMMENT '编译错误 / 系统错误信息（截断后保存，便于用户自纠）',
    `caseResult`    MEDIUMTEXT   COMMENT '逐测试点结果 JSON：[{index,verdict,timeMs,memoryBytes,accepted}]',
    `createdTime`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `isDeleted`     TINYINT      NOT NULL DEFAULT 0,
    KEY `idx_commit_user` (`userId`),
    KEY `idx_commit_problem` (`problemId`),
    KEY `idx_commit_status` (`status`),
    KEY `idx_commit_created` (`createdTime`),
    -- 排行榜按 (status) 过滤后聚合 userId，复合索引避免回表扫描
    KEY `idx_commit_status_user` (`status`, `userId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='提交记录';
