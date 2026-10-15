# 聊天室与消息模块数据库设计

## 1. chat_room

字段：`id uuid PK`、`topic_id uuid UNIQUE FK topic`、`name varchar(100)`、`status varchar(16) DEFAULT 'active'`、`created_at`、`closed_at`。

每个主题最多一个聊天室。主题首次出现第一个 accepted 参与者时创建，或发布时预创建，二者需通过唯一约束保证幂等。

## 2. chat_member

字段：`room_id FK`、`user_id FK`、`role varchar(16)`（owner/member）、`joined_at`、`left_at`、`last_read_message_id uuid NULL`；主键 `(room_id,user_id)`。只有 accepted 的主题参与者可以入室；发布者为 owner。

## 3. chat_message

字段：`id uuid PK`、`room_id FK`、`sender_id FK`、`client_message_id varchar(64)`、`message_type varchar(16)`（text/image/system）、`content text`、`content_meta jsonb`、`recalled_at`、`created_at`。

唯一 `(sender_id,client_message_id)` 支持客户端重试幂等；消息正文长度和附件类型由应用层校验。索引 `(room_id,created_at desc,id desc)`。

## 4. chat_message_read

字段：`room_id`、`user_id`、`last_read_message_id`、`read_at`，主键 `(room_id,user_id)`。更新已读使用 upsert，不重复产生记录。

## 5. 保留策略

聊天历史默认长期保留；删除用户时消息发送者置为匿名占位而非物理删除。管理员操作、撤回消息均写 `audit_log`。
