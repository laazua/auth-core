-- model/004：sys_permission 权限表（docs/01-architecture.md §3 契约：七列、无 status 列）
-- module_id 为逻辑外键→sys_module.id，承载 §6.1「权限必须归属模块」；引用完整性由 service 层维护（规范第 4 节），不建物理 FOREIGN KEY
-- name/code NOT NULL 与 description 可空为 §3 留白处的 Planner 定夺，登记于工作单
CREATE TABLE sys_permission (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    module_id   BIGINT       NOT NULL COMMENT '逻辑外键→sys_module.id',
    name        VARCHAR(64)  NOT NULL COMMENT '权限名',
    code        VARCHAR(64)  NOT NULL COMMENT '权限编码，全局唯一',
    description VARCHAR(255) NULL COMMENT '描述',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_permission_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限';
