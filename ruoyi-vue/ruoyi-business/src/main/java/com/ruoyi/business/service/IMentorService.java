package com.ruoyi.business.service;

import com.ruoyi.business.domain.Mentor;

import java.util.List;
import java.util.Map;

/**
 * 导师库 Service 接口
 *
 * @author ruoyi
 */
public interface IMentorService {

    /** 导师列表（部门管理员只看本部门；超管可跨部门或按 deptId 过滤） */
    List<Mentor> selectMentorList(Mentor mentor);

    /** 下拉选项（仅启用中） */
    List<Mentor> selectMentorOptions();

    /** 详情 */
    Mentor selectMentorById(Long id);

    /** 新增（部门管理员强制落在本部门） */
    int insertMentor(Mentor mentor);

    /** 修改 */
    int updateMentor(Mentor mentor);

    /** 删除（软删；有实习生在带时拒绝） */
    int deleteMentor(Long id);

    /** 给实习生分配 / 更换导师（写 mentor_id 并同步冗余展示列） */
    int assignMentorToIntern(Long userId, Long mentorId);

    /** 解除实习生的导师关联 */
    int clearInternMentor(Long userId);

    /** 当前登录人可操作的部门范围（null = 不限，超管） */
    Long currentScopeDeptId();

    /** 校验：该实习生是否在可操作范围内（供前端/其它模块复用） */
    Map<String, Object> getInternMentorInfo(Long userId);
}
