# 数据库设计总览

## 1. 技术与命名

- PostgreSQL 15+，UTF-8，时区统一使用 UTC，接口序列化为 ISO-8601（带 `Z`）。
- 主键统一 `uuid`；业务时间统一 `timestamptz`；软删除表使用 `deleted_at`。
- 表名和字段名使用小写蛇形命名；所有表包含 `created_at`、`updated_at`，需要审计的表增加 `created_by`。
- DDL 必须使用 `CREATE TABLE IF NOT EXISTS`、`CREATE INDEX IF NOT EXISTS`、`DO $$ ... EXCEPTION WHEN duplicate_object ... $$` 等方式，保证可重复执行。

## 2. 核心关系

```text
user ──< topic (发布者)
user ──< topic_recipient (主题收件/推送记录)
topic ──< topic_invitation ──> user
topic ──1 chat_room ──< chat_member ──> user
chat_room ──< chat_message ──> user
topic_invitation(accepted) ──> rating ──> user(被评价者)
user ──1 user_rating_stat
user ──< user_risk_tag
```

## 3. 公共约定

- 逻辑删除后不再出现在客户端列表；统计、审计查询仍可读取历史数据。
- 列表查询必须有稳定排序（通常 `created_at DESC, id DESC`）和索引；分页采用 `limit + cursor`。
- 重要操作写入 `audit_log`（发帖、邀请、接受、评价、标签变更）；HTTP 请求写入应用日志，级别为 `INFO`。

## 4. 公共表：audit_log

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | uuid | PK | 日志 ID |
| request_id | varchar(64) | NOT NULL | HTTP 请求关联 ID |
| actor_user_id | uuid | NULL, FK user | 操作者 |
| action | varchar(64) | NOT NULL | 如 `topic.create` |
| resource_type | varchar(32) | NOT NULL | 资源类型 |
| resource_id | uuid | NULL | 资源 ID |
| detail | jsonb | NOT NULL DEFAULT '{}' | 脱敏后的变更内容 |
| ip | inet | NULL | 客户端 IP |
| created_at | timestamptz | NOT NULL DEFAULT now() | 发生时间 |

索引：`(actor_user_id, created_at desc)`、`(resource_type, resource_id, created_at desc)`、`(request_id)`。

## 5. 部署建议

按 `database/01~04` 顺序执行迁移；每个文件可独立重复执行。生产环境使用迁移版本表（例如 `schema_migration(version primary key, applied_at)`）记录版本，不在启动时执行破坏性变更。
