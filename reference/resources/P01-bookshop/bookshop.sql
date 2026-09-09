-- =============================================================
-- 图书销售系统 · 完整 DDL 脚本
-- 配套文档：P01-实验项目1·项目立项与数据库设计.md（第 5.5 节）
-- 环境：MySQL 8.0+ / 字符集 utf8mb4 / 引擎 InnoDB
-- =============================================================
-- 使用方法：
--   方式1（命令行）：mysql -u root -p < bookshop.sql
--   方式2（客户端）：全选 → 执行
-- 验证：SHOW TABLES; 应返回 6 张表，且无 warning
-- =============================================================

DROP DATABASE IF EXISTS bookshop;
CREATE DATABASE bookshop DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE bookshop;

-- -------------------------------------------------------------
-- 1. 用户表 t_user
-- -------------------------------------------------------------
CREATE TABLE t_user
(
    id         INT AUTO_INCREMENT PRIMARY KEY                COMMENT '用户ID，主键',
    username   VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名，唯一',
    password   VARCHAR(100) NOT NULL COMMENT '密码（须 BCrypt 加密）',
    email      VARCHAR(100) UNIQUE COMMENT '邮箱',
    phone      VARCHAR(20) COMMENT '手机号',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1=正常，0=禁用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户表';

-- -------------------------------------------------------------
-- 2. 分类表 t_category（自关联，支持多级分类）
-- -------------------------------------------------------------
CREATE TABLE t_category
(
    id         INT AUTO_INCREMENT PRIMARY KEY                COMMENT '分类ID',
    name       VARCHAR(50) NOT NULL COMMENT '分类名',
    parent_id  INT                  DEFAULT NULL COMMENT '父分类ID，顶级为NULL',
    sort_order INT         NOT NULL DEFAULT 0 COMMENT '排序',
    FOREIGN KEY (parent_id) REFERENCES t_category (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='分类表（自关联，支持多级）';

-- -------------------------------------------------------------
-- 3. 图书表 t_book
-- -------------------------------------------------------------
CREATE TABLE t_book
(
    id          INT AUTO_INCREMENT PRIMARY KEY                COMMENT '图书ID',
    title       VARCHAR(200)   NOT NULL COMMENT '书名',
    author      VARCHAR(100)   NOT NULL COMMENT '作者',
    isbn        VARCHAR(20)    NOT NULL UNIQUE COMMENT 'ISBN',
    price       DECIMAL(10, 2) NOT NULL COMMENT '价格',
    stock       INT            NOT NULL DEFAULT 0 COMMENT '库存',
    cover_url   VARCHAR(255) COMMENT '封面URL',
    description TEXT COMMENT '描述',
    category_id INT            NOT NULL COMMENT '分类ID',
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '1=在售，0=下架',
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES t_category (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='图书表';
CREATE INDEX idx_book_title ON t_book (title);

-- -------------------------------------------------------------
-- 4. 收货地址表 t_address（弱实体：没有 t_user 就没有它）
-- -------------------------------------------------------------
CREATE TABLE t_address
(
    id         INT AUTO_INCREMENT PRIMARY KEY                COMMENT '地址ID',
    user_id    INT          NOT NULL COMMENT '所属用户ID',
    receiver   VARCHAR(50)  NOT NULL COMMENT '收货人',
    phone      VARCHAR(20)  NOT NULL COMMENT '电话',
    province   VARCHAR(20)  NOT NULL COMMENT '省',
    city       VARCHAR(20)  NOT NULL COMMENT '市',
    detail     VARCHAR(200) NOT NULL COMMENT '详细地址',
    is_default TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认',
    FOREIGN KEY (user_id) REFERENCES t_user (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='收货地址表';

-- -------------------------------------------------------------
-- 5. 订单表 t_order
-- -------------------------------------------------------------
CREATE TABLE t_order
(
    id           INT AUTO_INCREMENT PRIMARY KEY                COMMENT '订单ID',
    order_no     VARCHAR(32)    NOT NULL UNIQUE COMMENT '订单号',
    user_id      INT            NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '总金额',
    status       TINYINT        NOT NULL DEFAULT 0 COMMENT '0=待支付，1=已支付，2=已发货，3=已完成，4=已取消',
    receiver     VARCHAR(200)   NOT NULL COMMENT '收货信息（快照）',
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at      DATETIME                DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES t_user (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='订单表';
CREATE INDEX idx_order_user ON t_order (user_id);

-- -------------------------------------------------------------
-- 6. 订单明细表 t_order_item
--    M:N 关联表：t_order × t_book 的中间表，带业务属性（quantity / price）
-- -------------------------------------------------------------
CREATE TABLE t_order_item
(
    id       INT AUTO_INCREMENT PRIMARY KEY                COMMENT '明细ID',
    order_id INT            NOT NULL COMMENT '订单ID',
    book_id  INT            NOT NULL COMMENT '图书ID',
    quantity INT            NOT NULL COMMENT '数量',
    price    DECIMAL(10, 2) NOT NULL COMMENT '下单时单价（快照）',
    FOREIGN KEY (order_id) REFERENCES t_order (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (book_id) REFERENCES t_book (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='订单明细（M:N 关联表）';
CREATE INDEX idx_item_order ON t_order_item (order_id);

-- =============================================================
-- 附：测试数据（可选，做完 DDL 后单独执行）
-- =============================================================
INSERT INTO t_category (id, name, parent_id, sort_order)
VALUES (1, '计算机', NULL, 1),
       (2, '文学', NULL, 2),
       (3, 'Java', 1, 1),
       (4, '数据库', 1, 2);

INSERT INTO t_user (username, password, email, phone, status)
VALUES ('zhangsan', 'e10adc3949ba59abbe56e057f20f883e', 'zhangsan@example.com', '13800138000', 1),
       ('lisi', 'e10adc3949ba59abbe56e057f20f883e', 'lisi@example.com', '13900139000', 1);

INSERT INTO t_book (title, author, isbn, price, stock, category_id, status)
VALUES ('Java 核心技术 卷I', 'Cay S. Horstmann', '9787111642635', 119.00, 50, 3, 1),
       ('MySQL 是怎样运行的', '小孩子4919', '9787115575944', 89.00, 30, 4, 1),
       ('活着', '余华', '9787506365437', 28.00, 100, 2, 1);
