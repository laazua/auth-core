package com.authcore.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 模块实体，对应表 sys_module（docs/01-architecture.md §3 契约）。
 */
@TableName("sys_module")
public class SysModule {

    /** 主键，数据库自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块名。 */
    private String name;

    /** 模块编码，全局唯一（架构 §6.3）。 */
    private String code;

    /** 模块服务地址，可空。 */
    private String baseUrl;

    /** 描述，可空。 */
    private String description;

    /** 状态：1 启用 / 0 停用。 */
    private Integer status;

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
     * @return 模块名
     */
    public String getName() {
        return name;
    }

    /**
     * @param name 模块名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return 模块编码
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code 模块编码
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return 模块服务地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @param baseUrl 模块服务地址
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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
     * @return 状态
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * @param status 状态
     */
    public void setStatus(Integer status) {
        this.status = status;
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
