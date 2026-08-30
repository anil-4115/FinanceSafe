# 05 — Health Score

Automated coverage: ✅ `HealthScoreServiceTest` (5)

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 5.1 | Default (no data) | `GET /api/health-score` new user | 200 + numeric score, 8 components | ✅ PASS — score 43, 8 components |
| 5.2 | With rich profile/data | load sample data, get score | higher score, "Good"/"Excellent" label | ✅ PASS — score 84 (up from 43) |
| 5.3 | Recommendations present | inspect `recommendations` | non-empty | ✅ PASS — 8 recommendations |
| 5.4 | Strengths/weaknesses | inspect arrays | present | ✅ PASS |
| 5.5 | Requires auth | no token | 401 | ✅ PASS |
