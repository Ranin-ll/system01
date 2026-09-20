package com.ruoyi.business.service;

import com.ruoyi.business.domain.PracticeModule;

import java.util.List;

/**
 * 模拟考核模块 Service
 *
 * 「模块」是模拟考核（理论 + 实操）的顶层分组：管理员先建模块，
 * 再在模块内发布理论模拟考核与实操模拟题；实习生端先选模块再看模块内的考核。
 */
public interface IPracticeModuleService {

    /** 管理端列表（按登录账号数据范围，附带模块内实操题/理论考核数量） */
    List<PracticeModule> selectModuleList(PracticeModule query);

    /** 管理端按ID查询（校验数据范围） */
    PracticeModule selectModuleById(Long id);

    /** 新建模块 */
    int insertModule(PracticeModule module);

    /** 编辑模块（改名 / 说明 / 排序 / 状态） */
    int updateModule(PracticeModule module);

    /** 删除模块（逻辑删除；模块下仍有内容时拒绝） */
    int deleteByIds(Long[] ids);

    /** 启用 / 停用模块 */
    int changeStatus(Long id, Integer status);

    /** 管理端下拉选项：本部门（超管可指定部门）全部模块 */
    List<PracticeModule> listOptions(Long deptId);

    /** 实习生端：本部门已启用模块列表（附带数量），用于「先选模块」 */
    List<PracticeModule> selectEnabledForIntern();
}
