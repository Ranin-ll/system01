-- ============================================================================
-- 实习生学习考核系统 - 若依融合版数据库初始化脚本
-- 策略：沿用若依系统表（sys_dept/sys_user/sys_role）作为权限框架
--       扩展 sys_user 添加业务字段（账号状态机/岗位/导师/协议状态）
--       业务表保持独立设计，通过 user_id 关联
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `intern_assessment` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `intern_assessment`;

-- ============================================================================
-- 第一步：执行若依系统表（已包含在 sql/ry_20240629.sql 和 sql/quartz.sql）
-- 请在 MySQL 中先执行这两个文件，或取消下面注释执行
-- ============================================================================

-- source sql/ry_20240629.sql
-- source sql/quartz.sql

-- ============================================================================
-- 第二步：扩展若依 sys_user 表，添加业务字段
-- ============================================================================

ALTER TABLE `sys_user`
    ADD COLUMN `position_id` BIGINT DEFAULT NULL COMMENT '岗位类型ID（关联position表）' AFTER `dept_id`,
    ADD COLUMN `mentor_name` VARCHAR(64) DEFAULT NULL COMMENT '导师姓名（审核时人工登记，仅作为实习生档案信息）' AFTER `position_id`,
    ADD COLUMN `mentor_phone` VARCHAR(32) DEFAULT NULL COMMENT '导师联系方式（审核时人工登记）' AFTER `mentor_name`,
    ADD COLUMN `user_status` VARCHAR(24) NOT NULL DEFAULT 'WAIT_AUDIT' COMMENT '账号状态:WAIT_AUDIT待审核/REJECTED已驳回/PRE_TRAINEE预备/FORMAL_TRAINEE正式/PENDING_PROMOTE待转正/DISABLED禁用/ARCHIVED离职归档' AFTER `status`,
    ADD COLUMN `protocol_status` TINYINT NOT NULL DEFAULT 0 COMMENT '协议签署状态:0未签 1已签' AFTER `user_status`,
    ADD COLUMN `expected_entry_date` DATE DEFAULT NULL COMMENT '预计入职时间（非必填）' AFTER `protocol_status`,
    ADD COLUMN `id_card` VARCHAR(64) DEFAULT NULL COMMENT '身份证号（加密/脱敏存储）' AFTER `expected_entry_date`,
    ADD COLUMN `is_dept_admin` TINYINT NOT NULL DEFAULT 0 COMMENT '是否部门管理员' AFTER `id_card`,
    ADD COLUMN `fail_count` INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数' AFTER `is_dept_admin`,
    ADD INDEX `idx_user_position` (`position_id`),
    ADD INDEX `idx_user_status` (`user_status`),
    ADD INDEX `idx_user_dept_status` (`dept_id`, `user_status`);

-- `user_status` 的字段说明已在上方 ADD COLUMN 的 COMMENT 中定义。

-- ============================================================================
-- 第三步：创建岗位类型主数据表（独立，若依无此表）
-- ============================================================================

DROP TABLE IF EXISTS `position`;
CREATE TABLE `position` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `position_code` VARCHAR(32)  NOT NULL COMMENT '岗位编码(全局唯一)',
    `position_name` VARCHAR(64)  NOT NULL COMMENT '岗位名称',
    `sort_no`       INT                   DEFAULT 0 COMMENT '排序',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    `create_by`     VARCHAR(32)           DEFAULT NULL,
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_by`     VARCHAR(32)           DEFAULT NULL,
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_position_code` (`position_code`)
) ENGINE=InnoDB COMMENT='岗位类型表(实施/开发/设计/质检/建模)';

-- 初始化5个岗位类型
INSERT INTO `position` (`position_code`, `position_name`, `sort_no`, `status`) VALUES
    ('IMPLEMENTATION', '实施实习生', 1, 1),
    ('DEVELOPMENT', '开发实习生', 2, 1),
    ('DESIGN', '设计实习生', 3, 1),
    ('QA', '质检实习生', 4, 1),
    ('MODELING', '建模实习生', 5, 1);

-- ============================================================================
-- 第四步：创建部门-岗位绑定关系表（独立）
-- ============================================================================

DROP TABLE IF EXISTS `dept_position`;
CREATE TABLE `dept_position` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT,
    `dept_id`     BIGINT   NOT NULL COMMENT '部门ID（关联sys_dept）',
    `position_id` BIGINT   NOT NULL COMMENT '岗位ID（关联position）',
    `status`      TINYINT  NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    `create_time` DATETIME          DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_pos` (`dept_id`,`position_id`)
) ENGINE=InnoDB COMMENT='部门-岗位绑定关系表';

-- 初始化部门-岗位映射（假设5个部门对应5个岗位）
-- 注意：这里假设若依 sys_dept 中已有部门ID 103-107，根据实际情况调整
-- INSERT INTO `dept_position` (`dept_id`, `position_id`, `status`) VALUES
--     (103, 1, 1),  -- 研发部门 -> 实施实习生
--     (104, 2, 1),  -- 市场部门 -> 开发实习生
--     (105, 3, 1),  -- 测试部门 -> 设计实习生
--     (106, 4, 1),  -- 财务部门 -> 质检实习生
--     (107, 5, 1);  -- 运维部门 -> 建模实习生

-- ============================================================================
-- 第五步：初始化业务角色（映射到若依 sys_role）
-- ============================================================================

-- 在若依 sys_role 中插入业务角色
-- 注意：若依默认已有 admin 和 common 角色，这里添加业务角色
INSERT INTO `sys_role` (`role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `remark`) VALUES
    ('超级管理员', 'SUPER_ADMIN', 1, '1', 1, 1, '0', '0', 'admin', NOW(), '全局系统与业务规则管理'),
    ('部门管理员', 'DEPT_ADMIN', 2, '2', 1, 1, '0', '0', 'admin', NOW(), '本部门业务运营与培养评价'),
    ('预备实习生', 'PRE_TRAINEE', 3, '5', 1, 1, '0', '0', 'admin', NOW(), '培养与考核对象'),
    ('正式实习生', 'FORMAL_TRAINEE', 4, '5', 1, 1, '0', '0', 'admin', NOW(), '通过转正审批后的培养对象');

