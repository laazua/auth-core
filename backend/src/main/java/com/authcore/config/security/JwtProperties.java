package com.authcore.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 配置绑定类。
 * secret 仅从环境变量读取，配置文件不落真实密钥（架构 §5、规范 §6）。
 */
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {

    /** 签名密钥：仅环境变量注入，配置文件留空或占位。 */
    private String secret;

    /** Token 有效期（小时），默认 2h。 */
    private int expireHours = 2;

    /**
     * @return 签名密钥
     */
    public String getSecret() {
        return secret;
    }

    /**
     * @param secret 签名密钥
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * @return Token 有效期（小时）
     */
    public int getExpireHours() {
        return expireHours;
    }

    /**
     * @param expireHours Token 有效期（小时）
     */
    public void setExpireHours(int expireHours) {
        this.expireHours = expireHours;
    }
}