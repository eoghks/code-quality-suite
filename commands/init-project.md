---
name: init-project
description: code-quality-suite 플러그인을 프로젝트에 도입하기 위한 대화형 초기화 마법사. quality-config.yml 생성, baseline 초기화, 규칙 오버라이드 디렉터리 스캐폴딩.
---

# /init-project

## 사용법

```
/init-project [옵션]
```

## 옵션

| 옵션 | 설명 |
|---|---|
| (없음) | 대화형 Q&A 로 설정 |
| `--preset <name>` | 프리셋 적용 (없이 즉시 파일 생성) — `startup` \| `enterprise` \| `hexagonal` |
| `--skip-baseline` | 레거시 프로젝트라도 baseline 초기화 건너뜀 |
| `--dry-run` | 파일 생성 없이 생성될 내용만 미리보기 |

## 동작 — 대화형 Q&A

플러그인 호출 시 아래 질문에 답하면 자동 설정:

### Q1. 팀 규모

```
A) 1~5명 (스타트업/개인)      → startup 프리셋
B) 6~30명 (중소 규모)         → mid-team 프리셋
C) 30명 초과 (엔터프라이즈)   → enterprise 프리셋
```

### Q2. 프로젝트 성숙도

```
A) 신규 프로젝트 (첫 커밋~1주) → baseline 없음, 엄격 규칙
B) 초기 개발 (1주~3개월)       → baseline 없음, 표준 규칙
C) 운영 중 (3개월~)            → baseline 초기화 필수
D) 레거시 이관                 → baseline + 완화 규칙 권장
```

### Q3. 기술 스택 확인

```
- Maven / Gradle (자동 감지: pom.xml / build.gradle 존재 여부)
- Spring Boot 버전 (/meta-inf/spring.factories 또는 pom.xml 의존성)
- MyBatis / JPA (resources/mapper/ 또는 @Entity 검색)
- Flyway / Liquibase (db/migration/ 또는 changelog 검색)
```

### Q4. 파이프라인 엄격도

```
A) 엄격 (CC 7, 메서드 30줄, JaCoCo 90%)        → enterprise 프리셋
B) 표준 (CC 10, 메서드 50줄, JaCoCo 80%)       → default 프리셋 (플러그인 기본값)
C) 완화 (CC 15, 메서드 80줄, JaCoCo 60%)       → startup 프리셋
```

---

## 프리셋 정의

### startup

```yaml
refactor:
  method.max-lines: 80
  cc.threshold: 15
  params.max: 5
quality:
  jacoco.threshold: 60
architecture:
  layer.strict: false
```

### mid-team / default

```yaml
refactor:
  method.max-lines: 50
  cc.threshold: 10
  params.max: 3
quality:
  jacoco.threshold: 80
architecture:
  layer.strict: true
```

### enterprise

```yaml
refactor:
  method.max-lines: 30
  cc.threshold: 7
  params.max: 2
  fields.max: 7
  class.max-lines: 250
quality:
  jacoco.threshold: 90
architecture:
  layer.strict: true
  pkg.allowed-extra: ["adapter", "port", "infra"]
```

### hexagonal

```yaml
refactor:
  method.max-lines: 40
  cc.threshold: 8
quality:
  jacoco.threshold: 85
architecture:
  layer.strict: true
  pkg.allowed-extra: ["adapter", "port", "infra", "application", "usecase"]
```

---

## 생성 파일

대화 완료 후 자동 생성:

```
<project>/.claude/quality-config.yml          ← 선택된 프리셋
<project>/.claude/rules/                      ← 빈 디렉터리 (사용자 오버라이드용)
<project>/.claude/rules/README.md             ← 오버라이드 안내
<project>/.quality-baseline.json              ← 운영/레거시 선택 시 /baseline create 자동 실행
<project>/.gitignore                          ← .architecture-report.md 등 로컬 리포트 추가
```

---

## 실행 예시

```
/init-project

? 팀 규모? (A/B/C): B
? 프로젝트 성숙도? (A/B/C/D): C
? 감지된 스택: Maven + Spring Boot 3.2 + MyBatis + Flyway [맞음?]: 네
? 파이프라인 엄격도? (A/B/C): B

✅ .claude/quality-config.yml 생성 (mid-team / default 프리셋)
✅ .claude/rules/ 디렉터리 생성
✅ /baseline create 실행 → .quality-baseline.json 초기화
✅ .gitignore 갱신 (.architecture-report.md, .security-report.md, .migration-report.md)

다음 단계:
  1. git add .claude/ .quality-baseline.json .gitignore
  2. git commit -m "chore: code-quality-suite 초기 설정"
  3. /run-pipeline 으로 첫 파이프라인 실행
```

---

## 비대화 — `--preset` 옵션

```bash
/init-project --preset startup          # 스타트업 프리셋 즉시 적용
/init-project --preset enterprise       # 엔터프라이즈 프리셋
/init-project --preset hexagonal --skip-baseline
```

---

## 참고

- 생성된 파일은 언제든 수동 수정 가능
- 프리셋 변경은 `.claude/quality-config.yml` 직접 편집 또는 `/init-project` 재실행
- 기존 파일 존재 시 덮어쓰기 전 확인 프롬프트
- baseline 상세 운영: `docs/BASELINE.md` · `rules/baseline-policy.md`
- 전체 설정 옵션: `docs/CONFIG.md`
