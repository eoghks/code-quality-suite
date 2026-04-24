package com.example.controller;

import com.example.repository.OrderRepository;   // ❌ ARCH-LAYER-01: Controller → Repository 직접 import
import com.example.repository.UserRepository;    // ❌ ARCH-LAYER-01: Controller → Repository 직접 import
import org.springframework.jdbc.core.JdbcTemplate; // ❌ ARCH-LAYER-04: Controller 에서 JdbcTemplate 직접
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [시나리오 테스트] Architecture Agent 검증 대상
 *
 * 포함된 의도적 위반:
 *  - Controller → Repository 직접 import (ARCH-LAYER-01) [High]
 *  - Controller 에서 JdbcTemplate 직접 사용 (ARCH-LAYER-04) [High]
 */
@RestController
public class LayerViolation {

    // ❌ High — Service 없이 Repository 직접 주입
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate; // ❌ High

    public LayerViolation(OrderRepository orderRepository,
                          UserRepository userRepository,
                          JdbcTemplate jdbcTemplate) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ❌ Controller 에서 Repository 직접 호출 — Service 레이어 없음
    @GetMapping("/orders")
    public Object getOrders() {
        return orderRepository.findAll(); // ❌ orderService.getOrders() 로 위임해야 함
    }

    @GetMapping("/users")
    public Object getUsers() {
        return jdbcTemplate.queryForList("SELECT * FROM users"); // ❌ Controller 직접 SQL
    }
}