-- ============================================================================
-- 第六步：业务表（37张，已调整为关联若依 sys_user）
-- 注意：以下业务表中的 user_id 均关联若依 sys_user.user_id
-- ============================================================================

-- 注册申请单
DROP TABLE IF EXISTS `register_application`;
CREATE TABLE `register_application` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `application_no`VARCHAR(32)  NOT NULL COMMENT '申请编号(重新提交沿用)',
    `user_id`       BIGINT       NOT NULL COMMENT '申请人(关联sys_user.user_id)',
    `real_name`     VARCHAR(64)  NOT NULL COMMENT '姓名快照',
    `id_card`       VARCHAR(64)           DEFAULT NULL COMMENT '身份证快照',
    `login_account` VARCHAR(64)  NOT NULL COMMENT '登录账号快照',
    `position_id`   BIGINT       NOT NULL COMMENT '所选岗位(下拉主数据)',
    `dept_id`       BIGINT       NOT NULL COMMENT '岗位匹配的部门(只读)',
    `expected_entry_date` DATE            DEFAULT NULL COMMENT '预计入职时间',
    `status`        VARCHAR(24)  NOT NULL DEFAULT 'WAIT_AUDIT' COMMENT '待审核WAIT_AUDIT/已驳回REJECTED/已通过PASSED',
    `reject_reason` VARCHAR(255)          DEFAULT NULL COMMENT '最近驳回原因(驳回必填)',
    `audit_count`   INT          NOT NULL DEFAULT 0 COMMENT '提交次数',
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_app_user` (`user_id`),
    KEY `idx_app_dept_status` (`dept_id`,`status`)
) ENGINE=InnoDB COMMENT='注册申请单(审核对象)';

-- 通用审核/审批动作记录
DROP TABLE IF EXISTS `audit_record`;
CREATE TABLE `audit_record` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `biz_type`    VARCHAR(24)  NOT NULL COMMENT '业务类型:REGISTER注册/PROMOTION转正',
    `biz_id`      BIGINT       NOT NULL COMMENT '业务单据ID(申请单)',
    `user_id`     BIGINT       NOT NULL COMMENT '被审核/审批用户',
    `dept_id`     BIGINT                DEFAULT NULL COMMENT '审核人所在部门(部门数据边界)',
    `action`      VARCHAR(24)  NOT NULL COMMENT '动作:PASS通过/REJECT驳回',
    `from_status` VARCHAR(24)           DEFAULT NULL COMMENT '前状态',
    `to_status`   VARCHAR(24)           DEFAULT NULL COMMENT '后状态',
    `reason`      VARCHAR(500)          DEFAULT NULL COMMENT '驳回/人工判断原因',
    `operator_id` BIGINT       NOT NULL COMMENT '操作人',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_audit_biz` (`biz_type`,`biz_id`),
    KEY `idx_audit_user` (`user_id`)
) ENGINE=InnoDB COMMENT='注册审核/转正审批动作历史表';

-- 协议模板
DROP TABLE IF EXISTS `agreement_template`;
CREATE TABLE `agreement_template` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `agreement_name` VARCHAR(128) NOT NULL COMMENT '协议名称(保密协议/注册协议)',
    `version_no`     VARCHAR(32)  NOT NULL COMMENT '版本号',
    `content`        MEDIUMTEXT            DEFAULT NULL COMMENT '协议正文',
    `file_url`       VARCHAR(255)          DEFAULT NULL COMMENT '协议全文/下载文件',
    `effective_time` DATETIME              DEFAULT NULL COMMENT '生效时间',
    `status`         VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT草稿/EFFECTIVE生效中/HISTORY历史',
    `sign_count`     INT          NOT NULL DEFAULT 0 COMMENT '签署人数',
    `create_by`      VARCHAR(32)           DEFAULT NULL,
    `create_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_agree_name_version` (`agreement_name`,`version_no`)
) ENGINE=InnoDB COMMENT='协议模板表';

-- 协议签署记录
DROP TABLE IF EXISTS `agreement_signature`;
CREATE TABLE `agreement_signature` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL COMMENT '签署账号',
    `template_id`  BIGINT       NOT NULL COMMENT '所签协议模板(当时版本)',
    `agreement_name` VARCHAR(128)         DEFAULT NULL COMMENT '协议名称快照',
    `version_no`   VARCHAR(32)           DEFAULT NULL COMMENT '协议版本快照',
    `sign_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '签署时间',
    `sign_ip`      VARCHAR(64)           DEFAULT NULL COMMENT '签署IP',
    `terminal`     VARCHAR(64)           DEFAULT NULL COMMENT '终端/设备信息',
    `sign_mode`    VARCHAR(24)           DEFAULT NULL COMMENT '签名方式:MOUSE鼠标签名/STROKE手写',
    `signature_data` TEXT               DEFAULT NULL COMMENT '签名凭证/图像',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1有效',
    `create_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_sign_user` (`user_id`),
    KEY `idx_sign_template` (`template_id`)
) ENGINE=InnoDB COMMENT='保密协议签署记录表';

-- 课程
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `course_name` VARCHAR(128) NOT NULL COMMENT '课程名称',
    `position_id` BIGINT       NOT NULL COMMENT '适用岗位',
    `course_type` VARCHAR(24)  NOT NULL COMMENT 'THEORY理论/PRACTICE实操/章节测试等',
    `is_required` TINYINT      NOT NULL DEFAULT 1 COMMENT '是否必修:1必修 0选修',
    `cover_url`   VARCHAR(255)          DEFAULT NULL COMMENT '封面',
    `intro`       TEXT                  DEFAULT NULL COMMENT '课程简介',
    `status`      VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT草稿/PUBLISHED已发布/DISABLED停用',
    `published_at` DATETIME             DEFAULT NULL COMMENT '发布时间',
    `create_by`   VARCHAR(32)           DEFAULT NULL,
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_course_pos` (`position_id`)
) ENGINE=InnoDB COMMENT='课程表';

