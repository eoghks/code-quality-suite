# 공통 표준 (shared-standards)

> Refactor Agent 와 Quality Agent 가 **공통으로 참조**하는 기본 규칙입니다.
> 3단 오버라이드 우선순위: `~/.claude/rules/` > `<project>/.claude/rules/` > `<plugin>/rules/` (여기).

---

## 1. 대상 언어·프레임워크 스택

- **Java** (LTS: Java 17 이상 권장)
- **Jakarta EE** 네임스페이스 사용 — `javax.*` 금지, `jakarta.*` 사용
- **Spring Boot** 기반 애플리케이션

---

## 2. Lombok 사용 범위 (핵심 어노테이션만)

허용 어노테이션:
- `@Getter` — getter 자동 생성
- `@Setter` — setter 자동 생성 (불변 권장 시 생략)
- `@Slf4j` — 로거 자동 주입

**선택적 허용** (팀 합의 시):
- `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`

**금지** (명시적 의도 파악 어려움):
- `@Data` (equals/hashCode 자동 생성이 엔티티에서 문제 유발)
- `@EqualsAndHashCode` (무분별한 적용 금지, 필요 시 수동 구현)

---

## 3. 레이어 패턴 (Application stack 기반 조건부)

프로젝트의 데이터 접근 기술에 따라 레이어 네이밍이 달라집니다.

### MyBatis 사용 프로젝트

```
Controller → Service → Dao (+ Mapper XML)
```

- **Dao**: `*Dao.java` 네이밍, MyBatis Mapper 호출만 담당
- SQL 은 `*Mapper.xml` 에 분리

### JPA 사용 프로젝트

```
Controller → Service → Repository
```

- **Repository**: Spring Data JPA 규약 (`JpaRepository`, `CrudRepository`) 준수
- Repository 는 인터페이스로 정의

### 레이어 책임 공통 원칙

| 레이어 | 책임 | 금지 |
|---|---|---|
| **Controller** | 요청/응답 처리, 입력 바인딩·검증 | 비즈니스 로직 |
| **Service** | 비즈니스 로직, 트랜잭션 경계, 결과 조립 | DB 직접 접근 |
| **Dao / Repository** | DB 접근만 (MyBatis mapper 호출 / JPA 쿼리) | 비즈니스 로직 |

**데이터 응답:** Controller 에서는 `ResponseEntity` 사용.

---

## 4. 주석·커밋 메시지 언어 정책

- **주석**: 한국어 허용·권장
  - 도메인 용어는 한국어가 명확할 때 한국어 우선
  - 기술 용어·식별자(method/class name)는 영어 유지
- **커밋 메시지**: 한국어 허용
  - 예: `feat: 사용자 조회 API 추가`, `fix: NPE 오류 수정`, `refactor: UserService 메서드 분리`
  - 타입 prefix (feat/fix/refactor/docs/chore/test) 는 영어 유지

---

## 5. 금지 패턴 요약

- `javax.*` import
- `@Data`, `@EqualsAndHashCode` (무분별한 사용)
- Controller 내 비즈니스 로직
- Service 에서 DB 직접 접근
- Dao/Repository 에 비즈니스 로직
- null 반환 (→ `Optional` 또는 빈 컬렉션)
- `Map<String, Object>` 를 함수 인자·반환 타입으로 사용 (→ DTO)
- SQL `${}` 직접 치환 (→ `#{}` 바인딩)
