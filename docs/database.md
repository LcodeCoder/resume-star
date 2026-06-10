# 数据库说明

## 当前运行模式

项目当前默认支持**零数据库启动**：`ResumeLcodeApplication` 排除了 `DataSourceAutoConfiguration` 和 `MybatisPlusAutoConfiguration`，业务接口由 `InMemoryDataRepository` 提供演示数据。

这样可以先完成前后端联调、AI mock 演示和会员体系预留展示。生产接入 MySQL 时，再启用数据源与 Mapper。

## 脚本位置

- 建表脚本：`backend/src/main/resources/db/schema.sql`
- 示例数据：`backend/src/main/resources/db/data.sql`

## 核心表结构

| 表名 | 说明 | 会员预留点 |
|---|---|---|
| `rl_user` | 用户基础信息表 | `vip_level`、`vip_expire_time`、`daily_ai_used`、`total_ai_quota`、`daily_export_used`、`total_export_quota` |
| `rl_resume` | 简历草稿与画布 JSON 数据表 | `vip_component_limit` 高级组件数量上限预留 |
| `rl_template_category` | 模板分类表 | 无 |
| `rl_resume_template` | 简历模板表 | `vip_template` 会员模板标识 |
| `rl_ai_config` | AI 接口配置表 | API Key 仅后端保存，前端不暴露 |
| `rl_ai_call_log` | AI 调用日志表 | `vip_level`、`quota_cost` 额度统计字段 |
| `rl_export_record` | 导出记录表 | `high_definition`、`watermark`、导出额度统计预留 |
| `rl_member_level` | 会员等级表【预留】 | AI 额度、导出额度、会员模板权限 |
| `rl_member_package` | 会员套餐表【预留】 | 套餐价格、有效期、权益说明 |
| `rl_feature_quota_record` | 功能额度记录表【预留】 | AI / 导出 / 模板等功能消耗记录 |

## MySQL 接入步骤

1. 创建数据库：

```sql
CREATE DATABASE resume_lcode DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行脚本：

```bash
mysql -u root -p resume_lcode < backend/src/main/resources/db/schema.sql
mysql -u root -p resume_lcode < backend/src/main/resources/db/data.sql
```

3. 在 `backend/src/main/resources/application.yml` 或环境变量中补充数据源配置：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/resume_lcode?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your-password
```

4. 修改 `ResumeLcodeApplication`：移除数据源与 MyBatis Plus 自动配置排除项。

5. 将当前 Service 中的 `InMemoryDataRepository` 替换为 `mapper` 层查询实现。

## 注意事项

- 会员体系当前只做结构、字段、接口和页面预留，不做支付、扣费或强制权限拦截。
- AI 密钥严禁进入前端代码、静态资源或接口请求体。
- 接入真实数据库后，应补充密码加密、登录态、数据归属校验和操作审计。
