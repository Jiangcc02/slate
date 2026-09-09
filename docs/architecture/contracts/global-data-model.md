# 全局数据模型（主链骨架）

status: v0.1-draft（主链与支链锚点级骨架；列级设计归各域设计文档，本文件只锁 ID 契约与关系骨架）

> 契约先行的第一产物（蓝图 §十三）：锁主链实体、ID 体系与跨域关系骨架。
> 与 `data-ownership.md` 的分工：本文件定关系骨架，属主判定以 data-ownership 为准。

## 1. 主链

```
学生 ∈ 班级 —任课→ 教师 —讲授→ 课程 —覆盖→ 知识点 ←标注— 题目
```

```mermaid
erDiagram
    ORG ||--o{ CLASS : "拥有"
    CLASS ||--o{ STUDENT : "在籍"
    GUARDIAN }o--o{ STUDENT : "多对多绑定"
    TEACHER ||--o{ TEACHING : "任课"
    CLASS ||--o{ TEACHING : ""
    COURSE ||--o{ TEACHING : ""
    COURSE ||--o{ CHAPTER : ""
    CHAPTER ||--o{ SECTION : ""
    SECTION ||--o{ LESSON : ""
    CHAPTER ||--o{ KNOWLEDGE_POINT : "挂载"
    SECTION ||--o{ KNOWLEDGE_POINT : "挂载"
    QUESTION }o--o{ KNOWLEDGE_POINT : "标注"
    LESSON ||--o{ LESSON_RESOURCE : "课件资源包"
    LESSON ||--o{ CLASSROOM_RECORD : "课堂记录"
    STUDENT ||--o{ LEARNING_RECORD : "学习记录"
    STUDENT ||--o{ ASSIGNMENT_TASK : "作业任务流"
    QUESTION ||--o{ PAPER : "组卷"
    PAPER ||--o{ EXAM : "用于三形态"
    STUDENT ||--o{ ANSWER_RECORD : "答题"
    ANSWER_RECORD ||--o| SCORE : "判分"
    STUDENT ||--o{ GROWTH_ARCHIVE : "成长档案"
    CLASSROOM }o--o{ CLASS : "常驻绑定"
```

## 2. ID 契约（主链 ID 体系）

- 主链实体（学生/班级/教师/课程/章节课时/知识点/题目）ID 是全系统共享引用键：**由属主域生成，他域只引用、不复制**
- 类型 int64；生成策略（雪花 / 自增）由底座设计定稿，定稿后全系统统一
- **课表五要素**：一节课 = 时间 + 班级 + 课程 + 教师 + 教室；教室为教务一等实体，教室-班级常驻绑定，教师-教室经课表发生临时关系（蓝图 §六）

## 3. 三条支链锚点

1. **学习支链**：课时 → 学习记录（学生侧过程数据，属学习域）
2. **测评支链**：题目 → 试卷 → 三形态 → 答题记录 → 成绩（属测评域）→ 学情报告（属学情域）
3. **家校支链**：学生 —多对多绑定→ 家长（属底座用户中心）；学生 → 成长档案（属学情域）

## 4. 跨域引用规则

- 引用他域实体只存 ID，禁止复制业务字段；确需展示快照（如学生姓名冗余在成绩单）须标注来源与失效策略
- 需要他域数据视图时走契约接口实时查询，禁止建跨域同步表（对齐蓝图 §十：胶水层架构上避免出现）

## 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v0.1-draft | 2026-09-09 | 主链骨架 ER、ID 契约、课表五要素、支链锚点、跨域引用规则成文 |
