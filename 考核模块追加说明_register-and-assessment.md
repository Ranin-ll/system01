# 只追加「考核模块」操作说明

来源分支：`feature/register-and-assessment`（tip `c5ba2ff`）
操作范围：**只取题库 / 模拟考核 / 正式考核 / 批改相关改动，完全绕开注册、报名、保密协议域。**
执行状态（2026-09-16 18:20 更新）：代码已提交为 **`c7d00cd`**；**8 步 SQL 已全部执行成功（0 失败）**；后端已全量重建并启动；「模拟考核」端到端已跑通（详见文末〈执行记录〉）。

---

## 一、结论：可以，而且边界非常干净

同事这次提交的 90 个文件里，**69 个是纯新增、21 个是修改**。考核相关的代码**几乎全部落在"纯新增"里**，所以能整块拿走而不碰注册域。

我做了三项依赖验证，结果都是零耦合：

| 验证项 | 结果 |
|---|---|
| 新增 Java 有没有 import 注册域类（Register / InternAuth / SysUser） | **0 处** |
| 新增 Java 有没有 import main 上已有的类 | **0 处**（只 import 同一批新增类） |
| 新增前端页有没有反向依赖 `register.js` / `AgreementGate` / `views/index.vue` | **0 处**（只依赖本模块新增的 5 个 api） |

也就是说：**这是一个自包含模块**，拿走它不会牵动任何已合并的东西。

---

## 二、实际追加内容（68 个文件）

| 分类 | 数量 | 说明 |
|---|---|---|
| 后端 Controller / Domain / Mapper / Service | 37 | AnswerSheet、Exam、Practice、PracticeSubject、QuestionBank、Question 六套 |
| Mapper XML | 7 | AnswerSheet / Exam / PracticeRecord / PracticeRecordItem / PracticeSubject / QuestionBank / Question |
| 前端 API | 5 | `api/business/{exam,practice,practiceSubject,question,questionBank}.js` |
| 前端样式 | 1 | `assets/styles/practice-module.scss` |
| 前端页面 | 9 | 实习生端 6（考核 / 模拟列表 / 回顾 / 实操练习 / 实操详情）+ 管理端 4（考核管理 / 答卷批改 / 题库管理 / 模拟实操管理）※ 9 = 5+4 中新增目录去重后计 |
| SQL 脚本 | 7 | 建表 / 改表 / 菜单 / 种子题 |
| 题库模板 | 2 | `题库导入-48题.xlsx`、`题目导入模板-示例.xlsx` |

**唯一跳过的"新增"文件**：`RegisterStatusQueryBody.java` —— 属注册域，且 main 上已有**完全相同内容**的文件，取过来只会造成 add/add 噪音。

---

## 三、必须手工处理的两个共享文件（不能整文件覆盖）

这两个文件同事也"新增"了改动，但它们同时是我们已合并内容的战场：

### 1. `ruoyi-ui/src/router/index.js` —— ⚠️ 绝对不要整体 checkout

同事的分支是从**根提交**拉出来的，他这份 router 里包含一处对我们的**回退**：

```
       path: 'course',
       name: 'BusinessCourse',
-      component: () => import('@/views/business/course/runtime'),   ← 我方 phase2
+      component: () => import('@/views/business/course/index'),     ← 同事的旧版本
```

如果整体覆盖，**课程学习页（runtime）会打不开**。所以我只手工追加了考核相关路由，`course/runtime` 原样保留：

- 实习生端：`exam`（参与考核）、`mock-exam` 指向改到 `assessment/practice/index`、`mock-exam/record/:recordId`、`practice-subject`、`practice-subject/:id`
- 管理端：`assessment/department/exam`（考核管理，perms `business:bank:list`）+ 隐藏路由 `exam/grading`（答卷批改）

### 2. `ruoyi-ui/src/permission.js` —— 追加 1 行

`formalInternBlockedPaths` 增加 `'/assessment/intern/practice-subject'`（带原注释）。这个改动相对 main 是纯追加，无风险。

> 题库管理 / 模拟实操管理这两个页面**不在 router 里**——它们走若依的**菜单动态加载**，由 SQL 里的 `component` 字段指定（`business/questionBank/index`、`business/practiceSubject/index`）。所以菜单 SQL 必须执行，否则页面无入口。

---

## 四、同事的 SQL 有 3 类问题，跑之前必须补

这是本次追加最需要注意的部分。**同事的代码和随附 DDL 明显不同步**——他在本机手工加过字段，但漏提交了脚本。

### 问题 1：11 个字段"代码在用、脚本没建"

我用「Mapper 的 INSERT / UPDATE 列 vs 建表脚本列」做了一次全量比对，缺 11 个：

