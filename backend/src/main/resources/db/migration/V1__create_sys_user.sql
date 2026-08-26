-- model/001：sys_user 用户表（docs/01-architecture.md §3 契约，字段不得增删改名）
CREATE TABLE sys_user (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username   VARCHAR(64)  NOT NULL COMMENT '登录名，全局唯一',
    password   VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    nickname   VARCHAR(64)  NULL COMMENT '显示名',
    email      VARCHAR(128) NULL COMMENT '邮箱',
    phone      VARCHAR(32)  NULL COMMENT '手机号',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用/0停用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户';
