# 리팩토링 규칙 인덱스 (refactor-rules)

> v0.6.1 에서 파일당 200줄 이하 분리. Agent 는 아래 하위 파일들을 로드합니다.
> 사용자/프로젝트 오버라이드는 이 파일명(`refactor-rules.md`)으로 그대로 사용 가능 (하위 파일 교체 불필요).

---

## 하위 규칙 파일

| 파일 | 내용 |
|---|---|
| `rules/refactor/basics.md` | §1~7: 메서드 크기·null·Map·매직넘버·설계 패턴·DRY·네이밍 |
| `rules/refactor/metrics-git.md` | §8~10: 테스트 병행·브랜치/커밋·CC/Cognitive/파라미터 메트릭 |
| `rules/refactor/exceptions-coupling.md` | §11~12: 예외 처리·Resource 안전·응집도·결합도 |
| `rules/refactor/immutability-guard.md` | §13~14: Immutability·final·record·Guard Clause |
