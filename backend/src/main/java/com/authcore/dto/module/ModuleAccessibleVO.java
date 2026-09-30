package com.authcore.dto.module;

/**
 * 可访问模块精简记录（web/038b，GET /modules/accessibles 返回项）。
 * 字段集对齐设计文档第 3 节：id/name/code/baseUrl/description/status，
 * 不含 createdAt/updatedAt 等管理字段，与 ModuleVO 区分以隔离 038c 字段错位问题。
 */
public record ModuleAccessibleVO(
        Long id,
        String name,
        String code,
        String baseUrl,
        String description,
        Integer status
) {
}
