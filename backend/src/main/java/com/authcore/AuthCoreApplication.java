package com.authcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * auth-core 后端启动类。
 *
 * <p>组件扫描根包为 com.authcore，业务代码按 docs/01-architecture.md 第 2 节
 * 组织在 config/controller/service/mapper/entity/dto/common 七个固定业务包内。
 */
@SpringBootApplication
public class AuthCoreApplication {

    /**
     * 应用入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthCoreApplication.class, args);
    }
}
