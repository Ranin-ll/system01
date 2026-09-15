<template>
  <div class="course-management app-container">
    <header class="page-heading">
      <div>
        <div class="eyebrow">学习考核 / 部门运营</div>
        <div class="title-line"><h2>部门课程管理</h2><el-tag size="mini" effect="plain" :type="readOnly ? 'info' : 'success'">{{ readOnly ? '全局只读' : '本部门范围' }}</el-tag></div>
        <p>{{ readOnly ? '查看全组织课程与学习汇总。超级管理员不参与部门课程编辑、发布和停用。' : '维护本部门岗位课程、学习章节和学员学习情况，已发布课程会同步展示在实习生学习中心。' }}</p>
      </div>
      <div class="heading-actions">
        <el-button icon="el-icon-refresh" size="small" @click="refreshAll">刷新数据</el-button>
        <el-button v-if="!readOnly" v-hasPermi="['business:course:add']" type="primary" icon="el-icon-plus" size="small" @click="handleAdd">新建课程</el-button>
      </div>
    </header>

    <section class="scope-strip">
      <div class="scope-main"><span class="scope-icon"><i class="el-icon-office-building" /></span><div><strong>{{ readOnly ? '全局课程视图' : (deptName || '当前部门') }}</strong><span>{{ readOnly ? '可查看五个部门的课程数据' : '仅管理本部门已绑定岗位：' + managedPositionText }}</span></div></div>
      <div class="scope-meta"><span><i class="el-icon-lock" /> 数据范围由登录账号决定</span><el-button v-if="readOnly" type="text" size="mini" @click="comingSoon">查看全局统计</el-button></div>
    </section>

    <el-alert
      v-if="previewMode"
      class="preview-alert"
      :title="readOnly ? '课程接口暂不可用，当前为只读演示数据。超级管理员仅可查看，课程写入操作已关闭。' : '课程接口暂不可用，当前为前端演示数据；章节、资料和学习记录接口将在下一阶段接入。'"
      type="info"
      :closable="false"
      show-icon
    />

    <el-row :gutter="14" class="summary-row">
      <el-col v-for="card in summaryCards" :key="card.key" :xs="12" :sm="8" :lg="5">
        <section class="summary-card" :class="card.tone">
          <span class="summary-icon"><i :class="card.icon" /></span>
          <div>
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
            <small>{{ card.hint }}</small>
          </div>
        </section>
      </el-col>
    </el-row>

    <section class="content-panel catalog-panel">
      <div class="panel-heading">
        <div>
          <h3>课程目录</h3>
          <p>课程按岗位归属管理，发布后实习生可在在线学习中查看对应课程。</p>
        </div>
        <el-button v-if="!readOnly" v-hasPermi="['business:course:add']" type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新建课程</el-button>
      </div>

      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small" class="query-form" @submit.native.prevent>
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="queryParams.courseName" clearable placeholder="请输入课程名称" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="适用岗位" prop="positionId">
          <el-select v-model="queryParams.positionId" clearable filterable placeholder="全部岗位">
            <el-option v-for="position in positionOptions" :key="position.id" :label="position.positionName" :value="position.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程类型" prop="courseType">
          <el-select v-model="queryParams.courseType" clearable placeholder="全部类型">
            <el-option label="理论学习" value="THEORY" />
            <el-option label="实操训练" value="PRACTICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布状态" prop="status">
          <el-select v-model="queryParams.status" clearable placeholder="全部状态">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布时间" prop="publishDate">
          <el-date-picker v-model="queryParams.publishDate" type="daterange" value-format="yyyy-MM-dd" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" clearable />
        </el-form-item>
        <el-form-item class="query-actions">
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="courseList" stripe class="course-table" empty-text="暂无课程，点击“新建课程”开始配置">
        <el-table-column label="课程" min-width="225">
          <template slot-scope="scope">
            <div class="course-cell">
              <span class="course-cover" :class="courseTone(scope.row)"><i :class="scope.row.courseType === 'PRACTICE' ? 'el-icon-video-camera' : 'el-icon-reading'" /></span>
              <div>
                <strong>{{ scope.row.courseName }}</strong>
                <small>{{ scope.row.intro || '尚未填写课程简介' }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="适用岗位" min-width="120">
          <template slot-scope="scope"><span class="position-text"><i class="el-icon-user" />{{ scope.row.positionName || positionName(scope.row.positionId) || '未设置' }}</span></template>
        </el-table-column>
        <el-table-column label="类型 / 要求" width="122" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.courseType === 'PRACTICE' ? 'warning' : 'success'">{{ courseTypeLabel(scope.row.courseType) }}</el-tag>
            <span class="required-label" :class="Number(scope.row.isRequired) === 1 ? 'required' : ''">{{ Number(scope.row.isRequired) === 1 ? '必修' : '选修' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学习内容" width="110" align="center">
          <template slot-scope="scope">
            <strong class="table-number">{{ contentSummary(scope.row).chapterCount }}</strong>
            <span class="table-subtext">章节 · {{ contentSummary(scope.row).itemCount }} 项</span>
          </template>
        </el-table-column>
        <el-table-column label="学习情况" width="132" align="center">
          <template slot-scope="scope">
            <div class="progress-cell">
              <span>应学 {{ expectedCount(scope.row) }} 人 · 已学 {{ studentCount(scope.row) }} 人</span>
              <el-progress :percentage="averageProgress(scope.row)" :show-text="false" :stroke-width="5" />
              <small>平均完成 {{ averageProgress(scope.row) }}%</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="95" align="center">
          <template slot-scope="scope"><el-tag size="mini" effect="plain" :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="更新时间" width="150">
          <template slot-scope="scope">{{ formatDate(scope.row.updateTime || scope.row.publishedAt || scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="310" fixed="right" align="center" class-name="course-actions">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="openContent(scope.row)">{{ canEditContent(scope.row) ? '编排内容' : '查看内容' }}</el-button>
            <el-button type="text" size="mini" @click="openRecords(scope.row)">记录</el-button>
            <el-button v-if="!readOnly && scope.row.status !== 'PUBLISHED'" v-hasPermi="['business:course:edit']" type="text" size="mini" @click="handleUpdate(scope.row)">编辑信息</el-button>
            <el-button v-if="!readOnly && (scope.row.status === 'DRAFT' || scope.row.status === 'DISABLED')" v-hasPermi="['business:course:publish']" type="text" size="mini" @click="handlePublish(scope.row)">{{ scope.row.status === 'DISABLED' ? '重新发布' : '发布' }}</el-button>
            <el-button v-else-if="!readOnly && scope.row.status === 'PUBLISHED'" v-hasPermi="['business:course:edit']" type="text" size="mini" class="danger-text" @click="handleDisable(scope.row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0 && !previewMode" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-dialog :title="courseDialogTitle" :visible.sync="courseDialogOpen" width="620px" append-to-body @closed="resetCourseForm">
      <div class="dialog-tip"><i class="el-icon-info" />发布课程前请至少配置一个章节和学习资料。</div>
      <el-form ref="courseForm" :model="courseForm" :rules="courseRules" label-width="92px" class="course-form">
        <el-form-item label="课程名称" prop="courseName"><el-input v-model="courseForm.courseName" maxlength="128" placeholder="例如：开发流程与代码交付规范" /></el-form-item>
        <el-form-item label="适用岗位" prop="positionId">
          <el-select v-model="courseForm.positionId" filterable placeholder="请选择岗位" class="form-full">
            <el-option v-for="position in positionOptions" :key="position.id" :label="position.positionName" :value="position.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程类型" prop="courseType">
          <el-radio-group v-model="courseForm.courseType"><el-radio label="THEORY">理论学习</el-radio><el-radio label="PRACTICE">实操训练</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="课程要求" prop="isRequired">
          <el-radio-group v-model="courseForm.isRequired"><el-radio :label="1">必修课程</el-radio><el-radio :label="0">选修课程</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="课程简介" prop="intro"><el-input v-model="courseForm.intro" type="textarea" :rows="4" maxlength="300" show-word-limit placeholder="说明课程目标、适用场景和完成要求" /></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="courseDialogOpen = false">取消</el-button><el-button type="primary" :loading="courseSubmitting" @click="submitCourse">保存课程</el-button></div>
    </el-dialog>

    <el-drawer :visible.sync="contentDrawerOpen" :with-header="false" direction="rtl" size="680px" append-to-body class="manage-drawer">
      <template v-if="currentCourse">
        <div class="drawer-heading">
          <div class="drawer-heading-main">
            <span class="drawer-cover" :class="courseTone(currentCourse)"><i :class="currentCourse.courseType === 'PRACTICE' ? 'el-icon-video-camera' : 'el-icon-reading'" /></span>
            <div><span>课程内容</span><h3>{{ currentCourse.courseName }}</h3><p>{{ currentCourse.positionName || positionName(currentCourse.positionId) }} · {{ courseTypeLabel(currentCourse.courseType) }} · {{ Number(currentCourse.isRequired) === 1 ? '必修' : '选修' }}</p></div>
          </div>
          <el-button icon="el-icon-close" circle size="mini" @click="contentDrawerOpen = false" />
        </div>
        <div v-loading="contentLoading" class="drawer-body">
          <div class="content-toolbar">
            <div>
              <strong>{{ contentSummary(currentCourse).chapterCount }} 个章节</strong><span> · {{ contentSummary(currentCourse).itemCount }} 项学习资料</span>
              <span class="content-save-state" :class="contentDirty ? 'is-dirty' : 'is-saved'"><i :class="contentDirty ? 'el-icon-warning-outline' : 'el-icon-circle-check'" />{{ contentDirty ? '有待保存调整' : '编排已保存' }}</span>
            </div>
            <div class="content-toolbar-actions">
              <el-tag size="mini" effect="plain" :type="contentCheck(currentCourse).ready ? 'success' : 'warning'"><i :class="contentCheck(currentCourse).ready ? 'el-icon-circle-check' : 'el-icon-warning-outline'" /> {{ contentCheck(currentCourse).label }}</el-tag>
              <el-button v-if="canEditContent(currentCourse)" size="mini" icon="el-icon-check" :loading="contentSaving" @click="saveContentDraft">保存编排</el-button>
              <el-button v-if="canEditContent(currentCourse)" type="primary" plain size="mini" icon="el-icon-plus" @click="openChapterDialog()">新增章节</el-button>
            </div>
          </div>
          <el-alert class="drawer-alert" :title="contentStatusTip(currentCourse)" :type="canEditContent(currentCourse) ? 'info' : 'success'" :closable="false" show-icon />
          <draggable v-if="courseContents(currentCourse).length" :list="courseContents(currentCourse)" class="chapter-list" handle=".chapter-drag-handle" :disabled="!canEditContent(currentCourse)" @end="markContentDirty">
            <section v-for="(chapter, chapterIndex) in courseContents(currentCourse)" :key="chapter.id" class="chapter-block">
              <header class="chapter-heading">
                <div><button v-if="canEditContent(currentCourse)" type="button" class="drag-handle chapter-drag-handle" title="拖拽调整章节顺序"><i class="el-icon-rank" /></button><span class="chapter-order">{{ String(chapterIndex + 1).padStart(2, '0') }}</span><span class="chapter-heading-copy"><strong>{{ chapter.chapterName }}</strong><small>{{ chapter.chapterIntro || '暂未填写内容简介' }}</small></span><small class="chapter-item-count">{{ chapter.items.length }} 项资料</small></div>
                <div v-if="canEditContent(currentCourse)" class="chapter-actions"><el-button type="text" size="mini" @click="openChapterDialog(chapter)">编辑</el-button><el-button type="text" size="mini" class="danger-text" @click="removeChapter(chapter)">删除</el-button></div>
              </header>
              <draggable :list="chapter.items" class="resource-list" handle=".resource-drag-handle" :disabled="!canEditContent(currentCourse)" @end="markContentDirty">
                <div v-for="item in chapter.items" :key="item.id" class="resource-row">
                  <button v-if="canEditContent(currentCourse)" type="button" class="drag-handle resource-drag-handle" title="拖拽调整资料顺序"><i class="el-icon-rank" /></button>
                  <span class="resource-icon" :class="item.itemType.toLowerCase()"><i :class="resourceIcon(item.itemType)" /></span>
                  <div class="resource-info"><strong>{{ item.itemTitle }}</strong><small v-if="item.itemIntro" class="resource-intro">{{ item.itemIntro }}</small><span>{{ resourceTypeLabel(item.itemType) }} · {{ item.duration || 0 }} 分钟 · {{ completionRuleLabel(item.completionRule) }}</span><small class="resource-file-state" :class="item.fileName || item.contentUrl ? 'is-bound' : 'is-missing'"><i :class="item.fileName || item.contentUrl ? 'el-icon-paperclip' : 'el-icon-warning-outline'" /> {{ item.fileName || (item.itemType === 'QUIZ' ? '题目接口待接入' : '待上传文件') }}</small></div>
                  <div v-if="canEditContent(currentCourse)" class="resource-actions"><el-button type="text" size="mini" @click="openItemDialog(chapter, item)">编辑</el-button><el-button type="text" size="mini" class="danger-text" @click="removeItem(chapter, item)">删除</el-button></div>
                </div>
              </draggable>
              <el-button v-if="canEditContent(currentCourse)" class="add-resource" icon="el-icon-plus" size="mini" @click="openItemDialog(chapter)">添加学习资料</el-button>
            </section>
          </draggable>
          <el-empty v-else description="还没有学习章节"><el-button v-if="canEditContent(currentCourse)" type="primary" size="small" @click="openChapterDialog()">新增第一个章节</el-button></el-empty>
        </div>
      </template>
    </el-drawer>

    <el-dialog :title="chapterForm.id ? '编辑章节' : '新增章节'" :visible.sync="chapterDialogOpen" width="520px" append-to-body>
      <el-form ref="chapterForm" :model="chapterForm" :rules="chapterRules" label-width="82px">
        <el-form-item label="章节名称" prop="chapterName"><el-input v-model="chapterForm.chapterName" maxlength="128" placeholder="例如：第一章 入职与保密" /></el-form-item>
        <el-form-item label="内容简介" prop="chapterIntro"><el-input v-model="chapterForm.chapterIntro" type="textarea" :rows="4" maxlength="300" show-word-limit placeholder="简要说明本章节的学习目标、主要内容和学习重点" /></el-form-item>
        <el-form-item label="学习要求"><el-radio-group v-model="chapterForm.isRequired"><el-radio :label="1">必修</el-radio><el-radio :label="0">选修</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="chapterDialogOpen = false">取消</el-button><el-button type="primary" @click="saveChapter">保存章节</el-button></div>
    </el-dialog>

    <el-dialog :title="itemForm.id ? '编辑学习资料' : '添加学习资料'" :visible.sync="itemDialogOpen" width="560px" append-to-body>
      <el-form ref="itemForm" :model="itemForm" :rules="itemRules" label-width="98px">
        <el-form-item label="资料名称" prop="itemTitle"><el-input v-model="itemForm.itemTitle" maxlength="128" placeholder="请输入学习资料名称" /></el-form-item>
        <el-form-item label="内容简介" prop="itemIntro"><el-input v-model="itemForm.itemIntro" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="简要说明本节学习目标、主要内容和学习重点" /></el-form-item>
        <el-form-item label="资料类型" prop="itemType"><el-radio-group v-model="itemForm.itemType" @change="handleItemTypeChange"><el-radio label="DOC">文档</el-radio><el-radio label="VIDEO">视频</el-radio><el-radio label="QUIZ">章节测试</el-radio></el-radio-group></el-form-item>
        <el-form-item label="预计时长"><el-input-number v-model="itemForm.duration" :min="0" :max="600" controls-position="right" /><span class="unit-text">分钟</span></el-form-item>
        <el-form-item label="完成方式"><el-select v-model="itemForm.completionRule" class="form-full"><el-option label="阅读到底并确认" value="SCROLL_END" /><el-option label="观看至完成进度" value="PLAY_TO_END" /><el-option label="提交测试答案" value="QUIZ_SUBMIT" /></el-select></el-form-item>
        <el-form-item label="完成要求"><el-switch v-model="itemForm.isRequired" :active-value="1" :inactive-value="0" active-text="必修" inactive-text="选修" /><span v-if="itemForm.itemType === 'VIDEO'" class="threshold-text">视频完成阈值 {{ itemForm.completionThreshold || 100 }}%</span></el-form-item>
        <el-form-item v-if="itemForm.itemType === 'VIDEO'" label="完成阈值"><el-slider v-model="itemForm.completionThreshold" :min="80" :max="100" :step="5" show-stops /><span class="form-tip">达到该播放进度后自动完成，后端仍会校验进度。</span></el-form-item>
        <el-form-item v-if="itemForm.itemType !== 'QUIZ'" label="资料文件">
          <el-upload ref="assetUpload" class="asset-upload" action="#" :auto-upload="false" :show-file-list="false" :accept="itemAccept" :limit="1" :on-change="handleAssetChange" :on-exceed="handleAssetExceed">
            <el-button size="small" plain icon="el-icon-upload2" :disabled="assetUploadState === 'UPLOADING'">{{ itemForm.fileName ? '替换文件' : '选择文件' }}</el-button>
          </el-upload>
          <div v-if="itemForm.fileName" class="selected-file">
            <i class="el-icon-paperclip" />
            <div><strong>{{ itemForm.fileName }}</strong><span>{{ fileSizeText(itemForm.fileSize) }} · {{ assetUploadLabel() }}</span></div>
            <el-button type="text" size="mini" class="danger-text" :disabled="assetUploadState === 'UPLOADING'" @click="clearAsset">移除</el-button>
          </div>
          <div v-if="assetUploadState !== 'IDLE' && assetUploadState !== 'READY'" class="asset-upload-progress">
            <el-progress :percentage="assetUploadProgress" :status="assetUploadState === 'SUCCESS' ? 'success' : assetUploadState === 'ERROR' ? 'exception' : undefined" :stroke-width="7" />
            <span :class="'upload-' + assetUploadState.toLowerCase()">{{ assetUploadMessage }}</span>
          </div>
          <span class="form-tip">支持 {{ itemForm.itemType === 'VIDEO' ? 'MP4、WebM、MOV，单文件不超过 500MB' : 'PDF、DOCX、PPTX、TXT、ZIP，单文件不超过 50MB' }}。文件上传到服务器资源目录，数据库保存访问路径和文件元数据。</span>
        </el-form-item>
        <el-form-item v-else label="题目配置"><el-alert title="章节测试沿用考核题库接口，本轮先保留资料类型和完成规则入口。" type="info" :closable="false" show-icon /></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="itemDialogOpen = false">取消</el-button><el-button type="primary" :loading="itemSubmitting" @click="saveItem">保存资料</el-button></div>
    </el-dialog>

    <el-drawer :visible.sync="recordDrawerOpen" :with-header="false" direction="rtl" size="720px" append-to-body class="manage-drawer">
      <template v-if="currentCourse">
        <div class="drawer-heading">
          <div><span>学习记录</span><h3>{{ currentCourse.courseName }}</h3><p>查看已分配实习生的课程完成情况与最近学习动态。</p></div>
          <el-button icon="el-icon-close" circle size="mini" @click="recordDrawerOpen = false" />
        </div>
        <div class="drawer-body record-body">
          <el-row :gutter="10" class="record-summary">
            <el-col v-for="item in recordSummary" :key="item.label" :span="8"><div><span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.hint }}</small></div></el-col>
          </el-row>
          <div class="record-toolbar">
            <el-radio-group v-model="recordStatusFilter" size="small"><el-radio-button label="ALL">全部</el-radio-button><el-radio-button label="NOT_STARTED">未开始</el-radio-button><el-radio-button label="IN_PROGRESS">学习中</el-radio-button><el-radio-button label="DONE">已完成</el-radio-button></el-radio-group>
            <el-button icon="el-icon-download" size="mini" @click="comingSoon">导出记录</el-button>
          </div>
          <el-alert class="drawer-alert" title="历史学习记录会永久保留；课程停用、重新编辑和再次发布都不会清除既有进度。" type="info" :closable="false" show-icon />
          <el-table v-loading="recordLoading" :data="filteredRecords" stripe class="record-table" empty-text="暂无符合条件的学习记录">
            <el-table-column label="实习生" min-width="132"><template slot-scope="scope"><div class="student-cell"><span>{{ scope.row.name.slice(0, 1) }}</span><div><strong>{{ scope.row.name }}</strong><small>{{ scope.row.position }}</small></div></div></template></el-table-column>
            <el-table-column label="学习进度" min-width="160"><template slot-scope="scope"><div class="record-progress"><div><span>{{ scope.row.completed }}/{{ scope.row.total }} 项</span><strong>{{ scope.row.progress }}%</strong></div><el-progress :percentage="scope.row.progress" :show-text="false" :stroke-width="6" /></div></template></el-table-column>
            <el-table-column label="状态" width="92" align="center"><template slot-scope="scope"><el-tag size="mini" :type="recordStatusTag(scope.row.status)">{{ recordStatusLabel(scope.row.status) }}</el-tag></template></el-table-column>
            <el-table-column label="最近学习" prop="lastStudy" width="142" />
          </el-table>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script>
import { addCourse, disableCourse, listCourse, listCoursePositions, publishCourse, updateCourse } from '@/api/business/course'
import {
  addCourseChapter,
  addStudyItem,
  deleteCourseChapter,
  deleteStudyItem,
  getCourseContents,
  listCourseStudyRecords,
  saveCourseContents,
  updateCourseChapter,
  updateStudyItem,
  uploadStudyAsset
} from '@/api/business/courseContent'
import draggable from 'vuedraggable'

const CONTENT_STORAGE_KEY = 'intern-course-management-content:'

const sampleCourses = [
  { id: 'preview-development-foundation', deptName: '开发部门', courseName: '开发流程与代码交付规范', positionId: 2, positionName: '开发实习生', courseType: 'THEORY', isRequired: 1, intro: '从需求理解、分支协作到代码交付，建立统一的开发工作方法。', status: 'PUBLISHED', expectedStudentCount: 1, studentCount: 1, avgCompletionRate: 38, createTime: '2026-09-01 09:30:00', updateTime: '2026-09-10 16:20:00' },
  { id: 'preview-development-tools', deptName: '开发部门', courseName: '开发工具与项目环境', positionId: 2, positionName: '开发实习生', courseType: 'PRACTICE', isRequired: 0, intro: '了解项目本地环境、调试工具和基础协作约定。', status: 'DRAFT', expectedStudentCount: 1, studentCount: 0, avgCompletionRate: 0, createTime: '2026-09-07 10:10:00', updateTime: '2026-09-10 14:40:00' },
  { id: 'preview-delivery-foundation', deptName: '交付部门', courseName: '项目实施与交付规范', positionId: 1, positionName: '实施实习生', courseType: 'THEORY', isRequired: 1, intro: '掌握项目实施流程、交付标准与现场协作规范。', status: 'PUBLISHED', expectedStudentCount: 1, studentCount: 1, avgCompletionRate: 56, createTime: '2026-08-28 13:40:00', updateTime: '2026-09-09 11:30:00' },
  { id: 'preview-design-foundation', deptName: '设计部门', courseName: '设计规范与成果交付', positionId: 3, positionName: '设计实习生', courseType: 'THEORY', isRequired: 1, intro: '统一设计规范、交付格式和评审协作流程。', status: 'DRAFT', expectedStudentCount: 1, studentCount: 0, avgCompletionRate: 0, createTime: '2026-09-05 09:10:00', updateTime: '2026-09-10 09:10:00' },
  { id: 'preview-qa-foundation', deptName: '质检部门', courseName: '质量检查与缺陷处理规范', positionId: 4, positionName: '质检实习生', courseType: 'THEORY', isRequired: 1, intro: '掌握检查清单、缺陷分级和复测闭环。', status: 'DRAFT', expectedStudentCount: 1, studentCount: 0, avgCompletionRate: 0, createTime: '2026-09-06 09:10:00', updateTime: '2026-09-10 09:10:00' },
  { id: 'preview-model-foundation', deptName: '建模部门', courseName: '建模流程与文件规范', positionId: 5, positionName: '建模实习生', courseType: 'PRACTICE', isRequired: 1, intro: '建立模型制作、命名和文件交付标准。', status: 'DRAFT', expectedStudentCount: 1, studentCount: 0, avgCompletionRate: 0, createTime: '2026-09-06 09:10:00', updateTime: '2026-09-10 09:10:00' }
]

const fallbackPositions = [
  { id: 1, positionName: '实施实习生' },
  { id: 2, positionName: '开发实习生' },
  { id: 3, positionName: '设计实习生' },
  { id: 4, positionName: '质检实习生' },
  { id: 5, positionName: '建模实习生' }
]

const fallbackPositionsByDepartment = {
  '交付部门': [fallbackPositions[0]],
  '开发部门': [fallbackPositions[1]],
  '设计部门': [fallbackPositions[2]],
  '质检部门': [fallbackPositions[3]],
  '建模部门': [fallbackPositions[4]]
}

const departmentPositionMap = {
  '交付部门': '实施实习生',
  '开发部门': '开发实习生',
  '设计部门': '设计实习生',
  '质检部门': '质检实习生',
  '建模部门': '建模实习生'
}

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

function defaultContent(course) {
  const courseName = course.courseName || '岗位基础课程'
  return [
    { id: 'chapter-' + course.id + '-1', chapterName: '第一章 入职与岗位规范', chapterIntro: '了解岗位职责、工作边界及资料安全要求，为后续学习建立基础。', isRequired: 1, items: [
      { id: 'item-' + course.id + '-1', itemTitle: courseName + '说明', itemType: 'DOC', duration: 18, completionRule: 'SCROLL_END' },
      { id: 'item-' + course.id + '-2', itemTitle: '岗位资料安全操作演示', itemType: 'VIDEO', duration: 14, completionRule: 'PLAY_TO_END' }
    ] },
    { id: 'chapter-' + course.id + '-2', chapterName: '第二章 协作流程与质量要求', chapterIntro: '掌握日常协作流程、交付检查点和基本质量标准。', isRequired: 1, items: [
      { id: 'item-' + course.id + '-3', itemTitle: '流程检查清单', itemType: 'DOC', duration: 20, completionRule: 'SCROLL_END' },
      { id: 'item-' + course.id + '-4', itemTitle: '章节自测', itemType: 'QUIZ', duration: 10, completionRule: 'QUIZ_SUBMIT' }
    ] }
  ]
}

export default {
  name: 'Course',
  data() {
    return {
      loading: true,
      courseSubmitting: false,
      previewMode: false,
      courseApiAvailable: true,
      total: 0,
      courseList: [],
      positionOptions: clone(fallbackPositions),
      queryParams: { pageNum: 1, pageSize: 10, courseName: undefined, positionId: undefined, courseType: undefined, status: undefined, publishDate: undefined },
      courseDialogOpen: false,
      courseDialogTitle: '',
      courseForm: {},
      courseRules: {
        courseName: [{ required: true, message: '请填写课程名称', trigger: 'blur' }],
        positionId: [{ required: true, message: '请选择适用岗位', trigger: 'change' }],
        courseType: [{ required: true, message: '请选择课程类型', trigger: 'change' }]
      },
      contentDrawerOpen: false,
      contentLoading: false,
      contentSaving: false,
      recordDrawerOpen: false,
      recordLoading: false,
      recordList: [],
      currentCourse: null,
      contentStore: {},
      contentDirty: false,
      chapterDialogOpen: false,
      chapterForm: {},
      chapterRules: { chapterName: [{ required: true, message: '请填写章节名称', trigger: 'blur' }] },
      itemDialogOpen: false,
      itemSubmitting: false,
      itemForm: {},
      activeChapterId: null,
      itemRules: { itemTitle: [{ required: true, message: '请填写资料名称', trigger: 'blur' }], itemType: [{ required: true, message: '请选择资料类型', trigger: 'change' }] },
      recordStatusFilter: 'ALL'
    }
  },
  computed: {
    roles() {
      return this.$store.getters.roles || []
    },
    deptName() {
      return this.$store.getters.deptName || ''
    },
    readOnly() {
      return this.roles.indexOf('SUPER_ADMIN') > -1 || this.roles.indexOf('admin') > -1
    },
    managedPositionText() {
      const names = this.positionOptions.map(item => item.positionName).filter(Boolean)
      return names.length ? names.join('、') : '待加载'
    },
    summaryCards() {
      const courses = this.courseList
      const published = courses.filter(item => item.status === 'PUBLISHED')
      const draft = courses.filter(item => item.status === 'DRAFT')
      const learners = courses.reduce((sum, item) => sum + this.expectedCount(item), 0)
      const contentCount = courses.reduce((sum, item) => sum + this.contentSummary(item).itemCount, 0)
      const progressCourses = published.filter(item => this.expectedCount(item) > 0)
      const average = progressCourses.length ? Math.round(progressCourses.reduce((sum, item) => sum + this.averageProgress(item), 0) / progressCourses.length) + '%' : '暂无'
      return [
        { key: 'all', label: '课程总数', value: courses.length, hint: '当前部门课程目录', tone: 'blue', icon: 'el-icon-reading' },
        { key: 'draft', label: '草稿数', value: draft.length, hint: '待补充内容后发布', tone: 'orange', icon: 'el-icon-edit-outline' },
        { key: 'published', label: '已发布', value: published.length, hint: '实习生学习中心可见', tone: 'green', icon: 'el-icon-circle-check' },
        { key: 'learner', label: '应学人数', value: learners, hint: contentCount + ' 项学习资料已配置', tone: 'purple', icon: 'el-icon-user' },
        { key: 'completion', label: '平均完成率', value: average, hint: '按已发布课程汇总', tone: 'teal', icon: 'el-icon-data-analysis' }
      ]
    },
    recordSummary() {
      const records = this.courseRecords(this.currentCourse)
      const total = records.length
      const complete = records.filter(item => item.status === 'DONE').length
      const average = total ? Math.round(records.reduce((sum, item) => sum + item.progress, 0) / total) : 0
      return [
        { label: '应学人员', value: total + ' 人', hint: '本岗位应学人员' },
        { label: '已完成课程', value: complete + ' 人', hint: '达到课程完成要求' },
        { label: '平均完成率', value: average + '%', hint: '按学习单项汇总' }
      ]
    },
    filteredRecords() {
      const records = this.courseRecords(this.currentCourse)
      return this.recordStatusFilter === 'ALL' ? records : records.filter(item => item.status === this.recordStatusFilter)
    },
    itemAccept() {
      return this.itemForm.itemType === 'VIDEO' ? '.mp4,.webm,.mov' : '.pdf,.doc,.docx,.ppt,.pptx,.txt,.zip'
    }
  },
  components: { draggable },
  created() {
    this.loadContentStore()
    this.positionOptions = this.filterPositions(clone(fallbackPositions))
    this.getPositions()
    this.getList()
  },
  methods: {
    refreshAll() {
      this.getPositions()
      this.getList()
    },
    getPositions() {
      listCoursePositions().then(response => {
        const rows = response.data || []
        this.positionOptions = rows.length ? rows : this.filterPositions(clone(fallbackPositions))
      }).catch(() => {
        // 课程岗位接口暂不可用时仍保留当前部门的安全兜底，不展示其他部门岗位。
        this.positionOptions = this.filterPositions(clone(fallbackPositions))
      })
    },
    getList() {
      this.loading = true
      const query = Object.assign({}, this.queryParams)
      delete query.publishDate
      listCourse(query).then(response => {
        this.courseList = this.filterCourses((response.rows || []).filter(item => this.matchesQuery(item)))
        this.total = response.total || 0
        this.previewMode = false
        this.courseApiAvailable = true
      }).catch(() => {
        this.courseApiAvailable = false
        this.usePreviewCourses()
      }).finally(() => {
        this.loading = false
      })
    },
    usePreviewCourses() {
      this.previewMode = true
      const courses = this.filterCourses(clone(sampleCourses))
      this.total = courses.length
      this.courseList = courses.filter(item => this.matchesQuery(item))
    },
    filterPositions(rows) {
      if (this.readOnly || !this.deptName || !departmentPositionMap[this.deptName]) return rows
      const positionName = departmentPositionMap[this.deptName]
      const filtered = rows.filter(item => item.positionName === positionName || item.deptName === this.deptName)
      return filtered.length ? filtered : clone(fallbackPositionsByDepartment[this.deptName] || [])
    },
    filterCourses(rows) {
      if (this.readOnly || !this.deptName || !departmentPositionMap[this.deptName]) return rows
      const positionName = departmentPositionMap[this.deptName]
      return rows.filter(item => item.deptName === this.deptName || item.positionName === positionName)
    },
    hasQuery() {
      return Boolean(this.queryParams.courseName || this.queryParams.positionId || this.queryParams.courseType || this.queryParams.status || (this.queryParams.publishDate && this.queryParams.publishDate.length))
    },
    matchesQuery(item) {
      const name = String(item.courseName || '').toLowerCase()
      const queryName = String(this.queryParams.courseName || '').toLowerCase()
      return (!queryName || name.indexOf(queryName) > -1) &&
        (!this.queryParams.positionId || String(item.positionId) === String(this.queryParams.positionId)) &&
        (!this.queryParams.courseType || item.courseType === this.queryParams.courseType) &&
        (!this.queryParams.status || item.status === this.queryParams.status) &&
        this.matchesPublishDate(item)
    },
    matchesPublishDate(item) {
      if (!this.queryParams.publishDate || this.queryParams.publishDate.length !== 2) return true
      const value = String(item.publishedAt || item.updateTime || item.createTime || '').slice(0, 10)
      return value >= this.queryParams.publishDate[0] && value <= this.queryParams.publishDate[1]
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      if (this.previewMode) this.usePreviewCourses()
      else this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetCourseForm() {
      this.courseForm = { id: undefined, courseName: '', positionId: undefined, courseType: 'THEORY', isRequired: 1, intro: '' }
      this.$nextTick(() => this.resetForm('courseForm'))
    },
    handleAdd() {
      this.resetCourseForm()
      this.courseDialogTitle = '新建课程'
      this.courseDialogOpen = true
    },
    handleUpdate(row) {
      this.courseForm = { id: row.id, courseName: row.courseName, positionId: row.positionId, courseType: row.courseType, isRequired: Number(row.isRequired), intro: row.intro || '' }
      this.courseDialogTitle = '编辑课程'
      this.courseDialogOpen = true
    },
    submitCourse() {
      this.$refs.courseForm.validate(valid => {
        if (!valid) return
        if (!this.courseApiAvailable || (this.previewMode && this.courseForm.id && String(this.courseForm.id).indexOf('preview-') === 0)) {
          this.savePreviewCourse()
          return
        }
        this.courseSubmitting = true
        const request = this.courseForm.id ? updateCourse(this.courseForm) : addCourse(this.courseForm)
        request.then(() => {
          this.$modal.msgSuccess(this.courseForm.id ? '课程已更新' : '课程已创建，请继续配置章节资料')
          this.courseDialogOpen = false
          this.getList()
        }).finally(() => {
          this.courseSubmitting = false
        })
      })
    },
    savePreviewCourse() {
      const selectedPosition = this.positionOptions.find(item => String(item.id) === String(this.courseForm.positionId))
      const form = Object.assign({}, this.courseForm, { deptName: this.deptName, positionName: selectedPosition ? selectedPosition.positionName : '未设置', updateTime: this.nowText(), status: this.courseForm.status || 'DRAFT', expectedStudentCount: 1, studentCount: 0, avgCompletionRate: 0 })
      const isEdit = Boolean(form.id)
      if (form.id) {
        const index = this.courseList.findIndex(item => String(item.id) === String(form.id))
        if (index > -1) this.$set(this.courseList, index, Object.assign({}, this.courseList[index], form))
      } else {
        form.id = 'preview-' + Date.now()
        form.createTime = form.updateTime
        this.courseList.unshift(form)
      }
      this.total = this.courseList.length
      this.courseDialogOpen = false
      this.$modal.msgSuccess(isEdit ? '课程已保存' : '课程已创建，请继续配置章节资料')
    },
    handlePublish(row) {
      if (!(this.previewMode && String(row.id).indexOf('preview-') === 0)) {
        this.loadCourseContents(row).then(() => this.confirmPublish(row))
        return
      }
      this.confirmPublish(row)
    },
    confirmPublish(row) {
      const check = this.contentCheck(row)
      if (!check.ready) {
        this.$modal.msgWarning('发布前还需完善：' + check.missing.join('；'))
        this.openContent(row)
        return
      }
      const republish = row.status === 'DISABLED'
      this.$modal.confirm('确认' + (republish ? '重新发布' : '发布') + '课程“' + row.courseName + '”？发布后对应部门岗位实习生可在在线学习中查看，已有学习记录保持不变。').then(() => {
        if (this.previewMode && String(row.id).indexOf('preview-') === 0) {
          row.status = 'PUBLISHED'
          row.updateTime = this.nowText()
          this.$modal.msgSuccess(republish ? '课程已重新发布' : '课程已发布')
          return null
        }
        return publishCourse(row.id).then(() => {
          this.$modal.msgSuccess(republish ? '课程已重新发布' : '课程已发布')
          this.getList()
        })
      }).catch(() => {})
    },
    handleDisable(row) {
      this.$modal.confirm('确认停用课程“' + row.courseName + '”？已产生的学习记录会保留。').then(() => {
        if (this.previewMode && String(row.id).indexOf('preview-') === 0) {
          row.status = 'DISABLED'
          row.updateTime = this.nowText()
          this.$modal.msgSuccess('课程已停用')
          return null
        }
        return disableCourse(row.id).then(() => {
          this.$modal.msgSuccess('课程已停用')
          this.getList()
        })
      }).catch(() => {})
    },
    openContent(course) {
      this.currentCourse = course
      this.contentDirty = false
      this.contentDrawerOpen = true
      if (this.previewMode && String(course.id).indexOf('preview-') === 0) {
        this.ensureContent(course)
      } else {
        this.loadCourseContents(course)
      }
    },
    loadCourseContents(course) {
      if (!course) return Promise.resolve([])
      this.contentLoading = true
      return getCourseContents(course.id).then(response => {
        const chapters = (response.data || []).map(chapter => Object.assign({}, chapter, {
          items: (chapter.items || []).map(item => Object.assign({}, item, {
            assetStatus: item.contentUrl ? 'UPLOADED' : 'UNBOUND'
          }))
        }))
        this.$set(this.contentStore, String(course.id), chapters)
        return chapters
      }).finally(() => {
        this.contentLoading = false
      })
    },
    openRecords(course) {
      this.currentCourse = course
      this.recordStatusFilter = 'ALL'
      this.recordDrawerOpen = true
      if (this.previewMode && String(course.id).indexOf('preview-') === 0) {
        this.recordList = this.previewCourseRecords(course)
        return
      }
      this.recordLoading = true
      this.recordList = []
      listCourseStudyRecords(course.id, { status: 'ALL' }).then(response => {
        this.recordList = (response.data || []).map(record => this.normalizeCourseRecord(record, course))
      }).catch(() => {
        this.recordList = []
      }).finally(() => {
        this.recordLoading = false
      })
    },
    ensureContent(course) {
      const key = String(course.id)
      if (!this.contentStore[key]) {
        const preview = this.previewMode && String(course.id).indexOf('preview-') === 0
        this.$set(this.contentStore, key, preview ? defaultContent(course) : [])
        if (preview) this.saveContentStore()
      }
    },
    courseContents(course) {
      if (!course) return []
      this.ensureContent(course)
      return this.contentStore[String(course.id)] || []
    },
    contentSummary(course) {
      const stored = course && this.contentStore[String(course.id)]
      if (!stored && course && (course.chapterCount !== undefined || course.itemCount !== undefined)) {
        return { chapterCount: Number(course.chapterCount || 0), itemCount: Number(course.itemCount || 0) }
      }
      const chapters = this.courseContents(course)
      return { chapterCount: chapters.length, itemCount: chapters.reduce((sum, chapter) => sum + chapter.items.length, 0) }
    },
    contentCheck(course) {
      const chapters = this.courseContents(course)
      const missing = []
      if (!chapters.length) missing.push('至少新增一个章节')
      chapters.forEach((chapter, index) => {
        if (!chapter.items || !chapter.items.length) missing.push('第' + (index + 1) + '章还没有学习资料')
        ;(chapter.items || []).forEach(item => {
          if (item.itemType !== 'QUIZ' && !item.fileName && !item.contentUrl) missing.push('“' + item.itemTitle + '”尚未绑定文件')
        })
      })
      return { ready: missing.length === 0, label: missing.length ? '还缺 ' + missing.length + ' 项' : '发布条件已满足', missing: missing.slice(0, 4) }
    },
    markContentDirty() {
      this.contentDirty = true
      if (this.previewMode) this.saveContentStore()
    },
    saveContentDraft() {
      if (!this.currentCourse) return
      if (this.previewMode && String(this.currentCourse.id).indexOf('preview-') === 0) {
        this.saveContentStore()
        this.contentDirty = false
        this.$modal.msgSuccess('课程编排已保存')
        return
      }
      this.contentSaving = true
      saveCourseContents(this.currentCourse.id, { chapters: this.courseContents(this.currentCourse) }).then(() => {
        this.contentDirty = false
        this.$modal.msgSuccess('课程编排已保存')
        return this.loadCourseContents(this.currentCourse)
      }).then(() => this.getList()).finally(() => {
        this.contentSaving = false
      })
    },
    openChapterDialog(chapter) {
      if (!this.canEditContent(this.currentCourse)) return this.comingSoon()
      this.chapterForm = chapter ? Object.assign({}, chapter) : { id: undefined, chapterName: '', chapterIntro: '', isRequired: 1 }
      this.chapterDialogOpen = true
    },
    saveChapter() {
      if (!this.canEditContent(this.currentCourse)) return this.comingSoon()
      this.$refs.chapterForm.validate(valid => {
        if (!valid || !this.currentCourse) return
        if (!(this.previewMode && String(this.currentCourse.id).indexOf('preview-') === 0)) {
          const request = this.chapterForm.id
            ? updateCourseChapter(this.chapterForm.id, this.chapterForm)
            : addCourseChapter(this.currentCourse.id, this.chapterForm)
          request.then(() => {
            this.chapterDialogOpen = false
            this.$modal.msgSuccess('章节已保存')
            return this.loadCourseContents(this.currentCourse)
          }).then(() => this.getList())
          return
        }
        const chapters = this.courseContents(this.currentCourse)
        if (this.chapterForm.id) {
          const target = chapters.find(item => item.id === this.chapterForm.id)
          if (target) Object.assign(target, this.chapterForm)
        } else {
          chapters.push({ id: 'chapter-' + Date.now(), chapterName: this.chapterForm.chapterName, chapterIntro: this.chapterForm.chapterIntro, isRequired: this.chapterForm.isRequired, items: [] })
        }
        this.saveContentStore()
        this.contentDirty = true
        this.chapterDialogOpen = false
        this.$modal.msgSuccess('章节已保存')
      })
    },
    removeChapter(chapter) {
      if (!this.canEditContent(this.currentCourse)) return this.comingSoon()
      this.$modal.confirm('确认删除章节“' + chapter.chapterName + '”？章节中的资料也会一并移除。').then(() => {
        if (!(this.previewMode && String(this.currentCourse.id).indexOf('preview-') === 0)) {
          return deleteCourseChapter(chapter.id).then(() => {
            this.$modal.msgSuccess('章节已删除，历史学习记录仍保留')
            return this.loadCourseContents(this.currentCourse)
          }).then(() => this.getList())
        }
        const chapters = this.courseContents(this.currentCourse)
        const index = chapters.findIndex(item => item.id === chapter.id)
        if (index > -1) chapters.splice(index, 1)
        this.saveContentStore()
        this.contentDirty = true
        this.$modal.msgSuccess('章节已删除')
      }).catch(() => {})
    },
    openItemDialog(chapter, item) {
      if (!this.canEditContent(this.currentCourse)) return this.comingSoon()
      this.activeChapterId = chapter.id
      this.itemForm = item ? Object.assign({}, item, { pendingAsset: null }) : { id: undefined, itemTitle: '', itemIntro: '', itemType: 'DOC', duration: 10, completionRule: 'SCROLL_END', isRequired: 1, completionThreshold: 100, fileName: '', fileSize: 0, fileExt: '', assetStatus: 'UNBOUND', pendingAsset: null }
      this.itemDialogOpen = true
    },
    handleItemTypeChange(type) {
      this.itemForm.completionRule = type === 'VIDEO' ? 'PLAY_TO_END' : (type === 'QUIZ' ? 'QUIZ_SUBMIT' : 'SCROLL_END')
      this.itemForm.fileName = ''
      this.itemForm.fileSize = 0
      this.itemForm.fileExt = ''
      this.itemForm.assetStatus = 'UNBOUND'
      this.itemForm.pendingAsset = null
    },
    handleAssetChange(file) {
      if (!file || !file.raw) return
      const raw = file.raw
      const nameParts = String(raw.name || '').split('.')
      this.itemForm.fileName = raw.name
      this.itemForm.fileSize = raw.size || 0
      this.itemForm.fileExt = nameParts.length > 1 ? nameParts.pop().toLowerCase() : ''
      this.itemForm.assetStatus = 'LOCAL_ONLY'
      this.itemForm.pendingAsset = raw
    },
    handleAssetExceed() {
      this.$modal.msgInfo('如需更换资料，请先移除当前文件后再选择')
    },
    clearAsset() {
      this.itemForm.fileName = ''
      this.itemForm.fileSize = 0
      this.itemForm.fileExt = ''
      this.itemForm.assetStatus = 'UNBOUND'
      this.itemForm.contentUrl = ''
      this.itemForm.pendingAsset = null
      if (this.$refs.assetUpload) this.$refs.assetUpload.clearFiles()
    },
    fileSizeText(size) {
      const value = Number(size || 0)
      if (!value) return '大小待上传后计算'
      if (value < 1024 * 1024) return Math.max(1, Math.round(value / 1024)) + ' KB'
      return (value / 1024 / 1024).toFixed(1) + ' MB'
    },
    saveItem() {
      if (!this.canEditContent(this.currentCourse)) return this.comingSoon()
      this.$refs.itemForm.validate(valid => {
        if (!valid || !this.currentCourse) return
        const chapter = this.courseContents(this.currentCourse).find(item => item.id === this.activeChapterId)
        if (!chapter) return
        if (!(this.previewMode && String(this.currentCourse.id).indexOf('preview-') === 0)) {
          const pendingAsset = this.itemForm.pendingAsset
          const payload = Object.assign({}, this.itemForm)
          delete payload.pendingAsset
          delete payload.assetStatus
          if (pendingAsset) {
            delete payload.fileName
            delete payload.fileSize
            delete payload.fileExt
          }
          this.itemSubmitting = true
          const request = payload.id ? updateStudyItem(payload.id, payload) : addStudyItem(chapter.id, payload)
          request.then(response => {
            const itemId = payload.id || (response.data && response.data.id)
            if (!pendingAsset) return null
            if (!itemId) throw new Error('学习资料保存后未返回ID')
            const formData = new FormData()
            formData.append('file', pendingAsset)
            return uploadStudyAsset(itemId, formData)
          }).then(() => {
            this.itemDialogOpen = false
            this.$modal.msgSuccess(pendingAsset ? '学习资料和文件已保存' : '学习资料已保存')
            return this.loadCourseContents(this.currentCourse)
          }).then(() => this.getList()).finally(() => {
            this.itemSubmitting = false
          })
          return
        }
        if (this.itemForm.id) {
          const target = chapter.items.find(item => item.id === this.itemForm.id)
          if (target) Object.assign(target, this.itemForm)
        } else {
          chapter.items.push(Object.assign({}, this.itemForm, { id: 'item-' + Date.now() }))
        }
        this.saveContentStore()
        this.contentDirty = true
        this.itemDialogOpen = false
        this.$modal.msgSuccess('学习资料已保存')
      })
    },
    removeItem(chapter, item) {
      if (!this.canEditContent(this.currentCourse)) return this.comingSoon()
      this.$modal.confirm('确认删除学习资料“' + item.itemTitle + '”？').then(() => {
        if (!(this.previewMode && String(this.currentCourse.id).indexOf('preview-') === 0)) {
          return deleteStudyItem(item.id).then(() => {
            this.$modal.msgSuccess('学习资料已删除，历史学习记录仍保留')
            return this.loadCourseContents(this.currentCourse)
          }).then(() => this.getList())
        }
        const index = chapter.items.findIndex(content => content.id === item.id)
        if (index > -1) chapter.items.splice(index, 1)
        this.saveContentStore()
        this.contentDirty = true
        this.$modal.msgSuccess('学习资料已删除')
      }).catch(() => {})
    },
    loadContentStore() {
      try {
        const value = localStorage.getItem(CONTENT_STORAGE_KEY)
        this.contentStore = value ? JSON.parse(value) : {}
      } catch (error) {
        this.contentStore = {}
      }
    },
    saveContentStore() {
      try {
        localStorage.setItem(CONTENT_STORAGE_KEY, JSON.stringify(this.contentStore))
      } catch (error) {
        // 浏览器存储不可用时，当前页面仍可继续演示内容管理交互。
      }
    },
    courseRecords(course) {
      if (!course) return []
      if (!(this.previewMode && String(course.id).indexOf('preview-') === 0)) return this.recordList
      return this.previewCourseRecords(course)
    },
    normalizeCourseRecord(record, course) {
      const total = Number(record.totalItems || 0)
      const completed = Number(record.completedItems || 0)
      return {
        userId: record.userId,
        name: record.studentName || '未命名实习生',
        position: record.positionName || course.positionName || this.positionName(course.positionId) || '实习生',
        progress: Math.max(0, Math.min(100, Math.round(Number(record.progress || 0)))),
        completed,
        total,
        status: record.status || 'NOT_STARTED',
        lastStudy: record.lastStudyTime ? this.formatDate(record.lastStudyTime) : '尚未开始'
      }
    },
    previewCourseRecords(course) {
      const count = Math.max(this.expectedCount(course), this.studentCount(course), 0)
      const names = ['李晨', '王静', '陈浩', '刘薇', '张博', '周宁']
      const progressSeed = [100, 68, 0, 20, 86, 0]
      const totalItems = Math.max(this.contentSummary(course).itemCount, 1)
      return Array.from({ length: count }, (_, index) => {
        const progress = progressSeed[index % progressSeed.length]
        return { name: names[index % names.length], position: course.positionName || this.positionName(course.positionId) || '实习生', progress, completed: Math.round(totalItems * progress / 100), total: totalItems, status: progress === 100 ? 'DONE' : (progress > 0 ? 'IN_PROGRESS' : 'NOT_STARTED'), lastStudy: progress > 0 ? (index === 0 ? '今天 10:42' : (index + 1) + ' 天前') : '尚未开始' }
      })
    },
    studentCount(course) {
      return Number(course.studentCount || 0)
    },
    expectedCount(course) {
      return Number(course.expectedStudentCount || course.studentCount || 0)
    },
    canEditContent(course) {
      return Boolean(course && !this.readOnly && (course.status === 'DRAFT' || course.status === 'DISABLED'))
    },
    contentStatusTip(course) {
      if (!course) return ''
      if (course.status === 'DISABLED') return '课程已停用，可修改基础信息、章节和资料；历史学习记录会保留，重新发布后对应实习生可继续学习。'
      if (course.status === 'PUBLISHED') return '已发布课程为只读状态；需要调整内容时，请先停用课程。历史学习记录不会受影响。'
      return '章节和资料编排保存后写入数据库；文档和视频上传到服务器资源目录，数据库保存访问路径和文件元数据。'
    },
    averageProgress(course) {
      const value = Number(course.avgCompletionRate)
      if (!Number.isNaN(value) && value >= 0) return Math.round(value)
      const records = this.courseRecords(course)
      return records.length ? Math.round(records.reduce((sum, item) => sum + item.progress, 0) / records.length) : 0
    },
    positionName(positionId) {
      const position = this.positionOptions.find(item => String(item.id) === String(positionId))
      return position ? position.positionName : ''
    },
    courseTypeLabel(type) { return type === 'PRACTICE' ? '实操训练' : '理论学习' },
    resourceTypeLabel(type) { return { DOC: '文档学习', VIDEO: '视频学习', QUIZ: '章节测试' }[type] || type },
    completionRuleLabel(rule) { return { SCROLL_END: '阅读确认', PLAY_TO_END: '观看完成', QUIZ_SUBMIT: '提交测试' }[rule] || '完成学习' },
    resourceIcon(type) { return { DOC: 'el-icon-document', VIDEO: 'el-icon-video-camera', QUIZ: 'el-icon-edit-outline' }[type] || 'el-icon-document' },
    courseTone(course) { return course.courseType === 'PRACTICE' ? 'orange' : 'blue' },
    statusLabel(status) { return { DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用' }[status] || status || '草稿' },
    statusTagType(status) { return { DRAFT: 'info', PUBLISHED: 'success', DISABLED: 'danger' }[status] || 'info' },
    recordStatusLabel(status) { return { NOT_STARTED: '未开始', IN_PROGRESS: '学习中', DONE: '已完成' }[status] || '未开始' },
    recordStatusTag(status) { return { NOT_STARTED: 'info', IN_PROGRESS: 'warning', DONE: 'success' }[status] || 'info' },
    formatDate(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value).slice(0, 16)
      const pad = number => String(number).padStart(2, '0')
      return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes())
    },
    nowText() {
      const date = new Date()
      const pad = number => String(number).padStart(2, '0')
      return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds())
    },
    comingSoon() {
      this.$modal.msgInfo('功能开发中')
    }
  }
}
</script>

<style lang="scss" scoped>
.course-management { min-height: 100%; padding: 24px 26px 42px; color: #27384a; background: #f6f8fb; }
.page-heading, .panel-heading, .drawer-heading, .drawer-heading-main, .heading-actions, .content-toolbar, .record-toolbar, .course-cell, .student-cell { display: flex; align-items: center; }
.page-heading, .panel-heading, .drawer-heading, .content-toolbar, .record-toolbar { justify-content: space-between; }
.page-heading { align-items: flex-start; margin-bottom: 18px; }
.title-line { display: flex; align-items: center; gap: 9px; }
.eyebrow { margin-bottom: 7px; color: #347bc2; font-size: 12px; }
.page-heading h2 { margin: 0 0 8px; color: #1d2939; font-size: 25px; font-weight: 600; }
.page-heading p { margin: 0; color: #748092; font-size: 13px; }
.heading-actions { gap: 8px; }
.scope-strip { display: flex; align-items: center; justify-content: space-between; min-height: 64px; margin: -2px 0 16px; padding: 11px 15px; border: 1px solid #dbe7f2; background: #f7fbff; }
.scope-main, .scope-meta, .scope-main > div { display: flex; align-items: center; }
.scope-main { min-width: 0; }
.scope-icon { display: inline-flex; align-items: center; justify-content: center; width: 34px; height: 34px; margin-right: 10px; border-radius: 50%; background: #e6f1fb; color: #397fbe; font-size: 16px; }
.scope-main strong, .scope-main span { display: block; }
.scope-main strong { color: #344a60; font-size: 13px; font-weight: 600; }
.scope-main div span { margin-top: 4px; color: #7b8b9c; font-size: 11px; }
.scope-meta { gap: 10px; color: #7b8b9c; font-size: 11px; white-space: nowrap; }.scope-meta i { margin-right: 4px; color: #5c8db9; }
.preview-alert { margin: -3px 0 16px; }
.summary-row { margin-bottom: 16px; }
.summary-row .el-col { margin-bottom: 10px; }
.summary-card { display: flex; align-items: center; min-height: 102px; padding: 17px 18px; border: 1px solid #e3e9f0; border-left: 4px solid #3d82c7; background: #fff; }
.summary-card.green { border-left-color: #2f9b79; }.summary-card.orange { border-left-color: #d2872f; }.summary-card.purple { border-left-color: #7866b2; }
.summary-card.teal { border-left-color: #3c9896; }.teal .summary-icon { background: #eaf7f6; color: #3c9896; }
.summary-icon { display: inline-flex; align-items: center; justify-content: center; width: 39px; height: 39px; margin-right: 13px; border-radius: 50%; background: #edf4ff; color: #3d82c7; font-size: 19px; }
.green .summary-icon { background: #edf9f3; color: #2f9b79; }.orange .summary-icon { background: #fff5e8; color: #d2872f; }.purple .summary-icon { background: #f2effa; color: #7866b2; }
.summary-card span:not(.summary-icon), .summary-card small { display: block; color: #7c8896; font-size: 12px; }.summary-card strong { display: block; margin: 3px 0; color: #26384c; font-size: 25px; font-weight: 600; line-height: 1; }.summary-card small { font-size: 11px; }
.content-panel { border: 1px solid #e3e9f0; background: #fff; }.catalog-panel { padding: 20px 20px 8px; }
.panel-heading { margin-bottom: 18px; }.panel-heading h3 { margin: 0 0 6px; color: #2c3d50; font-size: 16px; font-weight: 600; }.panel-heading p { margin: 0; color: #8490a0; font-size: 12px; }
.query-form { margin-bottom: 12px; padding: 13px 14px 1px; border: 1px solid #edf0f4; background: #fafbfd; }.query-actions { margin-left: 3px; }
.course-table { border-top: 1px solid #edf0f4; }.course-cell { min-width: 0; gap: 10px; }.course-cover, .drawer-cover { display: inline-flex; align-items: center; justify-content: center; flex: 0 0 auto; width: 36px; height: 36px; border-radius: 5px; background: #edf4ff; color: #347bc2; font-size: 17px; }.course-cover.orange, .drawer-cover.orange { background: #fff4e7; color: #d2872f; }.course-cell > div { min-width: 0; }.course-cell strong, .course-cell small { display: block; }.course-cell strong { overflow: hidden; color: #304357; font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }.course-cell small { max-width: 245px; margin-top: 4px; overflow: hidden; color: #8793a1; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.position-text { color: #536476; font-size: 12px; }.position-text i { margin-right: 4px; color: #4b86c1; }.required-label { display: block; margin-top: 6px; color: #8793a1; font-size: 11px; }.required-label.required { color: #bc6c20; }.table-number { display: block; color: #3a4e63; font-size: 13px; }.table-subtext { display: block; margin-top: 2px; color: #8b96a4; font-size: 11px; }.progress-cell { padding: 3px 3px 0; text-align: left; }.progress-cell span { display: block; margin-bottom: 5px; color: #536476; font-size: 11px; }.progress-cell small { display: block; margin-top: 4px; color: #8c97a5; font-size: 10px; }.course-actions ::v-deep .el-button + .el-button { margin-left: 7px; }.danger-text { color: #c45656 !important; }
.dialog-tip { margin-bottom: 18px; padding: 10px 12px; background: #f2f7fd; color: #5a718a; font-size: 12px; }.dialog-tip i { margin-right: 5px; color: #3683cc; }.form-full { width: 100%; }.unit-text { margin-left: 8px; color: #8490a0; font-size: 12px; }.file-input { display: flex; gap: 8px; }.file-input .el-input { flex: 1; }.form-tip { display: block; margin-top: 5px; color: #9aa5b1; font-size: 11px; }
.drawer-heading { min-height: 86px; padding: 20px 24px; border-bottom: 1px solid #e8edf3; background: #fff; }.drawer-heading-main { min-width: 0; gap: 12px; }.drawer-cover { width: 43px; height: 43px; font-size: 20px; }.drawer-heading span:not(.drawer-cover) { display: block; margin-bottom: 5px; color: #7e8b9a; font-size: 11px; }.drawer-heading h3 { margin: 0 0 5px; overflow: hidden; color: #26384c; font-size: 17px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }.drawer-heading p { margin: 0; color: #8490a0; font-size: 12px; }.drawer-body { min-height: calc(100vh - 86px); padding: 20px 24px 32px; background: #f8fafc; }.content-toolbar { margin-bottom: 14px; color: #516174; font-size: 12px; }.content-toolbar strong { color: #35495e; }.drawer-alert { margin-bottom: 14px; }.chapter-list { display: grid; gap: 12px; }.chapter-block { overflow: hidden; border: 1px solid #e4eaf1; background: #fff; }.chapter-heading { display: flex; align-items: center; justify-content: space-between; min-height: 48px; padding: 0 14px; border-bottom: 1px solid #edf1f5; background: #fbfcfe; }.chapter-heading > div:first-child { display: flex; align-items: center; min-width: 0; }.chapter-order { display: inline-flex; align-items: center; justify-content: center; width: 25px; height: 25px; margin-right: 8px; border-radius: 50%; background: #eaf2fb; color: #397fc1; font-size: 10px; font-weight: 600; }.chapter-heading strong { overflow: hidden; color: #31465a; font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }.chapter-heading small { margin-left: 8px; color: #95a0ad; font-size: 11px; white-space: nowrap; }.chapter-actions, .resource-actions { white-space: nowrap; }.chapter-actions .el-button + .el-button, .resource-actions .el-button + .el-button { margin-left: 7px; }.resource-row { display: flex; align-items: center; min-height: 58px; padding: 0 14px; border-bottom: 1px solid #f0f3f6; }.resource-icon { display: inline-flex; align-items: center; justify-content: center; flex: 0 0 auto; width: 29px; height: 29px; margin-right: 9px; border-radius: 4px; background: #edf4ff; color: #4687c8; font-size: 14px; }.resource-icon.video { background: #fff5e9; color: #d28a35; }.resource-icon.quiz { background: #f2effa; color: #7866b2; }.resource-info { min-width: 0; flex: 1; }.resource-info strong, .resource-info span { display: block; }.resource-info strong { overflow: hidden; color: #405267; font-size: 12px; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }.resource-info span { margin-top: 3px; color: #95a0ad; font-size: 10px; }.add-resource { width: calc(100% - 28px); margin: 10px 14px 12px; border-style: dashed; color: #4e83b7; }.record-summary { margin-bottom: 17px; }.record-summary > .el-col > div { min-height: 78px; padding: 13px; border: 1px solid #e4eaf1; background: #fff; }.record-summary span, .record-summary strong, .record-summary small { display: block; }.record-summary span, .record-summary small { color: #8995a4; font-size: 11px; }.record-summary strong { margin: 5px 0; color: #33495f; font-size: 19px; font-weight: 600; }.record-toolbar { margin-bottom: 14px; }.record-table { border: 1px solid #e4eaf1; background: #fff; }.student-cell { gap: 8px; }.student-cell > span { display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 28px; border-radius: 50%; background: #eaf2fb; color: #397fc1; font-size: 12px; font-weight: 600; }.student-cell strong, .student-cell small { display: block; }.student-cell strong { color: #3c5066; font-size: 12px; font-weight: 600; }.student-cell small { margin-top: 3px; color: #8e9aaa; font-size: 10px; }.record-progress > div { display: flex; justify-content: space-between; margin-bottom: 6px; color: #657587; font-size: 11px; }.record-progress strong { color: #387ab8; font-weight: 600; }
.content-toolbar-actions { display: flex; align-items: center; gap: 8px; }
.content-toolbar-actions .el-tag i { margin-right: 2px; }
.content-save-state { margin-left: 12px; font-size: 11px; }
.content-save-state i { margin-right: 3px; }
.content-save-state.is-saved { color: #2f9b79; }.content-save-state.is-dirty { color: #c78325; }
.drag-handle { width: 22px; height: 22px; padding: 0; border: 0; color: #a6b2bf; background: transparent; cursor: grab; }
.drag-handle:hover { color: #397fc1; }.drag-handle:active { cursor: grabbing; }
.chapter-heading > div:first-child { display: flex; align-items: center; min-width: 0; }
.chapter-heading { min-height: 64px; gap: 12px; padding-top: 8px; padding-bottom: 8px; }
.chapter-heading > div:first-child { flex: 1; }
.chapter-heading-copy { min-width: 0; flex: 1; }
.chapter-heading-copy strong, .chapter-heading-copy small { display: block; margin-left: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chapter-heading-copy small { margin-top: 4px; color: #7f8c9b; }
.chapter-item-count { margin-left: 12px !important; }
.resource-list { min-height: 2px; }
.resource-file-state { display: block; margin-top: 3px; overflow: hidden; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.resource-intro { display: block; margin-top: 3px; overflow: hidden; color: #657587; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.resource-file-state.is-bound { color: #39856d; }.resource-file-state.is-missing { color: #c78325; }
.resource-actions { margin-left: 8px; }
.threshold-text { margin-left: 14px; color: #64748b; font-size: 11px; }
.asset-upload { display: inline-block; }
.selected-file { display: flex; align-items: center; gap: 8px; min-height: 38px; margin-top: 8px; padding: 7px 9px; border: 1px solid #dce6ef; background: #f8fbfd; }
.selected-file > i { color: #397fc1; }.selected-file > div { min-width: 0; flex: 1; }.selected-file strong, .selected-file span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.selected-file strong { color: #405267; font-size: 12px; }.selected-file span { margin-top: 3px; color: #8995a4; font-size: 10px; }@media (max-width: 960px) { .course-management { padding: 20px 16px 34px; }.course-table ::v-deep .el-table__fixed-right { box-shadow: -4px 0 8px rgba(31, 49, 67, .06); }.page-heading { align-items: flex-start; }.heading-actions { flex-wrap: wrap; justify-content: flex-end; } }
.asset-upload-progress { margin-top: 9px; padding: 8px 10px; border: 1px solid #e4e9f0; background: #f8fafc; }.asset-upload-progress .el-progress { margin-bottom: 5px; }.asset-upload-progress span { display: block; font-size: 12px; }.upload-uploading { color: #2878c7; }.upload-success { color: #23966f; }.upload-error { color: #d9485f; }
@media (max-width: 700px) { .course-management { padding: 16px 12px 28px; }.page-heading { display: block; }.heading-actions { justify-content: flex-start; margin-top: 14px; }.page-heading h2 { font-size: 23px; }.page-heading p { line-height: 1.6; }.scope-strip { display: block; }.scope-meta { margin-top: 8px; padding-left: 44px; }.panel-heading { align-items: flex-start; gap: 12px; }.panel-heading p { line-height: 1.5; }.panel-heading .el-button { flex: 0 0 auto; }.query-form { padding-bottom: 10px; }.query-form .el-form-item { display: block; margin-right: 0; }.query-form ::v-deep .el-form-item__content, .query-form ::v-deep .el-select, .query-form ::v-deep .el-date-editor { width: 100%; }.query-actions { display: flex !important; gap: 8px; margin-left: 0; }.drawer-heading { padding: 17px 16px; }.drawer-body { padding: 16px 12px 28px; }.drawer-heading h3 { max-width: 235px; }.chapter-heading { padding: 0 10px; }.chapter-heading small { display: none; }.resource-row { padding: 0 10px; }.resource-actions .el-button { padding: 5px 2px; }.record-summary > .el-col > div { min-height: 74px; padding: 10px; }.record-summary strong { font-size: 16px; }.record-summary small { min-height: 26px; line-height: 1.35; }.record-toolbar { align-items: flex-start; gap: 9px; flex-direction: column; }.record-toolbar .el-radio-button__inner { padding: 8px 10px; } }
</style>
