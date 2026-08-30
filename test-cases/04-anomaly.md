# 04 — Anomaly / Transaction Risk

Automated coverage: ✅ `AnomalyServiceTest` (3)

| # | Scenario | Steps | Expected | Actual |
| -- | -------- | ----- | -------- | ------ |
| 4.1 | No history | `POST /api/fraud/transaction-risk` on empty user | explanation "not enough history" (not error), LOW | ✅ PASS — LOW 13, reason "Not enough history" |
| 4.2 | Extreme amount w/ history | load sample data then risk ₹85k new merchant | HIGH / anomaly level + reasons | ✅ PASS — HIGH 73, 3 reasons |
| 4.3 | Normal amount | risk a small normal amount | LOW, no anomaly | ✅ PASS |
| 4.4 | Response explains why | check `reasons` array | non-empty, human-readable | ✅ PASS |
| 4.5 | Validation | missing required field | 400 | ✅ PASS |
