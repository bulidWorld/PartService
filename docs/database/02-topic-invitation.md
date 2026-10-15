# 主题与邀请模块数据库设计

## 1. topic

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | uuid | PK | |
| publisher_id | uuid | FK user | 发布者 |
| community_id | uuid | FK community | 发布时社区快照，避免用户换社区影响历史主题 |
| title | varchar(80) | NOT NULL | 标题 |
| detail | text | NOT NULL | 详情 |
| constraint_json | jsonb | NOT NULL DEFAULT '{}' | 年龄、性别等约束 |
| status | varchar(16) | NOT NULL DEFAULT 'open' | `open/closed/cancelled/expired` |
| expires_at | timestamptz | NULL | 可选过期时间 |
| created_at/updated_at | timestamptz | NOT NULL | |

`constraint_json` 推荐结构：`{"min_age":18,"max_age":35,"genders":["female","male"]}`。服务端必须校验上下限和枚举，不能依赖客户端。

索引：`(community_id,status,created_at desc)`、`(publisher_id,created_at desc)`。

## 2. topic_recipient

记录同社区推送结果，便于未读数和幂等：`id uuid PK`、`topic_id FK`、`user_id FK`、`match_reason jsonb`、`read_at`、`created_at`。唯一 `(topic_id,user_id)`。发布者默认不生成收件记录。

## 3. topic_invitation

字段：`id uuid PK`、`topic_id FK`、`invitee_id FK`、`inviter_id FK`、`status varchar(16)`（pending/accepted/rejected/expired/cancelled）、`responded_at`、`created_at`、`updated_at`。唯一 `(topic_id,invitee_id)`。

接受时在同一事务中：校验主题开放、邀请属于本人、约束仍满足；更新为 accepted；创建聊天室成员（见 03）；重复请求返回相同结果（幂等）。

## 4. topic_participant

用于主题参与者统一查询：`topic_id`、`user_id`、`source`（publisher/invitation）、`joined_at`、`left_at`，主键 `(topic_id,user_id)`。发布者创建主题时加入；邀请接受时加入。
