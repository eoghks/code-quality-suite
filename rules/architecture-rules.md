# 아키텍처 규칙 (architecture-rules)

> `architecture-review-agent` 가 참조하는 **아키텍처 검증 기준**입니다.
> 패키지 의존 방향·레이어 책임·DDD 경계를 정적 분석(import 분석)으로 검증합니다.

---

## 0. 심각도 체계

| 심각도 | 기준 | BLOCK |
|---|---|---|
| **High** | 아키텍처 붕괴 — 레이어 역방향 의존, 순환 의존, Entity 직접 노출 | ✅ |
| **Medium** | 설계 냄새 — 비즈니스 로직 Controller 직접 구현, 패키지 명명 위반 | ❌ |
| **Low** | 개선 권고 — Hexagonal Port 미적용, 힌트 수준 경고 | ❌ |

---

## 1. 레이어 의존 방향 (ARCH-LAYER)

### 1.1 허용 방향

```
Controller (web)
    ↓ (단방향만 허용)
Service (service / application)
    ↓
Repository / Dao (repository / dao / mapper)
    ↓
Domain / Entity (domain / model)
```

**역방향 import = High 위반.**

### 1.2 감지 패턴

| 위반 패턴 | 심각도 | 코드 |
|---|---|---|
| `@Controller` / `@RestController` 클래스에서 `repository` / `dao` / `mapper` 패키지 import | **High** | ARCH-LAYER-01 |
| `@Service` 클래스에서 `controller` / `web` 패키지 import | **High** | ARCH-LAYER-02 |
| `@Repository` / `@Mapper` 클래스에서 `service` / `controller` 패키지 import | **High** | ARCH-LAYER-03 |
| `@Controller` 에서 직접 SQL 실행 (`JdbcTemplate`·`SqlSession` import) | **High** | ARCH-LAYER-04 |

### 1.3 감지 예시

```java
// ❌ ARCH-LAYER-01 — Controller 가 Repository 직접 참조
import com.example.repository.OrderRepository;  // High

@RestController
public class OrderController {
    @Autowired
    private OrderRepository orderRepository;  // Service 없이 직접 접근
}

// ✅ Controller → Service → Repository 단방향
@RestController
public class OrderController {
    @Autowired
    private OrderService orderService;
}
```

---

## 2. 순환 의존 (ARCH-CYCLE)

### 2.1 정의

두 패키지가 서로를 import 하는 경우 순환 의존.

```
com.example.order   imports com.example.payment
com.example.payment imports com.example.order   ← 순환
```

### 2.2 감지 방법

1. 변경된 파일의 import 목록 추출
2. 각 import 대상 클래스 파일 열어 역방향 import 확인
3. 순환 경로 감지 시 High 보고

### 2.3 심각도

| 유형 | 심각도 | 코드 |
|---|---|---|
| 직접 순환 (`A → B → A`) | **High** | ARCH-CYCLE-01 |
| 간접 순환 (`A → B → C → A`) | **High** | ARCH-CYCLE-02 |

**예외:** 동일 패키지 내 클래스 간 참조는 순환 의존 아님.

---

## 3. DDD 경계 위반 (ARCH-DDD)

### 3.1 Entity 직접 노출 금지

**원칙:** Controller 는 DTO 만 수신·반환. Entity 를 Request/Response 로 직접 사용하면 내부 도메인 모델이 외부에 노출되고 Mass Assignment 위험 발생.

| 위반 패턴 | 심각도 | 코드 |
|---|---|---|
| `@RequestBody` 파라미터 타입이 `Entity` 명칭 (`*Entity`, `*Domain`) | **High** | ARCH-DDD-01 |
| `@RequestBody` 파라미터 타입이 JPA `@Entity` 어노테이션 보유 클래스 | **High** | ARCH-DDD-01 |
| Controller 메서드 반환 타입이 `@Entity` 클래스 직접 | **High** | ARCH-DDD-02 |

```java
// ❌ ARCH-DDD-01 — Entity 직접 수신
@PostMapping("/orders")
public ResponseEntity<Void> createOrder(@RequestBody OrderEntity order) { ... }

// ✅ DTO 사용
@PostMapping("/orders")
public ResponseEntity<Void> createOrder(@RequestBody OrderCreateRequest request) { ... }
```

### 3.2 비즈니스 로직 Controller 직접 구현

**원칙:** Controller 메서드는 요청 수신·검증·위임·응답 반환만 담당. 비즈니스 로직은 Service 레이어로.

