-- 演示/初始数据。
-- 因为用的是内存数据库，应用每次重启都会清空，所以这些数据每次启动都会重新插入。
-- 想要清空，直接删掉/注释这些行即可。
--
-- 注意：这里只插入 title/done，不写 user_id（该列可空）。
-- 启动后由 SystemUserInitializer 把「user_id 为空」的历史数据统一归属到
-- 不可登录的系统用户 system@local，无论是 H2 演示数据还是 MySQL 里的既有数据。

INSERT INTO todo (title, done) VALUES ('学习 Spring Boot', FALSE);
INSERT INTO todo (title, done) VALUES ('写一个前后端分离示例', FALSE);
INSERT INTO todo (title, done) VALUES ('跑通第一个 CRUD', TRUE);
