# 16 — Assistant (chat)

Automated coverage: ✅ `FeatureCoverageIntegrationTest` (assistant_chatAndHistory)

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | ----- | -------- |
| 16.1 | Send message | `POST /api/assistant/chat` | 200 + reply | |
| 16.2 | Chat history | `GET /api/assistant/history` | 200 list | |
| 16.3 | History after chat | chat then GET history | message included | |
| 16.4 | Auth required | no token | 401 | |
