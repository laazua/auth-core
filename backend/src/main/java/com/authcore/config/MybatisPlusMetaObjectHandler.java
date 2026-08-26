package com.authcore.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * 公共时间字段自动填充：created_at 插入填充、updated_at 插入与更新填充（规范第 4 节）。
 */
@Configuration
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入填充 createdAt 与 updatedAt。
     *
     * @param metaObject 实体元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    /**
     * 更新填充 updatedAt。
     *
     * @param metaObject 实体元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
