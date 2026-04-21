# INSTALL — 설치·업데이트·제거 절차

## 사전 요구사항

- **Claude Code** 1.0.0 이상 (데스크톱 앱 / CLI / IDE 확장 중 하나)
- **Git** 설치 및 `git` 명령 PATH 에 등록
- **Maven** 또는 **Gradle** (테스트 자동 실행을 위해 프로젝트에 `pom.xml` / `build.gradle` 존재)

## 1. 마켓플레이스 등록 (최초 1회)

```bash
/plugin marketplace add https://github.com/eoghks/code-quality-suite.git
```

정상 등록 확인:

```bash
/plugin marketplace list
```

출력에 `code-quality-suite` 가 표시되어야 합니다.

## 2. Plugin 설치

```bash
/plugin install code-quality-suite
```

설치 경로 확인:

```bash
/plugin list
```

활성화 상태(`active`) 와 Agent 2개, Hook 1개, Command 1개가 보이면 정상.

## 3. 설치 검증

### 3.1 Agent 호출 테스트

```bash
/agent code-refactoring-agent
```

Agent 가 안내 메시지와 함께 규칙 파일(shared-standards, refactor-rules) 을 로드한 상태로 대기하면 정상.

### 3.2 슬래시 커맨드 테스트

```bash
/run-pipeline
```

"인자 없이 호출 시 현재 브랜치 전체 대상" 안내가 출력되면 정상.

### 3.3 Hook 테스트

더미 자바 파일 커밋 시도:

```bash
echo "public class Test {}" > Test.java
git add Test.java
# Claude 에게: "Test.java 를 커밋해줘"
```

Claude 가 `git commit` 실행 직전 "Refactor → Quality 파이프라인 권장" 힌트를 출력하면 Hook 정상 작동.

## 4. 업데이트

```bash
/plugin update code-quality-suite
```

- Plugin 번들 Rules (`<plugin>/rules/*.md`) 는 덮어씁니다
- **사용자 오버라이드** (`~/.claude/rules/`) 와 **프로젝트 오버라이드** (`<project>/.claude/rules/`) 는 보존됩니다

## 5. 제거

```bash
/plugin uninstall code-quality-suite
```

마켓플레이스 등록 자체 제거:

```bash
/plugin marketplace remove https://github.com/eoghks/code-quality-suite.git
```

## 6. 문제 해결

### Agent 가 `claude-code` 에 표시되지 않음

- `.claude-plugin/plugin.json` 의 `agents` 경로가 올바른지 확인
- `/plugin list` 에 `active: true` 인지 확인
- 설치 후 Claude Code 재시작

### Hook 이 발화하지 않음

- `hooks/hooks.json` 의 matcher 가 `Bash(git commit:*)` 정확히 일치하는지 확인
- `hooks/pre-commit-pipeline.sh` 실행 권한(`chmod +x`) 확인
- `${CLAUDE_PLUGIN_ROOT}` 환경변수가 올바르게 치환되는지 로그 확인

### 규칙 파일이 로드되지 않음

- Agent 프롬프트의 `` !`cat ...` `` 쉘 치환이 실행되는지 확인
- Plugin 설치 경로 (`<plugin>/rules/`) 에 `.md` 파일 존재 확인
- 3단 오버라이드 경로 권한 확인

### 테스트 자동 실행이 안 됨

- 프로젝트 루트에 `pom.xml` 또는 `build.gradle` 존재 확인
- Windows 에서는 `gradlew.bat test` 사용 (Agent 가 자동 감지)
- 테스트 실행 권한이 Agent `tools` frontmatter 에 허용돼 있는지 확인

## 7. 환경별 주의사항

### Windows

- 경로 구분자 `\` 는 Git Bash 환경에서 자동 변환됨
- Hook 스크립트는 Git Bash / WSL 에서 실행 (`bash` 필수)
- Gradle 은 `gradlew.bat`, Maven 은 `mvn` (Bat 파일 PATH 등록 필요)

### macOS / Linux

- `chmod +x hooks/pre-commit-pipeline.sh` 가 이미 커밋에 포함되어 있음
- 별도 설정 없이 동작

## 8. 회사 GitLab + 개인 GitHub 공존

이 Plugin 은 개인 GitHub(`eoghks/code-quality-suite`)에 배포됩니다. 회사 GitLab 프로젝트에서 사용하려면:

1. Plugin 자체 설치는 위 절차로 한 번
2. 각 프로젝트 저장소는 GitLab 그대로 사용
3. Git remote 는 프로젝트별 고유 설정 (Plugin 과 무관)

사용자 이메일/이름이 저장소별로 다른 경우:

```bash
# 해당 저장소 로컬 설정
cd <프로젝트>
git config --local user.name "<회사 ID>"
git config --local user.email "<회사 이메일>"
```

전역 `~/.gitconfig` 는 그대로 두고 로컬만 덮으면 해당 저장소에서만 적용됩니다.
