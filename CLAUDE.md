# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ruoyi-vue-pro** (芋道) is a multi-module Java monolith built on Spring Boot 2.7 + JDK 8 (this branch). It provides admin management, IoT, workflow, CRM, ERP, and many other business modules. The frontend lives in a separate repository (`yudao-ui`).

## Build & Run

```bash
# Full build (skip tests)
mvn clean package -DskipTests

# Run the application (default profile: local, port: 48080)
mvn -pl yudao-server spring-boot:run

# Run a single module's tests
mvn -pl yudao-module-system test

# Run a single test class
mvn -pl yudao-module-system -Dtest=TenantServiceImplTest test

# Run a single test method
mvn -pl yudao-module-system -Dtest=TenantServiceImplTest#test_getTenant test
```

### Prerequisites

- MySQL 5.7/8.0+ on `127.0.0.1:3306`, database `ruoyi-vue-pro` (SQL init script: `sql/mysql/ruoyi-vue-pro.sql`)
- Redis 5.0+ on `127.0.0.1:6379`
- Configure credentials in `yudao-server/src/main/resources/application-local.yaml`

## Architecture

### Module Dependency Graph

```
yudao-server (Spring Boot main app, entry point)
  ├── yudao-module-system (users, roles, permissions, tenants, auth)
  │     └── yudao-module-infra (codegen, file storage, config, jobs)
  │           └── yudao-framework (reusable Spring Boot starters)
  │                 └── yudao-dependencies (Maven BOM - version management)
  ├── yudao-module-iot (IoT platform)
  │     ├── yudao-module-iot-core (shared API interfaces, enums, utils)
  │     ├── yudao-module-iot-biz (business logic, controllers, DAL)
  │     └── yudao-module-iot-gateway (MQTT/protocol gateway, separate Spring Boot app)
  └── (other modules commented out in root pom.xml)
```

Modules are enabled/disabled by commenting/uncommenting `<module>` entries in the root `pom.xml`. Currently active: `system`, `infra`, `iot`.

### Internal Package Structure (per module)

Every business module follows the same layered convention under `cn.iocoder.yudao.module.<name>`:

```
├── api/            # Internal API interfaces + implementations (cross-module calls)
│                     Interface: XxxApi.java  →  Impl: XxxApiImpl.java
├── controller/
│   ├── admin/      # Admin backend REST APIs (path: /<module>/...)
│   │   └── vo/     # Request/Response VOs: XxxSaveReqVO, XxxPageReqVO, XxxRespVO
│   └── app/        # User-facing (C-side) REST APIs (path: /<module>/app/...)
├── convert/        # MapStruct converters (XxxConvert)
├── dal/
│   ├── dataobject/ # MyBatis Plus entities: XxxDO (extends BaseDO)
│   ├── mysql/      # Mapper interfaces: XxxMapper (extends BaseMapperX)
│   └── redis/      # Redis DAOs for caching
├── enums/          # Module-specific enums + ErrorCodeConstants
├── framework/      # Module-specific Spring config, interceptors
├── job/            # Quartz scheduled jobs
├── mq/             # Message queue consumers, producers, messages
├── service/        # Business logic: XxxService (interface) → XxxServiceImpl
└── util/           # Module utilities
```

### Framework Starters (`yudao-framework`)

Reusable Spring Boot auto-configuration starters:

| Starter | Purpose |
|---------|---------|
| `yudao-common` | Base classes: `CommonResult<T>` (API envelope), `PageResult`, `BaseDO`, enums, exceptions |
| `yudao-spring-boot-starter-mybatis` | MyBatis Plus + dynamic datasource + `BaseMapperX` with pagination helpers |
| `yudao-spring-boot-starter-security` | Spring Security + Token auth + multi-terminal support |
| `yudao-spring-boot-starter-web` | Global exception handling, API log interceptors |
| `yudao-spring-boot-starter-redis` | Redis + Redisson config |
| `yudao-spring-boot-starter-mq` | Message queue abstraction (Redis Stream, RabbitMQ, Kafka, RocketMQ) |
| `yudao-spring-boot-starter-biz-tenant` | SaaS multi-tenant: `TenantContextHolder`, transparent data filtering |
| `yudao-spring-boot-starter-biz-data-permission` | Row-level data permission |
| `yudao-spring-boot-starter-test` | Test base classes: `BaseDbUnitTest`, `BaseDbAndRedisUnitTest`, `BaseMockitoUnitTest` |

### Key Conventions

- **API response envelope**: All controllers return `CommonResult<T>` with `code` (0=success), `msg`, `data`
- **DO naming**: Entity classes suffixed `DO` (e.g., `TenantDO`), extend `BaseDO` which provides `id`, `createTime`, `updateTime`, `creator`, `updater`, `deleted`
- **VO naming**: `XxxSaveReqVO` (create/update), `XxxPageReqVO` (paginated list), `XxxRespVO` (response)
- **Mapper naming**: `XxxMapper` extends `BaseMapperX` (which adds `selectPage`, `selectList` helpers)
- **DI**: Uses `@Resource` (not `@Autowired`) throughout
- **Validation**: `@Valid` on controller params, `@Validated` on service implementations
- **Lombok**: `lombok.config` at root enables chain accessors, `toString`/`equals` calling super
- **Error codes**: Defined as static fields in each module's `ErrorCodeConstants`, thrown via `ServiceExceptionUtil.exception(...)`
- **Cross-module calls**: Use `api/` layer — define interface in consumer module's classpath, implement in provider module

### Testing

- **JUnit 5 + Mockito** with custom base classes from `yudao-spring-boot-starter-test`
- `BaseDbUnitTest` — H2 in-memory DB test (auto-creates schema)
- `BaseDbAndRedisUnitTest` — H2 + embedded Redis
- `BaseMockitoUnitTest` — pure Mockito, no Spring context
- Tests go in `src/test/java`, mirroring the main source structure

### IoT Module Specifics

The IoT module has a 3-submodule split:
- **`iot-core`**: Shared interfaces (`IotDeviceCommonApi`), auth utils, enums — used by both biz and gateway
- **`iot-biz`**: Main business logic, controllers, DAL — runs inside `yudao-server`
- **`iot-gateway`**: Separate Spring Boot application (`IotGatewayServerApplication`) for MQTT/protocol handling — runs independently

## Configuration Profiles

| File | Purpose |
|------|---------|
| `application.yaml` | Base config (Spring profiles, Jackson, Swagger) |
| `application-local.yaml` | Local dev overrides (DB, Redis, ports). **Edit this for your environment** |
| `application-dev.yaml` | Shared dev environment config |

Default active profile: `local` (set in `application.yaml`). Server runs on port **48080**.


## 参考文档

1. 系统整体文档在 [UnaAI](/Users/sailer/Documents/Obsidian Vault/UnaAI/UnaAI.md);
2. App 文档 [UnaAI_App](/Users/sailer/Documents/Obsidian Vault/UnaAI/UnaAI_App.md) ;
1. iot 文档 [UnaAI_iot](/Users/sailer/Documents/Obsidian Vault/UnaAI/UnaAI_iot.md);
2. Server 文档定义 [UnaAI_Server](/Users/sailer/Documents/Obsidian Vault/UnaAI/UnaAI_Server.md);
2. 应用的接口定义在 apifox中，通过 apifox 这个 mcp 去访问所有的应用接口；
