package com.ruoyi.business.service;

import java.util.List;
import java.util.Map;

/**
 * 超管「人员与账号信息管理」：人员全量查询 + 业务字段维护。
 *
 * <p>账号级增删改（账号/姓名/手机/邮箱/性别/部门/角色/密码/账号状态）走 RuoYi 原生
 * {@code /system/user}；本服务只补原生做不到的部分：**基于 sys_user 的富查询** 与
 * **业务列写入**（position_id / user_status / protocol_status / mentor_* / expected_entry_date）。</p>
 */
public interface ISuperPersonnelService {

    /** 人员列表（角色名、岗位名、部门名一并带出） */
    List<Map<String, Object>> selectPersonnelList(Map<String, Object> query);

    /** 单个人员详情（含 roleIds 逗号串） */
    Map<String, Object> selectPersonnelDetail(Long userId);

    /**
     * 保存业务字段。只处理传入的键，未传的列不动。
     * 返回更新后的详情，便于前端立即回显。
     *
     * <p>★ 内置规则：持有「超级管理员」角色的账号，部门会被强制归位到公司根部门（融谷）；
     * 若显式传了别的部门则直接拒绝。</p>
     */
    Map<String, Object> saveBusinessFields(Map<String, Object> body);

    /**
     * 删除人员（逻辑删除）。
     *
     * <p>★ 服务端守卫，不依赖前端置灰：<br />
     * ① 平台内置超管账号（user_id=1）不可删；<br />
     * ② 持有「超级管理员」角色的账号不可删（须先改为其他角色）—— 保证系统始终有可用超管；<br />
     * ③ 不能删除当前登录账号。</p>
     */
    int deletePersonnel(Long userId);

    /**
     * 人员看板聚合数据（一次请求拿全，避免前端按角色/状态逐个拉列表）。
     * 返回：主指标（total / accountNormal / accountDisabled / st* / protocol*）
     *      + {@code roleCounts}（按角色）+ {@code deptCounts}（按部门）+ {@code noRole}（无角色账号数）。
     */
    Map<String, Object> selectSummary();
}