| 表 | 缺失列 | 来源（谁在用） |
|---|---|---|
| `exam` | `bank_id` | `ExamMapper.xml` INSERT/SELECT、`Exam.java` |
| `exam` | `single_count` / `multi_count` / `judge_count` | `ExamMapper.xml` resultMap + INSERT |
| `exam` | `single_score` / `multi_score` / `judge_score` | 同上 |
| `question_bank` | `bank_type` | `QuestionBankMapper.xml` resultMap / SELECT / INSERT，且 `selectPracticeBankByDept` 用 `WHERE b.bank_type='PRACTICE'` |
| `question` | `attachment_url` | `QuestionMapper.xml` INSERT/UPDATE、`Question.java` |
| `answer_sheet` | `manual_score` / `manual_comment` | `AnswerSheetMapper.xml` INSERT/UPDATE（注意：`answer_sheet_item` 上的同名字段是**逐题**的，不可混用） |

**造成的后果**：
- `bank_type` 缺失 → **题库管理页打开就报 `Unknown column 'b.bank_type'`**，模拟考核找不到本部门题库
- `exam.bank_id` 缺失 → `intern_assessment_exam_module.sql` 的 `ADD COLUMN ... AFTER bank_id` **直接报 `ERROR 1054`**
- 其余 → 新增考核 / 答卷批改的写操作全部抛异常

### 问题 2：`seed_questions.sql` 的题库行不存在

种子脚本硬编码 `bank_id = 2` 和 `3`（各 24 题，共 48 题），但 `intern_assessment_question_bank.sql` **只建空表，没有任何脚本插入这两行**。缺行会导致题目 `bank_id` 悬空，前端按题库筛选时**一道题都看不到**。

### 问题 3：两个脚本不可重复执行

- `intern_assessment_question_bank.sql`：`DROP TABLE IF EXISTS question_bank`（**破坏性**）+ 无守卫的 `ALTER`（含 `DROP INDEX uk_question_no`）
- `intern_assessment_exam_module.sql`：全部是无守卫的 `ALTER TABLE ... ADD COLUMN`

→ 只能执行一次；重复执行必报错。若要重跑，先手动回滚或改写成幂等。

### 我提供的补丁

新增 `ruoyi-vue/sql/intern_assessment_exam_bridge.sql`，**幂等**（全部经 `information_schema` 判定后再执行，复用你项目"先查后改"的约定），一次性补齐：

1. `exam` 的 7 个列（含 `bank_id`，带 `idx_exam_bank` 索引）
2. `question.attachment_url`
3. `answer_sheet.manual_score` / `manual_comment`
4. `question_bank.bank_type`（带 `idx_bank_type`）
5. 两个示例题库行（id 2 = `开发部门模拟题库` PRACTICE、id 3 = `开发部门正式题库` FORMAL）—— **名称 / 归属部门可按实际改**，脚本里有注释标明
6. 文末附「为其余 4 个部门补建 PRACTICE 题库」的注释块（`selectPracticeBankByDept` 按实习生所属部门找 PRACTICE 题库，缺行的部门进模拟考核会提示找不到题库）

### 正确执行顺序

```
1. intern_assessment_question_bank.sql        ← 建 question_bank、给 question 加 bank_id（破坏性，只跑一次）
2. intern_assessment_exam_bridge.sql          ← 本次新增的补丁（幂等）
3. intern_assessment_exam_module.sql          ← 补 exam.question_count/subject_*/exam_mode
4. intern_assessment_practice_subject.sql     ← 幂等
5. intern_assessment_practice_record_item.sql ← 幂等
6. intern_assessment_seed_questions.sql       ← 48 道种子题
7. intern_assessment_question_bank_menu.sql   ← 题库菜单 + 角色权限
8. intern_assessment_exam_menu.sql            ← 考核管理菜单 + 角色权限
```

> 补丁必须夹在 1 和 3 之间：它替 `exam_module.sql` 补上其 `AFTER bank_id` 所依赖的列，同时 `question_bank.bank_type` 又必须在建表之后才能加。

菜单 SQL 我读过全文，写得**很规范**：全部基于变量查询 + `NOT EXISTS` 守卫，**不硬编码 menu_id**，所以不会和你已有的菜单脚本撞 ID。

---

## 五、验收清单

- [x] **后端 `mvn -DskipTests compile` 已通过** —— 8 个模块全 SUCCESS，`ruoyi-business` 编译 75 个源文件（含新增 37 个），耗时 23.9s
- [x] 新增/修改的 JS 语法检查通过（`node --check`：router、permission、5 个 api、practice-mixin）
- [x] router 新增的 15 个 import 目标文件均存在
- [ ] 前端 `npm run build` 通过
- [ ] SQL 按上面 8 步顺序执行，每步看验证 SELECT 输出
- [ ] 启动后用部门管理员登录 → 「题库管理」能打开、能建题库、能导入 `题库导入-48题.xlsx`
- [ ] 「考核管理」建考核 → 发布 → 实习生端「参与考核」能作答 → 「答卷批改」能打分
- [ ] 实习生端「模拟考核」能看到题目（依赖 PRACTICE 题库的部门匹配）
- [ ] 回归确认：**课程学习页（course/runtime）仍然可打开** ← 验证 router 没被回退

