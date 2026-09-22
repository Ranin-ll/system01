package com.ruoyi.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.business.domain.Exam;

import java.util.List;

/**
 * 考核Service
 */
public interface IExamService extends IService<Exam> {

    List<Exam> selectExamList(Exam exam);

    Exam selectById(Long id);

    int insertExam(Exam exam);

    int updateExam(Exam exam);

    int deleteByIds(Long[] ids);

    /** 发布考核：DRAFT → PUBLISHED */
    int publish(Long id);

    /**
     * 发布考核（带配置）：可同时设定发布时间窗与指定人员。
     * params 为 null 时等价于 publish(id)。
     */
    int publish(Long id, Exam params);

    /** 题库知识点及题量（配置页「从题库导入知识点」，按 question.knowledge_point 聚合） */
    java.util.List<java.util.Map<String, Object>> bankKnowledgePoints(Long bankId);

    /** 本部门可选题库清单（含各库按题型的可用题量，供多题库组卷配置页选题库） */
    java.util.List<java.util.Map<String, Object>> bankOptions(Long deptId, String examMode, String bankKind);

    /**
     * 在培实习生花名册（「指定人员」的可选名单）。
     * 超级管理员可传 deptId 跨部门取人；部门管理员强制只看本部门。
     */
    java.util.List<java.util.Map<String, Object>> internOptions(Long deptId);

    /** 保存考核配置（多题库组卷 + 知识分布 + 指定人员 + 时间窗），不改变发布状态 */
    int saveConfig(Long id, Exam params);

    /** 读取考核配置（多题库组卷 + 知识分布 + 指定人员 + 时间窗） */
    java.util.Map<String, Object> configDetail(Long id);

    /** 按知识分布试抽一套卷（历史单库链路，配置页校验用，不落库） */
    java.util.List<java.util.Map<String, Object>> tryDraw(Long bankId, Exam params);

    /** 按多题库组卷配置试抽一套卷（配置页校验用，不落库） */
    java.util.List<java.util.Map<String, Object>> tryDrawByBanks(Exam params);

    /** 修改考核启用/停用状态（PUBLISHED ↔ DISABLED） */
    int changeStatus(Long id, String status);
}
