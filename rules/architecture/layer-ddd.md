# 아키텍처 레이어·DDD 규칙 (architecture/layer-ddd)

> §0~7: 심각도 체계·레이어 의존·순환 의존·DDD 경계·패키지 명명·Hexagonal·BLOCK 조건·Baseline

---

## 0. 심각도 체계

| 심각도 | 기준 | BLOCK |
|---|---|---|
| **High** | 아키텍처 붕괴 — 레이어 역방향 의존, 순환 의존, Entity 직접 노출 | ✅ |
| **Medium** | 설계 냄새 — 비즈니스 로직 Controller 직접 구현, 패키지 명명 위반 | ❌ |
| **Low** | 개선 권고 — Hexagonal Port 미적용, 힌트 수준 경고 | ❌ |

---

## 1. 레이어 의존 방향 (ARCH-LAYER)

**허용 방향:** Controller → Service → Repository/Dao → Domain/Entity (단방향)

**역방향 import = High 위반.**

| 위반 패턴 | 심각도 | 코드 |
|---|---|---|
| `@Controller`/`@RestController` 에서 `repository`/`dao`/`mapper` import | **High** | ARCH-LAYER-01 |
| `@Service` 에서 `controller`/`web` import | **High** | ARCH-LAYER-02 |
| `@Repository`/`@Mapper` 에서 `service`/`controller` import | **High** | ARCH-LAYER-03 |
| `@Controller` 에서 직접 SQL 실행 (`JdbcTemplate`·`SqlSession` import) | **High** | ARCH-LAYER-04 |

```java
// ❌ ARCH-LAYER-01
import com.example.repository.OrderRepository;
@RestController
public class OrderController {
    @Autowired
    private OrderRepository orderRepository; // Service 없이 직접 접근
}
// ✅ Controller → Service → Repository 단방향
@Autowired
private OrderService orderService;
```

---

## 2. 순환 의존 (ARCH-CYCLE)

두 패키지가 서로를 import 하는 경우 순환 의존.

| 유형 | 심각도 | 코드 |
|---|---|---|
| 직접 순환 (`A → B → A`) | **High** | ARCH-CYCLE-01 |
| 간접 순환 (`A → B → C → A`) | **High** | ARCH-CYCLE-02 |

**예외:** 동일 패키지 내 클래스 간 참조는 순환 의존 아님.

---

## 3. DDD 경계 위반 (ARCH-DDD)

### 3.1 Entity 직접 노출 금지

Controller 는 DTO 만 수신·반환. Entity 직접 사용 시 Mass Assignment 위험 발생.

| 위반 패턴 | 심각도 | 코드 |
|---|---|---|
| `@RequestBody` 파라미터 타입이 `*Entity`, `*Domain` | **High** | ARCH-DDD-01 |
| Controller 메서드 반환 타입이 `@Entity` 클래스 직접 | **High** | ARCH-DDD-02 |

```java
// ❌ ARCH-DDD-01
@PostMapping("/orders")
public ResponseEntity<Void> createOrder(@RequestBody OrderEntity order) { ... }
// ✅ DTO 사용
public ResponseEntity<Void> createOrder(@RequestBody OrderCreateRequest request) { ... }
```

### 3.2 비즈니스 로직 Controller 직접 구현

| 위반 패턴 | 심각도 | 코드 |
|---|---|---|
| Controller 메서드 50줄 초과 | Medium | ARCH-DDD-03 |
| Controller 에서 `if/else` 비즈니스 분기 3개 이상 | Medium | ARCH-DDD-04 |

### 3.3 Value Object 힌트

| 감지 신호 | 권고 | 심각도 |
|---|---|---|
| `String` 으로 도메인 식별자 관리 (`userId` 등을 `String`) | Value Object 도입 권고 | Low |
| 서로 다른 Aggregate 를 한 트랜잭션에서 다중 수정 | 이벤트 기반 분리 권고 | Low |

---

## 4. 패키지 명명 규칙 (ARCH-PKG)

| 레이어 | 허용 패키지명 |
|---|---|
| 프레젠테이션 | `controller`, `web`, `rest`, `api` |
| 비즈니스 | `service`, `application`, `usecase` |
| 영속성 | `repository`, `dao`, `mapper`, `persistence` |
| 도메인 | `domain`, `model`, `entity` |
| DTO | `dto`, `request`, `response`, `payload` |
| 공통 | `config`, `util`, `common`, `exception`, `support` |

| 위반 패턴 | 심각도 | 코드 |
|---|---|---|
| 표준 목록에 없는 임의 패키지명 (`misc`, `stuff`, `helper2` 등) | Low | ARCH-PKG-01 |
| 레이어명 오타 (`controler`, `sevice` 등) | Low | ARCH-PKG-02 |

---

## 5. Hexagonal Architecture 힌트 (ARCH-HEX)

> 적용 수준: **Low** — 강제 아님. Hexagonal 을 선택한 경우에만 유의미.

| 감지 신호 | 권고 |
|---|---|
| 외부 시스템을 Service 가 직접 구현 클래스로 참조 | Port 인터페이스 + Adapter 분리 |
| `RestTemplate`/`WebClient` 를 Service 에서 직접 사용 | `ExternalApiPort` 인터페이스 추출 |
| Infrastructure 클래스가 `service` 패키지에 위치 | `infrastructure`/`adapter` 패키지로 이동 |

---

## 6. BLOCK 조건

```
[BLOCK: ARCH STOP]   — 신규 High 위반 존재 시
[PASS: ARCH OK]      — 신규 High 위반 없음
```

**BLOCK 트리거:** ARCH-LAYER-01~04 · ARCH-CYCLE-01~02 · ARCH-DDD-01~02

**BLOCK 제외:** ARCH-DDD-03~04 (Medium) · ARCH-PKG-01~02 (Low) · ARCH-HEX (Low)

---

## 7. Baseline 연동

`.quality-baseline.json` 의 `code` 필드에 `ARCH-*` 코드 등록 가능.
레거시 아키텍처 위반은 Baseline 에 등록해 신규 위반만 차단.

```json
{
  "code": "ARCH-LAYER-01",
  "file": "src/main/java/com/example/OrderController.java",
  "line": 12,
  "message": "repository 직접 접근 — 레거시, 2026-Q4 Service 분리 예정",
  "fingerprint": "..."
}
```