-- 章节
DROP TABLE IF EXISTS `course_chapter`;
CREATE TABLE `course_chapter` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `course_id`  BIGINT       NOT NULL COMMENT '课程ID',
    `chapter_name` VARCHAR(128) NOT NULL COMMENT '章节名',
    `sort_no`    INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `is_required` TINYINT     NOT NULL DEFAULT 1 COMMENT '是否必修',
    `create_time` DATETIME             DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_chapter_course` (`course_id`)
) ENGINE=InnoDB COMMENT='课程章节表';

-- 学习单项
DROP TABLE IF EXISTS `study_item`;
CREATE TABLE `study_item` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `chapter_id`    BIGINT       NOT NULL COMMENT '所属章节',
    `item_title`    VARCHAR(128) NOT NULL COMMENT '单项标题',
    `item_type`     VARCHAR(24)  NOT NULL COMMENT 'DOC文档理论/VIDEO视频实操/QUIZ章节测试',
    `content_url`   VARCHAR(255)          DEFAULT NULL COMMENT '文档/视频地址或文件',
    `duration`      INT                   DEFAULT 0 COMMENT '时长(秒/分钟,视频)',
    `is_required`   TINYINT      NOT NULL DEFAULT 1 COMMENT '是否必修',
    `completion_rule` VARCHAR(24) NOT NULL DEFAULT 'SCROLL_END' COMMENT '完成规则:DOC按滚动到底/VIDEO按播放100%/QUIZ按提交',
    `quiz_json`     TEXT                  DEFAULT NULL COMMENT '章节测试题(JSON，简单场景内嵌)',
    `sort_no`       INT          NOT NULL DEFAULT 0,
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_item_chapter` (`chapter_id`)
) ENGINE=InnoDB COMMENT='学习单项表';

-- 学习记录
DROP TABLE IF EXISTS `study_record`;
CREATE TABLE `study_record` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL COMMENT '学员',
    `course_id`      BIGINT       NOT NULL COMMENT '课程(冗余便于统计)',
    `chapter_id`     BIGINT       NOT NULL COMMENT '章节(冗余)',
    `item_id`        BIGINT       NOT NULL COMMENT '学习单项',
    `item_type`      VARCHAR(24)  NOT NULL COMMENT '单项类型冗余',
    `status`         VARCHAR(24)  NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED未开始/IN_PROGRESS学习中/DONE已完成',
    `first_open_time` DATETIME             DEFAULT NULL COMMENT '首次打开时间',
    `start_time`     DATETIME              DEFAULT NULL COMMENT '本次开始时间',
    `last_study_time` DATETIME             DEFAULT NULL COMMENT '最近学习时间',
    `study_duration` INT          NOT NULL DEFAULT 0 COMMENT '累计学习时长(秒)',
    `progress`       DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '播放进度%(视频)',
    `finish_time`    DATETIME              DEFAULT NULL COMMENT '完成时间',
    `unfinished_reason` VARCHAR(128)       DEFAULT NULL COMMENT '未完成原因',
    `read_confirm`   TINYINT      NOT NULL DEFAULT 0 COMMENT '文档滚动到底确认:0否 1是',
    `version`        INT          NOT NULL DEFAULT 0,
    `create_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_user_item` (`user_id`,`item_id`),
    KEY `idx_record_user_course` (`user_id`,`course_id`),
    KEY `idx_record_item` (`item_id`)
) ENGINE=InnoDB COMMENT='学习记录表(单项级，最小粒度)';

