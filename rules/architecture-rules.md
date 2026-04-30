# 아키텍처 규칙 인덱스 (architecture-rules)

> v0.6.1 에서 파일당 200줄 이하 분리. Agent 는 아래 하위 파일들을 로드합니다.
> 사용자/프로젝트 오버라이드는 이 파일명(`architecture-rules.md`)으로 그대로 사용 가능.

---

## 하위 규칙 파일

| 파일 | 내용 |
|---|---|
| `rules/architecture/layer-ddd.md` | §0~7: 심각도·레이어 의존·순환 의존·DDD·패키지 명명·Hexagonal·BLOCK·Baseline |
| `rules/architecture/spring-rules.md` | §9~11: @Transactional 위치·Spring Security 설정·보고서 형식 |
