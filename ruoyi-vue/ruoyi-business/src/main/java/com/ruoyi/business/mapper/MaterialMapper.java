package com.ruoyi.business.mapper;

import com.ruoyi.business.domain.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 备考资料 Mapper
 *
 * 约定：business 模块写操作一律手写 XML，不使用 BaseMapper。
 */
@Mapper
public interface MaterialMapper {

    /** 管理员列表（按 scopeDeptId 限定数据范围，scopeDeptId 为 null 表示全部） */
    List<Material> selectMaterialList(Material query);

    /** 按ID查询（带岗位名称） */
    Material selectMaterialById(@Param("id") Long id);

    /** 实习生可见列表：本部门 + 已发布 */
    List<Material> selectPublishedByDept(@Param("deptId") Long deptId);

    /** 新增 */
    int insertMaterial(Material material);

    /** 修改 */
    int updateMaterial(Material material);

    /** 逻辑删除 */
    int deleteMaterialById(@Param("id") Long id);
}