-- 备考资料
DROP TABLE IF EXISTS `material`;
CREATE TABLE `material` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `material_name` VARCHAR(128) NOT NULL COMMENT '资料名称',
    `material_type` VARCHAR(24) NOT NULL COMMENT '类型:指南DOCUMENT/视频VIDEO/模拟题入口PRACTICE',
    `position_id` BIGINT       NOT NULL COMMENT '适用岗位',
    `summary`     VARCHAR(255)          DEFAULT NULL COMMENT '简介',
    `file_url`    VARCHAR(255)          DEFAULT NULL COMMENT '文件',
    `version_no`  VARCHAR(32)           DEFAULT NULL COMMENT '版本号',
    `is_public`   TINYINT      NOT NULL DEFAULT 1 COMMENT '是否公开',
    `status`      VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED已发布/DISABLED已停用',
    `valid_from`  DATETIME              DEFAULT NULL COMMENT '生效时间',
    `valid_to`    DATETIME              DEFAULT NULL COMMENT '失效时间',
    `create_by`   VARCHAR(32)           DEFAULT NULL,
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_material_pos` (`position_id`)
) ENGINE=InnoDB COMMENT='备考资料表';

-- 题目
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `question_no`   VARCHAR(64)  NOT NULL COMMENT '题目编号(模板内唯一)',
    `position_id`   BIGINT       NOT NULL COMMENT '适用岗位',
    `qtype`         VARCHAR(24)  NOT NULL COMMENT '题型:SINGLE单选/MULTI多选/JUDGE判断/SUBJECT主观',
    `difficulty`    VARCHAR(24)  NOT NULL DEFAULT 'MEDIUM' COMMENT '难度:EASY/MEDIUM/HARD',
    `knowledge_point` VARCHAR(64)          DEFAULT NULL COMMENT '知识点',
    `stem`          TEXT         NOT NULL COMMENT '题干',
    `options_json`  TEXT                  DEFAULT NULL COMMENT '选项JSON(客观题)',
    `answer`        TEXT                  DEFAULT NULL COMMENT '正确答案(客观题)/评分要点(主观题)',
    `analysis`      TEXT                  DEFAULT NULL COMMENT '解析',
    `score`         DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '默认分值',
    `is_subjective` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否主观题:0否 1是',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    `import_batch`  VARCHAR(64)           DEFAULT NULL COMMENT '导入批次号',
    `version`       INT          NOT NULL DEFAULT 1 COMMENT '版本',
    `create_by`     VARCHAR(32)           DEFAULT NULL,
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_no` (`question_no`),
    KEY `idx_question_pos_type` (`position_id`,`qtype`),
    KEY `idx_question_kp` (`knowledge_point`)
) ENGINE=InnoDB COMMENT='题库题目表';

-- 考试
DROP TABLE IF EXISTS `exam`;
CREATE TABLE `exam` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `exam_name`     VARCHAR(128) NOT NULL COMMENT '考试名称',
    `exam_type`     VARCHAR(24)  NOT NULL COMMENT '类型:PRACTICE模拟/FORMAL正式',
    `position_id`   BIGINT       NOT NULL COMMENT '适用岗位',
    `dept_id`       BIGINT                DEFAULT NULL COMMENT '发布部门(正式考试为部门业务)',
    `status`        VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT草稿/PUBLISHED已发布/DISABLED/ENDED已结束',
    `start_time`    DATETIME              DEFAULT NULL COMMENT '开始时间',
    `end_time`      DATETIME              DEFAULT NULL COMMENT '截止时间',
    `duration`      INT          NOT NULL DEFAULT 0 COMMENT '作答时长(分钟)',
    `pass_line`     DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '总分通过线',
    `published_at`  DATETIME              DEFAULT NULL COMMENT '发布时间',
    `publisher_id`  BIGINT                DEFAULT NULL COMMENT '发布人(部门管理员/超管)',
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_exam_pos_type` (`position_id`,`exam_type`),
    KEY `idx_exam_dept` (`dept_id`)
) ENGINE=InnoDB COMMENT='考试表';

-- 考试规则快照
DROP TABLE IF EXISTS `exam_rule_snapshot`;
CREATE TABLE `exam_rule_snapshot` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `exam_id`         BIGINT       NOT NULL COMMENT '考试ID',
    `rule_version`    INT          NOT NULL DEFAULT 1 COMMENT '规则版本',
    `theory_weight`   DECIMAL(5,2)          DEFAULT NULL COMMENT '理论权重%',
    `practice_weight` DECIMAL(5,2)          DEFAULT NULL COMMENT '实操权重%',
    `theory_pass_line` DECIMAL(6,2)         DEFAULT NULL COMMENT '理论单项通过线',
    `practice_pass_line` DECIMAL(6,2)       DEFAULT NULL COMMENT '实操单项通过线',
    `need_course_done` TINYINT     NOT NULL DEFAULT 1 COMMENT '是否须完成全部必修课程(门槛)',
    `course_done_threshold` DECIMAL(5,2)    DEFAULT NULL COMMENT '学习完成率阈值(如70)',
    `protocol_required` TINYINT     NOT NULL DEFAULT 1 COMMENT '协议是否必签(门槛)',
    `retake_count`    INT          NOT NULL DEFAULT 1 COMMENT '补考次数',
    `paper_spec_json` TEXT                  DEFAULT NULL COMMENT '组卷配比JSON(题型/题量/知识点/分值)',
    `practice_spec_json` TEXT               DEFAULT NULL COMMENT '实操规则JSON(评分要点/成果格式/大小上限/难度)',
    `freeze_time`     DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '冻结(发布)时间',
    `created_by`      BIGINT                DEFAULT NULL COMMENT '冻结操作人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exam_rule` (`exam_id`,`rule_version`),
    KEY `idx_rule_exam` (`exam_id`)
) ENGINE=InnoDB COMMENT='考试规则快照表(发布时冻结)';

