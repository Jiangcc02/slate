# P01 配套资源 · 图书销售系统数据库

对应文档：`P01-实验项目1·项目立项与数据库设计.md`

## 文件清单

| 文件                 | 说明                                        | 对应文档章节 |
|----------------------|---------------------------------------------|--------------|
| `bookshop.sql`       | 完整 DDL + 测试数据（6 张表）               | 5.5          |
| `P01-er-diagram.svg` | 图书销售系统 E-R 图（矢量，浏览器直接打开） | 3.3          |
| `README.md`          | 本文件                                      | —            |

> 原文档附录写作 `P01-er-diagram.png`，此处改为 SVG：矢量放大不失真，且能用文本编辑器改字。
> 如需 PNG：浏览器打开 SVG → 右键「存储为图片」，或用 `sips`/预览导出 300dpi PNG。

## 三步跑通（约 15 分钟）

```bash
# 1. 建库建表
mysql -u root -p < bookshop.sql

# 2. 验证（应返回 6 张表）
mysql -u root -p -e "USE bookshop; SHOW TABLES;"

# 3. 看表结构（确认外键生效）
mysql -u root -p -e "USE bookshop; SHOW CREATE TABLE t_order_item\G"
```

预期 6 张表：

```
t_address
t_book
t_category
t_order
t_order_item
t_user
```

## 表关系速记

```
t_user ──1:N──> t_address      （弱实体，CASCADE 删除）
t_user ──1:N──> t_order
t_order ──1:N──> t_order_item <──N:1── t_book
t_category ──1:N──> t_book
t_category ──1:N──> t_category  （自关联，多级分类）
```

## 常见错误对照（文档第 7 节）

| 错误码 | 含义                  | 排查                                              |
|--------|-----------------------|---------------------------------------------------|
| 1062   | 唯一键冲突            | 重复执行了 INSERT，或 username/isbn/order_no 撞车 |
| 1452   | 外键约束失败          | 先插父表（t_user/t_category/t_book），再插子表    |
| 1005   | 建表失败（errno 150） | 外键字段类型必须与父表主键完全一致（都是 INT）    |

## 交付物要求（本次课）

1. 项目立项说明书（1 页，含 5 步流程）
2. 全局 E-R 图（可参照本 SVG 改画）
3. DDL 脚本 + `SHOW TABLES` 截图
