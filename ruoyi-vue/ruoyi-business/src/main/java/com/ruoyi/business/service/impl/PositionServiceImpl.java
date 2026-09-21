package com.ruoyi.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.business.domain.Position;
import com.ruoyi.business.mapper.PositionMapper;
import com.ruoyi.business.service.IPositionService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 岗位类型Service业务层实现
 *
 * @author ruoyi
 */
@Service
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements IPositionService {

    @Autowired
    private PositionMapper positionMapper;

    @Override
    public List<Position> selectPositionList(Position position) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(position.getPositionName() != null && !position.getPositionName().isEmpty(),
                Position::getPositionName, position.getPositionName());
        wrapper.eq(position.getPositionCode() != null && !position.getPositionCode().isEmpty(),
                Position::getPositionCode, position.getPositionCode());
        wrapper.eq(position.getStatus() != null, Position::getStatus, position.getStatus());
        wrapper.orderByAsc(Position::getSortNo);
        return positionMapper.selectList(wrapper);
    }

    @Override
    public Position selectPositionById(Long id) {
        return positionMapper.selectById(id);
    }

    @Override
    public int insertPosition(Position position) {
        if (!checkPositionCodeUnique(position)) {
            throw new ServiceException("新增岗位'" + position.getPositionName() + "'失败，岗位编码已存在");
        }
        position.setCreateBy(com.ruoyi.common.utils.SecurityUtils.getUsername());
        position.setDeleted(0);
        return positionMapper.insert(position);
    }

    @Override
    public int updatePosition(Position position) {
        if (!checkPositionCodeUnique(position)) {
            throw new ServiceException("修改岗位'" + position.getPositionName() + "'失败，岗位编码已存在");
        }
        position.setUpdateBy(com.ruoyi.common.utils.SecurityUtils.getUsername());
        return positionMapper.updateById(position);
    }

    @Override
    public int deletePositionByIds(Long[] ids) {
        return positionMapper.deleteBatchIds(java.util.Arrays.asList(ids));
    }

    @Override
    public int updatePositionStatus(Long id, Integer status) {
        Position position = new Position();
        position.setId(id);
        position.setStatus(status);
        position.setUpdateBy(com.ruoyi.common.utils.SecurityUtils.getUsername());
        return positionMapper.updateById(position);
    }

    @Override
    public boolean checkPositionCodeUnique(Position position) {
        Long id = position.getId();
        Position existing = positionMapper.selectOne(
                new LambdaQueryWrapper<Position>().eq(Position::getPositionCode, position.getPositionCode()));
        return existing == null || existing.getId().equals(id);
    }

    @Override
    public List<Map<String, Object>> selectDeptBindings() {
        return positionMapper.selectDeptBindings();
    }

    /** 绑定关系的维护仅超管可做（接口复用了部门管理员的权限串，越权拦截必须落在 Service） */
    private void requireSuperAdmin() {
        Long uid = SecurityUtils.getUserId();
        if (!(SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(uid))) {
            throw new ServiceException("仅超级管理员可维护部门与岗位的绑定关系");
        }
    }

    @Override
    public Map<String, Object> addDeptBinding(Long deptId, Long positionId) {
        requireSuperAdmin();
        if (deptId == null || positionId == null) {
            throw new ServiceException("请选择部门与岗位");
        }
        if (positionMapper.countBinding(deptId, positionId) > 0) {
            throw new ServiceException("该部门已绑定此岗位，无需重复添加");
        }
        Position position = positionMapper.selectById(positionId);
        if (position == null || Integer.valueOf(1).equals(position.getDeleted())) {
            throw new ServiceException("岗位不存在或已删除");
        }
        if (!Integer.valueOf(1).equals(position.getStatus())) {
            throw new ServiceException("岗位已停用，请先启用再绑定");
        }
        // ★ 口径（2026-09-20 用户明确）：**一个岗位只能属于一个部门**。
        //
        //   部门可以有多个岗位；但岗位不能跨部门 —— 否则注册流程的「选岗位 → 自动匹配部门」
        //   会失去唯一性（selectDeptIdByPositionId 是 LIMIT 1，会随机落到别的部门）。
        //   所以这里**直接拒绝**，不是给个风险提示。
        int other = positionMapper.countBindingInOtherDept(positionId, deptId);
        if (other > 0) {
            throw new ServiceException("该岗位已属于其它" + other + "个部门 —— 一个岗位只能属于一个部门。"
                    + "若确实想让本部门也有这个方向的岗位，请先到「系统管理 › 岗位管理」新建一个岗位"
                    + "（例如「初级开发实习生」），再回来绑定。");
        }

        positionMapper.insertBinding(deptId, positionId);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("deptId", deptId);
        res.put("positionId", positionId);
        return res;
    }

    @Override
    public int removeDeptBinding(Long id) {
        requireSuperAdmin();
        if (id == null) {
            throw new ServiceException("绑定ID不能为空");
        }
        Map<String, Object> binding = positionMapper.selectBindingById(id);
        if (binding == null || binding.isEmpty()) {
            throw new ServiceException("绑定关系不存在或已被删除");
        }
        Object deptId = binding.get("deptId");
        if (deptId != null && positionMapper.countBindingsOfDept(((Number) deptId).longValue()) <= 1) {
            throw new ServiceException("该部门当前只绑定了一个岗位，解绑后部门将没有岗位可选（注册与课程可见性都会失效）；请先新增其它岗位再解绑");
        }
        return positionMapper.deleteBinding(id);
    }
}