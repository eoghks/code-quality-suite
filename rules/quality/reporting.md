# 보고 형식·동작 범위 (quality/reporting)

> §5~6: 보고서 형식·BLOCK 마커·Quality Agent 동작 권한

---

## 5. 보고 형식 (Report Format)

### 5.1 심각도 (Severity)

| 태그 | 의미 | 커밋 차단 |
|---|---|---|
| **Critical** | 보안 취약점·테스트 실패 | ✅ 커밋 중단 권고 |
| **High** | 중요 규칙 위반 (레이어·민감정보·N+1) | ⚠️ 커밋 전 검토 강력 권고 |
| **Medium** | 품질 저하 (50줄 초과·null 반환·로거 방식) | 📝 다음 리팩토링 대상 |
| **Low** | 개선 제안 (인덱스 힌트·Lombok 확인) | 💡 참고 사항 |

### 5.2 출력 템플릿

```
## Quality Report — <대상>

### Critical (차단 권고)
- [SQL-INJ] UserMapper.xml:45 — ${name} 직접 치환 검출
- [TEST-FAIL] UserServiceTest.findById — 통과 0건 / 실패 1건

### High
- [LAYER] UserController.java:32 — 비즈니스 분기 직접 구현 (Service 이관 권고)
- [N+1] OrderService.java:78 — 루프 내 lazy 접근 감지

### Medium
- [METHOD-LEN] UserService.java:102 — 73줄 (50줄 초과)
- [LOGGER] UserService.java:55 — `log.debug("x=" + obj)` → 파라미터 방식 권고

### Low
- [INDEX-HINT] OrderMapper.findByStatus → WHERE status 컬럼 인덱스 검토

### 요약
- Critical: 2건 · High: 2건 · Medium: 2건 · Low: 1건
- 권고: 커밋 중단 (Critical 해결 필요)
```

### 5.3 BLOCK 마커

| 상황 | 마지막 줄 마커 | Hook 동작 |
|---|---|---|
| Critical 1건 이상 | `[BLOCK: COMMIT STOP]` | Hook 이 `exit 2` → git commit 중단 |
| Critical 0건 | `[PASS: COMMIT READY]` | Hook 이 `exit 0` → 커밋 정상 진행 |

High/Medium/Low 는 차단하지 않음 — 보고만 하고 개발자 판단에 위임.

### 5.4 최종 결정은 개발자

자동 차단(Critical)도 개발자가 강제 커밋 가능. Quality Agent 는 신호자, 결정권자는 개발자.

---

## 6. Quality Agent 동작 범위 (권한)

- ✅ `Read`, `Grep`, `Glob` — 소스 스캔
- ✅ `Bash(git diff:*)`, `Bash(git log:*)`, `Bash(git status:*)` — 변경 파일 식별
- ✅ `Bash(mvn test:*)`, `Bash(./gradlew test:*)` — 테스트 실행
- ❌ `Edit`, `Write` — 수정 금지 (Refactor Agent 책임)
- ❌ `Bash(git add:*)`, `Bash(git commit:*)` — 커밋 금지
