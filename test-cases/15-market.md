# 15 — Market

Automated coverage: ✅ `FeatureCoverageIntegrationTest` (market_searchAndDetail)

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | ----- | -------- |
| 15.1 | Market search | `GET /api/market/search?q=` | 200 (may be empty) | |
| 15.2 | Market detail | `GET /api/market/{symbol}` | 200 or graceful empty | |
| 15.3 | Auth required | no token | 401 | |
