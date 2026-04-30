# Spring 규칙 (architecture/spring-rules)

> §9~11: @Transactional 위치 검증·Spring Security 설정·보고서 형식

---

## 9. @Transactional 위치 검증

`@Transactional` 은 Service 레이어의 책임. Controller 에 선언 시 레이어 책임 원칙이 깨진다.

### ARCH-TX-01 — Controller @Transactional [Medium]

```java
// ❌ Controller 메서드 레벨
@RestController
public class OrderController {
    @Transactional
    @PostMapping("/orders")
    public ResponseEntity<Void> createOrder(...) { ... }
}

// ❌ Controller 클래스 레벨
@Transactional
@RestController
public class UserController { ... }

// ✅ 올바른 위치 — Service
@Service
public class OrderService {
    @Transactional
    public void createOrder(...) { ... }
}
```

**감지 기준:** `@Controller` 또는 `@RestController` 클래스(또는 해당 클래스 메서드)에 `@Transactional` 선언.

### ARCH-TX-02 — Controller @Transactional(readOnly=true) [Medium]

```java
// ❌ readOnly 도 Controller 책임 아님
@RestController
public class ProductController {
    @Transactional(readOnly = true)
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDto> getProduct(...) { ... }
}
```

---

## 10. Spring Security 설정 검증

### ARCH-SEC-01 — anyRequest() 종결 선언 누락 [High]

```java
// ❌ anyRequest() 종결 선언 없음
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/public/**").permitAll()
        // ❌ anyRequest().authenticated() 누락
    );
    return http.build();
}

// ✅ 종결 선언 있음
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/public/**").permitAll()
    .anyRequest().authenticated()   // 필수
);
```

**감지 기준:** `SecurityFilterChain` 빈 메서드에서 `anyRequest()` 호출 부재.

### ARCH-SEC-02 — permitAll() 과다 적용 [Medium]

`permitAll()` 호출이 5개 이상이며 `/public/`·`/static/`·`/actuator/health`·`/swagger-ui` 등 명백한 공개 경로가 아닌 패턴 포함.

### ARCH-SEC-03 — httpBasic() 활성화 [Medium]

```java
// ❌ 프로덕션 코드에 httpBasic 활성화
http.httpBasic(Customizer.withDefaults());
```

**예외:** `@Profile("dev")` / `@Profile("local")` 어노테이션이 동일 클래스에 있으면 Low 완화.

---

## 11. 보고서 형식

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
