package com.example.demo.entity;

import com.example.demo.auth.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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

    /**
     * 归属用户。历史数据迁移前允许为空（user_id 可空），
     * 应用启动后由 {@code SystemUserInitializer} 把无归属数据绑定到系统用户。
     * 只通过 {@code TodoResponse} 对外输出，不序列化 User 实体。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** JPA 要求必须有一个无参构造方法 */
    public Todo() {
    }

    /** 方便代码里创建一条归属于指定用户的待办 */
    public Todo(String title, boolean done, User user) {
        this.title = title;
        this.done = done;
        this.user = user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
