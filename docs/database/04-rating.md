# 评价与自动评级模块数据库设计

## 1. rating_category

字段：`id uuid PK`、`code varchar(32) UNIQUE`、`name varchar(32)`、`polarity varchar(16)`（positive/neutral/negative）、`is_active boolean`、`sort_order int`、`created_at`。

初始分类建议：守信、准时、沟通顺畅、友善、有帮助（正向）；一般、沟通平淡（中性）；失约、迟到、恶意骚扰、言语攻击、发布不实信息（负向）。分类为配置数据，新增分类无需改表结构。

## 2. rating

字段：`id uuid PK`、`topic_id FK`、`from_user_id FK user`、`to_user_id FK user`、`score varchar(8)`（good/neutral/bad）、`comment varchar(500)`、`created_at`、`updated_at`、`deleted_at`。

唯一 `(topic_id,from_user_id,to_user_id)`；约束发送者与接收者不同。创建评价时必须校验两人均为该主题 participant 且 invitation 已 accepted。评价不可跨主题复用。

## 3. rating_category_item

字段：`rating_id FK`、`category_id FK`，主键 `(rating_id,category_id)`。

## 4. user_rating_stat

字段：`user_id PK/FK`、`good_count int`、`neutral_count int`、`bad_count int`、`total_count int`、`good_rate numeric(5,4)`、`bad_rate numeric(5,4)`、`calculated_at`、`algorithm_version varchar(32)`。统计由异步任务重算，不能作为评价写入事务的唯一事实来源。

## 5. user_risk_tag

字段：`id uuid PK`、`user_id FK`、`tag_code varchar(32)`（malicious_negative_rating 等）、`source varchar(16)`（system/manual）、`confidence numeric(5,4)`、`evidence jsonb`、`status varchar(16)`（active/removed）、`created_at`、`removed_at`。

## 6. 自动评级算法输入与输出

算法按窗口（默认近 90 天）统计：被评价者好评率、评价总量；评价者给出差评的比例；评价者的差评是否显著偏离群体和被评价者的其他反馈。达到最小样本量后才计算，避免少量数据误判。输出写入 `user_risk_tag`，`evidence` 保存统计窗口、样本量、阈值和算法版本；算法只添加风险提示，不自动删除评价或封禁用户。
