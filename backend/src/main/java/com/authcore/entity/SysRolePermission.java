package com.authcore.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 角色-权限关联实体，对应表 sys_role_permission（docs/01-architecture.md §6 RBAC0 骨架）。
 */
@TableName("sys_role_permission")
public class SysRolePermission {

    /** 主键，数据库自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑外键→sys_role.id。 */
    private Long roleId;

    /** 逻辑外键→sys_permission.id。 */
    private Long permissionId;

    /** 创建时间，插入时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入与更新时自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * @return 主键
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id 主键
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return 逻辑外键 role_id
     */
    public Long getRoleId() {
        return roleId;
    }

    /**
     * @param roleId 逻辑外键 role_id
     */
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    /**
     * @return 逻辑外键 permission_id
     */
    public Long getPermissionId() {
        return permissionId;
    }

    /**
     * @param permissionId 逻辑外键 permission_id
     */
    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    /**
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
