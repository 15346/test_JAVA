package com.example.demo.dto;

import com.example.demo.entity.Todo;

/**
 * 待办的对外表示，只暴露 id/title/done（与既有前端契约一致），
 * 不包含 User 或 userId。
 */
public record TodoResponse(Long id, String title, boolean done) {

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(todo.getId(), todo.getTitle(), todo.isDone());
    }
}
