package com.example.dto;

import java.util.List;

/**
 * [시나리오 테스트] Refactor Agent 검증 대상 (v0.3.0 — Immutability)
 *
 * 포함된 의도적 위반:
 *  - 인스턴스 필드 final 누락 [Medium]
 *  - 컬렉션 반환 시 방어적 복사 없음 [Medium]
 *  - 생성자에서 받은 컬렉션을 방어적 복사 없이 저장 [Medium]
 *  - record 로 전환 가능한 불변 DTO [Low]
 */
public class MutableDto {

    // ❌ final 미적용 — 재할당 없는 필드
    private String name;
    private int age;
    private String email;
    private List<String> tags;

    public MutableDto(String name, int age, String email, List<String> tags) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.tags = tags; // ❌ 방어적 복사 없음 → 외부에서 tags 수정 시 내부 상태 오염
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }

    // ❌ 내부 리스트 직접 반환 → 외부에서 수정 가능
    public List<String> getTags() {
        return tags; // ❌ Collections.unmodifiableList(tags) 또는 List.copyOf(tags) 사용 필요
    }
}
