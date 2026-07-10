package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置（CORS）。
 *
 * 为什么要它？前后端分离时，前端和后端通常跑在不同端口
 * （比如前端 http://localhost:8000，后端 http://localhost:8080）。
 * 浏览器有"同源策略"安全限制，会默认拦截这种跨源请求。
 * 这个配置就是告诉浏览器：这些来自本地前端的请求，放行。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 允许的前端地址（本地开发常用端口都放行）
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                )
                // 允许的 HTTP 方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许任意请求头
                .allowedHeaders("*");
    }
}
