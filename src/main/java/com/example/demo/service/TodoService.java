package com.example.demo.service;

import com.example.demo.entity.Todo;
import com.example.demo.repository.TodoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务逻辑层。
 *
 * Controller 不直接操作数据库，而是调用 Service。
 * 分层的好处：Controller 只负责"接收请求 / 返回结果"，
 * 业务规则、数据组装都放在 Service，职责清晰、便于维护和测试。
 */
@Service
public class TodoService {

    private final TodoRepository repository;

    /**
     * 构造器注入（推荐写法）。Spring 会自动把 TodoRepository 传进来。
     * 相比 @Autowired 字段注入，这种方式更清晰、也更容易写单元测试。
     */
    public TodoService(TodoRepository repository) {
        this.repository = repository;
    }

    /** 查询全部待办 */
    public List<Todo> findAll() {
        return repository.findAll();
    }

    /** 新增一条待办 */
    public Todo create(String title) {
        return repository.save(new Todo(title));
    }

    /** 更新待办（标题 / 是否完成） */
    public Todo update(Long id, Todo updated) {
        return repository.findById(id)
                .map(todo -> {                       // 找到记录就更新
                    todo.setTitle(updated.getTitle());
                    todo.setDone(updated.isDone());
                    return repository.save(todo);
                })
                .orElseThrow(() ->                   // 找不到就抛异常
                        new IllegalArgumentException("待办不存在，id = " + id));
    }

    /** 按 id 删除 */
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
