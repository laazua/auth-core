-- model/002：sys_role 角色表（docs/01-architecture.md §3 契约；name NOT NULL 为 §3 留白处的 Planner 定夺，登记于工作单）
CREATE TABLE sys_role (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(64)  NOT NULL COMMENT '角色名',
    code       VARCHAR(64)  NOT NULL COMMENT '角色编码，全局唯一',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用/0停用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色';
