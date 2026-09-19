package com.ruoyi.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.business.domain.Position;
import com.ruoyi.business.mapper.PositionMapper;
import com.ruoyi.business.service.IPositionService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}