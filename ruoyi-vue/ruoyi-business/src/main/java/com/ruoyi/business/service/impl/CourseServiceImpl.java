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
        getAccessibleCourse(course.getId());
        if (course.getPositionId() != null) {
            checkPositionScope(course.getPositionId(), deptId);
        }
        if (course.getStatus() != null && !isEditableStatus(course.getStatus())) {
            throw new ServiceException("课程状态只能通过发布或停用操作变更");
        }
        // 不允许通过通用编辑接口篡改删除标志、创建信息或发布时间。
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
        getAccessibleCourse(id);
        Course course = new Course();
        course.setId(id);
        course.setStatus("PUBLISHED");
        course.setPublishedAt(new Date());
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

    private Long managerScopeDeptId() {
        if (isGlobalReadOnly()) {
            throw new ServiceException("超级管理员仅可查看课程，不能执行课程写入操作");
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

    private void checkPositionScope(Long positionId, Long deptId) {
        if (positionId == null || courseMapper.countPositionInDept(positionId, deptId) == 0) {
            throw new ServiceException("只能选择当前部门已绑定的岗位");
        }
    }

    private boolean isGlobalReadOnly() {
        return SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.isAdmin(SecurityUtils.getUserId());
    }

    private boolean isEditableStatus(String status) {
        return "DRAFT".equals(status) || "PUBLISHED".equals(status) || "DISABLED".equals(status);
    }
}
