# 품질 검증 규칙 인덱스 (quality-rules)

> v0.6.1 에서 파일당 200줄 이하 분리. Agent 는 아래 하위 파일들을 로드합니다.
> 사용자/프로젝트 오버라이드는 이 파일명(`quality-rules.md`)으로 그대로 사용 가능.

---

## 하위 규칙 파일

| 파일 | 내용 |
|---|---|
| `rules/quality/compliance-testing.md` | §1~3: 보안 smoke check·규칙 준수 검증·테스트 커버리지 |
| `rules/quality/performance.md` | §4~4A: 성능 안티패턴·SpotBugs/JaCoCo 리포트 파싱 |
| `rules/quality/reporting.md` | §5~6: 보고서 형식·BLOCK 마커·Agent 동작 범위 |
