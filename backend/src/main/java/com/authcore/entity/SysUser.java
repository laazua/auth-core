package com.authcore.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户实体，对应表 sys_user（docs/01-architecture.md §3 契约）。
 */
@TableName("sys_user")
public class SysUser {

    /** 主键，数据库自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，全局唯一。 */
    private String username;

    /** BCrypt 密文，禁止存放明文。 */
    private String password;

    /** 显示名，可空。 */
    private String nickname;

    /** 邮箱，可空。 */
    private String email;

    /** 手机号，可空。 */
    private String phone;

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
     * @return 登录名
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username 登录名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return BCrypt 密文
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password BCrypt 密文
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return 显示名
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * @param nickname 显示名
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * @return 邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email 邮箱
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return 手机号
     */
    public String getPhone() {
        return phone;
    }

    /**
     * @param phone 手机号
     */
    public void setPhone(String phone) {
        this.phone = phone;
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
