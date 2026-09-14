const STORAGE_PREFIX = 'intern-learning-preview:'

const departmentCourses = {
  交付部门: {
    positionName: '实施实习生',
    courses: [
      {
        id: 'delivery-foundation',
        courseName: '项目实施与交付规范',
        courseType: 'THEORY',
        isRequired: 1,
        intro: '掌握项目实施流程、交付标准与现场协作规范。',
        duration: 128,
        chapters: [
          { id: 'delivery-chapter-1', chapterName: '第一章 入职与保密', items: [
            { id: 'delivery-item-1', itemTitle: '岗位规范与保密要求', itemType: 'DOC', duration: 18, progress: 100, status: 'DONE' },
            { id: 'delivery-item-2', itemTitle: '项目资料安全操作说明', itemType: 'VIDEO', duration: 12, progress: 42, status: 'IN_PROGRESS' }
          ] },
          { id: 'delivery-chapter-2', chapterName: '第二章 项目交付流程', items: [
            { id: 'delivery-item-3', itemTitle: '交付前检查清单', itemType: 'DOC', duration: 20, progress: 0, status: 'NOT_STARTED' },
            { id: 'delivery-item-4', itemTitle: '现场实施流程演示', itemType: 'VIDEO', duration: 25, progress: 0, status: 'NOT_STARTED' }
          ] },
          { id: 'delivery-chapter-3', chapterName: '第三章 复盘与验收', items: [
            { id: 'delivery-item-5', itemTitle: '项目验收与复盘要点', itemType: 'DOC', duration: 24, progress: 0, status: 'NOT_STARTED' }
          ] }
        ]
      },
      {
        id: 'delivery-tools',
        courseName: '交付工具基础操作',
        courseType: 'PRACTICE',
        isRequired: 0,
        intro: '熟悉交付阶段常用工具和资料归档方式。',
        duration: 76,
        chapters: [
          { id: 'delivery-tools-1', chapterName: '第一章 工具与模板', items: [
            { id: 'delivery-tools-item-1', itemTitle: '交付模板使用说明', itemType: 'DOC', duration: 16, progress: 0, status: 'NOT_STARTED' },
            { id: 'delivery-tools-item-2', itemTitle: '资料归档操作演示', itemType: 'VIDEO', duration: 14, progress: 0, status: 'NOT_STARTED' }
          ] }
        ]
      }
    ]
  },
  开发部门: {
    positionName: '开发实习生',
    courses: [
      {
        id: 'development-foundation',
        courseName: '开发流程与代码交付规范',
        courseType: 'THEORY',
        isRequired: 1,
        intro: '从需求理解、分支协作到代码交付，建立统一的开发工作方法。',
        duration: 146,
        chapters: [
          { id: 'development-chapter-1', chapterName: '第一章 入职与保密', items: [
            { id: 'development-item-1', itemTitle: '开发岗位规范与保密要求', itemType: 'DOC', duration: 18, progress: 100, status: 'DONE' },
            { id: 'development-item-2', itemTitle: '开发环境安全操作说明', itemType: 'VIDEO', duration: 14, progress: 42, status: 'IN_PROGRESS' }
          ] },
          { id: 'development-chapter-2', chapterName: '第二章 开发协作流程', items: [
            { id: 'development-item-3', itemTitle: '需求拆解与任务流转', itemType: 'DOC', duration: 22, progress: 0, status: 'NOT_STARTED' },
            { id: 'development-item-4', itemTitle: '分支开发与合并演示', itemType: 'VIDEO', duration: 24, progress: 0, status: 'NOT_STARTED' }
          ] },
          { id: 'development-chapter-3', chapterName: '第三章 代码交付与复盘', items: [
            { id: 'development-item-5', itemTitle: '代码评审与交付检查', itemType: 'DOC', duration: 26, progress: 0, status: 'NOT_STARTED' }
          ] }
        ]
      },
      {
        id: 'development-tools',
        courseName: '开发工具与项目环境',
        courseType: 'PRACTICE',
        isRequired: 0,
        intro: '了解项目本地环境、调试工具和基础协作约定。',
        duration: 84,
        chapters: [
          { id: 'development-tools-1', chapterName: '第一章 工具与环境', items: [
            { id: 'development-tools-item-1', itemTitle: '本地开发环境检查', itemType: 'DOC', duration: 20, progress: 0, status: 'NOT_STARTED' },
            { id: 'development-tools-item-2', itemTitle: '调试与日志定位演示', itemType: 'VIDEO', duration: 18, progress: 0, status: 'NOT_STARTED' }
          ] }
        ]
      }
    ]
  },
  设计部门: {
    positionName: '设计实习生',
    courses: [
      {
        id: 'design-foundation',
        courseName: '设计规范与成果交付',
        courseType: 'THEORY',
        isRequired: 1,
        intro: '掌握设计规范、评审流程与交付文件整理要求。',
        duration: 132,
        chapters: [
          { id: 'design-chapter-1', chapterName: '第一章 岗位规范', items: [
            { id: 'design-item-1', itemTitle: '设计岗位规范与保密要求', itemType: 'DOC', duration: 18, progress: 100, status: 'DONE' },
            { id: 'design-item-2', itemTitle: '设计文件安全管理', itemType: 'VIDEO', duration: 14, progress: 42, status: 'IN_PROGRESS' }
          ] },
          { id: 'design-chapter-2', chapterName: '第二章 设计协作流程', items: [
            { id: 'design-item-3', itemTitle: '需求分析与方案评审', itemType: 'DOC', duration: 22, progress: 0, status: 'NOT_STARTED' },
            { id: 'design-item-4', itemTitle: '设计交付演示', itemType: 'VIDEO', duration: 20, progress: 0, status: 'NOT_STARTED' }
          ] }
        ]
      }
    ]
  },
  质检部门: {
    positionName: '质检实习生',
    courses: [
      {
        id: 'qa-foundation',
        courseName: '质量检查与缺陷处理规范',
        courseType: 'THEORY',
        isRequired: 1,
        intro: '学习质量检查方法、缺陷分级和问题闭环流程。',
        duration: 126,
        chapters: [
          { id: 'qa-chapter-1', chapterName: '第一章 质量规范', items: [
            { id: 'qa-item-1', itemTitle: '质检岗位规范与保密要求', itemType: 'DOC', duration: 18, progress: 100, status: 'DONE' },
            { id: 'qa-item-2', itemTitle: '缺陷提交流程演示', itemType: 'VIDEO', duration: 15, progress: 42, status: 'IN_PROGRESS' }
          ] },
          { id: 'qa-chapter-2', chapterName: '第二章 缺陷闭环', items: [
            { id: 'qa-item-3', itemTitle: '测试用例与检查清单', itemType: 'DOC', duration: 20, progress: 0, status: 'NOT_STARTED' },
            { id: 'qa-item-4', itemTitle: '缺陷跟踪与复测演示', itemType: 'VIDEO', duration: 22, progress: 0, status: 'NOT_STARTED' }
          ] }
        ]
      }
    ]
  },
  建模部门: {
    positionName: '建模实习生',
    courses: [
      {
        id: 'model-foundation',
        courseName: '建模流程与文件规范',
        courseType: 'THEORY',
        isRequired: 1,
        intro: '熟悉建模流程、文件命名和成果交付规范。',
        duration: 138,
        chapters: [
          { id: 'model-chapter-1', chapterName: '第一章 建模规范', items: [
            { id: 'model-item-1', itemTitle: '建模岗位规范与保密要求', itemType: 'DOC', duration: 18, progress: 100, status: 'DONE' },
            { id: 'model-item-2', itemTitle: '模型文件安全操作', itemType: 'VIDEO', duration: 14, progress: 42, status: 'IN_PROGRESS' }
          ] },
          { id: 'model-chapter-2', chapterName: '第二章 建模交付流程', items: [
            { id: 'model-item-3', itemTitle: '文件命名与版本管理', itemType: 'DOC', duration: 22, progress: 0, status: 'NOT_STARTED' },
            { id: 'model-item-4', itemTitle: '建模成果交付演示', itemType: 'VIDEO', duration: 25, progress: 0, status: 'NOT_STARTED' }
          ] }
        ]
      }
    ]
  }
}

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

