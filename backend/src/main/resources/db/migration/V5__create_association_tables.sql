-- model/005：sys_user_role / sys_role_permission 关联表（docs/01-architecture.md §6 RBAC0 骨架）
-- user_id/role_id/permission_id 为逻辑外键，引用完整性由 service 层维护（规范第 4 节），不建物理 FOREIGN KEY
CREATE TABLE sys_user_role (
    id         BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT  NOT NULL COMMENT '逻辑外键→sys_user.id',
    role_id    BIGINT  NOT NULL COMMENT '逻辑外键→sys_role.id',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户-角色关联';

CREATE TABLE sys_role_permission (
    id            BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id       BIGINT  NOT NULL COMMENT '逻辑外键→sys_role.id',
    permission_id BIGINT  NOT NULL COMMENT '逻辑外键→sys_permission.id',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_permission (role_id, permission_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色-权限关联';
