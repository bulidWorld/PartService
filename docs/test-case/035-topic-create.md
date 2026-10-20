# TC-035 发布无条件主题并查询详情

模块：主题｜优先级：P0｜执行条件：可执行｜执行记录：未执行

## 主题概述与测试目的

验证发布无条件主题并查询详情，以 HTTP 响应和后续查询作为判定依据。

## 独立执行准备

- 本文件单独执行，所有操作及结果核查仅使用已公开的 HTTP 接口；不引用其他用例的数据、令牌或准备脚本。
- 服务应已运行，`BASE_URL=http://127.0.0.1:10201/api/v1`；远程测试替换为对应 HTTPS 地址。使用没有用户及业务数据的独立测试环境，允许存在服务提供的社区和评价分类字典。环境地址由执行者已有的测试环境提供，不在用例中部署服务或重置数据。
- 涉及社区的场景从本文件的 `GET /communities` 响应提取 ID，不能写死或臆造。需要注册或跨社区操作时，若所需社区不足则阻塞，当前没有社区创建 HTTP 接口可补齐前提。专门验证空列表及不需要社区的请求不受此条件限制。
- 注册验证码校验已按约定关闭；请求使用数字字符串 `246810`，不修改服务开关。有效邮箱统一使用 `782213745@qq.com`，多用户使用不同 phone 身份直接注册，无需发送短信。
- `{{变量}}` 仅来自本文件前序 HTTP 响应或步骤中明确说明的客户端计算。按顺序替换真实值；HTTP 块可复制到接口客户端调用。本文件为手工验收文档。
- 相同身份串行复用前，必须确认上一用例已通过 `DELETE /me` 完成账号注销及清理核查；未清理或清理失败时停止后续用例。不得并行使用相同邮箱/手机号；初态不满足时记录阻塞，不删除来源不明的已有账号。
- 每次注册成功后立即保存本用例创建的用户 ID、访问令牌和刷新令牌，纳入待清理名单；预期失败的注册若意外成功，也必须保存响应并清理。只处理本次用例实际创建的账号，不删除其他用例或已有用户。

## 接口调用步骤

### 步骤 1

```http
GET {{BASE_URL}}/communities
Accept: application/json
```

响应应为 HTTP 200、code=0。从 data 数组选择一个可用社区：保存第一项 id → COMMUNITY_A。数量不足则停止并记录阻塞；无社区创建 HTTP 接口，不能继续注册。

### 步骤 2

```http
POST {{BASE_URL}}/auth/register
Accept: application/json
Content-Type: application/json

{
  "identityType": "email",
  "identityValue": "782213745@qq.com",
  "code": "246810",
  "nickname": "测试用户A",
  "communityId": "{{COMMUNITY_A}}"
}
```

成功后保存 `data.user.id → USER_A`、`data.accessToken → TOKEN_A`、`data.refreshToken → REFRESH_A`；均只在本文件中使用。

### 步骤 3

```http
POST {{BASE_URL}}/topics
Accept: application/json
Authorization: Bearer {{TOKEN_A}}
Content-Type: application/json

{
  "title": "周末公园散步",
  "detail": "公园东门集合，步行一小时。",
  "genders": []
}
```

保存 `data.id → T`；检查创建结果后再继续。

### 步骤 4

```http
GET {{BASE_URL}}/topics/{{T}}
Accept: application/json
Authorization: Bearer {{TOKEN_A}}
```

### 步骤 5

```http
GET {{BASE_URL}}/topics?status=open&limit=20
Accept: application/json
Authorization: Bearer {{TOKEN_A}}
```

## 期待结果

