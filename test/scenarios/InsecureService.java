package com.example.service;

import java.security.MessageDigest;
import java.util.Random;

/**
 * [시나리오 테스트] Security Agent 검증 대상
 *
 * 포함된 의도적 위반:
 *  - MD5 해시 사용 (A02) [Critical]
 *  - java.util.Random 보안 맥락 사용 (A02) [Critical]
 *  - Runtime.exec(userInput) Command Injection (A03) [Critical]
 *  - String.equals() 비밀번호 비교 Timing Attack (A07) [High]
 *  - 민감정보 로그 출력 (A09) [Critical]
 */
public class InsecureService {

    // ❌ Critical — MD5 사용 (A02)
    public String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5"); // ❌ SHA-256 이상 사용
        byte[] hash = md.digest(password.getBytes());
        return new String(hash);
    }

    // ❌ Critical — java.util.Random 으로 토큰 생성 (A02)
    public String generateToken() {
        Random random = new Random(); // ❌ SecureRandom 사용 필요
        return String.valueOf(random.nextLong());
    }

    // ❌ Critical — Command Injection (A03)
    public void executeCommand(String userInput) throws Exception {
        Runtime.getRuntime().exec(userInput); // ❌ 사용자 입력 직접 전달
    }

    // ❌ High — Timing Attack: String.equals() 비밀번호 비교 (A07)
    public boolean authenticate(String stored, String input) {
        return stored.equals(input); // ❌ MessageDigest.isEqual() 사용 필요
    }

    // ❌ Critical — 민감정보 로그 출력 (A09)
    public void login(String username, String password) {
        System.out.println("login attempt: " + username + " / " + password); // ❌ 비밀번호 로그
        // log.info("password={}", password); // ❌ 이것도 동일하게 Critical
    }

    // ❌ Critical — AWS 하드코딩 키 (A02.3)
    private static final String AWS_KEY = "AKIAIOSFODNN7EXAMPLE"; // ❌ Secret 하드코딩
    private static final String AWS_SECRET = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"; // ❌
}
