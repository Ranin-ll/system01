-- =============================================================================
-- 模拟实操题库 演示数据（开发部门 dept_id=104）· 幂等：按题名判重
-- 对应设计稿「模拟考核 › 实操题库」的 3 个方向 / 15 题 / 最长 180 分钟
-- =============================================================================

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '用户登录接口开发', '后端接口开发', '适合接口设计、参数校验与异常处理训练', 'MEDIUM', 180,
       '完成登录接口与参数校验
梳理异常处理与统一返回结构
提交可运行的代码与说明文档', '接口符合 RESTful 命名规范
参数校验完整、返回结构统一', '.java 源码 + SQL 脚本 + .zip', 'MOCK_API-1_用户登录接口开发_v01.zip', 1,
       '实现用户名 + 密码登录接口：校验账号状态、签发 token、返回统一结构。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='用户登录接口开发' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '分页查询接口开发', '后端接口开发', '适合接口设计、参数校验与异常处理训练', 'MEDIUM', 150,
       '分页参数校验（pageNum/pageSize）
返回 total + rows 结构', '禁止一次性全表查询
分页参数越界时给默认值', '.java 源码 + .zip', 'MOCK_API-2_分页查询接口开发_v01.zip', 2,
       '为部门列表提供分页查询接口，支持关键字与状态筛选。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='分页查询接口开发' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '文件上传接口开发', '后端接口开发', '适合接口设计、参数校验与异常处理训练', 'EASY', 120,
       '限制单文件大小与后缀白名单
返回 { fileName, url } 结构', '路径必须落在上传根目录内，防目录穿越', '.java 源码 + .zip', 'MOCK_API-3_文件上传接口开发_v01.zip', 3,
       '实现附件上传接口，限制类型与大小，返回可访问的相对路径。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='文件上传接口开发' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '统一异常处理封装', '后端接口开发', '适合接口设计、参数校验与异常处理训练', 'HARD', 180,
       '业务异常返回可读提示
参数校验失败聚合全部字段错误', '不允许把堆栈直接返回给前端', '.java 源码 + .zip', 'MOCK_API-4_统一异常处理封装_v01.zip', 4,
       '用 @RestControllerAdvice 统一拦截业务异常与参数校验异常。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='统一异常处理封装' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '接口幂等与重试设计', '后端接口开发', '适合接口设计、参数校验与异常处理训练', 'HARD', 180,
       '给出幂等键设计
给出重复提交的判定与返回', '不得依赖前端去重
需说明并发场景下的行为', '.java 源码 + 设计说明.md + .zip', 'MOCK_API-5_接口幂等与重试设计_v01.zip', 5,
       '为提交类接口设计幂等方案，并说明客户端重试策略。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='接口幂等与重试设计' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '单表 CRUD 与 MyBatis 映射', '数据访问开发', '适合 SQL 编写、索引优化与 ORM 映射训练', 'EASY', 90,
       '四个操作各一个接口
实体与表字段映射完整', '使用 MyBatis-Plus 统一封装，不手写重复 SQL', '.java + .xml + SQL 脚本', 'MOCK_DB-1_单表CRUD与MyBatis映射_v01.zip', 6,
       '完成部门表的增删改查与 MyBatis-Plus 实体映射。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='单表 CRUD 与 MyBatis 映射' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '多表关联查询与分页', '数据访问开发', '适合 SQL 编写、索引优化与 ORM 映射训练', 'MEDIUM', 75,
       '返回字段包含部门名与岗位名
分页正确', '避免 N+1 查询', '.java + .xml', 'MOCK_DB-2_多表关联查询与分页_v01.zip', 7,
       '实现「实习生 + 部门 + 岗位」三表关联查询并分页。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='多表关联查询与分页' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '索引优化与慢查询分析', '数据访问开发', '适合 SQL 编写、索引优化与 ORM 映射训练', 'HARD', 100,
       '提交 EXPLAIN 前后对比
