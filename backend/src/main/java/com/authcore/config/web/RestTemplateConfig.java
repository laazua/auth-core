package com.authcore.config.web;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 装配：模块（外部服务）统一访问入口的下游转发客户端（modules/002）。
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 转发用 RestTemplate（经 RestTemplateBuilder 构建，沿用 Boot 默认编解码与超时策略）。
     *
     * @param builder Boot 自动装配的构建器
     * @return RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