-- 组卷题目快照
DROP TABLE IF EXISTS `exam_paper_question`;
CREATE TABLE `exam_paper_question` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `exam_id`     BIGINT        NOT NULL COMMENT '考试ID',
    `rule_id`     BIGINT                 DEFAULT NULL COMMENT '规则快照ID',
    `question_id` BIGINT                 DEFAULT NULL COMMENT '原题目ID(可空,保留溯源)',
    `seq`         INT           NOT NULL DEFAULT 0 COMMENT '题序',
    `qtype`       VARCHAR(24)   NOT NULL COMMENT '题型',
    `stem`        TEXT          NOT NULL COMMENT '题干快照',
    `options_json` TEXT                 DEFAULT NULL COMMENT '选项快照',
    `answer`      TEXT                  DEFAULT NULL COMMENT '答案/评分要点快照',
    `score`       DECIMAL(6,2)  NOT NULL DEFAULT 0 COMMENT '分值快照',
    `is_subjective` TINYINT     NOT NULL DEFAULT 0 COMMENT '主观题标记',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_paper_exam` (`exam_id`)
) ENGINE=InnoDB COMMENT='组卷题目快照表';

-- 答卷
DROP TABLE IF EXISTS `answer_sheet`;
CREATE TABLE `answer_sheet` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `exam_id`       BIGINT        NOT NULL COMMENT '考试ID',
    `user_id`       BIGINT        NOT NULL COMMENT '考生',
    `sheet_type`    VARCHAR(24)   NOT NULL COMMENT 'PRACTICE模拟/FORMAL正式理论',
    `status`        VARCHAR(24)   NOT NULL DEFAULT 'NOT_STARTED' COMMENT '状态机:NOT_STARTED/IN_PROGRESS/SUBMITTED已交卷/AI_SCORING/AI_DONE/WAIT_MANUAL/REVIEWED/PUBLISHED/PASSED/FAILED/ABSENT缺考等',
    `start_time`    DATETIME               DEFAULT NULL COMMENT '开始时间',
    `submit_time`   DATETIME               DEFAULT NULL COMMENT '交卷时间',
    `submit_type`   VARCHAR(24)            DEFAULT NULL COMMENT '交卷方式:MANUAL主动/AUTO自动/EXCEPTION异常/ABSENT缺考',
    `question_count` INT          NOT NULL DEFAULT 0 COMMENT '题量',
    `ai_score`      DECIMAL(8,2)           DEFAULT NULL COMMENT 'AI原始总分',
    `final_score`   DECIMAL(8,2)           DEFAULT NULL COMMENT '最终理论分',
    `pass_flag`     TINYINT                DEFAULT NULL COMMENT '是否通过(理论环节)',
    `retake_seq`    INT           NOT NULL DEFAULT 1 COMMENT '本次为第几次作答/补考',
    `version`       INT           NOT NULL DEFAULT 0 COMMENT '并发版本',
    `create_time`   DATETIME               DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_sheet_user_exam` (`user_id`,`exam_id`),
    KEY `idx_sheet_exam_status` (`exam_id`,`status`)
) ENGINE=InnoDB COMMENT='答卷表(理论答题，含状态机)';

-- 逐题作答记录
DROP TABLE IF EXISTS `answer_sheet_item`;
CREATE TABLE `answer_sheet_item` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `answer_sheet_id` BIGINT      NOT NULL COMMENT '答卷ID',
    `paper_q_id`     BIGINT                DEFAULT NULL COMMENT '组卷题目快照ID',
    `question_id`    BIGINT                DEFAULT NULL COMMENT '原题目ID(溯源)',
    `seq`            INT          NOT NULL DEFAULT 0,
    `qtype`          VARCHAR(24)  NOT NULL COMMENT '题型',
    `is_subjective`  TINYINT      NOT NULL DEFAULT 0,
    `user_answer`    TEXT                  DEFAULT NULL COMMENT '考生作答',
    `is_correct`     TINYINT                DEFAULT NULL COMMENT '客观题是否正确',
    `ai_score`       DECIMAL(6,2)           DEFAULT NULL COMMENT 'AI给分',
    `ai_comment`     TEXT                  DEFAULT NULL COMMENT 'AI评语',
    `ai_confidence`  DECIMAL(6,4)           DEFAULT NULL COMMENT 'AI置信度',
    `ai_status`      VARCHAR(24)            DEFAULT NULL COMMENT 'AI状态:OK/异常/超时/低置信度',
    `manual_score`   DECIMAL(6,2)           DEFAULT NULL COMMENT '人工调整分',
    `manual_comment` TEXT                  DEFAULT NULL COMMENT '人工评语',
    `manual_user_id` BIGINT                DEFAULT NULL COMMENT '人工评分人',
    `manual_time`    DATETIME              DEFAULT NULL COMMENT '人工评分时间',
    `create_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_sheet_item_sheet` (`answer_sheet_id`)
) ENGINE=InnoDB COMMENT='逐题作答与评分明细表';

-- 实操成果提交
DROP TABLE IF EXISTS `practical_submission`;
CREATE TABLE `practical_submission` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `exam_id`      BIGINT       NOT NULL COMMENT '考试ID',
    `user_id`      BIGINT       NOT NULL COMMENT '学员',
    `retake_seq`   INT          NOT NULL DEFAULT 1 COMMENT '第几次(补考)',
    `title`        VARCHAR(128)          DEFAULT NULL COMMENT '成果名称',
    `submit_no`    VARCHAR(32)           DEFAULT NULL COMMENT '提交编号',
    `file_url`     VARCHAR(255)          DEFAULT NULL COMMENT '成果文件/压缩包/源码',
    `demo_link`    VARCHAR(255)          DEFAULT NULL COMMENT 'Demo链接',
    `note`         TEXT                  DEFAULT NULL COMMENT '成果说明',
    `file_size`    BIGINT                DEFAULT NULL COMMENT '文件大小',
    `status`       VARCHAR(24)  NOT NULL DEFAULT 'NOT_SUBMITTED' COMMENT 'NOT_SUBMITTED未提交/SUBMITTED已提交/SCORING评分中/SCORED已评分/ABSENT缺考(未提交)',
    `submit_time`  DATETIME              DEFAULT NULL COMMENT '提交时间',
    `deadline`     DATETIME              DEFAULT NULL COMMENT '截止时间',
    `score`        DECIMAL(6,2)          DEFAULT NULL COMMENT '实操分',
    `comment`      TEXT                  DEFAULT NULL COMMENT '评分意见/评语',
    `scorer_id`    BIGINT                DEFAULT NULL COMMENT '评分人(部门管理员)',
    `score_time`   DATETIME              DEFAULT NULL COMMENT '评分时间',
    `version`      INT          NOT NULL DEFAULT 0,
    `create_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_prac_user_exam` (`user_id`,`exam_id`)
) ENGINE=InnoDB COMMENT='实操成果提交表';

