package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.repository.UserRepository;
import com.example.demo.config.ApiException;
import com.example.demo.dto.TodoRequest;
import com.example.demo.dto.TodoResponse;
import com.example.demo.entity.Todo;
import com.example.demo.repository.TodoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 待办的用户归属隔离：数据访问必须绑定当前认证用户。
 *
 * {@link TodoService} 不是 Repository，切片测试不会自动扫描，因此显式 {@code @Import}。
 */
@DataJpaTest
@Import(TodoService.class)
@ActiveProfiles("test")
class TodoOwnershipTest {

    @Autowired
    private TodoRepository repository;

    @Autowired
    private UserRepository users;

    @Autowired
    private TodoService service;

    private User user(String email) {
        return users.save(User.create(email, "hash", Role.USER, true));
    }

    @Test
    void findAllReturnsOnlyCurrentUsersTodos() {
        User alice = user("alice@example.com");
        User bob = user("bob@example.com");
        repository.save(new Todo("Alice task", false, alice));
        repository.save(new Todo("Bob task", false, bob));

        List<TodoResponse> result = service.findAll(alice);

        assertEquals(List.of("Alice task"),
                result.stream().map(TodoResponse::title).toList());
    }

    @Test
    void updateAndDeleteCannotCrossUserBoundary() {
        User alice = user("alice@example.com");
        User bob = user("bob@example.com");
        Todo bobTodo = repository.save(new Todo("Bob task", false, bob));

        ApiException updateError = assertThrows(ApiException.class, () ->
                service.update(alice, bobTodo.getId(), new TodoRequest("stolen", true)));
        assertEquals("TODO_NOT_FOUND", updateError.getCode());

        ApiException deleteError = assertThrows(ApiException.class, () ->
                service.delete(alice, bobTodo.getId()));
        assertEquals("TODO_NOT_FOUND", deleteError.getCode());
    }
}