## 六、未包含（按需再说）

以下改动我**没有**拿进来，因为它们属于注册 / 报名 / 保密协议域，且与已合并内容冲突最密集：

- `register.vue`（注册页，同事 +144/-2）
- `AgreementGate.vue`（协议拦截，同事 +88/-8）
- `views/index.vue`（工作台入口）
- `RegisterApplicationController` / `RegisterApplicationServiceImpl` / `IRegisterApplicationService`
- `InternAuthMapper` / `InternAuthServiceImpl`
- `RegisterApplicationMapper(.java/.xml)` / `api/business/register.js`
- `SysUserMapper(.java/.xml)` / `ISysUserService` / `SysUserServiceImpl`
- `application.yml` / `dev-env.ps1` / `.gitignore` / `pom.xml`

其中若你要用到「实操题附件上传 1G」，需要单独把 `application.yml` 的 `max-file-size` 调到 1024MB —— 但这会撞上第四轮分析里那处唯一冲突，建议单独处理，别混在这次追加里。

---

# 七、执行记录（2026-09-16 18:20）

## 7.1 触发问题
前端点「开始模拟考核」弹 **系统接口404异常**。

## 7.2 根因（两层，缺一层都跑不起来）
1. **运行中的后端是旧构建**：进程启动于 10:20:49，而考核模块代码 17:11 后才落盘 → 后端没有考核域 Controller。实测 `/business/course/list`、`/business/register/list` 正常（200），而 `/business/practice/*`、`/business/question-bank/*`、`/business/exam/*` 全部 404。
   其中偶发的 500 是 Tomcat 读 jar 的 `NullPointerException`，堆栈落在 **`ResourceHttpRequestHandler`（静态资源兜底）** —— 反证该 URL 没有匹配到任何 Controller。
2. **数据库一条迁移都没跑过**：`question_bank` / `practice_record` / `practice_record_item` / `practice_subject` 表不存在，11 个列全缺，菜单与角色授权全无。

## 7.3 已执行
- 迁移前表结构备份 → `ruoyi-vue/sql/_backup_before_exam_migration.sql`
- **8 步 SQL 按序执行，0 失败**（执行前已核对 `uk_question_no` / `submit_type` / `exam_type` 等依赖存在，涉及表全为 0 行）
- 修正 `question_bank.question_count` 冗余计数：0 → 24
- 后端全量重建（删各模块 `target` → 重建 `target/classes` → `mvn -DskipTests package`，8 模块 SUCCESS）并重启

## 7.4 ⚠️ 两个必须记住的环境坑
1. **本项目不能用 `clean`**：根 `pom.xml` 把 `maven-compiler-plugin` 钉在 **3.1**，它不会自建 `target/classes`；而 `ruoyi-framework` 没有 `src/main/resources`，`maven-resources-plugin` 也会 skip 建目录 → `clean package` 必报「系统找不到指定的路径」。
   **→ 日常用 `mvn -DskipTests package`（不带 clean）**；VS Code 任务「后端：编译打包」因带 `clean` 而一直失败，这正是曾出现半成品 jar 的原因。
   **建议（改共享文件 `pom.xml`，需你确认）**：`<version>3.1</version>` → `3.11.0`。
2. **半成品编译会导致启动崩溃**：症状是启动抛 `NullPointerException at AbstractAspectJAdvice.configurePointcutParameters`（AOP 参数名绑定失败）。这是编译产物的锅，**不是切面代码的问题**（`ruoyi-framework` 本次一行未改）。修法即上面的全量重建。

## 7.5 端到端验证结果（预备实习生 `test_dev_intern`，开发部门 104）
| 步骤 | 结果 |
|---|---|
| `POST /business/practice/start` | 200，题库「开发部门模拟题库」，**10 题** |
| `POST /business/practice/submit` | 200，得分 3/10，逐题回顾 10 条（含正确答案与解析） |
| `GET /business/practice/my` | 200，1 条记录（含成绩、题库名、部门） |
| `GET /business/practice/record/1` | 200，返回 `{record, items[10]}`，与前端 `record.vue` 读取的字段完全一致 |
| `GET /business/question-bank/list` | 200（2 个题库） |
| `GET /business/question/list` | 200（48 题） |

> 说明：`/business/answer-sheet/list` 返回 404 属**正常**，该 Controller 没有 `/list` 映射（只有 `/start/{examId}`、`/submit`、`/my`、`/grading/{examId}`、`/detail/{sheetId}`、`/grade`、`/publish/{examId}`）。

## 7.6 遗留
- 正式实习生**不**参与模拟考核（按你的选择保持现状）；菜单 SQL 已移除其授权。
- `CourseServiceImpl:143`、`CourseContentServiceImpl:323` 仍保留「超管只读」拦截，与你正在推进的「超管可写」口径未对齐。
- 后端当前由工具会话托管；若需长期驻留，请用 VS Code 任务「后端：启动 8080」重启。
