# Git 工作流规程（Agent 操作手册）

> **读者**：参与本仓库开发的 AI Agent 与人类协作者。
> 本文件是强制规程：执行任何 git / Issue / PR 操作前必须先阅读并遵循。
> 规程未覆盖的情况，一律按 §8 处理（停止操作，请示人类）。

---

## 1. 环境常量

| 项 | 值 |
|---|---|
| 仓库 | https://github.com/Jiangcc02/slate |
| 远端名 | `origin` |
| 主干分支 | `main`（稳定可演示版）、`dev`（日常集成主干） |
| 工作语言 | 中文（提交信息、PR、Issue 均使用中文） |
| 目录职责 | `docs/` 产品文档（`docs/guides/` 为规程与指南）　`reference/` 课程参考资料（**只读**） |

---

## 2. 硬性约束

违反以下任何一条：**立即停止操作**，按 §8 请示。

1. 禁止直接 push 到 `main`、`dev`，主干只接受 PR 合并
2. 禁止对任何共享分支（`main`/`dev`/他人分支）执行 `push --force`，禁止对已推送的提交做历史改写
3. 禁止合并未获 approve 的 PR；禁止 approve 自己的 PR；禁止合并自己发起的 PR
4. 禁止删除未合并的分支
5. 禁止创建 Issue；只能读取并处理 **assignee 是自己** 的 Issue，禁止处理他人 Issue
6. 禁止修改 `docs/guides/`、`.github/` 下的规程文件，除非 Issue 明确要求或人类指示
7. 禁止修改 `reference/` 目录（课程参考资料，只读），除非 Issue 明确要求
8. 一个 PR 只对应一个 Issue；禁止提交与 Issue 无关的改动
9. git 身份：Agent 必须使用 `agent-<名字>` 格式（如 `agent-claude`），名字由成员提供、协调者侧由协调者定；人类用真实名；禁止伪造他人身份。**Agent 首次在本仓库工作前自检**：`git config user.name` 为空或不以 `agent-` 开头 → 停下请成员输入名字，完成仓库级配置（`git config user.name`，`user.email` 用成员 GitHub 账号的 noreply 邮箱）后方可提交
10. 禁止提交：密钥 / 口令 / token、`node_modules/`、`target/` 等构建产物、临时文件

---

## 3. 分支模型与命名

| 分支 | 从哪拉 | 合入哪 | 生命周期 | 用途 |
|---|---|---|---|---|
| `main` | — | 接收 `dev` 的阶段合并 | 永久 | 稳定可演示版，每次合并打 tag |
| `dev` | `main` | 接收功能 PR | 永久 | 日常集成主干 |
| `feat/<Issue号>-<业务域>-<简述>` | `dev` | `dev` | 短 | 新功能 |
| `fix/<Issue号>-<简述>` | `dev` | `dev` | 短 | 缺陷修复 |
| `docs/<Issue号>-<主题>` / `chore/…` / `refactor/…` | `dev` | `dev` | 短 | 文档 / 工程 / 重构 |
| `hotfix/<Issue号>-<简述>` | **`main`** | **`main`** + `dev` | 短 | 紧急修复，仅 §8 允许后使用 |

命名规则：

- `<Issue号>` 为对应 Issue 的数字编号；示例：`feat/12-course-排课冲突检测`
- `<业务域>` 取以下之一：招生、课程、学习、测评、学情、教务、家校、运营、平台
- 分支名中的空格一律用 `-` 代替

---

## 4. 标准工作流

按 WF-1 → WF-6 顺序执行。每步核对完成判据，未达标不得进入下一步。

### WF-1 领取任务

```bash
gh issue list --assignee @me --state open
# 网页等价过滤器：is:issue is:open assignee:@me
```

- 只处理 assignee 是自己的 Issue；无任务时**停止并报告**，禁止自行认领他人 Issue 或新建 Issue
- 确认验收标准可检验、需求依据存在；有依赖 Issue 且未关闭 → 停止等待
- 记录 Issue 号（下文记作 `<N>`）

**完成判据**：已明确 Issue 目标、验收标准、范围边界。

### WF-2 创建分支

```bash
git switch dev
git pull origin dev
git switch -c <分支名>
```

**完成判据**：`git branch --show-current` 输出新分支名，工作区干净。

### WF-3 开发与提交

- 提交信息格式见 §5；每完成一个可运行、可验证的单元即提交一次
- `git add` 只加与 Issue 相关的文件，逐项核对 `git status`；确需批量添加时先审查完整清单
- 提交前自检：不包含硬性约束第 10 条所列内容

**完成判据**：验收标准逐条达成，提交历史干净、信息合规。

### WF-4 推送并创建 PR

```bash
git push -u origin <分支名>
gh pr create --base dev --title "<PR标题>" --body-file pr.md
# 无 gh CLI 时在网页创建，内容同样必须满足 §6
```

- PR 描述必须含 `Closes #<N>`，四要素模板见 §6，缺一不可

**完成判据**：PR 已创建、已关联 Issue、描述四要素齐全。

### WF-5 响应评审与合并

