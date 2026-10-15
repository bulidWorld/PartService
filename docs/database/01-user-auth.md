# 用户与认证模块数据库设计

## 1. user

| 字段 | 类型 | 约束/默认值 | 说明 |
| --- | --- | --- | --- |
| id | uuid | PK | 用户 ID |
| nickname | varchar(32) | NOT NULL | 昵称，应用层校验长度和敏感词 |
| community_id | uuid | NOT NULL, FK community | 当前社区 |
| gender | varchar(16) | NULL | `male/female/other` |
| birth_date | date | NULL | 客户端可传年龄，服务端建议落出生日期 |
| avatar_url | text | NULL | 头像地址 |
| status | varchar(16) | NOT NULL DEFAULT 'active' | `active/blocked/deleted` |
| community_changed_at | timestamptz | NOT NULL DEFAULT now() | 最近一次切换社区时间 |
| last_login_at | timestamptz | NULL | 最近登录 |
| created_at/updated_at | timestamptz | NOT NULL | 公共时间字段 |

约束：昵称非空；`birth_date` 不得晚于当前日期；社区切换通过事务锁检查 `community_changed_at <= now()-interval '7 days'`。

## 2. user_identity

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | uuid | PK | |
| user_id | uuid | FK user, NOT NULL | |
| identity_type | varchar(16) | NOT NULL | `email` 或 `phone` |
| identity_value | varchar(254) | NOT NULL | 规范化后的邮箱/手机号 |
| verified_at | timestamptz | NULL | 验证成功时间 |
| is_primary | boolean | NOT NULL DEFAULT false | 主登录方式 |
| created_at | timestamptz | NOT NULL | |

唯一索引：`(identity_type, identity_value)`；同一用户最多一个主身份（部分唯一索引 `WHERE is_primary`）。

## 3. verification_code

字段：`id uuid PK`、`identity_type`、`identity_value`、`purpose varchar(16)`（register/login/reset）、`code_hash varchar(128)`、`expires_at`、`consumed_at`、`attempt_count int default 0`、`created_at`。

只保存摘要；同一身份同一用途 60 秒内最多发送一次，5 分钟过期，连续错误达到 5 次作废。索引 `(identity_type, identity_value, purpose, created_at desc)`。

## 4. community

字段：`id uuid PK`、`name varchar(64) UNIQUE`、`status varchar(16) DEFAULT 'active'`、`created_at`、`updated_at`。社区停用后不可注册或切换，但保留历史数据。

## 5. refresh_token

字段：`id uuid PK`、`user_id FK`、`token_hash varchar(128) UNIQUE`、`device_id varchar(128)`、`expires_at`、`revoked_at`、`created_at`。服务端只返回原始 token 一次，数据库保存哈希；支持单设备注销和全设备注销。
