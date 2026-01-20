# ddd-admin-micronaut
Micronaut 4 + Micronaut Data + Multi-Tenant

## 功能

- [x] 用户认证登录
- [x] 多租户管理
- [x] 用户管理（CRUD）
- [x] 角色管理
- [x] 权限配置
- [x] 菜单配置
- [x] 在线用户查询与强制下线
- [x] 接口限流保护（基于 Resilience4j）

## 项目结构说明

项目采用多模块架构和DDD（领域驱动设计）分层架构，结合模块化思想对包结构进行了划分，以实现高内聚、低耦合。

### 模块结构

```
ddd-admin-micronaut
├── ddd-common          // 公共模块：基础设施、领域基础类、多租户实现等
├── ddd-sys             // 系统管理模块：用户、角色、权限、菜单等业务功能
└── startup             // 启动模块：应用入口和配置文件
```

### 包结构

```
com.mok.ddd (各模块内的包结构)
├── application         // 应用层: 负责业务流程编排、DTO转换
│   ├── common          // 通用应用服务 (如 BaseService)
│   └── sys             // 系统管理模块 (用户、角色、菜单等)
│       ├── dto         // - 数据传输对象 (Data Transfer Object)
│       ├── mapper      // - MapStruct 转换接口
│       └── service     // - 应用服务实现
│
├── domain              // 领域层: 核心业务逻辑和领域模型
│   ├── common          // 通用领域对象 (如 BaseEntity)
│   └── sys             // 系统管理域
│       ├── model       // - 领域模型/实体 (Entity)
│       └── repository  // - 仓库接口 (Repository Interface)
│
├── infrastructure      // 基础设施层: 提供通用技术能力
│   ├── common          // 通用基础设施组件 (如 AOP 切面)
│   ├── config          // - Micronaut 配置类
│   ├── file            // - 文件服务实现 (Local)
│   ├── limiter         // - 接口限流实现
│   ├── repository      // - 仓库通用实现 (CustomRepositoryImpl)
│   ├── security        // - Micronaut Security 通用配置
│   ├── sys             // 系统管理模块相关基础设施
│   │   └── security    //   - CustomUserDetailsService
│   └── tenant          // - 多租户核心实现
│
├── web                 // 接口层: 暴露 RESTful API
│   ├── common          // 通用 Web 组件 (如全局异常处理)
│   └── sys             // 系统管理模块 Controller
│
└── Application.java    // Micronaut 启动类
```

### 分层职责

*   **web**: 接口层。负责接收 HTTP 请求，参数校验，并调用 `application` 层的服务。
*   **application**: 应用层。负责编排 `domain` 层的领域服务和仓库，处理 DTO 与领域模型的转换，实现具体的业务用例。
*   **domain**: 领域层。包含项目的核心业务逻辑。定义领域实体（Entity）和仓库接口（Repository），实现纯粹的领域服务。**此层不依赖任何其他层**。
*   **infrastructure**: 基础设施层。为其他层提供技术实现，如数据库访问（JPA 实现）、缓存（Redis）、安全（Micronaut Security）、限流等。

## 接口限流功能

项目集成了基于 **Resilience4j** 的灵活限流保护，支持根据请求头动态选择限流策略。

### 限流配置

在 `application.yml` 中配置了多种限流策略和路径映射：

```yaml
resilience4j:
  ratelimiter:
    instances:
      # 默认限流：适用于大部分 API 请求，30 req/s
      default:
        limitForPeriod: 30
        limitRefreshPeriod: 1s
        timeoutDuration: 0
      # 高频限流：适用于特殊高并发场景，如秒杀，300 req/s
      high:
        limitForPeriod: 300
        limitRefreshPeriod: 1s
        timeoutDuration: 0
      # 敏感操作限流：适用于写操作，如创建用户，10 req/s
      sensitive:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
        timeoutDuration: 0

# 路径映射配置（后台配置，无需前端参与）
rate-limit:
  mappings:
    # 高频接口，如秒杀
    "/api/seckill/**": "high"
    # 敏感写操作：路径:方法 格式
    "/api/users:post": "sensitive"
```

### 限流规则

通过配置文件中的路径映射自动选择限流策略，无需前端设置请求头：

| 路径模式 | 限流器 | 限制频率 | 说明 |
|---------|--------|----------|------|
| 默认所有路径 | default | 30 req/s | 普通 API 请求 |
| `/api/seckill/**` | high | 300 req/s | 高并发场景，如秒杀 |
| `/api/users:post` | sensitive | 10 req/s | 创建用户等写操作 |

### 使用方式

**完全后台配置**：开发者在 `application.yml` 中配置路径映射，前端无需任何特殊处理。

**扩展配置**：
- 支持路径通配符：`/api/seckill/**`
- 支持方法指定：`/api/users:post`
- 易于维护：修改配置即可调整限流策略

### 响应行为

- **正常请求**：返回 200 OK
- **超出限制**：返回 429 TOO_MANY_REQUESTS

### 使用方式

**无需注解**：限流通过 `RateLimitFilter` 自动应用，根据请求特征动态选择限流策略。

**响应行为**：
- 正常请求：返回 200 OK
- 超出限制：返回 429 TOO_MANY_REQUESTS

**测试**：
- 运行 `RateLimitFilterTest` 验证限流逻辑
- 运行 `UserControllerRateLimitTest` 验证端到端限流功能

### 为什么这样设计

1. **自动应用**：无需在 Controller 上添加注解，减少代码侵入
2. **基于路径和方法**：根据业务场景智能选择限流级别
3. **保护写操作**：对创建等敏感操作设置更严格限制
4. **可扩展**：可通过配置轻松调整限流策略

## 后续计划

1.  **日志管理**
    - [x] 文件日志配置
    - [x] 接入操作日志（AOP），记录关键业务操作。
    - [x] 接入登录日志，记录用户登录行为及IP。

2.  **数据字典**
    - [x] 实现数据字典管理，支持动态配置系统参数。

3.  **代码生成器**
    - [ ] 开发基于 DDD 架构的代码生成插件或工具，自动生成 Controller, Service, Repository, Entity, DTO。

4.  **文件服务**
    - [x] 封装统一文件服务接口。
    - [x] 支持本地存储
    - [ ] RustFS等多种存储策略。

5.  **定时任务**
    - [ ] 集成 Quartz，实现任务的可视化管理（暂停、恢复、立即执行）。

6.  **系统监控**
    - [ ] 接入 Prometheus + Grafana 进行性能指标监控。

7.  **消息通知**
    - [ ] 封装邮件、短信发送服务。
    - [ ] 集成 WebSocket 实现后端主动推送。

8.  **部署与运维**
    - [ ] 编写 Dockerfile。
    - [ ] 提供 Docker Compose 一键部署脚本。
