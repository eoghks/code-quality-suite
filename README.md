# code-quality-suite

Claude Code 용 **코드 리팩토링 + 품질 검증 전문 Agent** 를 번들링한 Plugin 입니다.

- `code-refactoring-agent` — 자바 코드 구조 개선·테스트 동시 갱신·브랜치/커밋 생성
- `code-quality-agent` — 보안·테스트 커버리지·규칙 준수 검증 (읽기 전용)
- `git commit` 직전 Hook 으로 파이프라인 자동 제안 + 수동 호출 지원

## 빠른 설치

```bash
/plugin marketplace add https://github.com/eoghks/code-quality-suite.git
/plugin install code-quality-suite
```

## 상세 문서

- [docs/README.md](docs/README.md) — 전체 가이드
- [docs/INSTALL.md](docs/INSTALL.md) — 설치·업데이트 절차
- [docs/CUSTOMIZATION.md](docs/CUSTOMIZATION.md) — Rules 오버라이드 가이드
- [docs/BUILD-PLAN.md](docs/BUILD-PLAN.md) — 구축 Q&A 결정 이력
- [docs/CHANGELOG.md](docs/CHANGELOG.md) — 버전별 변경 이력
