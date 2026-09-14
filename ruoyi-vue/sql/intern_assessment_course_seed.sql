-- 实习生学习考核系统：课程初始数据
-- 可重复执行，不删除管理员已经创建的课程、章节或学习记录。
-- 课程按“一个部门对应一个岗位”初始化，每个岗位提供一门已发布课程和一门草稿课程。

USE `intern_assessment`;

START TRANSACTION;

INSERT INTO course
    (course_name, position_id, course_type, is_required, intro, status, published_at, create_by, create_time, update_time, deleted)
SELECT '项目实施与交付规范', p.id, 'THEORY', 1,
       '掌握项目实施流程、交付标准与现场协作规范。', 'PUBLISHED', NOW(), 'seed', NOW(), NOW(), 0
FROM position p
WHERE p.position_code = 'IMPLEMENTATION' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM course c WHERE c.course_name = '项目实施与交付规范' AND c.position_id = p.id AND c.deleted = 0);

INSERT INTO course
    (course_name, position_id, course_type, is_required, intro, status, published_at, create_by, create_time, update_time, deleted)
SELECT '交付工具基础操作', p.id, 'PRACTICE', 0,
       '熟悉交付资料整理、现场记录和成果提交工具。', 'DRAFT', NULL, 'seed', NOW(), NOW(), 0
FROM position p
WHERE p.position_code = 'IMPLEMENTATION' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM course c WHERE c.course_name = '交付工具基础操作' AND c.position_id = p.id AND c.deleted = 0);

INSERT INTO course
    (course_name, position_id, course_type, is_required, intro, status, published_at, create_by, create_time, update_time, deleted)
SELECT '开发流程与代码交付规范', p.id, 'THEORY', 1,
       '从需求理解、分支协作到代码交付，建立统一的开发工作方法。', 'PUBLISHED', NOW(), 'seed', NOW(), NOW(), 0
FROM position p
WHERE p.position_code = 'DEVELOPMENT' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM course c WHERE c.course_name = '开发流程与代码交付规范' AND c.position_id = p.id AND c.deleted = 0);

INSERT INTO course
    (course_name, position_id, course_type, is_required, intro, status, published_at, create_by, create_time, update_time, deleted)
SELECT '开发工具与项目环境', p.id, 'PRACTICE', 0,
       '了解项目本地环境、调试工具和基础协作约定。', 'DRAFT', NULL, 'seed', NOW(), NOW(), 0
FROM position p
WHERE p.position_code = 'DEVELOPMENT' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM course c WHERE c.course_name = '开发工具与项目环境' AND c.position_id = p.id AND c.deleted = 0);

INSERT INTO course
    (course_name, position_id, course_type, is_required, intro, status, published_at, create_by, create_time, update_time, deleted)
SELECT '设计规范与成果交付', p.id, 'THEORY', 1,
       '统一设计规范、交付格式和评审协作流程。', 'PUBLISHED', NOW(), 'seed', NOW(), NOW(), 0
FROM position p
WHERE p.position_code = 'DESIGN' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM course c WHERE c.course_name = '设计规范与成果交付' AND c.position_id = p.id AND c.deleted = 0);

INSERT INTO course
    (course_name, position_id, course_type, is_required, intro, status, published_at, create_by, create_time, update_time, deleted)
SELECT '设计工具与标注协作', p.id, 'PRACTICE', 0,
       '练习设计工具、标注规范和多人协作交付。', 'DRAFT', NULL, 'seed', NOW(), NOW(), 0
FROM position p
WHERE p.position_code = 'DESIGN' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM course c WHERE c.course_name = '设计工具与标注协作' AND c.position_id = p.id AND c.deleted = 0);

INSERT INTO course
    (course_name, position_id, course_type, is_required, intro, status, published_at, create_by, create_time, update_time, deleted)
SELECT '质量检查与缺陷处理规范', p.id, 'THEORY', 1,
       '掌握检查清单、缺陷分级和复测闭环。', 'PUBLISHED', NOW(), 'seed', NOW(), NOW(), 0
FROM position p
WHERE p.position_code = 'QA' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM course c WHERE c.course_name = '质量检查与缺陷处理规范' AND c.position_id = p.id AND c.deleted = 0);

INSERT INTO course
    (course_name, position_id, course_type, is_required, intro, status, published_at, create_by, create_time, update_time, deleted)
SELECT '缺陷跟踪与复测流程', p.id, 'PRACTICE', 0,
       '熟悉缺陷记录、定位、修复验证与关闭流程。', 'DRAFT', NULL, 'seed', NOW(), NOW(), 0
FROM position p
WHERE p.position_code = 'QA' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM course c WHERE c.course_name = '缺陷跟踪与复测流程' AND c.position_id = p.id AND c.deleted = 0);

INSERT INTO course
    (course_name, position_id, course_type, is_required, intro, status, published_at, create_by, create_time, update_time, deleted)
SELECT '建模流程与文件规范', p.id, 'THEORY', 1,
       '建立模型制作、命名和文件交付标准。', 'PUBLISHED', NOW(), 'seed', NOW(), NOW(), 0