-- 成绩汇总记录
DROP TABLE IF EXISTS `assessment_result`;
CREATE TABLE `assessment_result` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `exam_id`          BIGINT        NOT NULL COMMENT '考试ID',
    `user_id`          BIGINT        NOT NULL COMMENT '学员',
    `retake_seq`       INT           NOT NULL DEFAULT 1 COMMENT '第几次',
    `theory_sheet_id`  BIGINT                 DEFAULT NULL COMMENT '理论答卷ID',
    `practice_sub_id`  BIGINT                 DEFAULT NULL COMMENT '实操提交ID',
    `theory_score`     DECIMAL(8,2)           DEFAULT NULL COMMENT '理论最终分',
    `practice_score`   DECIMAL(8,2)           DEFAULT NULL COMMENT '实操最终分',
    `total_score`      DECIMAL(8,2)           DEFAULT NULL COMMENT '加权综合分',
    `pass_flag`        TINYINT                DEFAULT NULL COMMENT '最终通过结论:0否 1是',
    `result_status`    VARCHAR(24)   NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '总状态:IN_PROGRESS/AI_SCORING/WAIT_MANUAL/PENDING_PUBLISH待发布/PUBLISHED已发布/ABSENT缺考',
    `publish_status`   VARCHAR(24)   NOT NULL DEFAULT 'UNPUBLISHED' COMMENT 'UNPUBLISHED/PUBLISHED已发布/CORRECTED已更正',
    `published_at`     DATETIME               DEFAULT NULL COMMENT '成绩发布时间',
    `publisher_id`     BIGINT                 DEFAULT NULL COMMENT '发布人',
    `eval_comment`     TEXT                   DEFAULT NULL COMMENT '部门管理员总体评价/改进建议',
    `eval_visible`     VARCHAR(24)            DEFAULT NULL COMMENT '评价可见范围',
    `result_version`   INT           NOT NULL DEFAULT 1 COMMENT '成绩版本(更正后+1)',
    `create_time`      DATETIME               DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_res_user_exam_retake` (`user_id`,`exam_id`,`retake_seq`),
    KEY `idx_res_exam_status` (`exam_id`,`result_status`)
) ENGINE=InnoDB COMMENT='正式考核成绩汇总表';

-- 成绩更正记录
DROP TABLE IF EXISTS `score_correction`;
CREATE TABLE `score_correction` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `result_id`      BIGINT       NOT NULL COMMENT '成绩记录ID',
    `old_version`    INT          NOT NULL,
    `new_version`    INT          NOT NULL,
    `old_total`      DECIMAL(8,2)          DEFAULT NULL COMMENT '更正前总分',
    `new_total`      DECIMAL(8,2)          DEFAULT NULL COMMENT '更正后总分',
    `reason`         VARCHAR(500) NOT NULL COMMENT '更正原因(必填)',
    `operator_id`    BIGINT       NOT NULL COMMENT '操作人',
    `create_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_corr_result` (`result_id`)
) ENGINE=InnoDB COMMENT='成绩更正留痕表';

-- 转正申请
DROP TABLE IF EXISTS `promotion_application`;
CREATE TABLE `promotion_application` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `application_no` VARCHAR(32)  NOT NULL COMMENT '转正申请编号',
    `user_id`        BIGINT       NOT NULL COMMENT '申请人',
    `dept_id`        BIGINT       NOT NULL COMMENT '所属部门',
    `status`         VARCHAR(24)  NOT NULL DEFAULT 'DEPT_PENDING' COMMENT 'DEPT_PENDING待部门审核/SUPER_PENDING待超管审批/PASSED已通过/REJECTED已驳回',
    `gate_check_json` TEXT                 DEFAULT NULL COMMENT '门槛校验结果JSON(协议/课程/理论/实操/待人工项)',
    `supplement`     TEXT                 DEFAULT NULL COMMENT '补充材料说明',
    `dept_approve_by` BIGINT               DEFAULT NULL COMMENT '部门审核人',
    `dept_approve_time` DATETIME           DEFAULT NULL COMMENT '部门审核时间',
    `super_approve_by` BIGINT              DEFAULT NULL COMMENT '超管审批人',
    `super_approve_time` DATETIME          DEFAULT NULL COMMENT '超管审批时间',
    `reject_reason`  VARCHAR(500)          DEFAULT NULL COMMENT '驳回原因(必填)',
    `cert_no`        VARCHAR(64)           DEFAULT NULL COMMENT '生成证书编号(通过后)',
    `create_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_promo_user` (`user_id`),
    KEY `idx_promo_dept_status` (`dept_id`,`status`)
) ENGINE=InnoDB COMMENT='转正申请表';

-- 电子证书
DROP TABLE IF EXISTS `certificate`;
CREATE TABLE `certificate` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `cert_no`     VARCHAR(64)  NOT NULL COMMENT '证书编号(全局唯一)',
    `user_id`     BIGINT       NOT NULL COMMENT '持证人',
    `promotion_id` BIGINT               DEFAULT NULL COMMENT '关联转正申请',
    `template_url` VARCHAR(255)         DEFAULT NULL COMMENT '证书模板/图片',
    `status`      VARCHAR(24)  NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED已生成',
    `generate_time` DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cert_no` (`cert_no`),
    KEY `idx_cert_user` (`user_id`)
) ENGINE=InnoDB COMMENT='电子证书表';