- 所有响应包含 `code`、`message`、`data`、非空 `requestId`；成功按 OpenAPI 为 HTTP 200、`code=0`。失败不泄露堆栈或凭证明文。
- 各步骤先核对其预期再继续；准备请求失败或变量无法取得时停止后续业务步骤并记录阻塞，行为与预期不符时记录失败；结束前仍须清理本用例已创建的全部用户。
- 仅判定本文件 HTTP 请求可以观察的结果；没有 HTTP 查询能力的内部存储、广播、日志等项目不计为已验证。
- 所有调用成功；主题的 publisherId=USER_A、communityId=社区 A、status=open，title/detail 与请求一致。
- genders=[]、minAge/maxAge/expiresAt 无限制；主题流出现一次 T。
- 通过主题列表与详情核对同一 T；发布者、社区和状态一致。聊天室可立即或在首位接受者加入时创建，本用例不推断内部参与关系。

## 测试后清理（必执行）

完成业务断言后执行；业务失败、准备阶段部分成功或中途阻塞时，也必须进入本节（自动执行时放在 `finally`/teardown 中）。仅清理本次成功注册的账号，未创建的用户跳过；一个用户清理失败仍继续尝试其他已创建用户。预期失败的注册若意外成功，立即保存其 `data.user.id`、`data.accessToken`、`data.refreshToken` 并加入本节名单；同一用户 ID 只注销一次，若意外创建了不同 ID 则逐个处理。

注销使用各用户自己的令牌，无请求体、无用户 ID 参数。登录或刷新成功后，将该用户最新的 `data.accessToken`、`data.refreshToken` 更新到下表变量；业务步骤使用 TOKEN_NEW、TOKEN_LOGIN 或 REFRESH_NEW 等名称时，清理前复制最新有效值到对应变量，并保留本用例签发过的其他令牌供失效核查。

| 本次创建的用户 | 访问令牌 | 刷新令牌 |
| --- | --- | --- |
| USER_A | TOKEN_A | REFRESH_A |

### 清理 USER_A

仅在 USER_A 已创建且访问令牌过期时，先调用以下刷新请求；成功后用响应覆盖 TOKEN_A、REFRESH_A。令牌仍有效时直接注销。刷新失败时，仍尝试其他用户的清理，并记录本用户未清理；不能把凭证失效当作账号已删除。

```http
POST {{BASE_URL}}/auth/refresh
Accept: application/json
Content-Type: application/json

{
  "refreshToken": "{{REFRESH_A}}"
}
```

```http
DELETE {{BASE_URL}}/me
Accept: application/json
Authorization: Bearer {{TOKEN_A}}
```

注销应返回 HTTP 200、`code=0`、`data={}`、非空 `requestId`。随后核查旧访问令牌和刷新令牌均已失效：

```http
GET {{BASE_URL}}/me
Accept: application/json
Authorization: Bearer {{TOKEN_A}}
```

```http
POST {{BASE_URL}}/auth/refresh
Accept: application/json
Content-Type: application/json

{
  "refreshToken": "{{REFRESH_A}}"
}
```

以上两个核查请求均应返回 HTTP 401、`code=40101`；本用户在用例中通过注册、登录、刷新签发过的其他令牌，也分别使用相同接口核查失效。`/auth/logout` 只撤销刷新会话，不能代替本节的账号注销。

### 清理结果记录

逐用户记录注销及失效核查的实际 HTTP/业务码、requestId 和清理结论，不保存令牌原文。只有所有本次创建用户均注销成功且核查通过，才允许执行后续用例。失败、响应丢失或凭证不足导致无法确认清理时，记录“清理失败/未确认”，停止复用当前环境；不能仅凭 HTTP 401 推断用户已删除，也不能用后台 SQL 替代。

## 执行记录

| 环境/版本 | 执行时间（含时区） | 实际 HTTP/业务码 | requestId | 结论 | 缺陷/阻塞说明 |
| --- | --- | --- | --- | --- | --- |
| 待填写 | 待填写 | 待填写 | 待填写 | 未执行 | 待填写 |

保存脱敏 HTTP 响应，结论填写通过、失败或阻塞。部分可验证时分别记录已核查与阻塞的范围；不得把前提查询成功记作目标场景通过。执行记录不保存验证码或令牌原文。