| 위반 패턴 | 심각도 | 코드 |
|---|---|---|
| Controller 메서드 50줄 초과 (Refactor §1 과 별도 카운트) | Medium | ARCH-DDD-03 |
| Controller 에서 `if/else` 비즈니스 분기 3개 이상 | Medium | ARCH-DDD-04 |

### 3.3 Value Object / Aggregate 경계 힌트

| 감지 신호 | 권고 | 심각도 |
|---|---|---|
| `String` 타입으로 도메인 식별자 관리 (`userId`, `orderId` 등을 `String`으로) | Value Object (`UserId`, `OrderId`) 도입 권고 | Low |
| 서로 다른 Aggregate 를 한 트랜잭션에서 다중 수정 | 이벤트 기반 분리 권고 | Low |

---

## 4. 패키지 명명 규칙 (ARCH-PKG)

### 4.1 표준 패키지명

Spring Boot 표준 레이어 패키지명:

| 레이어 | 허용 패키지명 |
|---|---|
| 프레젠테이션 | `controller`, `web`, `rest`, `api` |
| 비즈니스 | `service`, `application`, `usecase` |
| 영속성 | `repository`, `dao`, `mapper`, `persistence` |
| 도메인 | `domain`, `model`, `entity` |
| DTO | `dto`, `request`, `response`, `payload` |
| 공통 | `config`, `util`, `common`, `exception`, `support` |

### 4.2 감지

| 위반 패턴 | 심각도 | 코드 |
|---|---|---|
| 표준 목록에 없는 임의 패키지명 (`misc`, `stuff`, `helper2` 등) | Low | ARCH-PKG-01 |
| 레이어명 오타 (`controler`, `sevice` 등) | Low | ARCH-PKG-02 |

---

## 5. Hexagonal Architecture 힌트 (ARCH-HEX)

> 적용 수준: **Low (힌트)** — 강제 아님. 팀이 Hexagonal 을 선택한 경우에만 유의미.

| 감지 신호 | 권고 |
|---|---|
| 외부 시스템 (HTTP Client, MQ, DB) 을 Service 가 직접 구현 클래스로 참조 | Port 인터페이스 + Adapter 분리 권고 |
| `RestTemplate` / `WebClient` 를 Service 에서 직접 사용 | `ExternalApiPort` 인터페이스 추출 권고 |
| Infrastructure 클래스가 `service` 패키지에 위치 | `infrastructure` / `adapter` 패키지로 이동 권고 |

---

## 6. BLOCK 조건

`.architecture-report.md` 마지막 줄:

```
[BLOCK: ARCH STOP]   — 신규 High 위반 존재 시
[PASS: ARCH OK]      — 신규 High 위반 없음
```

**BLOCK 트리거 위반 코드:**
- ARCH-LAYER-01 ~ 04
- ARCH-CYCLE-01 ~ 02
- ARCH-DDD-01 ~ 02

**BLOCK 제외 (경고만):**
- ARCH-DDD-03 ~ 04 (Medium)
- ARCH-PKG-01 ~ 02 (Low)
- ARCH-HEX (Low)

---

## 7. Baseline 연동

`.quality-baseline.json` 의 `code` 필드에 `ARCH-*` 코드 등록 가능.
기존 레거시 아키텍처 위반은 Baseline 에 등록해 신규 위반만 차단.

```json
{
  "code": "ARCH-LAYER-01",
  "file": "src/main/java/com/example/OrderController.java",
  "line": 12,
  "message": "repository 직접 접근 — 레거시, 2026-Q4 Service 분리 예정",
  "fingerprint": "..."
}
```

---

## 8. 보고서 형식

```
## Architecture Report — feature/order-refactor

### High (신규 — 차단)
- [ARCH-LAYER-01] OrderController.java:12 — import com.example.repository.OrderRepository
  → Controller 에서 Repository 직접 접근. OrderService 를 통해 위임할 것.

### Medium
- [ARCH-DDD-03] UserController.java:45 — processPayment() 72줄 (Controller 메서드 50줄 초과)
  → 비즈니스 로직을 UserService 로 이동.

### Baseline (기존 위반 — 비차단)
- [BASELINE][ARCH-LAYER-01] LegacyController.java:8 — 등록일: 2026-04-21

### 요약
- 신규 High: 1건 (BLOCK)
- Medium: 1건
- Baseline 제외: 1건

[BLOCK: ARCH STOP]
```
