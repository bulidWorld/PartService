# XML 测试用例

每个编号 Markdown 用例由一个独立 agent 转换为同名 XML；具体覆盖、阻塞和格式限制记录在 `notes/编号.md`。不转换目录索引 README.md 和空白编写模板 demo.md。

## 规范来源与兼容性

本次开始时 `docs/task.xsd` 在工作区已删除，TC-001、TC-002 的 Markdown 也已删除；不恢复这些用户改动。生成和离线校验使用工作区已有的 `docs/test-case-xml/task.xsd`，未修改该文件。

经逐字节比较，这份 XSD 与本机 `/opt/apps/Deployment/AutoTestService/docs/task.xsd` 一致；表达式和断言兼容性也参考了该源码实现。

这份现存 XSD 与 Git HEAD 中的 `docs/task.xsd` 不同。HEAD 旧规范只有顺序 REST、统一 body 绑定、二参数断言，缺少逐步鉴权、原始负向请求、四参数 HTTP/响应头断言、失败后的 finally 清理、等待、并发、计算、条件阻塞和参数更新等能力。本目录产物面向现存扩展规范；旧导入器不兼容时，应先补齐导入器能力，不能只靠 XML 文件通过 XSD 校验认定实际可导入。已离线调用本机导入器的转换和配置校验；未向真实服务上传任务，未执行业务 HTTP 请求。

## 使用及验证

2026-10-23 批次（TC-093 ~ TC-142）转换期间，工作区被并行会话同步重构：`invitations.yaml` 已删除，邀请/接受/拒绝接口改为 `POST /topics/{topicId}/accept` 回执模型（新增 `GET /topics/receipts`、`POST /topics/{topicId}/read`）。源用例 Markdown 仍描述旧邀请流程；部分 agent 按生成时刻的现行契约适配了新接口（112/119/120/121/142 等），其余仍按源用例旧流程编写。当前全量 `validate.py` 的 156 个错误全部是旧邀请接口「matched 0 operations」，无 XSD、参数引用或任务名问题。接口重构稳定后需统一迁移或恢复邀请接口，见 [SCHEMA-GAPS.md](SCHEMA-GAPS.md)。

先在目标项目导入 `src/main/resources/rest-api/*.yaml` 的接口，base_url 使用独立测试环境（源用例默认 `http://127.0.0.1:10201/api/v1`）。XML 不保存部署地址或真实令牌。用例共享身份时必须串行运行；生成 XML 的 agent 并行工作，不代表业务用例可以并行执行。清理未确认时停止复用当前环境。

离线校验需要 Python 及 lxml、PyYAML、jsonschema：

```bash
python docs/test-case-xml/validate.py
```

校验包括 XSD、Python 断言语法、参数引用/类型/并发写冲突、OpenAPI 路径和请求字段。它不替代真实导入、断言沙箱和 HTTP 执行验证。

若有本机 AutoTestService 源码及其 Python 环境，可进一步离线调用其 XML 转换和任务配置校验：

```bash
python docs/test-case-xml/validate_importer.py --importer-root /path/to/AutoTestService
```

该脚本只在内存构造接口记录，不连接数据库、不发送 HTTP、不保存任务。它没有执行接口 YAML 上传流程。本机接口导入器拒绝 OpenAPI 3.0 的 `allOf`，而本仓库六份 YAML 均使用了 `allOf` 响应定义；尚未导入接口的项目需先解决这一接口资产导入兼容性问题。这与任务 XML 的格式校验是不同的前提。

源用例不可通过公开 HTTP 准备或观察的目标，明确记录为阻塞或部分可验证；不以成功的前提查询代替目标场景通过。格式能力缺口与接口能力缺口分别记在 notes。
