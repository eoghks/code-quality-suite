# 접근 제어 규칙 (A01 — Broken Access Control)

> `security-audit-agent` 가 로드하는 카테고리 파일.
> **인가 어노테이션 누락·IDOR** 감지.

---

## A01.1 Controller 엔드포인트 인가 누락 **[High]**

`@RestController` / `@Controller` 의 public 핸들러에 인가 어노테이션 부재.

```java
// ❌ High
@GetMapping("/admin/users")
public List<User> listUsers() { ... }

// ✅
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public List<User> listUsers() { ... }
```

**허용 어노테이션:** `@PreAuthorize` · `@PostAuthorize` · `@Secured` · `@RolesAllowed` · `@PermitAll`.

**예외:**
- 클래스 레벨 `@PreAuthorize` 선언 시 핸들러별 누락 허용
- `/public`·`/health`·`/actuator` 등 공개 경로는 `@PermitAll` 명시 권고

---

## A01.2 IDOR (Insecure Direct Object Reference) **[High]**

`@PathVariable Long id` 를 소유권 검증 없이 Repository 조회에 직접 전달.

```java
// ❌ 다른 사용자 주문 조회 가능
@GetMapping("/orders/{id}")
public Order getOrder(@PathVariable Long id) {
    return orderRepository.findById(id).orElseThrow();
}

// ✅ 소유권 확인
public Order getOrder(@PathVariable Long id, @AuthenticationPrincipal User user) {
    Order order = orderRepository.findById(id).orElseThrow();
    if (!order.getUserId().equals(user.getId())) throw new ForbiddenException();
    return order;
}
```

**감지 기준:** `findById(pathVariable)` 직접 호출 + 소유권 체크 부재.
