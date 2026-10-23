# 接口业务测试用例

共 **142 个编号场景**，每个文件独立描述自己的前提、HTTP 数据准备、请求参数、预期及执行记录；数字只表示序号，无文件执行依赖。

**所有测试操作与核查仅使用公开 HTTP 接口。** 已移除 SQL、后台造数、直查数据、日志文件读取、服务重启及故障注入步骤。无法通过现有 HTTP 完成的场景保留编号并明确记录阻塞；部分可验证的场景不宣称内部状态已通过验收。

旧邀请场景已替换为主题推送、收件与接受场景；保留原编号和文件名以维持已有链接。接受请求无请求体，身份取自访问令牌，返回 `topicId/status/roomId/readAt/createdAt`。

## 执行方式

1. 使用已运行、无用户及业务数据的独立测试环境，填写文件内 BASE_URL。允许已有可查询的基础字典；用例本身不部署、清库或重置环境。
2. 从该文件 GET /communities 响应选取 COMMUNITY_A；跨社区场景还需另一个 COMMUNITY_B。若社区不足，直接阻塞。当前没有创建社区的 HTTP 接口，不能补写数据或用固定 UUID 绕过。
3. 涉及分类时，由该文件 GET /rating-categories 确认所需编码；用户、主题、消息和评价均由同一文件的 HTTP 请求创建；主题收件由发布自动触发生成。
4. 按顺序执行请求，替换本文件取得的变量并核查后续 HTTP 查询结果。每次注册成功立即记录用户 ID 和令牌；预期失败的注册若意外成功，也纳入清理名单。
5. 无论通过、失败或中途阻塞，均执行文件内“测试后清理”：逐个使用本次创建用户自己的访问令牌调用 `DELETE /me`，再核查旧访问/刷新令牌失效。清理失败仍尝试其他用户，并记录逐用户清理结果；全部清理确认成功后才允许复用环境或执行下一用例。退出登录 `/auth/logout` 不能代替账号注销。
6. 冷却、过期和令牌有效期通过真实时间等待。缺少接口的场景记录阻塞，不使用后台、运维或数据库手段替代。

编号文件是 Markdown 手工验收用例，尚未调用实际服务执行。静态检查只验证文档结构、变量、路径、参数与链接。

## 契约及执行边界

