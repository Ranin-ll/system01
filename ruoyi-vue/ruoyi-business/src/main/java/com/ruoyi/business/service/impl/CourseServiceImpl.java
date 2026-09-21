package com.ruoyi.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.business.domain.Course;
import com.ruoyi.business.domain.Position;
import com.ruoyi.business.mapper.CourseMapper;
import com.ruoyi.business.service.ICourseService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {

    @Autowired
    private CourseMapper courseMapper;

    @Override
    public List<Course> selectCourseList(Course course) {
        course.setScopeDeptId(currentScopeDeptId());
        return courseMapper.selectCourseList(course);
    }

    @Override
    public List<Position> selectCoursePositions() {
        return courseMapper.selectCoursePositions(currentScopeDeptId());
    }

    @Override
    public Course selectById(Long id) {
        Course course = courseMapper.selectCourseById(id, currentScopeDeptId());
        if (course == null) {
            throw new ServiceException("课程不存在或无权访问");
        }
        return course;
    }

    @Override
    public int insertCourse(Course course) {
        Long deptId = managerScopeDeptId();
        checkPositionScope(course.getPositionId(), deptId);
        course.setCreateBy(SecurityUtils.getUsername());
        course.setDeleted(0);
        course.setStatus("DRAFT");
        return courseMapper.insert(course);
    }

    @Override
    public int updateCourse(Course course) {
        Long deptId = managerScopeDeptId();
        Course current = getAccessibleCourse(course.getId());
        if ("PUBLISHED".equals(current.getStatus())) {
            throw new ServiceException("已发布课程请先停用，再修改基础信息或课程内容");
        }
        if (course.getPositionId() != null) {
            checkPositionScope(course.getPositionId(), deptId);
        }
        // 课程状态只能通过发布、停用接口流转。
        course.setStatus(null);
        course.setDeleted(null);
        course.setCreateBy(null);
        course.setCreateTime(null);
        course.setPublishedAt(null);
        course.setUpdateBy(SecurityUtils.getUsername());
        return courseMapper.updateById(course);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        managerScopeDeptId();
        if (ids == null || ids.length == 0) {
            return 0;
        }
        // 先逐条校验范围，再执行逻辑删除，避免批量操作部分成功。
        for (Long id : ids) {
            getAccessibleCourse(id);
        }
        for (Long id : ids) {
            Course c = new Course();
            c.setId(id);
            c.setDeleted(1);
            courseMapper.updateById(c);
        }
        return ids.length;
    }

    @Override
    public int publish(Long id) {
        managerScopeDeptId();
        Course current = getAccessibleCourse(id);
        if (!"DRAFT".equals(current.getStatus()) && !"DISABLED".equals(current.getStatus())) {
            throw new ServiceException("只有草稿或已停用课程可以发布");
        }
        if (courseMapper.countActiveChapters(id) == 0) {
            throw new ServiceException("发布前请至少配置一个章节");
        }
        if (courseMapper.countEmptyChapters(id) > 0) {
            throw new ServiceException("发布前请为每个章节至少配置一项学习资料");
        }
        if (courseMapper.countUnboundAssets(id) > 0) {
            throw new ServiceException("发布前请为所有文档和视频资料上传文件");
        }
        Course course = new Course();
        course.setId(id);
        course.setStatus("PUBLISHED");
        course.setPublishedAt(new Date());
        course.setUpdateBy(SecurityUtils.getUsername());
        return courseMapper.updateById(course);
    }

    @Override
    public int disable(Long id) {
        managerScopeDeptId();
        Course current = getAccessibleCourse(id);
        if (!"PUBLISHED".equals(current.getStatus())) {
            throw new ServiceException("只有已发布课程可以停用");
        }
        Course course = new Course();
        course.setId(id);
        course.setStatus("DISABLED");
        course.setUpdateBy(SecurityUtils.getUsername());
        return courseMapper.updateById(course);
    }

    /** 全局超级管理员仅查看课程，部门管理员和其他业务账号按部门取数。 */
    private Long currentScopeDeptId() {
        if (isGlobalReadOnly()) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法访问课程数据");
        }
        return deptId;
    }

    /**
     * 写操作的部门范围。
     *
     * <p><b>超管返回 null</b>，表示「不限部门」—— 可管理所有部门的课程。
     * 这是 2026-09-20 的规则调整：超管由「业务实例一律只读」改为
     * <b>「可管理全部课程与题库」</b>（与 {@code QuestionBankServiceImpl} 的同一方法口径对齐，
     * 那边早已是 {@code return null}）。</p>
     */
    private Long managerScopeDeptId() {
        if (isGlobalReadOnly()) {
            return null;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            throw new ServiceException("当前账号未配置部门，无法管理课程");
        }
        return deptId;
    }

    private Course getAccessibleCourse(Long id) {
        if (id == null) {
            throw new ServiceException("课程ID不能为空");
        }
        Course course = courseMapper.selectCourseById(id, currentScopeDeptId());
        if (course == null) {
            throw new ServiceException("课程不存在或无权操作");
        }
        return course;
    }

    /**
     * 岗位范围校验。
     *
     * <p>{@code deptId} 为 null 表示<b>超管（不限部门）</b> → 只校验岗位真实存在，
     * 允许把课程挂到任意岗位；否则要求岗位属于该部门（`dept_position` 绑定关系）。</p>
     */
    private void checkPositionScope(Long positionId, Long deptId) {
        if (positionId == null) {
            throw new ServiceException("请选择课程所属岗位");
        }
        if (deptId == null) {
            if (courseMapper.countPosition(positionId) == 0) {
                throw new ServiceException("岗位不存在");
            }
            return;
        }
        if (courseMapper.countPositionInDept(positionId, deptId) == 0) {
            throw new ServiceException("只能选择当前部门已绑定的岗位");
        }
    }

    private boolean isGlobalReadOnly() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }

}
