package com.ruoyi.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.business.domain.Position;

import java.util.List;
import java.util.Map;

/**
 * 岗位类型Service接口
 *
 * @author ruoyi
 */
public interface IPositionService extends IService<Position> {

    /**
     * 查询岗位列表（含已删除）
     */
    List<Position> selectPositionList(Position position);

    /**
     * 根据ID查询岗位
     */
    Position selectPositionById(Long id);

    /**
     * 新增岗位
     */
    int insertPosition(Position position);

    /**
     * 修改岗位
     */
    int updatePosition(Position position);

    /**
     * 批量删除岗位
     */
    int deletePositionByIds(Long[] ids);

    /**
     * 修改岗位状态
     */
    int updatePositionStatus(Long id, Integer status);

    /**
     * 校验岗位编码是否唯一
     */
    boolean checkPositionCodeUnique(Position position);

    /**
     * 部门 ↔ 岗位绑定列表（权威表 dept_position，仅生效中）
     */
    List<Map<String, Object>> selectDeptBindings();

    /**
     * 新增「部门 ↔ 岗位」绑定（**超管专属**，2026-09-20）。
     *
     * <p>支持<b>一部门多岗位</b>：同一部门可绑多个岗位。同一岗位也可绑到多个部门，
     * 但后者会让注册流程的「选岗位 → 自动匹配部门」失去唯一性 ——
     * 返回体里会带 {@code risk} 字段提示这一点。</p>
     *
     * @return {@code {deptId, positionId, risk?}}
     */
    Map<String, Object> addDeptBinding(Long deptId, Long positionId);

    /**
     * 解绑（**超管专属**）。
     *
     * <p>若某部门解绑后一个岗位都不剩会被拒绝 —— 没有岗位的部门无法被注册选中，
     * 课程可见性（按岗位命中部门）也会失效。</p>
     */
    int removeDeptBinding(Long id);
}