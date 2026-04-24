package com.example.controller;

/**
 * [시나리오 테스트] Refactor Agent 검증 대상 (v0.3.0 — Guard Clause)
 *
 * 포함된 의도적 위반:
 *  - 중첩 if 4레벨 [Medium]
 *  - 전제 조건 후 else 블록 [Low]
 */
public class NestedIfController {

    // ❌ 중첩 if 4레벨 — Guard clause 로 전환 필요
    public String processOrder(Order order) {
        if (order != null) {
            if (order.isValid()) {
                if (order.hasItems()) {
                    if (order.getUser() != null) {
                        return doProcess(order); // 핵심 로직이 4레벨 안에 묻힘
                    } else {
                        return "NO_USER";
                    }
                } else {
                    return "NO_ITEMS";
                }
            } else {
                return "INVALID";
            }
        } else {
            return "NULL_ORDER";
        }
    }

    // ❌ 전제 조건 후 불필요한 else
    public void validate(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("입력 필수");
        } else {
            // ❌ else 불필요 — 위에서 이미 throw 됨
            doValidate(input);
        }
    }

    // ❌ 중첩 if 3레벨 초과 + else 중첩
    public String categorize(int score, boolean isPremium, boolean isActive) {
        if (isActive) {
            if (isPremium) {
                if (score >= 90) {
                    if (score == 100) {
                        return "PERFECT_PREMIUM"; // 4레벨
                    } else {
                        return "HIGH_PREMIUM";
                    }
                } else {
                    return "NORMAL_PREMIUM";
                }
            } else {
                return "ACTIVE_BASIC";
            }
        } else {
            return "INACTIVE";
        }
    }

    private String doProcess(Order order) { return "OK"; }
    private void doValidate(String input) {}

    static class Order {
        boolean isValid() { return true; }
        boolean hasItems() { return true; }
        Object getUser() { return new Object(); }
    }
}