- 评审意见逐条回应：修改后追加提交（`fix(<scope>): 按评审意见修改<要点>`），并在 PR 评论中说明
- 评审通过（≥1 approve）后由评审人执行 **squash merge**；Agent 不得合并自己发起的 PR
- 合并后清理分支：

```bash
git push origin --delete <分支名>
git branch -D <分支名>
```

**完成判据**：PR 已 squash 合并进 `dev`，本地与远程分支已删除。

### WF-6 同步与收尾

```bash
git switch dev
git pull origin dev
```

- 确认 Issue 已随合并自动关闭；未关闭则手动关闭并附 PR 链接
- 向任务来源报告完成

### 阶段发布（仅获人类明确授权者执行）

```bash
git switch main && git pull origin main
git merge dev --no-ff
git tag v<X.Y>.0
git push origin main --tags
```

---

## 5. 提交信息格式

格式：`<type>(<scope>): <简述>`

| type | 用途 |
|---|---|
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `docs` | 文档 |
| `refactor` | 重构（不改变行为） |
| `test` | 测试 |
| `chore` | 构建、依赖、工程配置等杂项 |

规则：

- `<scope>` 用业务域（§3）或模块名；无明确归属可省略：`<type>: <简述>`
- 简述 ≤ 50 字，中文，陈述改了什么，结尾不加句号

| 正例 | 反例 |
|---|---|
| `feat(测评): 支持错题自动归组` | `update` |
| `fix(登录): 修复 token 过期后死循环` | `修改了一些文件` |
| `docs(workflow): 新增 git 工作流规程` | `feat: 完成所有功能`（范围失真） |

---

## 6. PR 标题与描述模板

标题：`<type>(<scope>): <简述>`，与主提交一致。

描述按以下模板逐项填写，不得留空：

```markdown
## 改了什么
<改动内容清单>

## 为什么 / 关联任务
Closes #<Issue号>
<一句话背景>

## 怎么自测的
<执行的验证步骤与结果，含运行的命令>

## 影响范围与风险
<波及的模块、需特别注意的点；无则写"无">

## A 组自检表
<设计/实现 PR 均须附：docs/guides/design-guide.md §8 A1–A5 逐条自检结果>
``

评审规则：

- ≥1 名其他协作者 approve 后方可合并；approve 资格：阶段一（设计）PR 由协调者审方向，阶段二（实现）PR 可由**任何非该模块的成员** approve
- 合并方式统一为 **squash merge**，squash 后的提交信息沿用 PR 标题
- PR 保持小而聚焦：建议有效变更 ≤ 400 行，超出则拆分 Issue 分批提交

---

## 7. 冲突与异常恢复

| 场景 | 处理 |
|---|---|
| 每次开工 | `git switch dev && git pull origin dev` 后再切工作分支 |
| push 被拒（分支落后） | `git fetch origin && git rebase origin/dev` → 解决冲突 → `git push`；**禁止 --force** |
| rebase 遇冲突 | 逐文件修改 → `git add <file>` → `git rebase --continue`；放弃：`git rebase --abort` |
| merge 遇冲突 | 同上逐文件解决；放弃：`git merge --abort` |
| 提交到错误的本地分支（未推送） | `git reset --soft HEAD~1` → 切到正确分支重新提交 |
| 改动需临时搁置 | `git stash` / `git stash pop` |
| 已推送的提交需要撤销 | 用 `git revert <hash>` 生成反向提交，**禁止 force push** |
| 密钥疑似已提交 | **立即报告人类**：第一动作作废并轮换密钥，第二动作清理历史（需 force push → Escalation）。仅 revert 不够，密钥已留在历史里 |

原则：历史修正只允许发生在自己未合并的分支上；提交一旦进入共享分支，只能 revert。

---

## 8. 必须请示人类的情形（Escalation）

出现下列任一情况，**停止操作，请示人类并等待指示**：

1. 需要 force push、删除共享分支、改写 `main`/`dev` 历史
2. 需要 hotfix（从 `main` 拉分支）
3. Issue 描述与实际冲突、验收标准无法检验、任务范围必须变更
4. 需要修改本规程（`docs/guides/`、`.github/` 下的文件）
5. 需要新建 Issue，或处理 assignee 不是自己的 Issue
6. 发现与其他模块设计或契约层的冲突（接缝问题，禁止自行裁定）
7. 涉及批量删除文件、依赖变更、数据库结构变更、密钥与配置变更
8. 任何本规程未覆盖的情况

---

## 9. 命令速查

| 目的 | 命令 |
|---|---|
| 领任务 | `gh issue list --assignee @me --state open` |
| 同步 dev | `git switch dev && git pull origin dev` |
| 建分支 | `git switch -c feat/<N>-<域>-<简述>` |
| 提交 | `git add <file> && git commit -m "feat(<域>): <简述>"` |
| 推送 | `git push -u origin <分支名>` |
| 建 PR | `gh pr create --base dev`（描述按 §6 模板补全） |
| 更新自己的分支 | `git fetch origin && git rebase origin/dev` |
| 合并后清理 | `git push origin --delete <分支名> && git branch -D <分支名>` |
| 撤销已推送提交 | `git revert <hash>` |
