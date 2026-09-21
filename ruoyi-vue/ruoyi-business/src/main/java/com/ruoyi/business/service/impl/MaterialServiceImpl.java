package com.ruoyi.business.service.impl;

import com.ruoyi.business.domain.Material;
import com.ruoyi.business.domain.Position;
import com.ruoyi.business.mapper.InternAuthMapper;
import com.ruoyi.business.mapper.MaterialMapper;
import com.ruoyi.business.service.IMaterialService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 备考资料 Service 实现
 *
 * 数据范围控制（与题库/课程/实操题模块保持一致）：
 * - 超级管理员：查看全部资料，并可维护任意岗位的资料。
 * - 部门管理员：只能维护本部门岗位下的资料，适用岗位下拉锁定为本部门岗位。
 * - 实习生：只看本部门「已发布」的资料，只读、支持预览与下载。
 */
@Service
public class MaterialServiceImpl implements IMaterialService {

    private final MaterialMapper materialMapper;
    private final InternAuthMapper internAuthMapper;

    public MaterialServiceImpl(MaterialMapper materialMapper, InternAuthMapper internAuthMapper) {
        this.materialMapper = materialMapper;
        this.internAuthMapper = internAuthMapper;
    }

    @Override
    public List<Material> selectMaterialList(Material query) {
        if (query == null) {
            query = new Material();
        }
        query.setScopeDeptId(currentScopeDeptId());
        return materialMapper.selectMaterialList(query);
    }

    @Override
    public Material selectMaterialById(Long id) {
        return getAccessibleMaterial(id);
    }

    @Override
    public int insertMaterial(Material material) {
        if (material == null || isBlank(material.getMaterialName())) {
            throw new ServiceException("请填写资料名称");
        }
        if (material.getPositionId() == null) {
            throw new ServiceException("请选择适用岗位");
        }
        validatePositionAccessible(material.getPositionId());
        material.setId(null);
        normalizeForWrite(material);
        material.setCreateBy(SecurityUtils.getUsername());
        material.setDeleted(0);
        // 有效期字段已废弃，统一置空
        material.setValidFrom(null);
        material.setValidTo(null);
        return materialMapper.insertMaterial(material);
    }

    @Override
    public int updateMaterial(Material material) {
        if (material == null || material.getId() == null) {
            throw new ServiceException("资料ID不能为空");
        }
        getAccessibleMaterial(material.getId());
        if (material.getPositionId() != null) {
            validatePositionAccessible(material.getPositionId());
        }
        if (material.getMaterialName() != null) {
            if (material.getMaterialName().trim().isEmpty()) {
                throw new ServiceException("请填写资料名称");
            }
            material.setMaterialName(material.getMaterialName().trim());
        }
        // 有效期字段已废弃，禁止通过编辑接口写入
        material.setValidFrom(null);
        material.setValidTo(null);
        // 不允许通过通用编辑接口篡改创建信息或删除标志
        material.setCreateBy(null);
        material.setDeleted(null);
        return materialMapper.updateMaterial(material);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int count = 0;
        for (Long id : ids) {
            getAccessibleMaterial(id);
            materialMapper.deleteMaterialById(id);
            count++;
        }
        return count;
    }

    @Override
    public int changeStatus(Long id, String status) {
        if (isBlank(status)) {
            throw new ServiceException("状态不能为空");
        }
        String s = status.trim().toUpperCase();
        if (!"PUBLISHED".equals(s) && !"DRAFT".equals(s) && !"DISABLED".equals(s)) {
            throw new ServiceException("状态值不合法");
        }
        getAccessibleMaterial(id);
        Material update = new Material();
        update.setId(id);
        update.setStatus(s);
        return materialMapper.updateMaterial(update);
    }

    @Override
    public List<Material> selectPublishedForIntern() {
        Long deptId = requireDeptId("当前账号未配置部门，无法查看备考资料");
        List<Material> list = materialMapper.selectPublishedByDept(deptId);
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    public Material selectPublishedDetailForIntern(Long id) {
        if (id == null) {
            throw new ServiceException("资料ID不能为空");
        }
        Long deptId = requireDeptId("当前账号未配置部门，无法查看备考资料");
        Material material = materialMapper.selectMaterialById(id);
        // 不存在、已删除、非本部门、未发布，统一按"不存在"处理，避免暴露其它部门的资料
        if (material == null
                || !"PUBLISHED".equals(material.getStatus())
                || !deptId.equals(material.getScopeDeptId())) {
            throw new ServiceException("备考资料不存在或未发布");
        }
        return material;
    }

    @Override
    public List<Position> selectApplicablePositions() {
        List<Position> all = internAuthMapper.selectEnabledPositions();
        if (all == null) {
            all = new ArrayList<>();
        }
        if (isGlobalReadOnly()) {
            return all;
        }
        Long deptId = requireDeptId("当前账号未配置部门，无法获取适用岗位");
        List<Position> result = new ArrayList<>();
        for (Position p : all) {
            if (deptId.equals(p.getDeptId())) {
                result.add(p);
            }
        }
        return result;
    }

    /** 校验传入的岗位在当前账号可见范围内（防止跨部门指定岗位） */
    private void validatePositionAccessible(Long positionId) {
        if (isGlobalReadOnly()) {
            return;
        }
        Long deptId = requireDeptId("当前账号未配置部门，无法维护备考资料");
        Long posDeptId = internAuthMapper.selectDeptIdByPositionId(positionId);
        if (posDeptId == null || !deptId.equals(posDeptId)) {
            throw new ServiceException("只能选择本部门的适用岗位");
        }
    }

    /** 新增时的字段归一化 */
    private void normalizeForWrite(Material material) {
        material.setMaterialName(material.getMaterialName().trim());
        material.setSummary(trimN(material.getSummary()));
        material.setFileUrl(trimN(material.getFileUrl()));
        material.setVersionNo(trimN(material.getVersionNo()));
        if (isBlank(material.getMaterialType())) {
            material.setMaterialType("DOCUMENT");
        } else {
            material.setMaterialType(material.getMaterialType().trim().toUpperCase());
        }
        if (material.getIsPublic() == null) {
            material.setIsPublic(1);
        }
        if (isBlank(material.getStatus())) {
            material.setStatus("DRAFT");
        } else {
            material.setStatus(material.getStatus().trim().toUpperCase());
        }
    }

    private String trimN(String v) {
        return v == null ? null : v.trim();
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    /** 超级管理员返回 null（看全部），部门管理员/实习生返回本部门ID。 */
    private Long currentScopeDeptId() {
        if (isGlobalReadOnly()) {
            return null;
        }
        return requireDeptId("当前账号未配置部门，无法访问备考资料数据");
    }

    private Long requireDeptId(String msg) {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException(msg);
        }
        return deptId;
    }

    private Material getAccessibleMaterial(Long id) {
        if (id == null) {
            throw new ServiceException("资料ID不能为空");
        }
        Material material = materialMapper.selectMaterialById(id);
        if (material == null) {
            throw new ServiceException("备考资料不存在或已被删除");
        }
        Long scope = currentScopeDeptId();
        if (scope != null && !scope.equals(material.getScopeDeptId())) {
            throw new ServiceException("备考资料不存在或无权操作");
        }
        return material;
    }

    private boolean isGlobalReadOnly() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }
}
