# 14 — Products & Compare

Automated coverage: ✅ `FeatureCoverageIntegrationTest` (products_listFilterDetailAndCompare)

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 14.1 | List products | `GET /api/products` | 200 list (non-empty, seeded) | |
| 14.2 | Filter by category | `GET /api/products?category=X` | 200 filtered | |
| 14.3 | Product detail | `GET /api/products/{id}` | 200 | |
| 14.4 | Compare products | `GET /api/products/compare?ids=1,2` | 200 comparison | |
| 14.5 | Invalid compare ids | bad/empty ids | 400 | |
| 14.6 | Auth required | no token | 401 | |
