package com.ruoyi.business.service;

import com.ruoyi.business.domain.PracticeSubject;

import java.util.List;

/**
 * 模拟考核实操题 Service
 */
public interface IPracticeSubjectService {

    /** 管理端列表（按登录账号数据范围） */
    List<PracticeSubject> selectSubjectList(PracticeSubject query);

    /** 管理端按ID查询（校验数据范围） */
    PracticeSubject selectSubjectById(Long id);

    /** 发布实操题 */
    int insertSubject(PracticeSubject subject);

    /** 编辑实操题 */
    int updateSubject(PracticeSubject subject);

    /** 删除实操题（逻辑删除） */
    int deleteByIds(Long[] ids);

    /** 启用/停用 */
    int changeStatus(Long id, Integer status);

    /** 实习生端：本部门已发布的实操题列表（moduleId 非空时只取该模块下的题） */
    List<PracticeSubject> selectPublishedForIntern(Long moduleId);

    /** 实习生端：本部门已发布的单条实操题详情（供独立详情页直链访问） */
    PracticeSubject selectPublishedDetailForIntern(Long id);
}