FROM position p
WHERE p.position_code = 'MODELING' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM course c WHERE c.course_name = '建模流程与文件规范' AND c.position_id = p.id AND c.deleted = 0);

INSERT INTO course
    (course_name, position_id, course_type, is_required, intro, status, published_at, create_by, create_time, update_time, deleted)
SELECT '模型检查与交付标准', p.id, 'PRACTICE', 0,
       '练习模型检查、问题修正和最终成果交付。', 'DRAFT', NULL, 'seed', NOW(), NOW(), 0
FROM position p
WHERE p.position_code = 'MODELING' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM course c WHERE c.course_name = '模型检查与交付标准' AND c.position_id = p.id AND c.deleted = 0);

-- 为初始课程补齐可直接查看的章节和资料。后续管理员新增内容由课程内容接口接管。
INSERT INTO course_chapter (course_id, chapter_name, sort_no, is_required, create_time, update_time, deleted)
SELECT c.id, '第一章 入职与岗位规范', 1, 1, NOW(), NOW(), 0
FROM course c
WHERE c.deleted = 0
  AND c.create_by = 'seed'
  AND NOT EXISTS (SELECT 1 FROM course_chapter cc WHERE cc.course_id = c.id AND cc.chapter_name = '第一章 入职与岗位规范' AND cc.deleted = 0);

INSERT INTO course_chapter (course_id, chapter_name, sort_no, is_required, create_time, update_time, deleted)
SELECT c.id, '第二章 协作流程与质量要求', 2, 1, NOW(), NOW(), 0
FROM course c
WHERE c.deleted = 0
  AND c.create_by = 'seed'
  AND NOT EXISTS (SELECT 1 FROM course_chapter cc WHERE cc.course_id = c.id AND cc.chapter_name = '第二章 协作流程与质量要求' AND cc.deleted = 0);

INSERT INTO study_item
    (chapter_id, item_title, item_type, duration, is_required, completion_rule, sort_no, create_time, update_time, deleted)
SELECT cc.id, CONCAT(c.course_name, '说明'), 'DOC', 18, 1, 'SCROLL_END', 1, NOW(), NOW(), 0
FROM course_chapter cc
INNER JOIN course c ON c.id = cc.course_id
WHERE c.deleted = 0 AND c.create_by = 'seed' AND cc.chapter_name = '第一章 入职与岗位规范'
  AND NOT EXISTS (SELECT 1 FROM study_item si WHERE si.chapter_id = cc.id AND si.item_title = CONCAT(c.course_name, '说明') AND si.deleted = 0);

INSERT INTO study_item
    (chapter_id, item_title, item_type, duration, is_required, completion_rule, sort_no, create_time, update_time, deleted)
SELECT cc.id, '岗位资料安全操作演示', 'VIDEO', 14, 1, 'PLAY_TO_END', 2, NOW(), NOW(), 0
FROM course_chapter cc
INNER JOIN course c ON c.id = cc.course_id
WHERE c.deleted = 0 AND c.create_by = 'seed' AND cc.chapter_name = '第一章 入职与岗位规范'
  AND NOT EXISTS (SELECT 1 FROM study_item si WHERE si.chapter_id = cc.id AND si.item_title = '岗位资料安全操作演示' AND si.deleted = 0);

INSERT INTO study_item
    (chapter_id, item_title, item_type, duration, is_required, completion_rule, sort_no, create_time, update_time, deleted)
SELECT cc.id, '流程检查清单', 'DOC', 20, 1, 'SCROLL_END', 1, NOW(), NOW(), 0
FROM course_chapter cc
INNER JOIN course c ON c.id = cc.course_id
WHERE c.deleted = 0 AND c.create_by = 'seed' AND cc.chapter_name = '第二章 协作流程与质量要求'
  AND NOT EXISTS (SELECT 1 FROM study_item si WHERE si.chapter_id = cc.id AND si.item_title = '流程检查清单' AND si.deleted = 0);

INSERT INTO study_item
    (chapter_id, item_title, item_type, duration, is_required, completion_rule, sort_no, create_time, update_time, deleted)
SELECT cc.id, '章节自测', 'QUIZ', 10, 1, 'QUIZ_SUBMIT', 2, NOW(), NOW(), 0
FROM course_chapter cc
INNER JOIN course c ON c.id = cc.course_id
WHERE c.deleted = 0 AND c.create_by = 'seed' AND cc.chapter_name = '第二章 协作流程与质量要求'
  AND NOT EXISTS (SELECT 1 FROM study_item si WHERE si.chapter_id = cc.id AND si.item_title = '章节自测' AND si.deleted = 0);

COMMIT;

-- Navicat 核验：每个岗位应有两门初始课程。
SELECT d.dept_name, p.position_name, c.course_name, c.status,
       (SELECT COUNT(1) FROM course_chapter cc WHERE cc.course_id = c.id AND cc.deleted = 0) AS chapter_count
FROM course c
INNER JOIN position p ON p.id = c.position_id
INNER JOIN dept_position dp ON dp.position_id = p.id AND dp.status = 1
INNER JOIN sys_dept d ON d.dept_id = dp.dept_id
WHERE c.deleted = 0 AND c.create_by = 'seed'
ORDER BY dp.dept_id, c.id;
