-- 课程内容接口增量迁移
-- 可重复执行；只增加文件元数据字段，不删除课程或学习记录。
USE `intern_assessment`;

SET @db = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.tables
               WHERE table_schema = @db AND table_name = 'course_chapter') = 0,
              'CREATE TABLE course_chapter (id BIGINT NOT NULL AUTO_INCREMENT, course_id BIGINT NOT NULL, chapter_name VARCHAR(128) NOT NULL, chapter_intro VARCHAR(300) DEFAULT NULL, sort_no INT NOT NULL DEFAULT 0, is_required TINYINT NOT NULL DEFAULT 1, create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted TINYINT NOT NULL DEFAULT 0, PRIMARY KEY (id), KEY idx_chapter_course (course_id)) ENGINE=InnoDB COMMENT=''课程章节表''',
              'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = @db AND table_name = 'course_chapter' AND column_name = 'chapter_intro') = 0,
              'ALTER TABLE course_chapter ADD COLUMN chapter_intro VARCHAR(300) DEFAULT NULL COMMENT ''章节内容简介'' AFTER chapter_name',
              'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.tables
               WHERE table_schema = @db AND table_name = 'study_item') = 0,
              'CREATE TABLE study_item (id BIGINT NOT NULL AUTO_INCREMENT, chapter_id BIGINT NOT NULL, item_title VARCHAR(128) NOT NULL, item_intro VARCHAR(500) DEFAULT NULL, item_type VARCHAR(24) NOT NULL, content_url VARCHAR(255) DEFAULT NULL, duration INT DEFAULT 0, is_required TINYINT NOT NULL DEFAULT 1, completion_rule VARCHAR(24) NOT NULL DEFAULT ''SCROLL_END'', quiz_json TEXT DEFAULT NULL, sort_no INT NOT NULL DEFAULT 0, create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted TINYINT NOT NULL DEFAULT 0, file_name VARCHAR(255) DEFAULT NULL, file_size BIGINT DEFAULT NULL, file_ext VARCHAR(16) DEFAULT NULL, completion_threshold INT DEFAULT NULL, PRIMARY KEY (id), KEY idx_item_chapter (chapter_id)) ENGINE=InnoDB COMMENT=''学习单项表''',
              'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = @db AND table_name = 'study_item' AND column_name = 'item_intro') = 0,
              'ALTER TABLE study_item ADD COLUMN item_intro VARCHAR(500) DEFAULT NULL COMMENT ''学习资料内容简介'' AFTER item_title',
              'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.tables
               WHERE table_schema = @db AND table_name = 'study_record') = 0,
              'CREATE TABLE study_record (id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, course_id BIGINT NOT NULL, chapter_id BIGINT NOT NULL, item_id BIGINT NOT NULL, item_type VARCHAR(24) NOT NULL, status VARCHAR(24) NOT NULL DEFAULT ''NOT_STARTED'', first_open_time DATETIME DEFAULT NULL, start_time DATETIME DEFAULT NULL, last_study_time DATETIME DEFAULT NULL, study_duration INT NOT NULL DEFAULT 0, progress DECIMAL(6,2) NOT NULL DEFAULT 0, finish_time DATETIME DEFAULT NULL, unfinished_reason VARCHAR(128) DEFAULT NULL, read_confirm TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0, create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY (id), UNIQUE KEY uk_record_user_item (user_id,item_id), KEY idx_record_user_course (user_id,course_id), KEY idx_record_item (item_id)) ENGINE=InnoDB COMMENT=''学习记录表''',
              'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = @db AND table_name = 'study_item' AND column_name = 'file_name') = 0,
              'ALTER TABLE study_item ADD COLUMN file_name VARCHAR(255) DEFAULT NULL COMMENT ''原始文件名'' AFTER deleted',
              'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = @db AND table_name = 'study_item' AND column_name = 'file_size') = 0,
              'ALTER TABLE study_item ADD COLUMN file_size BIGINT DEFAULT NULL COMMENT ''文件字节数'' AFTER file_name',
              'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = @db AND table_name = 'study_item' AND column_name = 'file_ext') = 0,
              'ALTER TABLE study_item ADD COLUMN file_ext VARCHAR(16) DEFAULT NULL COMMENT ''文件扩展名'' AFTER file_size',
              'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = @db AND table_name = 'study_item' AND column_name = 'completion_threshold') = 0,
              'ALTER TABLE study_item ADD COLUMN completion_threshold INT DEFAULT NULL COMMENT ''视频完成阈值'' AFTER file_ext',
              'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'course content migration ready' AS result;
