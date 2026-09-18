package com.example.demo.dto;

/**
 * 新增/更新待办的请求体。
 *
 * 刻意不包含 userId：归属一律取自当前认证主体，绝不信任前端传入。
 */
public record TodoRequest(String title, boolean done) {
}
