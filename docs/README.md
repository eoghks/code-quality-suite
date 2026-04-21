# code-quality-suite — 전체 가이드

Claude Code 용 **자바 코드 리팩토링 + 품질 검증 전문 Agent** 번들 Plugin 입니다. Refactor Agent 가 구조를 개선하고 테스트를 동시 갱신하며, Quality Agent 가 보안·규칙·성능을 검증합니다. `git commit` 직전에는 Hook 이 자동으로 파이프라인 실행을 권고합니다.

---

## 구성 요소

| 종류 | 이름 | 역할 |
|---|---|---|
| Agent | `code-refactoring-agent` | 구조 개선 · 설계 패턴 적용 · 테스트 동시 갱신 · 브랜치/커밋 자동화 |
| Agent | `code-quality-agent` | 보안 스캔 · 규칙 준수 · 테스트 커버리지 · 성능 안티패턴 (읽기 전용) |
| Hook | `PreToolUse: Bash(git commit:*)` | 파이프라인 권고 힌트 주입 + BLOCK 마커 감지 시 exit 2 |
| Command | `/run-pipeline` | Refactor → Quality 수동 실행 |
| Rules | `shared-standards.md` · `refactor-rules.md` · `quality-rules.md` | 3단 오버라이드 (사용자 > 프로젝트 > Plugin) |

---

## 빠른 설치

```bash
/plugin marketplace add https://github.com/eoghks/code-quality-suite.git
/plugin install code-quality-suite
/plugin list   # 활성 확인
```

자세한 절차는 [INSTALL.md](INSTALL.md) 참고.

---

## 기본 사용법

### 1. 수동 호출 — 리팩토링 + 검증

```bash
# 파이프라인 전체
/run-pipeline

# 특정 파일 지정
/run-pipeline src/main/java/com/example/UserService.java

# 특정 브랜치 대상
/run-pipeline feature/user-login
```

### 2. 개별 Agent 호출

```bash
# 리팩토링만 (구조 개선 + 테스트 갱신 + 아토믹 커밋)
/agent code-refactoring-agent UserService.java

# 품질 검증만 (읽기 전용 보고서)
/agent code-quality-agent
```

### 3. 커밋 직전 자동 파이프라인 (Hook)

사용자가 Claude 에게 "커밋해줘" 라고 요청하면:

```
사용자: UserService 변경사항 커밋해줘

→ Claude 가 git commit 실행 시도
→ Hook 발화 (Bash(git commit:*) 매치)
→ 스테이징 파일 중 .java/.xml/.jsp 등 감지
→ "Refactor → Quality 파이프라인 권장" 컨텍스트 주입
→ Claude 가 /agent 순차 호출 후 정상 커밋 진행
→ Quality 보고서에 [BLOCK: COMMIT STOP] 있으면 커밋 자동 중단
```

---

## Quality 보고서 샘플

```
## Quality Report — feature/user-login

### Critical (차단 권고)
- [SQL-INJ] UserMapper.xml:45 — ${name} 직접 치환 검출

### High
- [LAYER] UserController.java:32 — Repository 직접 주입, Service 이관 권고
- [N+1] OrderService.java:78 — @OneToMany lazy + 루프 접근 감지

### Medium
- [METHOD-LEN] UserService.java:102 — 73줄 (50줄 초과)
- [LOGGER] UserService.java:55 — log.debug("id=" + id) → 파라미터 방식 권고

### Low
- [INDEX-HINT] OrderMapper.findByStatus → WHERE status 인덱스 검토

### 테스트
- 통과 42건 / 실패 0건

### 요약
- Critical: 1건 · High: 2건 · Medium: 2건 · Low: 1건

[BLOCK: COMMIT STOP]
```

---

## Rules 커스터마이징

기본 규칙이 조직/개인 환경에 맞지 않으면 **3단 오버라이드**로 보완:

```bash
# 개인 전역 규칙 (최우선)
mkdir -p ~/.claude/rules
vi ~/.claude/rules/refactor-rules.md   # 메서드 최대 줄 수 조정 등

# 프로젝트별 규칙 (중간)
mkdir -p <project>/.claude/rules
vi <project>/.claude/rules/quality-rules.md
```

자세한 가이드는 [CUSTOMIZATION.md](CUSTOMIZATION.md) 참고.

---

## 조직 규칙 자동 준수

- ❌ main / master 직접 커밋 → Refactor Agent 가 **자동 차단** 후 `feature/`·`bugfix/`·`refactor/` 브랜치 생성·이동
- ❌ `git push --force`, `git reset --hard`, `--no-verify` → Agent 권한 설정으로 차단
- ✅ 브랜치명 자동 감지 (작업 유형별 prefix)
- ✅ 아토믹 커밋 (리팩토링 단위 분리)
- ✅ 한국어 커밋 메시지 + 영어 타입 prefix (`feat`, `fix`, `refactor`, ...)

---

## 지원 스택

- **Java** 17+
- **Jakarta EE** (`jakarta.*`, `javax.*` 금지)
- **Spring Boot**
- **MyBatis** (Controller → Service → Dao) / **JPA** (Controller → Service → Repository) 자동 감지
- **Lombok** 핵심 어노테이션 (`@Getter` / `@Setter` / `@Slf4j`, `@Data`·`@EqualsAndHashCode` 금지)
- **빌드 도구** Maven (`mvn test`) / Gradle (`./gradlew test`) 자동 감지

---

## 문서

- [INSTALL.md](INSTALL.md) — 설치·업데이트·제거 절차
- [CUSTOMIZATION.md](CUSTOMIZATION.md) — Rules 3단 오버라이드 가이드
- [BUILD-PLAN.md](BUILD-PLAN.md) — 구축 Q&A 결정 이력 (v0.1.0)
- [CHANGELOG.md](CHANGELOG.md) — 버전별 변경 이력

---

## 라이선스

MIT License — 상업/개인 사용, 수정, 배포 자유.

## 저자

daehwan · [github.com/eoghks](https://github.com/eoghks)
