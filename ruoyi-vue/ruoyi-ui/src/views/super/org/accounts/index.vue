<template>
  <div class="s-page">
    <header class="s-head">
      <div>
        <span class="eyebrow">SUPER ADMIN · PEOPLE &amp; ACCOUNTS</span>
        <h1>人员与账号信息管理</h1>
        <p>
          <b>全量账号一站式维护</b>：实习生 / 部门管理员 / 超管都在这里，可新增、修改、删除人员与账号信息，
          并<b>直接处理实习生的注册申请</b>。
          <span class="hint">账号级字段（账号 / 姓名 / 手机 / 邮箱 / 部门 / 角色 / 密码 / 账号状态）走平台原生用户接口；培养类字段（岗位 / 培养状态 / 协议 / 导师 / 预计入职）走业务接口。</span>
        </p>
      </div>
      <div class="s-head-actions">
        <el-button size="small" type="primary" icon="el-icon-plus" @click="openAdd">新增人员</el-button>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </header>

    <!-- 页签 -->
    <div class="s-tabs">
      <span :class="{ on: tab === 'people' }" @click="tab = 'people'">
        人员与账号 <em class="cnt">{{ total }}</em>
      </span>
      <span :class="{ on: tab === 'register' }" @click="tab = 'register'">
        注册申请
        <em v-if="pendingCount" class="cnt warn">{{ pendingCount }}</em>
      </span>
    </div>

    <!-- ==================== 页签一：人员与账号 ==================== -->
    <div v-show="tab === 'people'">
      <!-- 人员数据看板 -->
      <div class="s-kpis" v-loading="summaryLoading">
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#1764f5" />在册账号</div>
          <div class="vl">{{ num(summary.total) }}<small>人</small></div>
          <div class="ft">正常 {{ num(summary.accountNormal) }} · 停用 {{ num(summary.accountDisabled) }}</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#12b76a" />预备实习生</div>
          <div class="vl">{{ num(summary.stPreTrainee) }}<small>人</small></div>
          <div class="ft">转正审核中 {{ num(summary.stPendingPromote) }} 人</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#0e7490" />正式实习生</div>
          <div class="vl">{{ num(summary.stFormalTrainee) }}<small>人</small></div>
          <div class="ft">跨部门合计（已转正）</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#7a5af8" />部门管理员</div>
          <div class="vl">{{ roleCount('DEPT_ADMIN') }}<small>人</small></div>
          <div class="ft">超管 {{ roleCount('SUPER_ADMIN') }} 人（固定属融谷）</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#168561" />协议已签</div>
          <div class="vl">{{ num(summary.protocolSigned) }}<small>人</small></div>
          <div class="ft" :class="{ warn: num(summary.protocolUnsigned) > 0 }">
            未签 {{ num(summary.protocolUnsigned) }} 人
          </div>
        </div>
        <div class="s-kpi kpi-click" @click="goRegister">
          <div class="lb"><i class="dot" style="background:#f79009" />待审注册申请</div>
          <div class="vl">{{ num(pendingCount) }}<small>条</small></div>
          <div class="ft" :class="{ warn: num(pendingCount) > 0 }">
            {{ num(pendingCount) > 0 ? '点击到「注册申请」处理' : '暂无待处理' }}
          </div>
        </div>
      </div>

      <!-- 分布看板 -->
      <div class="s-grid">
        <section class="s-card s-c6">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">培</span><h3>培养状态分布</h3></div>
            <span class="hint">{{ num(summary.total) }} 人为基数</span>
          </div>
          <div class="s-hbars">
            <div v-for="r in statusRows" :key="r.label" class="s-hbar">
              <span class="nm" :title="r.label">{{ r.label }}</span>
              <span class="track"><i :style="{ width: r.pct + '%' }" /></span>
              <span class="pc">{{ r.cnt }}</span>
            </div>
          </div>
        </section>
        <section class="s-card s-c6">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">部</span><h3>部门分布</h3></div>
            <span v-if="num(summary.noRole) > 0" class="s-badge warn">
              无角色 {{ num(summary.noRole) }} 人待处理
            </span>
            <span v-else class="hint">全部账号均已分配角色</span>
          </div>
          <div class="s-hbars">
            <div v-for="r in deptRows" :key="r.label" class="s-hbar">
              <span class="nm" :title="r.label">{{ r.label }}</span>
              <span class="track"><i :style="{ width: r.pct + '%' }" /></span>
              <span class="pc">{{ r.cnt }}</span>
            </div>
          </div>
          <p v-if="num(summary.noRole) > 0" class="s-note">
            <b>无角色账号 {{ num(summary.noRole) }} 个</b>：这类账号能登录但看不到任何菜单（多为历史遗留），在下方列表里「角色」列标红，点「修改」补上角色即可。
          </p>
        </section>
      </div>

      <section class="s-card">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx">筛</span><h3>查询条件</h3></div>
          
        </div>
        <div class="s-filters">
          <el-input v-model="query.keyword" size="small" clearable placeholder="账号 / 姓名 / 手机号" style="width:200px"
                    @keyup.enter.native="search" />
          <el-select v-model="query.deptId" size="small" clearable placeholder="全部部门" style="width:140px">
            <el-option v-for="d in deptOptions" :key="d.deptId" :label="d.deptName" :value="Number(d.deptId)" />
          </el-select>
          <el-select v-model="query.roleKey" size="small" clearable placeholder="全部角色" style="width:150px">
            <el-option v-for="r in roleOptions" :key="r.roleKey" :label="r.roleName" :value="r.roleKey" />
          </el-select>
          <el-select v-model="query.userStatus" size="small" clearable placeholder="全部培养状态" style="width:150px">
            <el-option v-for="(label, key) in statusText" :key="key" :label="label" :value="key" />
          </el-select>
          <el-select v-model="query.status" size="small" clearable placeholder="全部账号状态" style="width:130px">
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
          <el-button size="small" type="primary" icon="el-icon-search" @click="search">查询</el-button>
          <el-button size="small" icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </div>

        <table v-if="rows.length" class="s-tbl" v-loading="loading">
          <thead>
            <tr>
              <th style="width:78px">ID</th>
              <th style="width:150px">账号 / 姓名</th>
              <th style="width:110px">部门</th>
              <th style="width:130px">岗位</th>
              <th style="width:110px">角色</th>
              <th style="width:120px">培养状态</th>
              <th style="width:80px">协议</th>
              <th style="width:126px">手机号</th>
              <th style="width:86px">账号状态</th>
              <th style="width:180px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in rows" :key="r.userId">
              <td class="num">{{ r.userId }}<i v-if="r.userId === 1" class="el-icon-lock" title="平台内置账号" /></td>
              <td class="nm">
                <b>{{ r.nickName || '—' }}</b>
                <small class="sub">{{ r.userName }}</small>
              </td>
              <td>{{ r.deptName || '—' }}</td>
              <td>
                <span v-if="r.positionName">{{ r.positionName }}</span>
                <span v-else class="muted">未设置</span>
              </td>
              <td><span v-if="r.roleNames">{{ r.roleNames }}</span><span v-else class="danger-text">无角色</span></td>
              <td><el-tag :type="statusTone(r.userStatus)" size="mini" effect="plain">{{ statusText[r.userStatus] || r.userStatus || '—' }}</el-tag></td>
              <td>
                <span :class="Number(r.protocolStatus) === 1 ? 'ok-text' : 'muted'">
                  {{ Number(r.protocolStatus) === 1 ? '已签' : '未签' }}
                </span>
              </td>
              <td>{{ r.phonenumber || '—' }}</td>
              <td>
                <span :class="r.status === '0' ? 'ok-text' : 'danger-text'">{{ r.status === '0' ? '正常' : '停用' }}</span>
              </td>
              <td>
                <template v-if="frameworkAccount(r)">
                  <span class="muted" title="平台内置超管账号：系统必须始终保留一个可用超管，故不允许修改 / 重置密码 / 删除">内置超管 · 仅可查看</span>
                </template>
                <template v-else>
                  <el-button type="text" size="mini" @click="openEdit(r)">修改</el-button>
                  <el-button type="text" size="mini" @click="openPwd(r)">重置密码</el-button>
                  <el-button v-if="deletable(r)" type="text" size="mini" class="danger-text" @click="removePerson(r)">删除</el-button>
                  <span v-else class="muted" :title="deleteBlockReason(r)">—</span>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="s-empty"><i class="el-icon-user" /><span>{{ loading ? '加载中…' : '没有符合条件的人员' }}</span></div>

        <el-pagination
          v-if="total > 0"
          class="s-pager"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page.sync="pageNum"
          :page-size.sync="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="loadPersonnel"
          @size-change="loadPersonnel"
        />

        
        
        
      </section>
    </div>

    <!-- ==================== 页签二：注册申请 ==================== -->
    <div v-show="tab === 'register'">
      <!-- 注册申请统计板块 -->
      <div class="s-kpis" v-loading="regStatLoading">
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#f79009" />待审核</div>
          <div class="vl">{{ num(regStat.pending) }}<small>条</small></div>
          <div class="ft" :class="{ warn: num(regStat.pending) > 0 }">
            {{ num(regStat.pending) > 0 ? ('涉及 ' + regDeptRows.length + ' 个部门，待部门管理员处理') : '已全部处理完毕' }}
          </div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#12b76a" />已通过</div>
          <div class="vl">{{ num(regStat.passed) }}<small>条</small></div>
          <div class="ft">通过率 {{ regPassRate }}%</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#f04438" />已驳回</div>
          <div class="vl">{{ num(regStat.rejected) }}<small>条</small></div>
          <div class="ft">申请人可改资料后重新提交</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#1764f5" />提交总数</div>
          <div class="vl">{{ num(regStat.total) }}<small>条</small></div>
          <div class="ft">全部历史申请（含已处理）</div>
        </div>
        <div class="s-kpi">
          <div class="lb"><i class="dot" style="background:#7a5af8" />最久待审</div>
          <div class="vl">{{ regStat.oldest ? regStat.oldest.days : '—' }}<small v-if="regStat.oldest">天</small></div>
          <div class="ft" :class="{ warn: regStat.oldest && regStat.oldest.days >= 3 }">
            {{ regStat.oldest ? (regStat.oldest.realName + ' · ' + regStat.oldest.createTime) : '暂无待审申请' }}
          </div>
        </div>
        <div class="s-kpi kpi-click" @click="urgeAllPending">
          <div class="lb"><i class="dot" style="background:#0e7490" />待催办管理员</div>
          <div class="vl">{{ regStat.adminsWithPending.length }}<small>位</small></div>
          <div class="ft" :class="{ warn: regStat.adminsWithPending.length > 0 }">
            {{ regStat.adminsWithPending.length > 0 ? '点击一键催办（通知 TA 的完整待办）' : '当前无需催办' }}
          </div>
        </div>
      </div>

      <!-- 注册申请分布 -->
      <div class="s-grid">
        <section class="s-card s-c6">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">状</span><h3>申请状态分布</h3></div>
            <span class="hint">{{ num(regStat.total) }} 条为基数</span>
          </div>
          <div class="s-hbars">
            <div v-for="r in regStatusRows" :key="r.label" class="s-hbar">
              <span class="nm" :title="r.label">{{ r.label }}</span>
              <span class="track"><i :style="{ width: r.pct + '%' }" /></span>
              <span class="pc">{{ r.cnt }}</span>
            </div>
          </div>
        </section>
        <section class="s-card s-c6">
          <div class="s-card-h">
            <div class="tt"><span class="s-idx">部</span><h3>待审按部门分布</h3></div>
            <span v-if="regDeptRows.length" class="s-badge warn">待催办 {{ regStat.adminsWithPending.length }} 位管理员</span>
            <span v-else class="hint">没有待审申请</span>
          </div>
          <div class="s-hbars">
            <div v-for="r in regDeptRows" :key="r.label" class="s-hbar">
              <span class="nm" :title="r.label">{{ r.label }}</span>
              <span class="track"><i :style="{ width: r.pct + '%' }" /></span>
              <span class="pc">{{ r.cnt }}</span>
            </div>
            <div v-if="!regDeptRows.length" class="s-empty"><i class="el-icon-success" /><span>全部报名均已处理</span></div>
          </div>
        </section>
      </div>

      <section class="s-card">
        <div class="s-card-h">
          <div class="tt"><span class="s-idx">审</span><h3>实习生注册申请</h3></div>
          <div class="head-right">
            <span class="hint">超管不受部门限制，可见全部申请</span>
            <el-button v-if="num(regStat.pending) > 0" type="warning" size="mini" plain
                       icon="el-icon-bell" @click="urgeAllPending">
              一键催办（{{ regStat.adminsWithPending.length }}）
            </el-button>
          </div>
        </div>
        <div class="s-filters">
          <el-input v-model="reg.query.realName" size="small" clearable placeholder="姓名" style="width:150px"
                    @keyup.enter.native="loadRegister" />
          <el-input v-model="reg.query.loginAccount" size="small" clearable placeholder="登录账号" style="width:170px"
                    @keyup.enter.native="loadRegister" />
          <el-select v-model="reg.query.status" size="small" clearable placeholder="全部状态" style="width:140px">
            <el-option label="待审核" value="WAIT_AUDIT" />
            <el-option label="已通过" value="PASSED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          <el-button size="small" type="primary" icon="el-icon-search" @click="loadRegister">查询</el-button>
          <el-button size="small" icon="el-icon-refresh-left" @click="resetRegQuery">重置</el-button>
        </div>

        <div v-if="reg.rows.length" class="s-tbl-scroll">
          <table class="s-tbl" v-loading="reg.loading">
            <thead>
              <tr>
                <th style="width:150px">申请人</th>
                <th style="width:130px">登录账号</th>
                <th style="width:170px">身份证号</th>
                <th style="width:190px">意向岗位 / 所属部门</th>
                <th style="width:100px">预计入职</th>
                <th style="width:92px">申请状态</th>
                <th style="width:150px">账号状态 / 业务身份</th>
                <th style="width:170px">导师（实习生管理页分配）</th>
                <th style="width:205px">审核情况</th>
                <th style="width:170px">驳回原因</th>
                <th style="width:140px">提交时间</th>
                <th style="width:176px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in reg.rows" :key="r.id" :class="{ 'row-pending': r.status === 'WAIT_AUDIT' }">
                <td class="nm">
                  <b>{{ r.realName || '—' }}</b>
                  <small class="sub">{{ r.applicationNo }}</small>
                </td>
                <td>{{ r.loginAccount || '—' }}</td>
                <td>{{ maskIdCard(r.idCard) }}</td>
                <td>
                  <span v-if="r.positionName">{{ r.positionName }}</span>
                  <span v-else class="muted">岗位缺失</span>
                  <small class="sub"><i class="el-icon-location-outline" /> {{ r.deptName || '部门待配置' }}</small>
                </td>
                <td>{{ fmt(r.expectedEntryDate, false) === '—' ? '未填写' : fmt(r.expectedEntryDate, false) }}</td>
                <td><el-tag :type="regTone(r.status)" size="mini" effect="plain">{{ regText[r.status] || r.status }}</el-tag></td>
                <td>
                  <span :class="accountStatusText(r.accountStatus) === '正常' ? 'ok-text' : 'warn-text'">{{ accountStatusText(r.accountStatus) }}</span>
                  <small class="sub">{{ statusText[r.userStatus] || r.userStatus || '—' }}</small>
                </td>
                <td>
                  <span>{{ r.mentorName || '待分配' }}</span>
                  <small class="sub">{{ r.mentorPhone || '待分配' }}</small>
                </td>
                <td>
                  <span>共提交 <b>{{ num(r.auditCount) }}</b> 次</span>
                  <small class="sub">最近更新 {{ fmt(r.updateTime, true) }}</small>
                  <small class="sub" :class="auditHintTone(r)">{{ auditHint(r) }}</small>
                </td>
                <td>
                  <span v-if="r.rejectReason" class="reason" :title="r.rejectReason">{{ r.rejectReason }}</span>
                  <span v-else class="muted">—</span>
                </td>
                <td>{{ fmt(r.createTime, true) }}</td>
                <td>
                  <el-button v-if="r.status === 'WAIT_AUDIT'" type="text" size="mini" @click="openAudit(r, 'PASS')">通过</el-button>
                  <el-button v-if="r.status === 'WAIT_AUDIT'" type="text" size="mini" class="danger-text" @click="openAudit(r, 'REJECT')">驳回</el-button>
                  <el-button v-if="r.status === 'WAIT_AUDIT'" type="text" size="mini" icon="el-icon-bell" @click="urgeOne(r)">催办</el-button>
                  <span v-if="r.status !== 'WAIT_AUDIT'" class="muted">已处理</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="s-empty"><i class="el-icon-document-checked" /><span>{{ reg.loading ? '加载中…' : '没有符合条件的注册申请' }}</span></div>

        <el-pagination
          v-if="reg.total > 0"
          class="s-pager"
          background
          layout="total, prev, pager, next"
          :total="reg.total"
          :current-page.sync="reg.pageNum"
          :page-size="reg.pageSize"
          @current-change="loadRegister"
        />

        
      </section>
    </div>

    <!-- ==================== 新增 / 修改人员 ==================== -->
    <el-dialog :title="formTitle" :visible.sync="formVisible" width="640px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="92px" size="small">
        <div class="s-form-sec">账号信息</div>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="登录账号" prop="userName">
              <el-input v-model="form.userName" maxlength="30" placeholder="登录用户名，唯一" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="nickName">
              <el-input v-model="form.nickName" maxlength="30" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phonenumber">
              <el-input v-model="form.phonenumber" maxlength="11" placeholder="11 位手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" maxlength="50" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.sex" style="width:100%">
                <el-option label="男" value="0" />
                <el-option label="女" value="1" />
                <el-option label="未知" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="正常" value="0" />
                <el-option label="停用" value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-select v-model="form.deptId" :clearable="!isSuperRoleSelected" :disabled="isSuperRoleSelected"
                         :placeholder="isSuperRoleSelected ? '超管固定为公司根部门' : '请选择部门'" style="width:100%">
                <el-option v-for="d in deptSelectOptions" :key="d.deptId" :label="d.deptName" :value="Number(d.deptId)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="form.roleId" clearable placeholder="请选择角色（单选）" style="width:100%">
                <el-option v-for="r in roleOptions" :key="r.roleId" :label="r.roleName" :value="Number(r.roleId)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="!form.userId" :span="12">
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="新增必填" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 培养信息只在「实习生」角色下出现：非实习生（部门管理员 / 超管）不存在岗位与培养状态 -->
        <template v-if="isInternRole">
          <div class="s-form-sec">培养信息</div>
          <el-row :gutter="14">
            <el-col :span="12">
              <el-form-item label="岗位">
                <el-select v-model="form.positionId" clearable placeholder="请选择岗位" style="width:100%">
                  <el-option v-for="p in positionOptions" :key="p.id" :label="p.label" :value="Number(p.id)" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="培养状态">
                <el-select v-model="form.userStatus" style="width:100%">
                  <el-option v-for="k in statusOptions" :key="k" :label="statusText[k]" :value="k" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="协议状态">
                <el-select v-model="form.protocolStatus" :disabled="!form.userId" style="width:100%">
                  <el-option label="未签" :value="0" />
                  <el-option label="已签" :value="1" />
                </el-select>
                <div v-if="!form.userId" class="field-tip">新账号默认「未签」，由实习生本人首次登录签署后置为已签</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预计入职">
                <el-date-picker v-model="form.expectedEntryDate" type="date" value-format="yyyy-MM-dd"
                                placeholder="选填" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="导师">
                <el-select v-model="form.mentorId" filterable clearable placeholder="从导师库中选择"
                           style="width:100%" :loading="mentorOptionsLoading">
                  <el-option v-for="m in mentorOptions" :key="m.id"
                             :label="m.mentorName + '（' + (m.mentorPhone || '无联系方式') + '）'"
                             :value="m.id" />
                </el-select>
                <div class="field-tip">
                  导师库由「组织与人员 › 导师管理」维护
                  <router-link to="/super/org/mentor">去导师管理</router-link>
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <el-alert v-else type="info" :closable="false" show-icon
                  title="所选角色不是实习生（预备 / 正式），无需填写培养信息；保存后该账号会被置为「非实习生」并清空岗位。" />

        <el-alert v-if="positionDeptHint" type="warning" :closable="false" show-icon :title="positionDeptHint" />
      </el-form>
      <span slot="footer">
        <el-button size="small" @click="formVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </span>
    </el-dialog>

    <!-- ==================== 重置密码 ==================== -->
    <el-dialog title="重置密码" :visible.sync="pwdVisible" width="420px" append-to-body>
      <el-form ref="pwdForm" :model="pwdForm" :rules="pwdRules" label-width="86px" size="small">
        <el-form-item label="账号">
          <span class="muted">{{ pwdForm.nickName }}（{{ pwdForm.userName }}）</span>
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button size="small" @click="pwdVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="submitPwd">确定</el-button>
      </span>
    </el-dialog>

    <!-- ==================== 注册申请审核 ==================== -->
    <el-dialog title="处理注册申请" :visible.sync="audit.visible" width="520px" append-to-body :close-on-click-modal="false">
      <div v-if="audit.row" class="s-audit-head">
        <b>{{ audit.row.realName }}</b>
        <span class="muted"> · {{ audit.row.loginAccount }}</span>
        <div class="muted">
          {{ audit.row.positionName || '岗位缺失' }}<template v-if="audit.row.deptName"> · 自动匹配「{{ audit.row.deptName }}」</template>
          <template v-if="audit.row.expectedEntryDate"> · 预计入职 {{ fmt(audit.row.expectedEntryDate, false) }}</template>
        </div>
      </div>
      <el-radio-group v-model="audit.action" size="small" class="s-audit-action">
        <el-radio-button label="PASS">通过申请</el-radio-button>
        <el-radio-button label="REJECT">驳回申请</el-radio-button>
      </el-radio-group>
      <el-form ref="auditForm" :model="audit" :rules="auditRules" label-width="86px" size="small" style="margin-top:12px">
        <template v-if="audit.action === 'PASS'">
          <el-alert type="info" :closable="false" show-icon
                    title="通过后账号自动启用并进入「预备实习」。导师不再在这里填写 —— 请到「导师管理」维护导师库，再在「实习生管理」页给实习生选一位。" style="margin-bottom:10px" />
        </template>
        <template v-else>
          <el-alert type="warning" :closable="false" show-icon
                    title="驳回后账号保持停用，申请人可修改资料后重新提交。" style="margin-bottom:10px" />
          <el-form-item label="驳回原因" prop="reason">
            <el-input v-model="audit.reason" type="textarea" :rows="3" maxlength="200" show-word-limit
                      placeholder="请说明需要修改的内容" />
          </el-form-item>
        </template>
      </el-form>
      <span slot="footer">
        <el-button size="small" @click="audit.visible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="audit.submitting" @click="submitAudit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listUser, addUser, updateUser, resetUserPwd } from '@/api/system/user'
