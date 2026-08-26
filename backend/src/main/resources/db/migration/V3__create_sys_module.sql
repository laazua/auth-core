-- model/003：sys_module 模块表（docs/01-architecture.md §3 契约；name/status NOT NULL 为 §3 留白处的 Planner 定夺，登记于工作单）
CREATE TABLE sys_module (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '模块名',
    code        VARCHAR(64)  NOT NULL COMMENT '模块编码，全局唯一',
    base_url    VARCHAR(255) NULL COMMENT '模块服务地址，可空',
    description VARCHAR(255) NULL COMMENT '描述',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用/0停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_module_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模块';