-- 能力维度配置
DROP TABLE IF EXISTS `ability_dimension`;
CREATE TABLE `ability_dimension` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `position_id`   BIGINT       NOT NULL COMMENT '适用岗位',
    `dim_code`      VARCHAR(64)  NOT NULL COMMENT '维度编码',
    `dim_name`      VARCHAR(64)  NOT NULL COMMENT '维度名称',
    `sort_no`       INT          NOT NULL DEFAULT 0,
    `enabled`       TINYINT      NOT NULL DEFAULT 1,
    `create_by`     VARCHAR(32)           DEFAULT NULL,
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_dim_pos` (`position_id`)
) ENGINE=InnoDB COMMENT='能力维度配置表';

-- 部门管理员阶段评价
DROP TABLE IF EXISTS `stage_evaluation`;
CREATE TABLE `stage_evaluation` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL COMMENT '被评价实习生',
    `dept_id`       BIGINT       NOT NULL COMMENT '所属部门',
    `period`        VARCHAR(24)  NOT NULL COMMENT '评价周期，如2026-Q3',
    `dim_score_json` TEXT                 DEFAULT NULL COMMENT '各维度等级/分值(JSON)',
    `strength`      TEXT                 DEFAULT NULL COMMENT '优势',
    `improvement`   TEXT                 DEFAULT NULL COMMENT '待提升项',
    `advice`        TEXT                 DEFAULT NULL COMMENT '改进建议',
    `score`         DECIMAL(6,2)          DEFAULT NULL COMMENT '总体等级/分',
    `visible_scope` VARCHAR(24)  NOT NULL DEFAULT 'ADMIN_ONLY' COMMENT 'ADMIN_ONLY仅管理员/OPEN管理员与实习生可见',
    `status`        VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT草稿/PUBLISHED已提交/WITHDRAWN已撤回',
    `version`       INT          NOT NULL DEFAULT 1 COMMENT '版本(改版留痕)',
    `evaluator_id`  BIGINT       NOT NULL COMMENT '评价人(部门管理员)',
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_eval_user` (`user_id`),
    KEY `idx_eval_dept` (`dept_id`)
) ENGINE=InnoDB COMMENT='部门管理员阶段评价表';

-- 能力画像快照
DROP TABLE IF EXISTS `profile_snapshot`;
CREATE TABLE `profile_snapshot` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL COMMENT '被分析用户',
    `position_id`     BIGINT       NOT NULL COMMENT '岗位',
    `period`          VARCHAR(24)  NOT NULL COMMENT '统计周期',
    `dim_scores_json` TEXT                 DEFAULT NULL COMMENT '各维度表现JSON(含来源/样本量)',
    `data_completeness` DECIMAL(6,2)       DEFAULT NULL COMMENT '数据完整度%',
    `source_summary_json` TEXT             DEFAULT NULL COMMENT '来源摘要(课程/练习/考核/评价)',
    `algorithm_version` VARCHAR(32)        DEFAULT NULL COMMENT '算法版本',
    `status`          VARCHAR(24)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待生成/READY已生成/NEED_REFRESH待刷新',
    `generated_at`    DATETIME             DEFAULT NULL COMMENT '生成时间',
    `create_time`     DATETIME             DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_profile_user_period` (`user_id`,`period`)
) ENGINE=InnoDB COMMENT='个人能力画像快照表(预留)';

-- 学习任务
DROP TABLE IF EXISTS `learning_task`;
CREATE TABLE `learning_task` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `task_name`      VARCHAR(128) NOT NULL COMMENT '任务名称',
    `course_id`      BIGINT                DEFAULT NULL COMMENT '关联课程',
    `position_id`    BIGINT                DEFAULT NULL COMMENT '适用岗位',
    `target`         TEXT                  DEFAULT NULL COMMENT '任务目标',
    `description`    TEXT                  DEFAULT NULL COMMENT '任务描述',
    `theory_hours`   DECIMAL(6,2)          DEFAULT NULL COMMENT '理论学时',
    `practice_hours` DECIMAL(6,2)          DEFAULT NULL COMMENT '实践学时',
    `start_time`     DATETIME              DEFAULT NULL COMMENT '开始时间',
    `deadline`       DATETIME              DEFAULT NULL COMMENT '截止时间',
    `need_assignment` TINYINT     NOT NULL DEFAULT 0 COMMENT '是否需要作业',
    `discussion_enabled` TINYINT  NOT NULL DEFAULT 1 COMMENT '是否启用讨论区',
    `reviewer_id`    BIGINT                DEFAULT NULL COMMENT '批阅人(部门管理员)',
    `publisher_id`   BIGINT                DEFAULT NULL COMMENT '发布人',
    `status`         VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED已发布/ENDED已截止',
    `create_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_task_pos` (`position_id`)
) ENGINE=InnoDB COMMENT='学习任务表';

-- 任务-学员分配
DROP TABLE IF EXISTS `task_assignment`;
CREATE TABLE `task_assignment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `task_id`     BIGINT       NOT NULL,
    `user_id`     BIGINT       NOT NULL,
    `status`      VARCHAR(24)  NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED/IN_PROGRESS/DONE已完成/OVERDUE逾期',
    `finish_time` DATETIME              DEFAULT NULL,
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_assign_task_user` (`task_id`,`user_id`),
    KEY `idx_assign_user` (`user_id`)
) ENGINE=InnoDB COMMENT='学习任务分配表';

-- 作业提交
DROP TABLE IF EXISTS `task_submission`;
CREATE TABLE `task_submission` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `assignment_id` BIGINT       NOT NULL COMMENT '任务分配ID',
    `task_id`       BIGINT       NOT NULL,
    `user_id`       BIGINT       NOT NULL,
    `content`       TEXT                 DEFAULT NULL COMMENT '作业内容',
    `file_url`      VARCHAR(255)         DEFAULT NULL COMMENT '作业附件',
    `submit_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `status`        VARCHAR(24)  NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED已提交/OVERDUE逾期/REVIEWED已批阅',
    `review_status` VARCHAR(24)           DEFAULT NULL COMMENT '批阅状态',
    `review_comment` TEXT                DEFAULT NULL COMMENT '批阅意见',
    `reviewer_id`   BIGINT               DEFAULT NULL COMMENT '批阅人',
    `review_time`   DATETIME             DEFAULT NULL,
    `version`       INT          NOT NULL DEFAULT 0,
    `create_time`   DATETIME             DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_sub_assign` (`assignment_id`),
    KEY `idx_sub_user` (`user_id`)
) ENGINE=InnoDB COMMENT='作业提交与批阅表';

-- 任务讨论区
DROP TABLE IF EXISTS `task_post`;
CREATE TABLE `task_post` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `task_id`     BIGINT       NOT NULL,
    `user_id`     BIGINT       NOT NULL COMMENT '发帖/回复人',
    `parent_id`   BIGINT                DEFAULT NULL COMMENT '父帖(回复)',
    `content`     TEXT         NOT NULL COMMENT '纯文本内容',
    `deleted_by`  BIGINT                DEFAULT NULL COMMENT '删除人(管理员删帖留痕)',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_post_task` (`task_id`)
) ENGINE=InnoDB COMMENT='学习任务讨论区帖子表';

