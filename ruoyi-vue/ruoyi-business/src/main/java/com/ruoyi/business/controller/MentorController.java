package com.ruoyi.business.controller;

import com.ruoyi.business.domain.Mentor;
import com.ruoyi.business.service.IMentorService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 导师库 Controller（2026-09-23）
 *
 * <p>背景：导师此前只是 {@code sys_user} 上的自由文本，注册审核时手工敲入，脏值很多。
 * 现在抽成独立主表，实习生通过 {@code sys_user.mentor_id} 关联，
 * 注册审核通过时<b>不再</b>填写导师，改由「实习生管理页」从本库里选。</p>
 *
 * <p>权限：{@code business:mentor:*} 已授予 admin / SUPER_ADMIN / DEPT_ADMIN。
 * 部门范围不靠前端，Service 层再校验一次（{@code currentScopeDeptId()}）。</p>
 *
 * <p>★ 路径前缀 {@code /business/mentor} 是独立前缀，不会被既有 {@code /{id}} 路由吞掉。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/business/mentor")
public class MentorController extends BaseController {

    @Autowired
    private IMentorService mentorService;

    /**
     * 导师列表（分页）
     */
    @PreAuthorize("@ss.hasPermi('business:mentor:list')")
    @GetMapping("/list")
    public TableDataInfo list(Mentor mentor) {
        startPage();
        List<Mentor> list = mentorService.selectMentorList(mentor);
        return getDataTable(list);
    }

    /**
     * 导师下拉选项（仅启用中，按当前人的部门范围）
     *
     * <p>供「实习生管理页」分配导师时直接选。路径是字面量，优先于 {@code /{id}} 匹配。</p>
     */
    @PreAuthorize("@ss.hasPermi('business:mentor:list')")
    @GetMapping("/options")
    public AjaxResult options() {
        return AjaxResult.success(mentorService.selectMentorOptions());
    }

    /**
     * 导师详情
     */
    @PreAuthorize("@ss.hasPermi('business:mentor:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(mentorService.selectMentorById(id));
    }

    /**
     * 新增导师
     */
    @PreAuthorize("@ss.hasPermi('business:mentor:add')")
    @Log(title = "导师管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Mentor mentor) {
        return toAjax(mentorService.insertMentor(mentor));
    }

    /**
     * 修改导师
     */
    @PreAuthorize("@ss.hasPermi('business:mentor:edit')")
    @Log(title = "导师管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Mentor mentor) {
        return toAjax(mentorService.updateMentor(mentor));
    }

    /**
     * 删除导师（软删）
     */
    @PreAuthorize("@ss.hasPermi('business:mentor:remove')")
    @Log(title = "导师管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(mentorService.deleteMentor(id));
    }

    /**
     * 查看某实习生当前的导师关联情况（供「分配导师」弹窗回显）
     */
    @PreAuthorize("@ss.hasPermi('business:intern:mentor')")
    @GetMapping("/intern/{userId}")
    public AjaxResult internInfo(@PathVariable Long userId) {
        return AjaxResult.success(mentorService.getInternMentorInfo(userId));
    }

    /**
     * 给实习生分配 / 更换导师
     *
     * @param body {@code {userId, mentorId}}
     */
    @PreAuthorize("@ss.hasPermi('business:intern:mentor')")
    @Log(title = "实习生导师分配", businessType = BusinessType.UPDATE)
    @PostMapping("/assign")
    public AjaxResult assign(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        Long mentorId = toLong(body.get("mentorId"));
        return toAjax(mentorService.assignMentorToIntern(userId, mentorId));
    }

    /**
     * 解除实习生的导师关联
     *
     * @param body {@code {userId}}
     */
    @PreAuthorize("@ss.hasPermi('business:intern:mentor')")
    @Log(title = "实习生导师分配", businessType = BusinessType.UPDATE)
    @PostMapping("/clear")
    public AjaxResult clear(@RequestBody Map<String, Object> body) {
        return toAjax(mentorService.clearInternMentor(toLong(body.get("userId"))));
    }

    /**
     * ★ 禁止裸 {@code Long.valueOf(map.get("id"))}：泛型擦除后插入 checkcast，
     * JSON 数字会被解成 Integer，必然 ClassCastException。
     * 统一走这个安全转换。
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equals(s)) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
