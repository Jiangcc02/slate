# onboarding — 新成员入驻指引

> 面向：刚加入 slate 的成员（人类）。你负责模块设计，你的 Code Agent 负责按规程执行。
> 本文件只回答"我怎么开始"；流程细则全部指针到对应规程，不在本文重复。

## 1. 分工与节奏（30 秒版）

- 你是模块设计者：在模块边界内，设计由你做主；你的 Agent 负责领任务、写文档、发 PR
- 协调者负责：发任务 Issue、审设计方向（你的阶段一 PR 由他 approve）、管蓝图与契约
- 两阶段节奏：**阶段一交付设计文档**（纯文档活，零环境依赖）→ 方向过线后发**阶段二实现 Issue**（那时才需要按 [quickstart](../../quickstart.md) 起本地环境）

## 2. 一次性准备（你的电脑，约 10 分钟）

1. **GitHub 账号**：github.com/signup 注册（只需一个邮箱），把用户名发给协调者
2. **接受协作邀请**：打开 github.com/Jiangcc02/slate/invitations 点接受——不接受的账号无法被指派任务
3. **克隆仓库**：`git clone https://github.com/Jiangcc02/slate.git`
4. **装 GitHub CLI 并登录你自己的账号**：`gh auth login`——Agent 领任务靠它
5. **Agent 就位**：ZCode / Claude Code / Cursor 皆可，打开仓库目录——Agent 会自动读根目录 `AGENTS.md` 开始加载上下文

## 3. 跟你的 Agent 说什么

首次开工，一句话（复制即用）：

> 按仓库规程干活：先读根目录 AGENTS.md 和它的阅读动线，用 gh 领取指派给我的 open Issue（`gh issue list --assignee @me --state open`），完成阶段一设计，按 docs/guides/git-workflow.md 的 WF 流程发 PR 进 dev。

之后每个新任务只需一句：「按规程领取我的 Issue #N，做阶段一设计」。

## 4. 三个正常停顿（不是 bug）

| 你的 Agent 停下来… | 为什么 | 你要做的 |
|---|---|---|
| 问你叫什么名字（首次提交前） | 规程要求 Agent 以 `agent-<你的名字>` 身份提交（git-workflow §2.9） | 答一个名字，如"小王" → agent-xiaowang |
| 领了任务却不动某个 Issue | 该 Issue 依赖栏在等上游设计合并（WF-1 判据） | 不用管，先做能做的 |
| PR 发出后进入等待 | 评审是流程一环：阶段一 PR 由协调者审方向，approve 后才可 squash 合并 | 等评审；有意见按 WF-5 逐条回应再追加提交 |

## 5. 流程全景（你在哪一环）

领取 Issue → 写设计（`docs/design/<模块>/`）→ 发 PR（附 A 组自检表）→ 协调者审方向（B 组四条）→ squash 合并 = **方向锁定** → 阶段二实现 Issue → 实现 PR（非本模块成员 approve）→ 合并

细则指针：[git-workflow](git-workflow.md)（分支/提交/PR 红线 + WF 全流程）、[design-guide](design-guide.md)（设计文档怎么写、怎么被评审）。

## 6. 红线速览（完整版：iron-laws 与 git-workflow §2）

- 只做指派给自己的 Issue；不新建 Issue、不碰他人 Issue
- 不修改只读区：`reference/`、`docs/product/core.md`、`docs/architecture/`、`docs/guides/`（修改走 Escalation）
- 主干只收 PR：不直推 main/dev，不 force push，不删未合并分支
- 发现接缝冲突（要用的数据/接口与别的模块对不上）→ 停下报告协调者，禁止自行裁定

## 7. 卡住了怎么办

环境问题看 [quickstart](../../quickstart.md)，git 异常看 git-workflow §7；仍无解 → 找协调者。
