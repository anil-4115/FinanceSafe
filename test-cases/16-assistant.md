# 16 — Assistant (chat)

Automated coverage: ✅ `FeatureCoverageIntegrationTest` (assistant_chatAndHistory, assistant_investUsesProfileDataAndReturnsAllocations, assistant_marketReturnsLiveSnapshot)

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | ----- | -------- |
| 16.1 | Send message | `POST /api/assistant/chat` | 200 + reply | |
| 16.2 | Chat history | `GET /api/assistant/history` | 200 list | |
| 16.3 | History after chat | chat then GET history | message included | |
| 16.4 | Auth required | no token | 401 | |
| 16.5 | Health (real data) | ask "How is my financial health?" | score + components/recommendation from live account | |
| 16.6 | Spending (real data) | ask "Where am I overspending?" | this-month total, savings rate, top categories, avg comparison | |
| 16.7 | Budget vs actual (real data) | ask "How is my budget?" | per-budget spent / limit + % + over/under status | |
| 16.8 | Risk (real data) | ask "Any risk on my account?" | flagged count + top flagged + unresolved alerts | |
| 16.9 | Goals (real data) | ask "Are my goals on track?" | per-goal current/target + progress % | |
| 16.10 | Invest (personalized) | ask "Should I invest 50000?" | allocation %s from user profile (risk, savings, horizon) | |
| 16.11 | Learn (real data) | ask "What should I learn?" | live literacy score + attempts | |
| 16.12 | Market snapshot | ask "market trend for TCS?" | TCS symbol + trend/risk/move | |
| 16.13 | Scam scan | paste suspicious SMS | verdict + confidence + why | |
