# code-quality-suite

Claude Code 용 **자바 코드 리팩토링 + 보안 + 품질 검증 전문 Agent** 번들 Plugin 입니다 (v0.2.0).

- `code-refactoring-agent` — 구조 개선·메트릭·예외/Resource 안전·테스트 동시 갱신·브랜치/커밋 자동화
- `security-audit-agent` — OWASP Top 10 보안 스캔·하드코딩 Secret·취약 의존성 (읽기 전용)
- `code-quality-agent` — 규칙 준수·SpotBugs/JaCoCo 통합·테스트 커버리지·성능 (읽기 전용)
- `git commit` 직전 Hook 이 3-stage 파이프라인 자동 권고 + Baseline 시스템

## 빠른 설치

> **참고:** Claude Code 에는 중앙 마켓플레이스가 없습니다. 각 사용자가 저장소 URL 을 직접 등록해야 합니다.

```bash
/plugin marketplace add https://github.com/eoghks/code-quality-suite.git
/plugin install code-quality-suite
/plugin list   # active 상태 확인
```

## 상세 문서

- [docs/README.md](docs/README.md) — 전체 가이드 (사용법·보고서 샘플·스택)
- [docs/INSTALL.md](docs/INSTALL.md) — 설치·업데이트·SpotBugs/JaCoCo 설정
- [docs/BASELINE.md](docs/BASELINE.md) — Baseline 생성·업데이트·팀 운영
- [docs/CUSTOMIZATION.md](docs/CUSTOMIZATION.md) — Rules 3단 오버라이드 가이드
- [docs/BUILD-PLAN.md](docs/BUILD-PLAN.md) — 구축 Q&A 결정 이력 (v0.1.0~v0.2.0)
- [docs/CHANGELOG.md](docs/CHANGELOG.md) — 버전별 변경 이력