function deptConfig(deptName) {
  return departmentCourses[deptName] || departmentCourses.交付部门
}

function syncCourse(course) {
  const items = course.chapters.reduce((all, chapter) => all.concat(chapter.items), [])
  const total = items.length
  const progress = total ? Math.round(items.reduce((sum, item) => sum + Number(item.progress || 0), 0) / total) : 0
  course.progress = progress
  course.completedItems = items.filter(item => item.status === 'DONE').length
  course.itemCount = total
  course.chapterCount = course.chapters.length
  course.lastStudyTime = items.filter(item => item.lastStudyTime).sort((a, b) => String(b.lastStudyTime).localeCompare(String(a.lastStudyTime)))[0]?.lastStudyTime || '尚未开始'
  return course
}

export function getPositionName(deptName) {
  return deptConfig(deptName).positionName
}

export function loadPreviewCourses(userKey, deptName) {
  const storageKey = STORAGE_PREFIX + (userKey || 'preview')
  try {
    const saved = localStorage.getItem(storageKey)
    if (saved) return JSON.parse(saved).map(syncCourse)
  } catch (error) {
    // localStorage 不可用时继续使用内存演示数据。
  }
  return clone(deptConfig(deptName).courses).map(course => syncCourse(course))
}

export function savePreviewCourses(userKey, courses) {
  const storageKey = STORAGE_PREFIX + (userKey || 'preview')
  try {
    localStorage.setItem(storageKey, JSON.stringify(courses.map(course => syncCourse(course))))
  } catch (error) {
    // 页面仍可继续交互，接口接入后由服务端持久化。
  }
}

export function findPreviewCourse(courses, courseId) {
  return courses.find(course => String(course.id) === String(courseId))
}

export function flattenItems(course) {
  return course.chapters.reduce((all, chapter) => all.concat(chapter.items), [])
}

export function learningSummary(courses) {
  const required = courses.filter(course => Number(course.isRequired) === 1)
  const source = required.length ? required : courses
  const progress = source.length ? Math.round(source.reduce((sum, course) => sum + Number(course.progress || 0), 0) / source.length) : null
  const items = courses.reduce((all, course) => all.concat(flattenItems(course)), [])
  return {
    progress,
    courseCount: courses.length,
    completedCourses: courses.filter(course => course.progress === 100).length,
    learningCourses: courses.filter(course => course.progress > 0 && course.progress < 100).length,
    completedItems: items.filter(item => item.status === 'DONE').length,
    itemCount: items.length,
    duration: items.reduce((sum, item) => sum + Number(item.studyDuration || 0), 0),
    lastStudyTime: items.filter(item => item.lastStudyTime).sort((a, b) => String(b.lastStudyTime).localeCompare(String(a.lastStudyTime)))[0]?.lastStudyTime || '尚未开始'
  }
}

export function formatLearningDuration(minutes) {
  const value = Number(minutes || 0)
  if (value < 60) return value + ' 分钟'
  const hours = Math.floor(value / 60)
  const rest = value % 60
  return hours + ' 小时' + (rest ? ' ' + rest + ' 分钟' : '')
}

