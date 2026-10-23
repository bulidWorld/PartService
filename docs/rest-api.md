# REST API 对接文档

## 1. 基础约定

- Base URL：`https://{host}/api/v1`；HTTPS 必须开启。
- 请求头：`Authorization: Bearer <access_token>`、`X-Request-Id`（可选，服务端缺省生成）、`Content-Type: application/json`。
- 时间使用 ISO-8601 UTC；ID 使用 UUID；列表使用 `limit`（默认 20，最大 100）和 `cursor`。
- 统一响应：

```json
{"code":0,"message":"ok","data":{},"request_id":"req_123"}
```

`code=0` 成功；非 0 时 `data` 可为 `null`。常用错误码：`40001` 参数错误、`40101` 未登录/令牌失效、`40301` 无权限、`40401` 资源不存在、`40901` 状态冲突、`42901` 请求过于频繁、`50000` 服务端错误。

列表统一返回：

```json
{"code":0,"message":"ok","data":{"items":[],"next_cursor":null,"has_more":false},"request_id":"req_123"}
```

`next_cursor` 为空表示没有下一页；客户端不要自行按页码拼接，避免新增数据导致重复或漏项。

## 2. 认证与用户

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/send-code` | 发送注册/登录验证码；body `{identity_type,identity_value,purpose}` |
| POST | `/auth/register` | body `{identity_type,identity_value,code,nickname,community_id}`；返回 `access_token,refresh_token,user` |
| POST | `/auth/login` | body `{identity_type,identity_value,code,device_id}` |
| POST | `/auth/refresh` | body `{refresh_token}`，返回新 access token |
| POST | `/auth/logout` | 撤销当前 refresh token |
| GET | `/me` | 当前用户资料 |
| DELETE | `/me` | 注销当前账号，无请求体；删除账号、身份、全部令牌及关联业务数据，但保留聊天室、历史消息和其他成员的已读位置，原发送者 ID、主题 ID 不变；释放邮箱/手机号供重新注册；HTTP 200、`code=0`、`data={}` |
| PATCH | `/me` | 修改 `nickname,gender,birth_date,avatar_url` |
| PATCH | `/me/community` | body `{community_id}`；7 天内切换返回 `40901` |
| GET | `/communities` | 获取可用社区 |

`user` 返回：`id,nickname,gender,age,avatar_url,community{id,name},risk_tags`。服务端根据 `birth_date` 计算 `age`，不返回出生日期给其他用户。

注册/登录成功响应示例：

```json
{"code":0,"message":"ok","data":{"access_token":"...","expires_in":7200,"refresh_token":"...","user":{"id":"...","nickname":"小明","community":{"id":"...","name":"城市社区"}}},"request_id":"..."}
```

## 3. 主题推送与接受

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/topics` | 发布；body `{title,detail,constraints,expires_at}` |
| GET | `/topics` | 同社区主题流；query `status,cursor,limit` |
| GET | `/topics/{topic_id}` | 主题详情 |
| PATCH | `/topics/{topic_id}` | 发布者修改标题、详情、约束 |
| POST | `/topics/{topic_id}/close` | 发布者关闭主题 |
| GET | `/topics/receipts` | 我的主题收件，query `status,cursor,limit` |
| POST | `/topics/{topic_id}/accept` | 接受主题，首次接受创建聊天室，幂等 |
| POST | `/topics/{topic_id}/read` | 标记主题收件已读，幂等 |

主题响应示例：`{id,title,detail,constraints:{min_age,max_age,genders},publisher,community,status,expires_at,participant_count,room_id}`。跨社区主题统一返回 `40401`，避免泄露信息。

`constraints` 所有字段可选；`genders` 为空数组表示不限制性别。年龄按用户当前年龄计算，缺少年龄资料的用户不会匹配设置了年龄上下限的主题。

## 4. 聊天

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/rooms` | 当前用户聊天室列表 |
| GET | `/rooms/{room_id}` | 聊天室详情和成员 |
| GET | `/rooms/{room_id}/messages` | query `before,limit` 获取历史消息 |
| POST | `/rooms/{room_id}/messages` | body `{client_message_id,message_type,content,content_meta}`；重复 client ID 返回原消息 |
| POST | `/rooms/{room_id}/read` | body `{last_read_message_id}` 更新已读 |
| POST | `/rooms/{room_id}/messages/{message_id}/recall` | 撤回自己的消息 |

聊天室仅允许 `chat_member` 中的 active 成员访问。实时推送可在此 REST 契约之上使用 WebSocket，连接鉴权仍使用 access token。

## 5. 评价

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/rating-categories` | 获取启用的评价分类 |
| POST | `/topics/{topic_id}/ratings` | body `{to_user_id,score,category_codes,comment}` |
| GET | `/users/{user_id}/ratings` | 获取用户公开评价汇总；支持 `cursor,limit` |
| GET | `/me/rating-summary` | 当前用户好/中/差数量、好评率及风险标签 |

创建评价返回 `40901` 表示重复评价、未共同参与主题或主题状态不允许评价。评价列表默认隐藏评价者联系方式，仅返回昵称、头像、评分、分类、评论和时间。

评价摘要示例：`{good_count:12,neutral_count:2,bad_count:1,total_count:15,good_rate:0.8,bad_rate:0.0667,risk_tags:[]}`。`good_rate` 在没有评价时为 `null`，客户端应显示“暂无评价”，不要显示为 0%。

## 6. 客户端错误处理与安全

- 客户端应以 `code` 而非 HTTP 文案判断业务结果；HTTP 状态码仍按语义返回（成功 2xx、参数 4xx、服务端 5xx）。
- 所有 POST/PATCH 建议携带 `Idempotency-Key`；服务端至少对注册、接受主题、发消息、提交评价做幂等。
- 验证码、token、评论中的敏感信息不得写入日志；服务端日志通过 `request_id` 串联，HTTP 请求以 INFO 级别记录。
- 客户端收到 `40101` 应尝试 refresh 一次，仍失败则清理本地会话并回到登录页；收到 `42901` 按 `Retry-After` 等待。