import { listRole } from '@/api/system/role'
import { listDept } from '@/api/system/dept'
import { listPosition, getDeptBindings } from '@/api/business/position'
import { listPersonnel, getPersonnel, savePersonnelBusiness, deletePersonnel, getPersonnelSummary } from '@/api/business/personnel'
import { listRegister, auditRegister, getRegisterSummary } from '@/api/business/register'
import { getMentorOptions } from '@/api/business/mentor'
// 催办：复用既有的「超管督办」通道（定向通知 msgType=URGE，同一人每天最多被催一次）
import { listDeptAdmins, urgeTodo } from '@/api/business/superTodo'

/** 培养状态文案：与系统其余页面（如部门端档案页）保持同一口径 */
const STATUS_TEXT = {
  WAIT_AUDIT: '注册待审',
  REJECTED: '已驳回',
  PRE_TRAINEE: '预备实习中',
  PENDING_PROMOTE: '转正审核中',
  FORMAL_TRAINEE: '已转正',
  NON_INTERN: '非实习生',
  DISABLED: '已停用',
  ARCHIVED: '已归档'
}

export default {
  name: 'SuperPersonnel',
  data() {
    return {
      tab: 'people',
      loading: false,
      submitting: false,
      // ---- 顶部数据看板 ----
      summaryLoading: false,
      summary: {
        total: 0, accountNormal: 0, accountDisabled: 0,
        stWaitAudit: 0, stPreTrainee: 0, stPendingPromote: 0, stFormalTrainee: 0,
        stNonIntern: 0, stDisabled: 0, stArchived: 0,
        protocolSigned: 0, protocolUnsigned: 0,
        roleCounts: [], deptCounts: [], noRole: 0
      },
      // ---- 人员列表 ----
      rows: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      query: { keyword: '', deptId: null, roleKey: '', userStatus: '', status: '' },
      deptOptions: [],
      rootDept: null,
      roleOptions: [],
      positionOptions: [],
      positionDeptMap: {},
      statusText: STATUS_TEXT,
      // ---- 表单 ----
      formVisible: false,
      formTitle: '新增人员',
      form: this.emptyForm(),
      rules: {
        userName: [
          { required: true, message: '请输入登录账号', trigger: 'blur' },
          { min: 2, max: 30, message: '长度在 2 到 30 个字符', trigger: 'blur' }
        ],
        nickName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        phonenumber: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
        password: [
          { required: true, message: '请输入初始密码', trigger: 'blur' },
          { min: 5, max: 20, message: '长度在 5 到 20 个字符', trigger: 'blur' }
        ]
      },
      // ---- 重置密码 ----
      pwdVisible: false,
      pwdForm: { userId: null, userName: '', nickName: '', password: '' },
      pwdRules: {
        password: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 5, max: 20, message: '长度在 5 到 20 个字符', trigger: 'blur' }
        ]
      },
      // ---- 注册申请 ----
      reg: {
        loading: false,
        rows: [],
        total: 0,
        pageNum: 1,
        pageSize: 10,
        query: { realName: '', loginAccount: '', status: '' }
      },
      pendingCount: 0,
      // ---- 注册申请统计板块 ----
      regStatLoading: false,
      regStat: {
        pending: 0, passed: 0, rejected: 0, total: 0,
        byDept: [],              // 待审按部门分布 [{label, cnt}]
        oldest: null,            // 最久待审 {realName, days, createTime}
        adminsWithPending: []     // 有注册待审的部门管理员（催办对象）
      },
      deptAdminList: [],          // 全部部门管理员（按部门定位催办对象）
      regText: { WAIT_AUDIT: '待审核', PASSED: '已通过', REJECTED: '已驳回' },
      audit: {
        visible: false,
        submitting: false,
        row: null,
        action: 'PASS',
        reason: ''
      },
      // ---- 导师库（2026-09-23：导师改为从主表选，不再手敲文本）----
      mentorOptions: [],
      mentorOptionsLoading: false
    }
  },
  computed: {
    /** 「超级管理员」角色的 roleId（内置 admin 角色已软删，不会被列出来） */
    superRoleId() {
      const r = this.roleOptions.find(x => x.roleKey === 'SUPER_ADMIN')
      return r ? Number(r.roleId) : null
    },
    /** 当前表单是否选中了超级管理员角色 */
    isSuperRoleSelected() {
      return this.superRoleId !== null && Number(this.form.roleId) === this.superRoleId
    },
    /** 当前表单选中的角色（对象） */
    selectedRole() {
      return this.roleOptions.find(r => Number(r.roleId) === Number(this.form.roleId)) || null
    },
    /**
     * 是否「实习生」角色（预备 / 正式）。
     * ★ 只有实习生才有培养信息（岗位 / 培养状态 / 协议 / 预计入职 / 导师）；
     * 部门管理员与超管不存在这些字段，表单里整块隐去。
     */
    isInternRole() {
      const k = this.selectedRole ? this.selectedRole.roleKey : ''
      return k === 'PRE_TRAINEE' || k === 'FORMAL_TRAINEE'
    },
    /** 培养状态可选值：实习生角色下不列「非实习生」，避免出现自相矛盾的状态 */
    statusOptions() {
      return Object.keys(STATUS_TEXT).filter(k => (this.isInternRole ? k !== 'NON_INTERN' : true))
    },
    /** 看板：培养状态分布（百分比以在册总人数为基数） */
    statusRows() {
      const s = this.summary
      const total = this.num(s.total)
      const defs = [
        ['WAIT_AUDIT', s.stWaitAudit],
        ['PRE_TRAINEE', s.stPreTrainee],
        ['PENDING_PROMOTE', s.stPendingPromote],
        ['FORMAL_TRAINEE', s.stFormalTrainee],
        ['NON_INTERN', s.stNonIntern],
        ['DISABLED', s.stDisabled],
        ['ARCHIVED', s.stArchived]
      ]
      return defs
        .map(([k, v]) => ({ label: STATUS_TEXT[k], cnt: this.num(v) }))
        .map(r => Object.assign(r, { pct: total ? Math.round(r.cnt / total * 100) : 0 }))
    },
    /** 看板：部门分布 */
    deptRows() {
      const total = this.num(this.summary.total)
      return (this.summary.deptCounts || []).map(d => {
        const cnt = this.num(d.cnt)
        return { label: d.deptName || ('部门' + d.deptId), cnt: cnt, pct: total ? Math.round(cnt / total * 100) : 0 }
      })
    },
    /** 注册申请：通过率（已通过 ÷ 提交总数） */
    regPassRate() {
      const t = this.regStat.total
      return t ? Math.round(this.regStat.passed / t * 100) : 0
    },
    /** 注册申请：状态分布（以提交总数为基数） */
    regStatusRows() {
      const t = this.regStat.total
      const rows = [
        { label: '待审核', cnt: this.regStat.pending },
        { label: '已通过', cnt: this.regStat.passed },
        { label: '已驳回', cnt: this.regStat.rejected }
      ]
      return rows.map(r => Object.assign(r, { pct: t ? Math.round(r.cnt / t * 100) : 0 }))
    },
    /** 注册申请：待审按部门分布（以待审数为基数） */
    regDeptRows() {
      const t = this.regStat.pending
      return (this.regStat.byDept || []).map(d => Object.assign({}, d, {
        pct: t ? Math.round(d.cnt / t * 100) : 0
      }))
    },
    /** 部门下拉：勾了超管就只能选公司根部门（融谷）；否则是 5 个培养部门 */
    deptSelectOptions() {
      if (this.isSuperRoleSelected) {
        return this.rootDept ? [{ deptId: this.rootDept.deptId, deptName: this.rootDept.deptName + '（公司根节点）' }] : []
      }
      return this.deptOptions
    },
    /** 岗位与实际所属部门不一致时给出提示（一岗位只属一个部门，错配会让课程可见性与部门口径打架） */
    positionDeptHint() {
      // 超管分支放最前：不依赖是否选了岗位，勾了超管就要解释部门为什么被锁
      if (this.isSuperRoleSelected) {
        return '超级管理员固定属于公司根部门（融谷），不归属任何子部门，所以部门不可改；服务端保存时也会强制归位。'
      }
      // 非实习生角色没有培养信息，岗位/部门错配的提示无意义（上面已用 v-else 说明）
      if (!this.isInternRole) return ''
      const pid = this.form.positionId
      if (!pid || !this.form.deptId) return ''
      const ownerDeptId = this.positionDeptMap[pid]
      if (ownerDeptId && Number(ownerDeptId) !== Number(this.form.deptId)) {
        const owner = this.deptOptions.find(d => Number(d.deptId) === Number(ownerDeptId))
        return `该岗位属于「${owner ? owner.deptName : ownerDeptId}」，与所选部门不一致 —— 课程可见性按岗位、部门口径按部门，请确认这是有意为之。`
      }
      return ''
    }
  },
  watch: {
    // 页签切换时按需加载（避免首屏多打一次无用的注册申请请求）
    tab(v) {
      if (v === 'register') this.loadRegister()
      else this.loadPersonnel()
    },
    // 勾选「超级管理员」→ 部门自动切到公司根部门；取消勾选且当前正是根部门 → 清空，避免误留
    isSuperRoleSelected(v) {
      if (v) {
        if (this.rootDept) this.form.deptId = this.rootDept.deptId
      } else if (this.rootDept && Number(this.form.deptId) === Number(this.rootDept.deptId)) {
        this.form.deptId = null
      }
    }
  },
  created() {
    const t = this.$route.query.tab
    if (t === 'register' || t === 'people') this.tab = t
    this.loadRefs()
    this.loadMentorOptions()
    this.loadPersonnel()
    this.loadPendingCount()
    this.loadSummary()
    this.loadRegOverview()
    // 直接带 ?tab=register 进来时，人员列表之外还要把申请列表也拉起来（看门 -> 首次渲染为空表）
    if (this.tab === 'register') this.loadRegister()
  },
  methods: {
    emptyForm() {
      return {
        userId: null, userName: '', nickName: '', phonenumber: '', email: '',
        sex: '0', status: '0', deptId: null, roleId: null, password: '',
        positionId: null, userStatus: 'PRE_TRAINEE', protocolStatus: 0,
        mentorId: null, expectedEntryDate: null
      }
    },
    // ---------------- 参照数据 ----------------
    /** 导师库选项（2026-09-23）：超管不限部门，拿到的是全部启用中导师 */
    loadMentorOptions() {
      this.mentorOptionsLoading = true
      getMentorOptions().then(res => {
        this.mentorOptions = (res && res.data) || []
      }).catch(() => {
        this.mentorOptions = []
      }).finally(() => {
        this.mentorOptionsLoading = false
      })
    },
    loadRefs() {
      listDept({}).then(res => {
        const all = res.data || []
        // 根节点 = 公司本体（融谷）：它**不是**可自由分配的业务部门，
        // 但超管必须落在它下面，所以单独留一份，不混进常规部门下拉。
        const root = all.find(d => Number(d.parentId) === 0)
        this.rootDept = root ? { deptId: Number(root.deptId), deptName: root.deptName } : null
        const rootIds = all.filter(d => Number(d.parentId) === 0).map(d => Number(d.deptId))
        this.deptOptions = all.filter(d => rootIds.indexOf(Number(d.deptId)) === -1)
      }).catch(() => { this.deptOptions = [] })

      listRole({ pageNum: 1, pageSize: 100 }).then(res => {
        // 排除框架内置 admin 角色：它已停用且是通配身份，不应再分配给人
        this.roleOptions = (res.rows || []).filter(r => Number(r.roleId) !== 1 && r.status === '0')
      }).catch(() => { this.roleOptions = [] })

      const bindMap = {}
      getDeptBindings().then(res => {
        (res.data || []).forEach(b => { bindMap[Number(b.positionId)] = Number(b.deptId) })
        this.positionDeptMap = bindMap
        this.buildPositionOptions()
      }).catch(() => { this.positionDeptMap = {} })

      listPosition({ pageNum: 1, pageSize: 200 }).then(res => {
        this.rawPositions = res.rows || []
        this.buildPositionOptions()
      }).catch(() => { this.rawPositions = [] })
    },
    buildPositionOptions() {
      const list = this.rawPositions || []
      if (!list.length) return
      const deptName = id => {
        const d = this.deptOptions.find(x => Number(x.deptId) === Number(id))
        return d ? d.deptName : null
      }
      this.positionOptions = list.map(p => {
        const owner = deptName(this.positionDeptMap[Number(p.id)])
        return {
          id: p.id,
          label: owner ? `${p.positionName}（${owner}）` : `${p.positionName}（未绑定部门）`
        }
      })
    },
    // ---------------- 人员列表 ----------------
    loadAll() {
      this.loadRefs()
      if (this.tab === 'people') this.loadPersonnel()
      else this.loadRegister()
      this.loadPendingCount()
      this.loadSummary()
      this.loadRegOverview()
    },
    search() {
      this.pageNum = 1
      this.loadPersonnel()
    },
    resetQuery() {
      this.query = { keyword: '', deptId: null, roleKey: '', userStatus: '', status: '' }
      this.pageNum = 1
      this.loadPersonnel()
    },
    loadPersonnel() {
      this.loading = true
      const q = Object.assign({}, this.query, { pageNum: this.pageNum, pageSize: this.pageSize })
      listPersonnel(q).then(res => {
        this.rows = res.rows || []
        this.total = res.total || 0
      }).catch(() => {
        this.rows = []
        this.total = 0
      }).finally(() => { this.loading = false })
    },
    // ---------------- 顶部数据看板 ----------------
    loadSummary() {
      this.summaryLoading = true
      getPersonnelSummary().then(res => {
        const d = res.data || {}
        this.summary = Object.assign({}, this.summary, d, {
          roleCounts: d.roleCounts || [],
          deptCounts: d.deptCounts || []
        })
      }).catch(() => {}).finally(() => { this.summaryLoading = false })
    },
    /** 数值兜底：聚合结果可能是字符串 / BigDecimal / null */
    num(v) {
      const n = Number(v)
      return isNaN(n) ? 0 : n
    },
    /** 从 roleCounts 中取某角色人数 */
    roleCount(roleKey) {
      const hit = (this.summary.roleCounts || []).find(r => r.roleKey === roleKey)
      return hit ? this.num(hit.cnt) : 0
    },
    goRegister() {
      this.tab = 'register'
      this.loadRegister()
      this.loadRegOverview()
    },
    // ---------------- 注册申请：统计板块 + 催办 ----------------
    /**
     * 一次拉齐统计板块所需数据：
     *   ① 计数（/business/register/summary：待审/通过/驳回/总数）
     *   ② 待审明细（用于算「待审部门分布」与「最久待审」）
     *   ③ 部门管理员清单（`pendingRegisters>0` 的就是催办对象，`deptId` 用于逐条催办定位）
     */
    loadRegOverview() {
      this.regStatLoading = true
      Promise.all([
        getRegisterSummary(),
        listRegister({ pageNum: 1, pageSize: 200, status: 'WAIT_AUDIT' }),
        listDeptAdmins()
      ]).then(([sumRes, pendRes, admRes]) => {
        const s = sumRes.data || {}
        const pendingRows = pendRes.rows || []
        const admins = admRes.data || []
        const byDeptMap = {}
        let oldest = null
        pendingRows.forEach(row => {
          const key = row.deptName || '部门待配置'
          byDeptMap[key] = (byDeptMap[key] || 0) + 1
          if (!row.createTime) return
          // 'YYYY-MM-DD HH:mm:ss' → 换成斜杠形式，兼容不进 Date 的浏览器
          const time = new Date(String(row.createTime).replace(/-/g, '/'))
          if (isNaN(time.getTime())) return
          const days = Math.max(0, Math.floor((Date.now() - time.getTime()) / 86400000))
          if (!oldest || days > oldest.days) {
            oldest = { realName: row.realName, days: days, createTime: String(row.createTime).slice(0, 16) }
          }
        })
        this.deptAdminList = admins
        this.regStat = {
          pending: this.num(s.pending_count),
          passed: this.num(s.passed_count),
          rejected: this.num(s.rejected_count),
          total: this.num(s.total_count),
          byDept: Object.keys(byDeptMap).map(label => ({ label: label, cnt: byDeptMap[label] }))
            .sort((a, b) => b.cnt - a.cnt),
          oldest: oldest,
          adminsWithPending: admins.filter(a => this.num(a.pendingRegisters) > 0)
        }
        this.pendingCount = this.num(s.pending_count)
      }).catch(() => {}).finally(() => { this.regStatLoading = false })
    },
    /** 催办结果统一处理（后端会返回 sent/skipped 与逐条说明） */
    handleUrgeResult(res) {
      const d = res.data || {}
      const details = d.details || []
      if (d.sent) {
        this.$message.success('已催办 ' + d.sent + ' 人' + (d.skipped ? ('，跳过 ' + d.skipped + ' 人') : ''))
      } else {
        this.$message.warning(details.length ? details.join('；') : '没有可催办的对象（同一人每天最多被催一次）')
      }
      this.loadRegOverview()
    },
    /** 一键催办：通知所有「有注册待审」的部门管理员 */
    urgeAllPending() {
      const admins = this.regStat.adminsWithPending || []
      if (!admins.length) {
        this.$message.info('当前没有待审的注册申请，无需催办')
        return
      }
      const names = admins.map(a => a.nickName || a.userName).join('、')
      this.$prompt(
        `将通知 ${admins.length} 位部门管理员：${names}。\n` +
        `注意：催办通知会自动附上该管理员的完整待办清单（不止注册审核），且同一人每天最多被催一次。\n` +
        '附加说明（可空）：',
        '催办 · 注册待审',
        { confirmButtonText: '发送催办', cancelButtonText: '取消', inputPlaceholder: '例如：请今天内处理完待审报名' }
      ).then(({ value }) => {
        return urgeTodo({ ownerIds: admins.map(a => a.userId), content: value || '' })
      }).then(res => this.handleUrgeResult(res)).catch(() => {})
    },
    /** 逐条催办：通知该申请所属部门的部门管理员 */
    urgeOne(row) {
      const admin = (this.deptAdminList || []).find(a => Number(a.deptId) === Number(row.deptId))
      if (!admin) {
        this.$message.warning('未找到该部门的部门管理员，无法催办')
        return
      }
      const name = admin.nickName || admin.userName
      this.$prompt(
        `将通知「${name}」处理该报名：${row.realName}（${row.applicationNo}）。\n` +
        `注意：催办通知会自动附上 TA 的完整待办清单，且同一人每天最多被催一次。\n` +
        '附加说明（可空）：',
        '催办 · ' + (row.realName || '注册申请'),
        { confirmButtonText: '发送催办', cancelButtonText: '取消' }
      ).then(({ value }) => {
        return urgeTodo({ ownerIds: [admin.userId], content: value || '' })
      }).then(res => this.handleUrgeResult(res)).catch(() => {})
    },
    // ---------------- 新增 / 修改 ----------------
    openAdd() {
      this.form = this.emptyForm()
      this.formTitle = '新增人员'
      this.formVisible = true
      this.$nextTick(() => { this.$refs.form && this.$refs.form.clearValidate() })
    },
    openEdit(row) {
      this.form = this.emptyForm()
      this.formTitle = '修改人员：' + (row.nickName || row.userName)
      this.formVisible = true
      this.$nextTick(() => { this.$refs.form && this.$refs.form.clearValidate() })
      getPersonnel(row.userId).then(res => {
        const d = res.data || {}
        this.form = {
          userId: d.userId,
          userName: d.userName || '',
          nickName: d.nickName || '',
          phonenumber: d.phonenumber || '',
          email: d.email || '',
          sex: d.sex != null ? String(d.sex) : '0',
          status: d.status != null ? String(d.status) : '0',
          deptId: d.deptId != null ? Number(d.deptId) : null,
          // 角色单选：取第一个有效角色（后端 roleIds 是逗号串；已软删的 role_id=1 过滤掉）
          roleId: (String(d.roleIds || '').split(',').map(Number).filter(n => n && n !== 1)[0]) || null,
          password: '',
          positionId: d.positionId != null ? Number(d.positionId) : null,
          userStatus: d.userStatus || 'NON_INTERN',
          protocolStatus: d.protocolStatus != null ? Number(d.protocolStatus) : 0,
          mentorId: d.mentorId != null ? Number(d.mentorId) : null,
          expectedEntryDate: d.expectedEntryDate ? String(d.expectedEntryDate).slice(0, 10) : null
        }
      }).catch(() => { this.formVisible = false })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const isAdd = !this.form.userId
        // 自己改自己时若摘掉超管角色，会立刻失去本页与超管菜单 —— 先确认
        if (!isAdd && Number(this.form.userId) === Number(this.$store.getters.id)) {
          const superRole = this.roleOptions.find(r => r.roleKey === 'SUPER_ADMIN')
          if (superRole && Number(this.form.roleId) !== Number(superRole.roleId)) {
            this.$confirm('你正在移除自己的「超级管理员」角色，保存后将立即失去超管菜单与本页访问权（需由他人改回）。确认继续？',
              '风险提示', { type: 'warning', confirmButtonText: '仍要保存', cancelButtonText: '返回修改' })
              .then(() => this.doSave(isAdd))
              .catch(() => {})
            return
          }
        }
        this.doSave(isAdd)
      })
    },
    doSave(isAdd) {
      this.submitting = true
      const account = {
        userName: this.form.userName,
        nickName: this.form.nickName,
        phonenumber: this.form.phonenumber,
        email: this.form.email,
        sex: this.form.sex,
        status: this.form.status,
        deptId: this.form.deptId,
        // ★ 必须回传角色：原生 updateUser 会「先清空角色再按 roleIds 重建」，少传就等于把人的角色全删了。
        //   界面是单选，这里包成单元素数组。
        roleIds: this.form.roleId ? [Number(this.form.roleId)] : []
      }
      const business = () => {
        const body = {
          userId: this.form.userId,
          // 部门走业务接口提交：它是「超管必须属融谷」这条规则的服务端校验/归位入口
          deptId: this.form.deptId
        }
        if (this.isInternRole) {
          body.positionId = this.form.positionId
          body.userStatus = this.form.userStatus
          // 新账号一律「未签」：协议须由实习生本人首次登录签署，不允许建号时就标成已签
          body.protocolStatus = isAdd ? 0 : Number(this.form.protocolStatus)
          // ★ 2026-09-23：导师改为从导师库选（mentor_id）。姓名/联系方式由后端按 mentorId 回填，
          // 前端不再传文本，避免又出现「001 / 圣诞 / (⊙﹏⊙)图」这类脏值。
          // 传 null 表示显式清空关联（后端用 containsKey 判定）。
          body.mentorId = this.form.mentorId != null ? Number(this.form.mentorId) : null
          body.expectedEntryDate = this.form.expectedEntryDate
        } else {
          // 非实习生（部门管理员 / 超管）：置为「非实习生」，并清空岗位（岗位只对实习生有意义）
          body.userStatus = 'NON_INTERN'
          body.positionId = null
        }
        return savePersonnelBusiness(body)
      }

      if (isAdd) {
        addUser(Object.assign({}, account, { password: this.form.password })).then(() => {
          // 原生新增接口不返回 userId → 用登录名（唯一）反查，再补写培养类字段
          return listUser({ userName: this.form.userName, pageNum: 1, pageSize: 1 }).then(res => {
            const created = (res.rows || [])[0]
            if (!created) {
              this.$message.warning('账号已创建，但未能定位到新账号，培养信息未写入，请手动修改一次')
              return null
            }
            this.form.userId = created.userId
            return business()
          })
        }).then(() => {
          this.$message.success('新增成功')
          this.formVisible = false
          this.loadPersonnel()
          this.loadPendingCount()
          this.loadSummary()
        }).catch(() => {
          this.$message.error('新增失败（账号可能已创建，请刷新列表确认后再补培养信息）')
          this.loadPersonnel()
        }).finally(() => { this.submitting = false })
      } else {
        updateUser(Object.assign({}, account, { userId: this.form.userId })).then(() => business()).then(() => {
          this.$message.success('修改成功')
          this.formVisible = false
          this.loadPersonnel()
          this.loadSummary()
        }).catch(() => {
          this.$message.error('修改失败')
        }).finally(() => { this.submitting = false })
      }
    },
    // ---------------- 重置密码 / 删除 ----------------
    openPwd(row) {
      this.pwdForm = { userId: row.userId, userName: row.userName, nickName: row.nickName, password: '' }
      this.pwdVisible = true
      this.$nextTick(() => { this.$refs.pwdForm && this.$refs.pwdForm.clearValidate() })
    },
    submitPwd() {
      this.$refs.pwdForm.validate(valid => {
        if (!valid) return
        this.submitting = true
        resetUserPwd(this.pwdForm.userId, this.pwdForm.password).then(() => {
          this.$message.success('密码已重置')
          this.pwdVisible = false
        }).finally(() => { this.submitting = false })
      })
    },
    /** user_id=1 是平台内置账号：RuoYi 的 checkUserAllowed 会拦下改 / 删 / 重置密码，前端一并置灰 */
    frameworkAccount(row) {
      return Number(row.userId) === 1
    },
    /** 该行是否持有「超级管理员」角色（后端返回的 roleKeys 是逗号串） */
    holdsSuperAdmin(row) {
      return String(row.roleKeys || '').split(',').indexOf('SUPER_ADMIN') > -1
    },
    /** 不可删除的原因（用于 tooltip；真正的拦截在服务端，前端只是不给点） */
    deleteBlockReason(row) {
      if (this.holdsSuperAdmin(row)) return '超管账号不可删除；如需移除请先把它改为其他角色'
      if (Number(row.userId) === Number(this.$store.getters.id)) return '当前登录账号，不可删除'
      return ''
    },
    /** 不能删内置账号、不能删超管账号、不能删自己 */
    deletable(row) {
      if (this.frameworkAccount(row)) return false
      if (this.holdsSuperAdmin(row)) return false
      return Number(row.userId) !== Number(this.$store.getters.id)
    },
    removePerson(row) {
      this.$confirm(`确认删除「${row.nickName || row.userName}」？删除后该账号将无法登录（逻辑删除，可在数据库恢复）。`,
        '删除人员', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' })
        // 走超管专属删除接口：服务端会再拦一次「内置超管 / 超管账号 / 自己」，失败原因由请求层直接提示
        .then(() => deletePersonnel(row.userId))
        .then(() => {
          this.$message.success('已删除')
          if (this.rows.length === 1 && this.pageNum > 1) this.pageNum--
          this.loadPersonnel()
          this.loadSummary()
        })
        .catch(() => {})
    },
    // ---------------- 注册申请 ----------------
    loadPendingCount() {
      listRegister({ pageNum: 1, pageSize: 1, status: 'WAIT_AUDIT' })
        .then(res => { this.pendingCount = res.total || 0 })
        .catch(() => { this.pendingCount = 0 })
    },
    loadRegister() {
      this.reg.loading = true
      const q = Object.assign({}, this.reg.query, { pageNum: this.reg.pageNum, pageSize: this.reg.pageSize })
      listRegister(q).then(res => {
        this.reg.rows = res.rows || []
        this.reg.total = res.total || 0
      }).catch(() => {
        this.reg.rows = []
        this.reg.total = 0
      }).finally(() => { this.reg.loading = false })
    },
    resetRegQuery() {
      this.reg.query = { realName: '', loginAccount: '', status: '' }
      this.reg.pageNum = 1
      this.loadRegister()
    },
    openAudit(row, action) {
      this.audit = {
        visible: true, submitting: false, row: row, action: action,
        reason: ''
      }
      this.$nextTick(() => { this.$refs.auditForm && this.$refs.auditForm.clearValidate() })
    },
    submitAudit() {
      const a = this.audit
      if (!a.row) return
      // ★ 2026-09-23：通过申请不再需要导师（导师改到导师库 + 实习生管理页分配）
      if (a.action === 'REJECT' && !a.reason) {
        this.$message.warning('驳回需填写原因')
        return
      }
      a.submitting = true
      const body = { id: a.row.id, action: a.action }
      if (a.action === 'REJECT') {
        body.reason = a.reason
      }
      auditRegister(body).then(() => {
        this.$message.success(a.action === 'PASS' ? '已通过，账号已启用；请到「实习生管理」页分配导师' : '已驳回')
        a.visible = false
        this.loadRegister()
        this.loadPendingCount()
        this.loadPersonnel()
        this.loadSummary()
        this.loadRegOverview()
      }).catch(() => {
        this.$message.error('处理失败，请刷新后重试')
      }).finally(() => { a.submitting = false })
    },
    // ---------------- 展示小工具 ----------------
    fmt(v, withTime) {
      if (v === null || v === undefined || v === '') return '—'
      const s = String(v).replace('T', ' ')
      return withTime ? s.slice(0, 16) : s.slice(0, 10)
    },
    statusTone(s) {
      if (s === 'FORMAL_TRAINEE') return 'success'
      if (s === 'PRE_TRAINEE' || s === 'PENDING_PROMOTE') return 'primary'
      if (s === 'DISABLED' || s === 'REJECTED' || s === 'ARCHIVED') return 'danger'
      if (s === 'WAIT_AUDIT') return 'warning'
      return 'info'
    },
    regTone(s) {
      if (s === 'PASSED') return 'success'
      if (s === 'REJECTED') return 'danger'
      return 'warning'
    },
    accountStatusText(s) {
      if (s === '0' || s === 0) return '正常'
      if (s === '1' || s === 1) return '停用'
      return '—'
    },
    /** 身份证号脱敏（与部门端注册审核页保持同一口径） */
    maskIdCard(idCard) {
      if (!idCard) return '—'
      if (String(idCard).length < 10) return idCard
      const s = String(idCard)
      return s.slice(0, 6) + '********' + s.slice(-4)
    },
    /** 审核情况：由注册申请列表已有字段派生，不额外请求审核记录 */
    auditHint(r) {
      if (r.status === 'WAIT_AUDIT') {
        return this.num(r.auditCount) > 1 ? '前次被驳回，待重新审核' : '等待部门管理员处理'
      }
      if (r.status === 'PASSED') {
        return r.mentorName ? '已通过并启用账号' : '已通过，待分配导师'
      }
      if (r.status === 'REJECTED') {
        return '已驳回，待申请人修改后重提'
      }
      return '—'
    },
    auditHintTone(r) {
      if (r.status === 'PASSED') return 'ok-text'
      if (r.status === 'REJECTED') return 'danger-text'
      if (r.status === 'WAIT_AUDIT') return 'warn-text'
      return ''
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/assets/styles/super-module.scss';

/* 页签：共享基线里没有 tabs，这里自包含（与「组织与岗位管理」页保持同一视觉）。
   注意：@import 进 scoped 时不能用 :root，令牌一律走 SCSS 变量（本项目铁律）。 */
.s-tabs {
  display: flex; gap: 6px; margin-bottom: 14px;
  span {
    height: 32px; line-height: 32px; padding: 0 16px; border-radius: 8px;
    background: #fff; border: 1px solid #e4e9f0; color: #667085;
    font-size: 13px; cursor: pointer; user-select: none;
    &:hover { color: #1764f5; }
    &.on { background: #1764f5; border-color: #1764f5; color: #fff; }
    .cnt {
      display: inline-block; margin-left: 5px; padding: 0 6px; height: 17px; line-height: 17px;
      border-radius: 9px; background: #f2f4f7; color: #475467; font-size: 11px; font-style: normal;
      &.warn { background: #fffaeb; color: #b54708; border: 1px solid #fedf89; }
    }
    &.on .cnt { background: rgba(255, 255, 255, .25); color: #fff; border-color: transparent; }
  }
}

.s-filters { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 12px; }
/* 卡片头右侧：提示文案 + 操作按钮（如注册申请的「一键催办」） */
.head-right { display: flex; align-items: center; gap: 10px; }
.s-pager { margin-top: 14px; text-align: right; }
.s-form-sec {
  margin: 4px 0 12px; padding-left: 8px; border-left: 3px solid #1764f5;
  color: #344054; font-size: 13px; font-weight: 600;
}
.s-audit-head {
  padding: 10px 12px; margin-bottom: 12px; border-radius: 8px;
  background: #f8fafc; border: 1px solid #e4e9f0;
  b { color: #101828; }
  .muted { display: block; margin-top: 3px; color: #667085; font-size: 12px; }
}
.s-audit-action { display: block; }

.s-tbl {
  td .sub { display: block; margin-top: 2px; color: #98a2b3; font-size: 11px; }
  td .muted { color: #98a2b3; }
  td .ok-text { color: #027a48; }
  td i.el-icon-lock { margin-left: 4px; color: #98a2b3; }
}
/* 注册申请表：展平后列多，横向滚动 + 待审行高亮 */
.s-tbl-scroll { overflow-x: auto; }
.s-tbl-scroll .s-tbl { min-width: 1790px; }
.s-tbl .warn-text { color: #b54708; }
.s-tbl .reason {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.s-tbl tr.row-pending td { background: #fffaf1; }
.field-tip { margin-top: 3px; color: #98a2b3; font-size: 11px; line-height: 1.5; }
/* 看板：可点击的 KPI（待审注册 → 跳到注册申请页签） */
.kpi-click { cursor: pointer; transition: border-color .15s; }
.kpi-click:hover { border-color: #1764f5; }
/* 看板：分布条容器（s-hbar 是共享样式，这里只补栅格间距） */
.s-hbars { display: grid; gap: 3px; }
.s-badge.warn { color: #b54708; background: #fffaeb; }
.danger-text { color: #f04438; }
</style>
