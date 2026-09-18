package com.example.demo.config;

/**
 * 统一错误响应体（JSON）：{@code {"code":"...","message":"..."}}。
 *
 * 前端只读取这两个字段，HTTP 状态码单独使用。
 */
public record ApiError(String code, String message) {
}
