-- 初始测试数据。
-- 因为用的是内存数据库，应用每次重启都会清空，所以这些数据每次启动都会重新插入。
-- 想要清空，直接删掉/注释这些行即可。

INSERT INTO todo (title, done) VALUES ('学习 Spring Boot', FALSE);
INSERT INTO todo (title, done) VALUES ('写一个前后端分离示例', FALSE);
INSERT INTO todo (title, done) VALUES ('跑通第一个 CRUD', TRUE);
