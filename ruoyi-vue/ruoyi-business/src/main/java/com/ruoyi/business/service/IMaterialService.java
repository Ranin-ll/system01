package com.ruoyi.business.service;

import com.ruoyi.business.domain.Material;
import com.ruoyi.business.domain.Position;

import java.util.List;

/**
 * 备考资料 Service
 */
public interface IMaterialService {

    /** 管理端列表（按登录账号数据范围） */
    List<Material> selectMaterialList(Material query);

    /** 管理端按ID查询（校验数据范围） */
    Material selectMaterialById(Long id);

    /** 新增资料 */
    int insertMaterial(Material material);

    /** 编辑资料 */
    int updateMaterial(Material material);

    /** 删除资料（逻辑删除） */
    int deleteByIds(Long[] ids);

    /** 发布 / 停用（status: PUBLISHED / DRAFT / DISABLED） */
    int changeStatus(Long id, String status);

    /** 实习生端：本部门已发布资料列表 */
    List<Material> selectPublishedForIntern();

    /** 实习生端：本部门已发布单条资料详情 */
    Material selectPublishedDetailForIntern(Long id);

    /** 管理端上传表单：适用岗位下拉（超管看全部启用岗位，部门管理员只看到本部门岗位） */
    List<Position> selectApplicablePositions();
}
