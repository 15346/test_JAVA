package com.example.demo.controller;

import com.example.demo.entity.Todo;
import com.example.demo.service.TodoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 接口层（控制器）。定义前端可以访问的 REST 接口。
 *
 * @RestController = @Controller + @ResponseBody，
 * 表示每个方法的返回值会自动转成 JSON 返回给前端（这正是前后端分离要的）。
 *
 * 接口一览（统一前缀 /api/todos）：
 *   GET    /api/todos        获取全部待办
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

    /** 获取全部待办 —— GET */
    @GetMapping
    public List<Todo> list() {
        return service.findAll();
    }

    /** 新增待办 —— POST，@RequestBody 把请求体的 JSON 转成 Todo 对象 */
    @PostMapping
    public Todo create(@RequestBody Todo todo) {
        return service.create(todo.getTitle());
    }

    /** 更新待办 —— PUT，@PathVariable 取地址里的 id */
    @PutMapping("/{id}")
    public Todo update(@PathVariable Long id, @RequestBody Todo todo) {
        return service.update(id, todo);
    }

    /** 删除待办 —— DELETE */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
