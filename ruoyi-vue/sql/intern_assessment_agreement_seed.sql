-- 实习生学习考核系统：当前生效保密协议。
-- 可重复执行，不删除历史版本和签署记录。
USE `intern_assessment`;

UPDATE agreement_template
SET status = 'HISTORY', update_time = NOW()
WHERE agreement_name = '实习生保密协议' AND version_no <> 'V1.2' AND status = 'EFFECTIVE';

INSERT INTO agreement_template
    (agreement_name, version_no, content, effective_time, status, sign_count, create_by, create_time)
SELECT '实习生保密协议', 'V1.2',
       '一、实习期间接触的业务资料、账号信息、客户信息、学习材料、考核内容和项目交付成果均属于保密信息。\n\n二、未经授权，不得复制、传播、对外展示或用于与实习工作无关的用途。\n\n三、应妥善保管系统账号和访问凭证，不得转借他人使用；发现泄露风险应立即向部门负责人报告。\n\n四、保密义务不因实习结束而自动终止。违反本协议造成损失的，应按公司制度及相关法律承担责任。\n\n五、点击确认签署即表示本人已完整阅读、理解并同意遵守本协议。',
       NOW(), 'EFFECTIVE', 0, 'seed', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM agreement_template
    WHERE agreement_name = '实习生保密协议' AND version_no = 'V1.2'
);

UPDATE agreement_template
SET status = 'EFFECTIVE', effective_time = COALESCE(effective_time, NOW())
WHERE agreement_name = '实习生保密协议' AND version_no = 'V1.2';
