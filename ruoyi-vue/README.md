# 融谷实习生学习考核系统 · 服务端

面向融谷内部实习生的学习与考核平台服务端：注册审核、在线学习、模拟考核、正式考核（批次 / 分环节发布 / 指定人员 / 时间窗）、
成绩与转正审批、超管全局运营。

## 技术栈

- JDK 8 + Spring Boot 2.5（Maven 多模块）
- MyBatis-Plus + MySQL 8.0 + Redis
- 权限模型：基于角色的接口级鉴权（`@PreAuthorize`），业务角色 = 超级管理员 / 部门管理员 / 预备实习生 / 正式实习生

## 模块

| 模块 | 说明 |
|---|---|
| `ruoyi-admin` | 启动模块与 Web 层（控制器、配置、拦截器） |
| `ruoyi-framework` | 安全、数据源、AOP、异常处理等基础设施 |
| `ruoyi-system` | 系统管理（用户 / 角色 / 菜单 / 部门 / 岗位 / 字典 / 参数 / 日志） |
| `ruoyi-common` | 通用工具、常量、注解 |
| `ruoyi-quartz` | 定时任务 |
| `ruoyi-generator` | 代码生成（可选） |
| `ruoyi-business` | **本系统业务模块**：注册审核 / 岗位 / 课程与内容 / 题库 / 考核与答卷 / 成绩与转正 |

## 本地启动

```bash
# 1) 建库并执行脚本（按顺序）
#    sql/ry_20240629.sql  → 系统基础表
#    sql/intern_assessment_business.sql → 业务表
#    sql/ 下其余迁移脚本按需执行（均为幂等）
# 2) 编译并启动
mvn -pl ruoyi-admin -am -DskipTests package
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

默认端口 8080，前端开发端口 9530。