说明索引选择理由', '不得使用 SELECT *
给出回滚方案', '分析报告.md + SQL 脚本', 'MOCK_DB-3_索引优化与慢查询分析_v01.zip', 8,
       '针对给定慢查询，给出执行计划分析与索引方案。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='索引优化与慢查询分析' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '事务边界与回滚处理', '数据访问开发', '适合 SQL 编写、索引优化与 ORM 映射训练', 'MEDIUM', 60,
       '说明事务传播行为
给出部分失败时的处理策略', '禁止在事务中做远程调用', '.java 源码', 'MOCK_DB-4_事务边界与回滚处理_v01.zip', 9,
       '为「批量导入」场景设计事务边界并验证回滚。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='事务边界与回滚处理' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '数据字典与字段规范', '数据访问开发', '适合 SQL 编写、索引优化与 ORM 映射训练', 'EASY', 90,
       '输出字段清单（字段/类型/允许空/默认值/说明）', '命名统一小写下划线', '数据字典.xlsx', 'MOCK_DB-5_数据字典与字段规范_v01.zip', 10,
       '整理给定业务表的字段字典，标注类型、约束与默认值。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='数据字典与字段规范' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '列表页与分页组件', '前端页面开发', '适合组件拆分、表单校验与前后端联调训练', 'EASY', 90,
       '查询条件可重置
翻页保持查询条件
空态有提示', '表格列宽与溢出处理规范', '.vue 源码', 'MOCK_FE-1_列表页与分页组件_v01.zip', 11,
       '基于 Element-UI 实现带查询条件的列表页与分页。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='列表页与分页组件' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '表单校验与提交交互', '前端页面开发', '适合组件拆分、表单校验与前后端联调训练', 'MEDIUM', 120,
       '必填项与格式校验完整
提交按钮 loading 防重复提交', '校验规则集中在 data 中声明', '.vue 源码', 'MOCK_FE-2_表单校验与提交交互_v01.zip', 12,
       '实现新增/编辑表单，含必填校验、提交中状态与失败提示。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='表单校验与提交交互' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '弹窗增删改流程', '前端页面开发', '适合组件拆分、表单校验与前后端联调训练', 'EASY', 60,
       '删除需二次确认
操作成功/失败有明确提示', '弹窗复用同一组件，模式区分新增与编辑', '.vue 源码', 'MOCK_FE-3_弹窗增删改流程_v01.zip', 13,
       '实现弹窗式增删改完整闭环，含二次确认与操作反馈。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='弹窗增删改流程' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '图表数据对接与渲染', '前端页面开发', '适合组件拆分、表单校验与前后端联调训练', 'HARD', 150,
       '接口异常时图表降级为空态
数据为空时不报错', '图表容器需自适应宽度', '.vue 源码 + 截图', 'MOCK_FE-4_图表数据对接与渲染_v01.zip', 14,
       '对接后端统计接口，渲染柱状图与环形图。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='图表数据对接与渲染' AND `deleted`=0);

INSERT INTO `practice_subject`
  (`dept_id`,`title`,`direction`,`direction_desc`,`difficulty`,`estimated_minutes`,
   `deliverables`,`dev_constraints`,`submit_format`,`naming_rule`,`sort_no`,
   `content`,`attachments_json`,`reference_images`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
SELECT 104, '按钮级权限控制', '前端页面开发', '适合组件拆分、表单校验与前后端联调训练', 'MEDIUM', 120,
       '前端隐藏无权限按钮
后端仍要校验（前端隐藏不作为安全手段）', '权限标识统一 xxx:yyy:zzz 三段式', '.vue + .java 源码', 'MOCK_FE-5_按钮级权限控制_v01.zip', 15,
       '按角色控制按钮显示与接口权限，前后端双重校验。', '', '', 1, 'seed', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `practice_subject` WHERE `dept_id`=104 AND `title`='按钮级权限控制' AND `deleted`=0);

SELECT `direction`, COUNT(*) AS cnt, MAX(`estimated_minutes`) AS max_min
FROM `practice_subject` WHERE `dept_id`=104 AND `deleted`=0 GROUP BY `direction`;