- HTTP 路径与请求字段以 src/main/resources/rest-api/*.yaml 的 28 个操作为准，业务语义参照 docs/requirements/；没有臆造初始化或测试管理接口。
- 响应统一字段为 code/message/data/requestId；分页为 items/nextCursor/hasMore。验证码发送响应和评价摘要保留 YAML 明确的 snake_case 字段。
- 常用错误预期为 400/40001、401/40101、403/40301、404/40401、409/40901、429/42901；尚未统一的错误语义在具体用例中注明。
- 所有会成功创建用户的 118 个编号用例均包含独立的注销、令牌失效核查及清理结果记录；TC-018 补存注册凭证，TC-136 仅发送非法 JSON，不应创建用户。相同邮箱/手机号仅在前一用例清理成功后串行复用，不能并行注册同一身份。
- `DELETE /me` 无请求体，只注销令牌对应的当前用户，并在同一事务中删除账号、身份、全部会话和关联业务数据，释放注册身份；注销失败时整体回滚。不能对来源不明或其他用例的账号执行清理。
- 有效邮箱统一为 782213745@qq.com，验证码示例为数字字符串 246810。按现有约定不改验证码开关；源码登录仍校验验证码的差异需记录，不能通过后台绕过。
- 完全空库且社区字典也为空时，TC-001 可验证空列表；注册链路因没有社区创建 HTTP 接口而阻塞。已有社区字典时可以执行其余前提满足的场景，不能同时声称完全空社区初态成立。
- TC-032、TC-056、TC-075 必须在各自注册后真实等待七天；不会借用其他文件的老用户。
- 停用社区、封禁、停用分类、算法控制、日志审计、重启和中途失败缺少公开 HTTP 入口，相关场景明确阻塞。主题已读可由收件列表的 readAt 核查；聊天室已读是否可查仍以对应接口字段为准。
- 外部通知送达、单侧年龄、匿名、申诉、附件规则等缺口保留待确认说明；未满足的前提不能算通过。

## 用例索引

| 编号 | 文件与场景 | 模块 | 优先级 | 执行条件 |
| --- | --- | --- | --- | --- |
| TC-001 | [完全空库时查询社区](001-empty-communities.md) | 账号与用户 | P1 | 可执行 |
| TC-002 | [发送邮箱注册验证码](002-email-code.md) | 账号与用户 | P0 | 可执行 |
| TC-003 | [身份类型不支持](003-invalid-identity-type.md) | 账号与用户 | P1 | 可执行 |
| TC-004 | [邮箱格式错误](004-invalid-email.md) | 账号与用户 | P1 | 可执行 |
| TC-005 | [验证码用途不支持](005-invalid-code-purpose.md) | 账号与用户 | P1 | 可执行 |
| TC-006 | [验证码发送频率限制](006-send-code-rate-limit.md) | 账号与用户 | P1 | 可执行 |
| TC-007 | [邮箱最小资料注册](007-email-register.md) | 账号与用户 | P0 | 可执行 |
| TC-008 | [手机号最小资料注册](008-phone-register.md) | 账号与用户 | P1 | 可执行 |
| TC-009 | [注册缺少昵称](009-register-missing-nickname.md) | 账号与用户 | P1 | 可执行 |
| TC-010 | [注册昵称全空白](010-register-blank-nickname.md) | 账号与用户 | P1 | 可执行 |
| TC-011 | [注册缺少社区](011-register-missing-community.md) | 账号与用户 | P1 | 可执行 |
| TC-012 | [注册社区 UUID 格式错误](012-register-invalid-community-uuid.md) | 账号与用户 | P1 | 可执行 |
| TC-013 | [注册社区不存在](013-register-unknown-community.md) | 账号与用户 | P1 | 可执行 |
| TC-014 | [注册昵称超过 32 字符](014-register-long-nickname.md) | 账号与用户 | P1 | 可执行 |
| TC-015 | [注册昵称恰好 32 字符](015-register-nickname-boundary.md) | 账号与用户 | P1 | 可执行 |
| TC-016 | [重复身份注册不创建第二个用户](016-register-duplicate-identity.md) | 账号与用户 | P0 | 可执行 |
| TC-017 | [不同身份允许昵称相同](017-duplicate-nickname.md) | 账号与用户 | P1 | 可执行 |
| TC-018 | [关闭注册验证码时省略 code](018-register-no-code.md) | 账号与用户 | P1 | 可执行 |
| TC-019 | [已注册邮箱登录](019-login-email.md) | 账号与用户 | P0 | 可执行 |
| TC-020 | [未注册邮箱不能登录](020-login-unregistered.md) | 账号与用户 | P1 | 可执行 |
| TC-021 | [有效刷新令牌续期](021-refresh-token.md) | 账号与用户 | P0 | 可执行 |
| TC-022 | [伪造刷新令牌](022-invalid-refresh-token.md) | 账号与用户 | P1 | 可执行 |
| TC-023 | [注销后刷新令牌失效](023-logout-revokes-refresh.md) | 账号与用户 | P0 | 可执行 |
| TC-024 | [禁止撤销其他用户的刷新会话](024-logout-other-user.md) | 账号与用户 | P1 | 可执行 |
| TC-025 | [修改并持久化个人资料](025-profile-update.md) | 账号与用户 | P1 | 可执行 |
| TC-026 | [只修改昵称保留其他资料](026-profile-partial.md) | 账号与用户 | P1 | 可执行 |
| TC-027 | [资料拒绝未知性别](027-profile-invalid-gender.md) | 账号与用户 | P1 | 可执行 |
| TC-028 | [资料拒绝未来出生日期](028-profile-future-birth.md) | 账号与用户 | P1 | 可执行 |
| TC-029 | [资料拒绝空白昵称](029-profile-blank-nickname.md) | 账号与用户 | P1 | 可执行 |
| TC-030 | [头像拒绝非受控附件地址](030-profile-invalid-avatar.md) | 账号与用户 | P1 | 待附件规则确认 |
| TC-031 | [注册后七天内不能切换社区](031-community-cooldown.md) | 账号与用户 | P1 | 可执行 |
| TC-032 | [超过七天后切换并重新开始冷却](032-community-after-seven-days.md) | 账号与用户 | P1 | 需真实等待 7 天 |
| TC-033 | [不能切换到停用社区](033-community-inactive.md) | 账号与用户 | P1 | 阻塞：缺少 HTTP 接口 |
| TC-034 | [封禁账号不能执行受保护业务](034-banned-user.md) | 账号与用户 | P1 | 阻塞：缺少 HTTP 接口 |
| TC-035 | [发布无条件主题并查询详情](035-topic-create.md) | 主题 | P0 | 可执行 |
| TC-036 | [发布主题缺少标题](036-topic-missing-title.md) | 主题 | P1 | 可执行 |
| TC-037 | [发布主题标题全空白](037-topic-blank-title.md) | 主题 | P1 | 可执行 |
| TC-038 | [发布主题缺少详情](038-topic-missing-detail.md) | 主题 | P1 | 可执行 |
| TC-039 | [发布主题详情全空白](039-topic-blank-detail.md) | 主题 | P1 | 可执行 |
| TC-040 | [发布主题标题超过 80 字符](040-topic-title-too-long.md) | 主题 | P1 | 可执行 |
| TC-041 | [发布主题最小年龄大于最大年龄](041-topic-reversed-age.md) | 主题 | P1 | 可执行 |
| TC-042 | [主题性别约束非法](042-topic-unknown-gender.md) | 主题 | P1 | 可执行 |
| TC-043 | [主题过期时间格式错误](043-topic-invalid-expiry.md) | 主题 | P1 | 可执行 |
| TC-044 | [主题标题恰好 80 字符](044-topic-title-boundary.md) | 主题 | P1 | 可执行 |
| TC-045 | [主题年龄上下限均可匹配](045-topic-age-boundary.md) | 主题 | P1 | 可执行 |
| TC-046 | [缺少出生日期不匹配年龄主题](046-topic-missing-age.md) | 主题 | P1 | 可执行 |
| TC-047 | [仅性别限制不要求出生日期](047-topic-gender-only.md) | 主题 | P1 | 可执行 |
| TC-048 | [性别不满足主题约束](048-topic-gender-mismatch.md) | 主题 | P1 | 可执行 |
| TC-049 | [跨社区主题列表和详情隔离](049-topic-cross-community.md) | 主题 | P0 | 可执行 |
| TC-050 | [发布者修改主题](050-topic-update-owner.md) | 主题 | P1 | 可执行 |
| TC-051 | [非发布者不能修改主题](051-topic-forbidden-update.md) | 主题 | P1 | 可执行 |
| TC-052 | [非发布者不能关闭主题](052-topic-forbidden-close.md) | 主题 | P1 | 可执行 |
| TC-053 | [关闭主题并重复关闭](053-topic-close.md) | 主题 | P1 | 可执行 |
| TC-054 | [主题流默认只包含开放主题](054-topic-default-open-feed.md) | 主题 | P1 | 可执行 |
| TC-055 | [到达过期时间后不能新增参与](055-topic-expiration.md) | 主题 | P1 | 需自然等待过期 |
| TC-056 | [切换社区不改变已发布主题归属](056-topic-community-snapshot.md) | 主题 | P1 | 需真实等待 7 天 |
| TC-057 | [主题流游标分页不重复不遗漏](057-topic-pagination.md) | 主题 | P1 | 可执行 |
| TC-058 | [仅提供一个年龄边界的规则确认](058-topic-single-age-bound.md) | 主题 | P1 | 待规则确认 |
| TC-059 | [发布后自动生成主题收件](059-invite-create.md) | 主题推送与参与 | P0 | 可执行 |
| TC-060 | [首次接受前不创建聊天室](060-invite-non-owner.md) | 主题推送与参与 | P1 | 可执行 |
| TC-061 | [发布者不能接受自己的主题](061-invite-self.md) | 主题推送与参与 | P1 | 可执行 |
| TC-062 | [跨社区用户不收到且不能接受主题](062-invite-other-community.md) | 主题推送与参与 | P1 | 可执行 |
| TC-063 | [接受不存在的主题](063-invite-unknown-user.md) | 主题推送与参与 | P1 | 可执行 |
| TC-064 | [不匹配用户不收到且不能接受主题](064-invite-mismatch.md) | 主题推送与参与 | P1 | 可执行 |
| TC-065 | [反复查询不重复生成收件](065-invite-duplicate.md) | 主题推送与参与 | P1 | 可执行 |
| TC-066 | [关闭使未接受收件失效](066-invite-closed-topic.md) | 主题推送与参与 | P1 | 可执行 |
| TC-067 | [首次接受创建聊天室并加入双方](067-invitation-accept.md) | 主题推送与参与 | P0 | 可执行 |
| TC-068 | [忽略主题不产生参与关系](068-invitation-reject.md) | 主题推送与参与 | P1 | 可执行 |
| TC-069 | [重复接受返回同一聊天室](069-invitation-accept-retry.md) | 主题推送与参与 | P0 | 可执行 |
| TC-070 | [过期后已接受用户重试仍成功](070-invitation-reject-retry.md) | 主题推送与参与 | P1 | 需自然等待过期 |
| TC-071 | [主题收件已读持久化与幂等](071-invitation-accepted-reject.md) | 主题推送与参与 | P1 | 可执行 |
| TC-072 | [接受身份由当前令牌确定](072-invitation-forged-actor.md) | 主题推送与参与 | P0 | 可执行 |
| TC-073 | [关闭后不能新增接受者](073-invitation-accept-closed.md) | 主题推送与参与 | P1 | 可执行 |
| TC-074 | [接受时重新校验用户资料](074-invitation-recheck-profile.md) | 主题推送与参与 | P1 | 可执行 |
| TC-075 | [接受时重新校验社区归属](075-invitation-recheck-community.md) | 主题推送与参与 | P1 | 需真实等待 7 天 |
| TC-076 | [主题收件按用户与状态隔离](076-invitations-isolation.md) | 主题推送与参与 | P1 | 可执行 |
| TC-077 | [主题收件游标分页无重复遗漏](077-invitations-pagination.md) | 主题推送与参与 | P1 | 可执行 |
| TC-078 | [多用户并发首次接受共享聊天室](078-invitation-concurrent-accept.md) | 主题推送与参与 | P1 | 可执行 |
| TC-079 | [自动分发排除跨社区及不匹配用户](079-topic-automatic-distribution.md) | 主题推送与参与 | P1 | 可执行 |
| TC-080 | [成员发送并读取文本消息](080-chat-send.md) | 聊天 | P0 | 可执行 |
| TC-081 | [同一客户端消息 ID 重试幂等](081-chat-message-retry.md) | 聊天 | P0 | 可执行 |
| TC-082 | [不同发送者可以使用相同客户端消息 ID](082-chat-two-senders.md) | 聊天 | P1 | 可执行 |
| TC-083 | [不能发送空文本](083-chat-empty-text.md) | 聊天 | P1 | 可执行 |
| TC-084 | [不能发送全空白文本](084-chat-blank-text.md) | 聊天 | P1 | 可执行 |
| TC-085 | [不能发送未知消息类型](085-chat-unknown-type.md) | 聊天 | P1 | 可执行 |
| TC-086 | [客户端消息 ID 超过 64 字符](086-chat-long-client-id.md) | 聊天 | P1 | 可执行 |
| TC-087 | [客户端消息 ID 恰好 64 字符](087-chat-client-id-boundary.md) | 聊天 | P1 | 可执行 |
| TC-088 | [非成员不能读取聊天室详情](088-chat-outsider-detail.md) | 聊天 | P0 | 可执行 |
| TC-089 | [非成员不能读取消息](089-chat-outsider-history.md) | 聊天 | P0 | 可执行 |
| TC-090 | [非成员不能发送消息](090-chat-outsider-send.md) | 聊天 | P0 | 可执行 |
| TC-091 | [非成员不能标记已读](091-chat-outsider-read.md) | 聊天 | P0 | 可执行 |
| TC-092 | [非成员不能撤回消息](092-chat-outsider-recall.md) | 聊天 | P0 | 可执行 |
| TC-093 | [成员标记已读并持久化](093-chat-read-position.md) | 聊天 | P1 | 部分可验证 |
| TC-094 | [只能撤回自己发送的消息](094-chat-recall-self.md) | 聊天 | P1 | 可执行 |
| TC-095 | [重复撤回不重复修改历史](095-chat-recall-idempotent.md) | 聊天 | P1 | 可执行 |
| TC-096 | [撤回时校验消息所属房间](096-chat-wrong-room-recall.md) | 聊天 | P0 | 可执行 |
| TC-097 | [已读消息必须属于当前房间](097-chat-wrong-room-read.md) | 聊天 | P1 | 可执行 |
| TC-098 | [主题关闭后原成员继续聊天](098-chat-closed-topic.md) | 聊天 | P1 | 可执行 |
| TC-099 | [消息历史按 before 游标分页](099-chat-pagination.md) | 聊天 | P1 | 可执行 |
| TC-100 | [聊天拒绝非受控图片地址](100-chat-untrusted-image.md) | 聊天 | P1 | 待附件规则确认 |
| TC-101 | [共同参与者提交好评](101-rating-good.md) | 评价 | P0 | 可执行 |
| TC-102 | [共同参与者提交中评](102-rating-neutral.md) | 评价 | P1 | 可执行 |
| TC-103 | [共同参与者提交差评](103-rating-bad.md) | 评价 | P1 | 可执行 |
| TC-104 | [评分以外的分类和评论可省略](104-rating-minimal.md) | 评价 | P1 | 可执行 |
| TC-105 | [不能评价自己](105-rating-self.md) | 评价 | P1 | 可执行 |
| TC-106 | [未接受主题不能评价](106-rating-unaccepted.md) | 评价 | P0 | 可执行 |
| TC-107 | [共同参与其他主题不能跨主题评价](107-rating-cross-topic.md) | 评价 | P1 | 可执行 |
| TC-108 | [同主题同方向评价只保存一次](108-rating-duplicate.md) | 评价 | P0 | 可执行 |
| TC-109 | [双方可以分别互评](109-rating-mutual.md) | 评价 | P1 | 可执行 |
| TC-110 | [拒绝未知评分](110-rating-invalid-score.md) | 评价 | P1 | 可执行 |
| TC-111 | [拒绝未知评价分类](111-rating-invalid-category.md) | 评价 | P1 | 可执行 |
| TC-112 | [评价评论超过 500 字符](112-rating-comment-too-long.md) | 评价 | P1 | 可执行 |
| TC-113 | [评价评论恰好 500 字符](113-rating-comment-boundary.md) | 评价 | P1 | 可执行 |
| TC-114 | [客户端缓存的停用评价分类被拒绝](114-rating-disabled-category.md) | 评价 | P1 | 阻塞：缺少 HTTP 接口 |
| TC-115 | [无评价时汇总为空样本](115-rating-empty-summary.md) | 评价 | P1 | 可执行 |
| TC-116 | [评价数量与好差评率统计](116-rating-summary-counts.md) | 评价 | P0 | 可执行 |
| TC-117 | [用户公开评价游标分页](117-rating-pagination.md) | 评价 | P1 | 可执行 |
| TC-118 | [公开评价不泄露联系方式和凭证](118-rating-public-privacy.md) | 评价 | P1 | 可执行 |
| TC-119 | [并发提交评价只计数一次](119-rating-concurrent.md) | 评价 | P1 | 可执行 |
| TC-120 | [匿名评价契约缺口](120-rating-anonymous.md) | 评价 | P1 | 待契约确认 |
| TC-121 | [评价申诉契约缺口](121-rating-appeal.md) | 评价 | P1 | 待契约确认 |
| TC-122 | [少量评价不能触发系统评级](122-risk-insufficient-samples.md) | 系统评级 | P1 | 待算法规则确认 |
| TC-123 | [达到配置阈值生成系统评级](123-risk-threshold.md) | 系统评级 | P1 | 阻塞：缺少 HTTP 接口 |
| TC-124 | [相同样本重复计算不增加计数和评级](124-risk-recompute.md) | 系统评级 | P1 | 阻塞：缺少 HTTP 接口 |
| TC-125 | [滚动窗口排除窗口外样本](125-risk-window.md) | 系统评级 | P1 | 阻塞：缺少 HTTP 接口 |
| TC-126 | [异步统计失败后重试补偿](126-risk-recovery.md) | 系统评级 | P1 | 阻塞：缺少 HTTP 接口 |
| TC-127 | [算法版本变更可追溯](127-risk-algorithm-version.md) | 系统评级 | P1 | 阻塞：缺少 HTTP 接口 |
| TC-128 | [不再满足条件时移除系统评级](128-risk-tag-removal.md) | 系统评级 | P1 | 阻塞：缺少 HTTP 接口 |
| TC-129 | [系统评级对其他用户可见](129-risk-public-visibility.md) | 系统评级 | P1 | 阻塞：缺少 HTTP 接口 |
| TC-130 | [受保护接口缺少访问令牌](130-auth-missing.md) | 通用 | P0 | 可执行 |
| TC-131 | [受保护接口伪造访问令牌](131-auth-forged.md) | 通用 | P0 | 可执行 |
| TC-132 | [访问令牌到期后只能刷新续期](132-auth-expired.md) | 通用 | P1 | 需自然等待令牌过期 |
| TC-133 | [分页 limit=0 被拒绝](133-pagination-limit-0.md) | 通用 | P1 | 可执行 |
| TC-134 | [分页 limit=101 被拒绝](134-pagination-limit-101.md) | 通用 | P1 | 可执行 |
| TC-135 | [分页拒绝无效游标](135-pagination-invalid-cursor.md) | 通用 | P1 | 可执行 |
| TC-136 | [JSON 解析错误统一返回参数错误](136-request-invalid-json.md) | 通用 | P1 | 可执行 |
| TC-137 | [资源 ID 非 UUID 返回参数错误](137-request-invalid-path-id.md) | 通用 | P1 | 可执行 |
| TC-138 | [请求链路 ID 自动生成和透传](138-request-id.md) | 通用 | P1 | 可执行 |
| TC-139 | [完整业务链路审计与敏感信息脱敏](139-audit-log-redaction.md) | 通用 | P1 | 阻塞：缺少 HTTP 接口 |
| TC-140 | [服务重启后用户主题成员消息评价仍存在](140-persistence-restart.md) | 通用 | P0 | 阻塞：缺少 HTTP 接口 |
| TC-141 | [接受主题写入故障时整体回滚](141-transaction-accept-rollback.md) | 通用 | P1 | 阻塞：缺少 HTTP 接口 |
| TC-142 | [注册到参与聊天互评完整链路](142-e2e.md) | 端到端 | P0 | 可执行 |

## 28 个 HTTP 操作覆盖

| 方法与路径 | operationId | 用例示例 |
| --- | --- | --- |
| `POST /auth/send-code` | `sendCode` | [TC-002](002-email-code.md)、[TC-003](003-invalid-identity-type.md)、[TC-004](004-invalid-email.md)、[TC-005](005-invalid-code-purpose.md) |
| `POST /auth/register` | `register` | [TC-007](007-email-register.md)、[TC-008](008-phone-register.md)、[TC-009](009-register-missing-nickname.md)、[TC-010](010-register-blank-nickname.md) |
| `POST /auth/login` | `login` | [TC-019](019-login-email.md)、[TC-020](020-login-unregistered.md) |
| `POST /auth/refresh` | `refresh` | [TC-021](021-refresh-token.md)、[TC-022](022-invalid-refresh-token.md)、[TC-023](023-logout-revokes-refresh.md)、[TC-024](024-logout-other-user.md) |
| `POST /auth/logout` | `logout` | [TC-023](023-logout-revokes-refresh.md)、[TC-024](024-logout-other-user.md) |
| `GET /me/rating-summary` | `getMyRatingSummary` | [TC-106](106-rating-unaccepted.md)、[TC-115](115-rating-empty-summary.md)、[TC-116](116-rating-summary-counts.md)、[TC-119](119-rating-concurrent.md) |
| `POST /topics/{topicId}/ratings` | `rateTopicParticipant` | [TC-101](101-rating-good.md)、[TC-102](102-rating-neutral.md)、[TC-103](103-rating-bad.md)、[TC-104](104-rating-minimal.md) |
| `GET /rating-categories` | `listRatingCategories` | [TC-101](101-rating-good.md)、[TC-102](102-rating-neutral.md)、[TC-103](103-rating-bad.md)、[TC-105](105-rating-self.md) |
| `GET /users/{userId}/ratings` | `listUserRatings` | [TC-101](101-rating-good.md)、[TC-106](106-rating-unaccepted.md)、[TC-108](108-rating-duplicate.md)、[TC-109](109-rating-mutual.md) |
| `GET /rooms` | `listRooms` | [TC-036](036-topic-missing-title.md)、[TC-037](037-topic-blank-title.md)、[TC-038](038-topic-missing-detail.md)、[TC-039](039-topic-blank-detail.md) |
| `GET /rooms/{roomId}` | `getRoom` | [TC-067](067-invitation-accept.md)、[TC-069](069-invitation-accept-retry.md)、[TC-073](073-invitation-accept-closed.md)、[TC-078](078-invitation-concurrent-accept.md) |
| `GET /rooms/{roomId}/messages` | `listMessages` | [TC-080](080-chat-send.md)、[TC-081](081-chat-message-retry.md)、[TC-082](082-chat-two-senders.md)、[TC-083](083-chat-empty-text.md) |
| `POST /rooms/{roomId}/messages` | `sendMessage` | [TC-080](080-chat-send.md)、[TC-081](081-chat-message-retry.md)、[TC-082](082-chat-two-senders.md)、[TC-083](083-chat-empty-text.md) |
| `POST /rooms/{roomId}/read` | `markRoomRead` | [TC-091](091-chat-outsider-read.md)、[TC-093](093-chat-read-position.md)、[TC-097](097-chat-wrong-room-read.md)、[TC-142](142-e2e.md) |
| `POST /rooms/{roomId}/messages/{messageId}/recall` | `recallMessage` | [TC-092](092-chat-outsider-recall.md)、[TC-094](094-chat-recall-self.md)、[TC-095](095-chat-recall-idempotent.md)、[TC-096](096-chat-wrong-room-recall.md) |
| `GET /topics/receipts` | `listTopicReceipts` | [TC-059](059-invite-create.md)、[TC-071](071-invitation-accepted-reject.md)、[TC-076](076-invitations-isolation.md)、[TC-077](077-invitations-pagination.md) |
| `POST /topics/{topicId}/accept` | `acceptTopic` | [TC-067](067-invitation-accept.md)、[TC-069](069-invitation-accept-retry.md)、[TC-070](070-invitation-reject-retry.md)、[TC-078](078-invitation-concurrent-accept.md) |
| `POST /topics/{topicId}/read` | `readTopic` | [TC-068](068-invitation-reject.md)、[TC-071](071-invitation-accepted-reject.md) |
| `GET /topics` | `listTopics` | [TC-035](035-topic-create.md)、[TC-036](036-topic-missing-title.md)、[TC-037](037-topic-blank-title.md)、[TC-038](038-topic-missing-detail.md) |
| `POST /topics` | `createTopic` | [TC-035](035-topic-create.md)、[TC-036](036-topic-missing-title.md)、[TC-037](037-topic-blank-title.md)、[TC-038](038-topic-missing-detail.md) |
| `GET /topics/{topicId}` | `getTopic` | [TC-035](035-topic-create.md)、[TC-049](049-topic-cross-community.md)、[TC-050](050-topic-update-owner.md)、[TC-051](051-topic-forbidden-update.md) |
| `PATCH /topics/{topicId}` | `updateTopic` | [TC-050](050-topic-update-owner.md)、[TC-051](051-topic-forbidden-update.md) |
| `POST /topics/{topicId}/close` | `closeTopic` | [TC-052](052-topic-forbidden-close.md)、[TC-053](053-topic-close.md)、[TC-054](054-topic-default-open-feed.md)、[TC-066](066-invite-closed-topic.md) |
| `DELETE /me` | `deleteCurrentUser` | 所有创建用户的用例的“测试后清理”，如 [TC-007](007-email-register.md)、[TC-017](017-duplicate-nickname.md)、[TC-142](142-e2e.md) |
| `GET /me` | `getCurrentUser` | [TC-007](007-email-register.md)、[TC-008](008-phone-register.md)、[TC-009](009-register-missing-nickname.md)、[TC-010](010-register-blank-nickname.md) |
| `PATCH /me` | `updateCurrentUser` | [TC-025](025-profile-update.md)、[TC-026](026-profile-partial.md)、[TC-027](027-profile-invalid-gender.md)、[TC-028](028-profile-future-birth.md) |
| `PATCH /me/community` | `changeCommunity` | [TC-031](031-community-cooldown.md)、[TC-032](032-community-after-seven-days.md)、[TC-056](056-topic-community-snapshot.md)、[TC-075](075-invitation-recheck-community.md) |
| `GET /communities` | `listCommunities` | [TC-001](001-empty-communities.md)、[TC-007](007-email-register.md)、[TC-008](008-phone-register.md)、[TC-009](009-register-missing-nickname.md) |