-- 公告
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `title`       VARCHAR(128) NOT NULL,
    `content`     TEXT                  DEFAULT NULL,
    `scope_type`  VARCHAR(24)  NOT NULL DEFAULT 'ALL' COMMENT '发布范围:ALL全部/DEPT部门/POSITION岗位',
    `scope_id`    BIGINT                DEFAULT NULL COMMENT '范围对象ID(部门/岗位)',
    `is_top`      TINYINT      NOT NULL DEFAULT 0,
    `effective_to` DATETIME             DEFAULT NULL COMMENT '有效期至',
    `publisher_id` BIGINT               DEFAULT NULL,
    `publish_time` DATETIME             DEFAULT CURRENT_TIMESTAMP,
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_notice_scope` (`scope_type`,`scope_id`)
) ENGINE=InnoDB COMMENT='公告表';

-- 站内消息中心
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL COMMENT '接收人',
    `notify_type` VARCHAR(32)  NOT NULL COMMENT '类型:注册审核/考试提醒/评分完成/成绩发布/转正审批/证书生成/公告/逾期等',
    `title`       VARCHAR(128) NOT NULL,
    `content`     VARCHAR(500)          DEFAULT NULL,
    `biz_type`    VARCHAR(32)           DEFAULT NULL COMMENT '关联业务类型(考试/成绩/任务/证书等)',
    `biz_id`      BIGINT                DEFAULT NULL COMMENT '关联业务ID(点击跳转)',
    `read_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    `read_time`   DATETIME              DEFAULT NULL COMMENT '阅读时间',
    `expire_time` DATETIME              DEFAULT NULL COMMENT '过期清理时间(保留期默认180天)',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_notify_user_read` (`user_id`,`read_status`),
    KEY `idx_notify_expire` (`expire_time`)
) ENGINE=InnoDB COMMENT='站内消息中心表';

-- 操作审计日志（若依已有 sys_oper_log，这里保留业务操作日志）
DROP TABLE IF EXISTS `operate_log`;
CREATE TABLE `operate_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `operator_id` BIGINT                DEFAULT NULL COMMENT '操作人',
    `operator_role` VARCHAR(32)         DEFAULT NULL COMMENT '操作人角色',
    `dept_id`     BIGINT                DEFAULT NULL COMMENT '部门范围',
    `module`      VARCHAR(32)  NOT NULL COMMENT '模块',
    `action`      VARCHAR(32)  NOT NULL COMMENT '动作(注册审核/评分/发布/审批/导入/导出/协议签署/证书生成等)',
    `object_type` VARCHAR(32)           DEFAULT NULL COMMENT '对象类型',
    `object_id`   BIGINT                DEFAULT NULL COMMENT '对象ID',
    `before_json` TEXT                  DEFAULT NULL COMMENT '操作前数据',
    `after_json`  TEXT                  DEFAULT NULL COMMENT '操作后数据',
    `result`      VARCHAR(24)  NOT NULL DEFAULT 'SUCCESS' COMMENT '结果',
    `reason`      VARCHAR(255)          DEFAULT NULL COMMENT '原因',
    `ip`          VARCHAR(64)           DEFAULT NULL,
    `device`      VARCHAR(128)          DEFAULT NULL,
    `trace_no`    VARCHAR(64)           DEFAULT NULL COMMENT '请求追踪编号',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_log_operator_time` (`operator_id`,`create_time`),
    KEY `idx_log_module_action` (`module`,`action`),
    KEY `idx_log_object` (`object_type`,`object_id`)
) ENGINE=InnoDB COMMENT='业务操作审计日志表(若依sys_oper_log之外的补充)';

-- ============================================================================
-- 完成！业务表创建完毕
-- ============================================================================

SELECT 'RuoYi 融合版数据库初始化完成' AS `提示信息`;
SELECT '已创建业务表' AS `类型`, COUNT(*) AS `数量`
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'intern_assessment' AND TABLE_NAME NOT LIKE 'sys_%' AND TABLE_NAME NOT LIKE 'qrtz_%';
