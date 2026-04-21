# CUSTOMIZATION — Rules 3단 오버라이드 가이드

Plugin 의 기본 규칙이 조직/프로젝트/개인 환경과 맞지 않을 때, **파일을 직접 수정하지 말고 오버라이드** 하세요. Plugin 업데이트 시 기본 규칙은 덮어써지지만 오버라이드는 보존됩니다.

---

## 1. 우선순위

```
1. ~/.claude/rules/*.md                    (사용자 — 최우선, 개인 설정)
2. <project>/.claude/rules/*.md            (프로젝트 — 중간, 팀 합의)
3. <plugin>/rules/*.md                     (Plugin 기본 — 최하, 조직 공통)
```

Agent 는 세 위치를 모두 로드하며 **뒤쪽이 앞쪽을 보완** 합니다 (덮지 않고 추가 / 재정의).

---

## 2. 파일 구조

세 위치 모두 동일한 파일명 사용:

```
rules/
├── shared-standards.md   (공통 표준)
├── refactor-rules.md     (Refactor Agent 전용)
└── quality-rules.md      (Quality Agent 전용)
```

---

## 3. 사용자 오버라이드 (`~/.claude/rules/`)

개인 선호 규칙을 모든 프로젝트에 일관되게 적용할 때 사용.

```bash
mkdir -p ~/.claude/rules

# 예: 메서드 최대 줄 수를 40줄로 더 엄격하게
cat > ~/.claude/rules/refactor-rules.md <<'EOF'
# 개인 오버라이드

## 메서드 크기
- **최대 40줄** (기본 50줄보다 엄격하게)
EOF
```

Agent 가 로드 시 기본 규칙을 읽은 뒤 이 파일의 "최대 40줄" 이 최종 우선 적용됩니다.

---

## 4. 프로젝트 오버라이드 (`<project>/.claude/rules/`)

팀 합의 기반 프로젝트별 규칙.

```bash
# 프로젝트 루트에서
mkdir -p .claude/rules

# 예: SQL ${} 를 차단 대신 경고로 완화 (레거시 프로젝트)
cat > .claude/rules/quality-rules.md <<'EOF'
# 프로젝트 오버라이드 (레거시 마이그레이션 중)

## SQL ${} 정책
- 심각도: **Warning** (기본 Critical 차단을 완화)
- 사유: 레거시 매퍼 점진적 개선 중, 차단 시 개발 정체
- 해제 조건: 전체 `${}` 제거 완료 후 이 오버라이드 삭제
EOF

git add .claude/rules/quality-rules.md
git commit -m "chore: 레거시 호환 SQL 규칙 완화"
```

프로젝트 팀 전체에 적용되며 git 으로 공유됩니다.

---

## 5. Plugin 기본 (`<plugin>/rules/`)

**수정 금지** — 업데이트 시 덮어써집니다. 조직 전체 표준 변경이 필요하면 Plugin 저장소에 PR 을 내세요.

---

## 6. 자주 쓰는 오버라이드 예시

### 6.1 메서드 줄 수 조정

```markdown
<!-- ~/.claude/rules/refactor-rules.md -->
# 개인 오버라이드

## 메서드 크기
- **최대 80줄** (느슨하게)
- 테스트 메서드는 100줄까지 허용
```

### 6.2 Lombok 허용 범위 확장

```markdown
<!-- <project>/.claude/rules/shared-standards.md -->
# 프로젝트 오버라이드

## Lombok 허용 어노테이션 (팀 합의)
기본 허용 외 추가:
- `@Builder` — 필수
- `@NoArgsConstructor` / `@AllArgsConstructor` — DTO 에서 허용
- `@RequiredArgsConstructor` — Service/Controller 의존성 주입 표준

금지 유지:
- `@Data`
- `@EqualsAndHashCode` (Entity)
```

### 6.3 민감정보 키워드 추가

```markdown
<!-- <project>/.claude/rules/quality-rules.md -->
# 프로젝트 오버라이드

## 민감정보 키워드 (추가)
기본 목록 외:
- `사업자등록번호`, `businessNo`
- `accountNumber`, `계좌번호`
- `cardNumber`, `카드번호`
```

### 6.4 레거시 JPA → MyBatis 레이어 패턴

```markdown
<!-- <project>/.claude/rules/shared-standards.md -->
# 프로젝트 오버라이드

## 레이어 패턴 (프로젝트 고유)
Controller → Facade → Service → Dao
- Facade: 여러 Service 조합 + 외부 API 호출
- 트랜잭션 경계는 Facade 가 아닌 Service 유지
```

---

## 7. 검증

오버라이드 반영 확인:

```bash
# Agent 호출 시 규칙 로드 순서 출력 요청
/agent code-refactoring-agent "현재 로드된 refactor-rules 의 메서드 최대 줄 수만 알려줘"
```

Agent 가 오버라이드 값(예: 40) 을 출력하면 정상.

---

## 8. 주의사항

- 오버라이드 파일은 **전체 규칙을 재작성할 필요 없음** — 변경할 항목만 덮어쓰면 됨
- 동일 항목이 3단계에 모두 있으면 **사용자 > 프로젝트 > Plugin** 순으로 최종 값 결정
- 오버라이드에 syntax error 가 있으면 Agent 가 로드 실패로 기본만 적용 (Low 보고)
- `.claude/` 폴더는 git 에 커밋해도 무방 (민감 정보 없음 기준)
- `.claude/settings.local.json` 등 개인 설정은 `.gitignore` 처리 유지

---

## 9. 원복

오버라이드 제거하면 Plugin 기본으로 돌아감:

```bash
# 개인 오버라이드 제거
rm -rf ~/.claude/rules

# 프로젝트 오버라이드 제거
rm -rf <project>/.claude/rules
git add -A && git commit -m "chore: 프로젝트 규칙 오버라이드 제거"
```
