package com.example.demo.repository;

import com.example.demo.entity.Todo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 数据访问层（DAO）。
 *
 * 只需要继承 JpaRepository，下面的方法 Spring Data JPA 会"自动实现"，
 * 不用我们自己写任何 SQL：
 *   save(entity)       新增或更新
 *   deleteById(id)     按 id 删除
 *
 * 待办查询必须绑定用户，避免越权读到别人的数据：
 *   findByUserId(userId)              当前用户的全部待办
 *   findByIdAndUserId(id, userId)     当前用户可访问的单条待办
 *   findByUserIsNull()                尚未归属的历史数据（迁移用）
 */
@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByUserId(Long userId);

    Optional<Todo> findByIdAndUserId(Long id, Long userId);

    List<Todo> findByUserIsNull();
}
