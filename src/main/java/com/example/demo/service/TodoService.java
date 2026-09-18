package com.example.demo.service;

import com.example.demo.auth.entity.User;
import com.example.demo.config.ApiException;
import com.example.demo.dto.TodoRequest;
import com.example.demo.dto.TodoResponse;
import com.example.demo.entity.Todo;
import com.example.demo.repository.TodoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 业务逻辑层。
 *
 * 所有操作都绑定传入的 {@link User}：查询条件里始终带 userId，
 * 找不到「id + 当前用户」的记录就抛 404，不泄露其他用户的资源是否存在。
 * 对外只返回 {@link TodoResponse}，不暴露 Todo 实体或 User。
 */
@Service
public class TodoService {

    private static final String NOT_FOUND_CODE = "TODO_NOT_FOUND";

    private static final String NOT_FOUND_MESSAGE = "待办不存在";

    private final TodoRepository repository;

    /**
     * 构造器注入（推荐写法）。Spring 会自动把 TodoRepository 传进来。
     */
    public TodoService(TodoRepository repository) {
        this.repository = repository;
    }

    /** 查询当前用户的全部待办 */
    public List<TodoResponse> findAll(User user) {
        return repository.findByUserId(user.getId()).stream()
                .map(TodoResponse::from)
                .toList();
    }

    /** 为当前用户新增一条待办 */
    public TodoResponse create(User user, String title) {
        return TodoResponse.from(repository.save(new Todo(title, false, user)));
    }

    /** 更新当前用户的一条待办（标题 / 是否完成） */
    public TodoResponse update(User user, Long id, TodoRequest request) {
        Todo todo = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException(404, NOT_FOUND_CODE, NOT_FOUND_MESSAGE));
        todo.setTitle(request.title());
        todo.setDone(request.done());
        return TodoResponse.from(repository.save(todo));
    }

    /** 删除当前用户的一条待办 */
    public void delete(User user, Long id) {
        Todo todo = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException(404, NOT_FOUND_CODE, NOT_FOUND_MESSAGE));
        repository.deleteById(todo.getId());
    }
}
