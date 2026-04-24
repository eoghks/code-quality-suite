package com.example.controller;

import com.example.domain.UserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * [시나리오 테스트] Architecture Agent 검증 대상
 *
 * 포함된 의도적 위반:
 *  - @RequestBody 에 Entity 직접 수신 (ARCH-DDD-01) [High]
 *  - 반환 타입에 Entity 직접 사용 (ARCH-DDD-02) [High]
 */
@RestController
@RequestMapping("/users")
public class EntityExposed {

    // ❌ High — ARCH-DDD-01: @RequestBody 에 UserEntity 직접 수신
    // Mass Assignment 위험: isAdmin, role 등 민감 필드 요청에서 직접 수정 가능
    @PostMapping
    public ResponseEntity<Void> createUser(@RequestBody UserEntity user) {
        // userService.save(user); // DTO 를 통해 필요한 필드만 바인딩해야 함
        return ResponseEntity.ok().build();
    }

    // ❌ High — ARCH-DDD-02: 반환 타입에 Entity 직접 노출
    // 내부 도메인 모델이 API 응답에 그대로 노출 → 필드 추가 시 자동 노출
    @GetMapping("/{id}")
    public UserEntity getUser(@PathVariable Long id) {
        return new UserEntity(); // ❌ UserResponse DTO 를 별도 정의해 반환해야 함
    }

    // ✅ 올바른 패턴 예시 (참고용)
    // @PostMapping
    // public ResponseEntity<Void> createUser(@RequestBody UserCreateRequest request) {
    //     userService.create(request);
    //     return ResponseEntity.ok().build();
    // }
    //
    // @GetMapping("/{id}")
    // public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
    //     return ResponseEntity.ok(userService.findById(id));
    // }
}
