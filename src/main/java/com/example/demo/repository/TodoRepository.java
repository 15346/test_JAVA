package com.example.demo.repository;

import com.example.demo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 数据访问层（DAO）。
 *
 * 只需要继承 JpaRepository，下面的方法 Spring Data JPA 会"自动实现"，
 * 不用我们自己写任何 SQL：
 *   findAll()          查全部
 *   findById(id)       按 id 查
 *   save(entity)       新增或更新
 *   deleteById(id)     按 id 删除
 *
 * 如果以后需要自定义查询，可以直接在这里写方法名，
 * 例如：List<Todo> findByDone(boolean done);
 */
@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
}
