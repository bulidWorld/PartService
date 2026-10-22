# 生成进度（2026-10-22）

当前编号源用例140个，已生成XML 90 个，剩余 50 个。

已生成文件通过 validate.py --partial 及 validate_importer.py 两层离线校验（0错误），未执行业务HTTP。每个用例分配独立agent；TC-092 agent在额度耗尽前已写出XML，主agent已补做校验。已生成文件均有对应notes。

本次继续执行在agent返回账号usage limit后中断；未将缺失XML或未完成目标标记为完成。下次从以下清单继续，每个用例仍分配独立agent。

## 待生成

- 093-chat-read-position.md
- 094-chat-recall-self.md
- 095-chat-recall-idempotent.md
- 096-chat-wrong-room-recall.md
- 097-chat-wrong-room-read.md
- 098-chat-closed-topic.md
- 099-chat-pagination.md
- 100-chat-untrusted-image.md
- 101-rating-good.md
- 102-rating-neutral.md
- 103-rating-bad.md
- 104-rating-minimal.md
- 105-rating-self.md
- 106-rating-unaccepted.md
- 107-rating-cross-topic.md
- 108-rating-duplicate.md
- 109-rating-mutual.md
- 110-rating-invalid-score.md
- 111-rating-invalid-category.md
- 112-rating-comment-too-long.md
- 113-rating-comment-boundary.md
- 114-rating-disabled-category.md
- 115-rating-empty-summary.md
- 116-rating-summary-counts.md
- 117-rating-pagination.md
- 118-rating-public-privacy.md
- 119-rating-concurrent.md
- 120-rating-anonymous.md
- 121-rating-appeal.md
- 122-risk-insufficient-samples.md
- 123-risk-threshold.md
- 124-risk-recompute.md
- 125-risk-window.md
- 126-risk-recovery.md
- 127-risk-algorithm-version.md
- 128-risk-tag-removal.md
- 129-risk-public-visibility.md
- 130-auth-missing.md
- 131-auth-forged.md
- 132-auth-expired.md
- 133-pagination-limit-0.md
- 134-pagination-limit-101.md
- 135-pagination-invalid-cursor.md
- 136-request-invalid-json.md
- 137-request-invalid-path-id.md
- 138-request-id.md
- 139-audit-log-redaction.md
- 140-persistence-restart.md
- 141-transaction-accept-rollback.md
- 142-e2e.md
