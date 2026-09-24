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

    /** 本部门题池的知识点及题量（配置页「从题池导入知识点」，按 question.knowledge_point 聚合） */
    java.util.List<java.util.Map<String, Object>> bankKnowledgePoints(Long deptId);

    /** 题池概览（本部门理论题池的可用题量，按题型；题库概念退场后固定 1 条） */
    java.util.List<java.util.Map<String, Object>> bankOptions(Long deptId, String examMode, String bankKind);

    /**
     * 在培实习生花名册（「指定人员」的可选名单）。
     * 超级管理员可传 deptId 跨部门取人；部门管理员强制只看本部门。
     */
    java.util.List<java.util.Map<String, Object>> internOptions(Long deptId);

    /** 保存考核配置（知识配比 + 指定人员 + 时间窗 + 题型数量/分值），不改变发布状态 */
    int saveConfig(Long id, Exam params);

    /** 读取考核配置（知识配比 + 指定人员 + 时间窗） */
    java.util.Map<String, Object> configDetail(Long id);

    /** 按知识点配比试抽一套卷（配置页校验用，不落库）；deptId 为目标部门题池 */
    java.util.List<java.util.Map<String, Object>> tryDraw(Long deptId, Exam params);

    /** 按知识点配比试抽一套卷（部门取请求参数 deptId 或当前账号部门） */
    java.util.List<java.util.Map<String, Object>> tryDraw(Exam params);

    /** 修改考核启用/停用状态（PUBLISHED ↔ DISABLED） */
    int changeStatus(Long id, String status);
}
