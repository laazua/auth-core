package com.authcore.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 权限实体，对应表 sys_permission（docs/01-architecture.md §3 契约）。
 *
 * <p>module_id 为逻辑外键指向 sys_module（§6.1 权限必须归属模块）；本实体无 status 字段。
 */
@TableName("sys_permission")
public class SysPermission {

    /** 主键，数据库自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑外键→sys_module.id。 */
    private Long moduleId;

    /** 权限名。 */
    private String name;

    /** 权限编码，全局唯一（架构 §6.3）。 */
    private String code;

    /** 描述，可空。 */
    private String description;

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
     * @return 逻辑外键 module_id
     */
    public Long getModuleId() {
        return moduleId;
    }

    /**
     * @param moduleId 逻辑外键 module_id
     */
    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }

    /**
     * @return 权限名
     */
    public String getName() {
        return name;
    }

    /**
     * @param name 权限名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return 权限编码
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code 权限编码
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return 描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description 描述
     */
    public void setDescription(String description) {
        this.description = description;
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
