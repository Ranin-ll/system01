# 融谷实习生学习考核系统 · 前端

Vue 2 + Element UI 的管理端与业务端界面。

## 开发

```bash
npm install --registry=https://registry.npmmirror.com
npm run dev -- --port 9530
```

## 构建

```bash
npm run build:prod
```

## 目录约定

- `src/views/business/**` —— 业务页面（课程 / 题库 / 考核等）
- `src/views/assessment/**` —— 实习生端（工作台 / 学习与考核 / 考核成绩与转正申请 / 能力画像）
- `src/views/department/**` —— 部门管理员端页面
- `src/views/super/**` —— 超级管理员端页面
- `src/api/business/**` —— 业务接口封装
- `src/assets/styles/*-module.scss` —— 各端共享样式基线（SCSS 变量，勿用 `:root`）
