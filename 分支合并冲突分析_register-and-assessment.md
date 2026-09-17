# `feature/register-and-assessment` 合并冲突分析

分析对象：同事分支 `feature/register-and-assessment`（tip = `c5ba2ff`）
比对基准：`main`（`07fadf4`）与 `feature/online-learning-phase2`（`f3edac3`）
验证方式：`git merge-tree --write-tree --messages`（git 2.55.0），两种基准各跑一次

---

## 一、结论

**可以直接合。全量 90 个文件里，有且只有 1 个文件会冲突：`application.yml`。其余 89 个全部自动合并。**

两种基准跑出来的冲突集完全一致（都只有 `application.yml`），说明 phase2 是否已并进 main 不影响判定。

```
状态: diverged | ahead_by: 1 | behind_by: 6 | total_commits: 1
merge_base: db86816 "Initial project upload"（根提交）
改动量: 90 files changed, 7415 insertions(+), 26 deletions(-)
按状态: modified 21 / added 69 / deleted 0
```

同事是从**根提交**直接拉的分支，从没见过我们的 PR#1（注册审核工作流）和 PR#2（验证码 / 上传加固 / 在线学习 phase2），所以 `behind_by = 6`。

---

## 二、唯一冲突：`ruoyi-vue/ruoyi-admin/src/main/resources/application.yml`

`merge-tree` 给出三阶段条目，1 个 CONFLICT：

```
100644 8858a9bb... 1	ruoyi-vue/ruoyi-admin/src/main/resources/application.yml   ← base
100644 1b6e373d... 2	ruoyi-vue/ruoyi-admin/src/main/resources/application.yml   ← ours(我方)
100644 7603cefe... 3	ruoyi-vue/ruoyi-admin/src/main/resources/application.yml   ← theirs(同事)
CONFLICT (content): Merge conflict in .../application.yml
```

### 冲突内容（4 处）

| # | 配置项 | 我方（main / phase2） | 同事 |
|---|---|---|---|
| 1 | `ruoyi.profile` | `${RUOYI_PROFILE:E:/ia-dev/uploads}` | `D:/vscodeworkspace/system01/uploads` |
| 2 | `spring.servlet.multipart.max-file-size` | `${UPLOAD_MAX_FILE_SIZE:500MB}` | `1024MB` |
| 3 | `spring.servlet.multipart.max-request-size` | `${UPLOAD_MAX_REQUEST_SIZE:510MB}` | `1024MB` |
| 4 | `upload:` 配置块 | 我方**新增**的整块（见下） | 无（他基于根提交，从没有这块） |

我方新增的块：

```yaml
# 课程视频允许大文件；其他 multipart 接口保持原有 20MB 请求上限。
upload:
  default-max-request-size: ${DEFAULT_UPLOAD_MAX_REQUEST_SIZE:20MB}
  course-max-request-size: ${COURSE_UPLOAD_MAX_REQUEST_SIZE:510MB}
```

### 为什么第 4 处要特别小心

`ruoyi-framework/src/main/java/com/ruoyi/framework/security/filter/MultipartRequestSizeFilter.java`（phase2 新增，我方的）用 `@Value` 读的就是这两个键：

```java
@Value("${upload.default-max-request-size:20MB}")
@Value("${upload.course-max-request-size:510MB}")
```

因为有 `:` 默认值兜底，**即使 `upload:` 整块被删掉，服务也不会启动失败**——只会静默退化成 20MB / 510MB，课程视频上传能力被悄悄砍掉。这种「不报错的坏掉」比编译失败更难发现，所以必须人工保住这块。

### 推荐解法

保留我方的「环境变量参数化」写法，同时吸收同事的 1G 需求：

```yaml
ruoyi:
  profile: ${RUOYI_PROFILE:E:/ia-dev/uploads}

spring:
  servlet:
    multipart:
      max-file-size: ${UPLOAD_MAX_FILE_SIZE:1024MB}
      max-request-size: ${UPLOAD_MAX_REQUEST_SIZE:1024MB}

upload:
  default-max-request-size: ${DEFAULT_UPLOAD_MAX_REQUEST_SIZE:1024MB}
  course-max-request-size: ${COURSE_UPLOAD_MAX_REQUEST_SIZE:1024MB}
```

> **不要采用同事的 `profile: D:/vscodeworkspace/system01/uploads`** —— 那是他本机的绝对路径，在你的 E 盘环境上会导致上传目录找不到。
> 同理，`dev-env.ps1` 里他把 `JAVA_HOME` 改成了 `D:\java\java8`，合并后要按你本机改回。

---

## 三、自动合并 / 静默处理，但**语义有风险**的文件

