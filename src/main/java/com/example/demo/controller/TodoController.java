package com.example.demo.controller;

import com.example.demo.auth.security.UserPrincipal;
import com.example.demo.dto.TodoRequest;
import com.example.demo.dto.TodoResponse;
import com.example.demo.service.TodoService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 接口层（控制器）。定义前端可以访问的 REST 接口。
 *
 * 归属一律取自 {@link AuthenticationPrincipal} 解出的认证主体，
 * 绝不从请求体读取 userId；响应只返回 {@link TodoResponse}，不暴露 User 实体。
 *
 * 接口一览（统一前缀 /api/todos，均需登录）：
 *   GET    /api/todos        获取当前用户的全部待办
 *   POST   /api/todos        新增待办      请求体：{"title":"xxx"}
 *   PUT    /api/todos/{id}   更新待办      请求体：{"title":"xxx","done":true}
 *   DELETE /api/todos/{id}   删除待办
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService service;

    public TodoController(TodoService service) {
        this.service = service;
    }

    /** 获取当前用户的全部待办 —— GET */
    @GetMapping
    public List<TodoResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.findAll(principal.user());
    }

    /** 新增待办 —— POST，归属当前登录用户 */
    @PostMapping
    public TodoResponse create(@AuthenticationPrincipal UserPrincipal principal,
            @RequestBody TodoRequest request) {
        return service.create(principal.user(), request.title());
    }

    /** 更新待办 —— PUT，@PathVariable 取地址里的 id */
    @PutMapping("/{id}")
    public TodoResponse update(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id, @RequestBody TodoRequest request) {
        return service.update(principal.user(), id, request);
    }

    /** 删除待办 —— DELETE */
    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        service.delete(principal.user(), id);
    }
}
