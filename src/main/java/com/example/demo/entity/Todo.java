package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * 待办事项实体 —— 对应数据库里的 todo 表。
 * 加了 @Entity 注解后，JPA（Hibernate）会根据这个类自动建表，
 * 见 application.properties 里的 spring.jpa.hibernate.ddl-auto=update。
 */
@Entity
public class Todo {

    /** 主键 id，由数据库自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 待办标题 */
    private String title;

    /** 是否已完成 */
    private boolean done;

    /** JPA 要求必须有一个无参构造方法 */
    public Todo() {
    }

    /** 方便代码里快速创建一条新待办（默认未完成） */
    public Todo(String title) {
        this.title = title;
        this.done = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
