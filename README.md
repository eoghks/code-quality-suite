# code-quality-suite

Claude Code 용 **자바 코드 리팩토링 + 아키텍처 + 보안 + 품질 + 테스트 생성 + DB 마이그레이션 검증 전문 Agent** 번들 Plugin (v0.6.0).

6개 Agent · 9개 Command · pipeline-state.json Agent 협업 · PMD/Checkstyle/OWASP-DC · Secret Scan · Multi-module · Prompt Injection 방어

---

## Agent 구성

| Agent | 역할 |
|---|---|
| `code-refactoring-agent` | 구조 개선 · CC/Cognitive · Immutability · Guard Clause · 브랜치/커밋 자동화 |
| `architecture-review-agent` | 레이어 의존 · 순환 의존 · DDD · @Transactional · Spring Security |
| `security-audit-agent` | OWASP Top 10 · Secret Scan (trufflehog/ggshield) · PMD/OWASP-DC |
| `code-quality-agent` | 규칙 준수 · SpotBugs/JaCoCo/PMD/Checkstyle · 성능 안티패턴 · Chunk 분할 |
| `test-generation-agent` | JUnit 5 + Mockito 스켈레톤 자동 생성 · mvn/gradlew test |
| `db-migration-agent` | Flyway/Liquibase · DROP/NOT NULL/버전 불연속 감지 |

## Command 구성

| Command | 역할 |
|---|---|
| `/run-pipeline` | 3~5-stage 파이프라인 (`--full` / `--with-tests` / `--strict` / `--chunk-size`) |
| `/architecture-review` | architecture-review-agent 단독 호출 |
| `/security-scan` | security-audit-agent 단독 호출 |
| `/generate-tests` | test-generation-agent 단독 호출 |
| `/db-check` | db-migration-agent 단독 호출 |
| `/baseline` | baseline 생성·업데이트·만료 감사·연장 |
| `/init-project` | 대화형 초기화 마법사 (4개 프리셋) |
| `/suppress-audit` | @suppress 사용 현황 감사 |
| `/agent-explain` | 위반 코드 배경·위험·해결책 설명 |

---

## 빠른 설치

```bash
/plugin marketplace add https://github.com/eoghks/code-quality-suite.git
/plugin install code-quality-suite
/plugin list   # active 상태 확인
```

## 빠른 시작

```bash
# 최초 도입 — 대화형 설정
/init-project

# 전체 파이프라인 실행
/run-pipeline

# 위반 코드 의미 확인
/agent-explain SQL-INJ
```

---

## 문서

| 문서 | 내용 |
|---|---|
| [docs/README.md](docs/README.md) | **전체 가이드** — 사용법·보고서 샘플·스택 전체 |
| [docs/INSTALL.md](docs/INSTALL.md) | 설치·업데이트·제거·SpotBugs/JaCoCo/PMD 설정 |
| [docs/CONFIG.md](docs/CONFIG.md) | `.claude/quality-config.yml` YAML 임계값 설정 |
| [docs/CUSTOMIZATION.md](docs/CUSTOMIZATION.md) | Rules 3단 오버라이드 가이드 |
| [docs/BASELINE.md](docs/BASELINE.md) | Baseline 생성·운영·만료 정책 |
| [docs/SCENARIOS.md](docs/SCENARIOS.md) | 시나리오별 위반 목록 + 검증 방법 |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | 버전별 변경 이력 |
| [docs/ROADMAP.md](docs/ROADMAP.md) | 버전별 계획 |
| [docs/BUILD-PLAN.md](docs/BUILD-PLAN.md) | 구축 Q&A 결정 이력 |