`merge-tree` 只报出 1 个文本冲突，但下面这些文件**双方都改过**，git 是靠「改在不相邻区域」自动合上的 —— **文本不冲突 ≠ 逻辑正确**。

`merge-tree` 明确列出做了三方合并的 4 个：

- `ruoyi-business/.../service/impl/InternAuthServiceImpl.java`
- `ruoyi-ui/src/router/index.js`
- `ruoyi-ui/src/views/index.vue`
- `ruoyi-ui/src/views/register.vue`

其余双方都改过、被 git 静默处理的（来自交集比对，共 17 个文件里剩下的）：

- `ruoyi-business/.../controller/RegisterApplicationController.java`
- `ruoyi-business/.../service/IRegisterApplicationService.java`
- `ruoyi-business/.../service/impl/RegisterApplicationServiceImpl.java`
- `ruoyi-business/.../mapper/InternAuthMapper.java`
- `ruoyi-business/.../mapper/RegisterApplicationMapper.java` + `RegisterApplicationMapper.xml`
- `ruoyi-system/.../mapper/SysUserMapper.java` + `SysUserMapper.xml`
- `ruoyi-system/.../service/ISysUserService.java`
- `ruoyi-system/.../service/impl/SysUserServiceImpl.java`
- `ruoyi-ui/src/api/business/register.js`

**风险根因**：同事的注册链路代码是基于根提交写的，我们 PR#1 加过「注册审核工作流 + 导师字段（mentor_name/mentor_phone）」，PR#2 加过验证码开关。两边改的是同一批文件的不同段落，git 合得上，但合出来的逻辑没人验证过。

**合并后必须回归的链路**：注册页提交 → 状态查询 → 部门管理员审核通过 → 自动建实习生账号（含导师字段写入）→ 首登保密协议拦截。这条链路上任何一环断掉，都会表现为「页面正常但账号建不出来 / 导师为空」。

---

## 四、合并前必须先处理的本地状态

当前工作区（`git status`）：

```
 M ruoyi-vue/ruoyi-ui/src/views/index.vue      ← 62 行未提交改动
?? .workbuddy/
?? 实习生端整体设计稿.html
?? 实习生端目录与页面布局设计方案.md
?? 超管端整体设计稿.html
```

- `views/index.vue` 有 **62 行未提交改动**，而同事也改了这个文件（+4/-3）。**必须先 commit 或 stash**，否则 merge 会被拒绝，或者把你的改动卷进冲突里。
- `.workbuddy/` 建议先加进 `.gitignore`（同事的 `.gitignore` 只加了 `uploads/`、`logs/`，与 main 无冲突，可放心自动合并）。

### 建议执行顺序

```bash
# 1. 先固化本地改动，避免卷入合并
git add -A && git commit -m "docs: 设计稿 + 工作台入口调整"

# 2. 确保拿到同事分支
git fetch origin feature/register-and-assessment

# 3. 执行合并（只有 application.yml 会冲突）
git merge origin/feature/register-and-assessment

# 4. 按第二节的「推荐解法」修 application.yml，然后
git add ruoyi-vue/ruoyi-admin/src/main/resources/application.yml
git commit

# 5. 验证
mvn -q -pl ruoyi-business -am compile      # 后端编译
cd ruoyi-vue/ruoyi-ui && npm run build      # 前端构建
```

> 若想先低风险试水，可 `git merge --no-commit --no-ff origin/feature/register-and-assessment`，解完冲突先不提交，本地起服务点一遍链路再 `git commit`。

---

## 五、附：同事分支带来的新增能力（供了解，不影响合并判定）

- 后端：Question / QuestionBank / Exam / AnswerSheet / PracticeSubject / PracticeRecord 全套 Controller-Domain-Mapper-Service + 7 个 Mapper XML
- 前端新增页：`views/business/questionBank`、`views/business/exam`（列表 + 批改）、`views/business/practiceSubject`、`views/assessment/exam`、`views/assessment/practice`（列表 / record / subject / subjectDetail）
- 新增 API：`api/business/{question,questionBank,exam,practice,practiceSubject}.js`
- SQL 新增 7 个：`intern_assessment_question_bank.sql`、`_question_bank_menu.sql`、`_exam_module.sql`、`_exam_menu.sql`、`_practice_subject.sql`、`_practice_record_item.sql`、`_seed_questions.sql`
- 其余：`AgreementGate.vue` 协议拦截强化、`practice-module.scss`、题库 Excel 模板 2 个

全部为**新增文件**，与我们已合并的内容零覆盖 —— 这也是为什么 90 个文件里只有 1 个冲突。
