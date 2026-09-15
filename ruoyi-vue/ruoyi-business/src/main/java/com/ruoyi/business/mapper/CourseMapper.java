package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.Course;
import com.ruoyi.business.domain.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 课程Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 查询课程列表（带岗位名称/章节数/统计）
     */
    List<Course> selectCourseList(Course course);

    /** 按当前账号的数据范围查询单个课程。scopeDeptId 为空表示全局范围。 */
    Course selectCourseById(@Param("id") Long id, @Param("scopeDeptId") Long scopeDeptId);

    /** 校验岗位是否绑定到指定部门。 */
    int countPositionInDept(@Param("positionId") Long positionId, @Param("deptId") Long deptId);

    /** 发布前校验课程至少有一个有效章节。 */
    int countActiveChapters(@Param("courseId") Long courseId);

    /** 发布前校验每个有效章节都至少有一项有效学习资料。 */
    int countEmptyChapters(@Param("courseId") Long courseId);

    /** 查询课程管理可用岗位；部门管理员只返回本部门绑定的岗位。 */
    List<Position> selectCoursePositions(@Param("scopeDeptId") Long scopeDeptId);

    /** 实习生仅可查看本部门、本岗位的已发布课程。 */
    List<Course> selectLearningCourses(@Param("userId") Long userId);

    Course selectLearningCourseById(@Param("courseId") Long courseId, @Param("userId") Long userId);
}
