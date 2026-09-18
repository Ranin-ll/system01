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
}