# 系统架构说明文档 - 多角色自动跳转系统

## 1. 概述
本系统实现了基于角色的访问控制 (RBAC) 和自动跳转机制。系统根据用户的角色（STUDENT, TEACHER, ADMIN）提供差异化的用户界面和功能访问权限，确保数据安全和良好的用户体验。

## 2. 技术栈
- **前端**: Vue 3, Vue Router 4, Pinia (状态管理), TypeScript
- **后端**: Spring Boot 2.7, Spring Security (密码加密), JWT (令牌认证)
- **数据库**: MySQL 8.0

## 3. 架构设计

### 3.1 用户角色模型
系统定义了三种核心角色，存储于 `sys_user` 表的 `role` 字段中：
- `USER` (学生): 普通学习者，访问学习计划、打卡、商城。
- `TEACHER` (教师): 教学管理者，访问教师管理后台。
- `ADMIN` (管理员): 系统维护者，访问系统管理控制台。

### 3.2 认证与授权流程

1.  **登录认证**:
    - 用户提交用户名/密码至 `/auth/login`。
    - 后端验证通过后，返回 JWT Token 和包含 `role` 的用户信息。
    - 前端 `UserStore` 存储 Token 和 User 对象。

2.  **自动跳转 (Login Redirect)**:
    - `Login.vue` 根据响应中的 `role` 字段进行路由跳转：
        - `ADMIN` -> `/admin-dashboard`
        - `TEACHER` -> `/teacher-dashboard`
        - 其他 -> `/student-dashboard`

3.  **路由守卫 (Router Guard)**:
    - 全局前置守卫 `router.beforeEach` 拦截所有路由导航。
    - **鉴权**: 检查 `to.meta.requiresAuth`，若未登录则重定向至 `/login`。
    - **角色检查**: 检查 `to.meta.role`。若当前用户角色与目标路由所需角色不匹配，触发“无权访问”拦截，并重定向回用户对应的 Dashboard。
    - **已登录重定向**: 若已登录用户访问 `/login` 或 `/`，自动跳转至其对应的 Dashboard，防止重复登录。

### 3.3 目录结构 (前端)

```
src/
├── router/
│   └── index.ts          # 路由定义与全局守卫配置
├── stores/
│   └── user.ts           # 用户状态管理 (Token, User Info)
├── views/
│   ├── Login.vue         # 登录页 (含跳转逻辑)
│   ├── student/
│   │   └── StudentDashboard.vue # 学生门户
│   ├── teacher/
│   │   └── TeacherDashboard.vue # 教师后台
│   └── admin/
│       └── AdminDashboard.vue   # 管理员控制台
```

### 3.4 数据库设计 (变更)

- **表**: `sys_user`
- **字段**: `role` (VARCHAR) - 存储角色标识。
- **初始化**: `RoleDataInitializer.java` 会在系统启动时检查并自动创建默认的 `teacher` 账号（如果不存在）。

## 4. 安全性考量
- **前端拦截**: 通过路由守卫防止用户通过 URL 直接访问未授权页面。
- **后端验证**: (建议) 后端 API 也应添加相应的角色校验注解 (如 `@PreAuthorize`)，以防止绕过前端直接调用接口。
- **Token 安全**: JWT 包含用户信息，并在每次请求头中携带，确保无状态认证的安全性。

## 5. 扩展性
- 新增角色只需在数据库添加对应枚举，在前端 `router/index.ts` 配置新路由和 Meta 信息，并在 `Login.vue` 添加跳转分支即可。
