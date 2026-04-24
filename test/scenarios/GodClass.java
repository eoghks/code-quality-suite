package com.example.service;

/**
 * [시나리오 테스트] Refactor Agent 검증 대상
 *
 * 포함된 의도적 위반:
 *  - 필드 16개 (10개 초과 → Medium, 15개 초과 AND 메서드 다수 → God Class High)
 *  - Law of Demeter 4단계 체인 [Medium]
 *  - 클래스 크기 과다 (400줄 초과 위반 시뮬레이션용 스켈레톤)
 */
public class GodClass {

    // ❌ 인스턴스 필드 16개 (10개 초과 Medium, God Class 조건 충족)
    private String field01;
    private String field02;
    private String field03;
    private String field04;
    private String field05;
    private String field06;
    private String field07;
    private String field08;
    private String field09;
    private String field10;
    private String field11;
    private String field12;
    private String field13;
    private String field14;
    private String field15;
    private String field16;

    // 메서드 31개 (God Class: 메서드 30개 초과 + 필드 15개 초과 → High)
    public void method01() {}
    public void method02() {}
    public void method03() {}
    public void method04() {}
    public void method05() {}
    public void method06() {}
    public void method07() {}
    public void method08() {}
    public void method09() {}
    public void method10() {}
    public void method11() {}
    public void method12() {}
    public void method13() {}
    public void method14() {}
    public void method15() {}
    public void method16() {}
    public void method17() {}
    public void method18() {}
    public void method19() {}
    public void method20() {}
    public void method21() {}
    public void method22() {}
    public void method23() {}
    public void method24() {}
    public void method25() {}
    public void method26() {}
    public void method27() {}
    public void method28() {}
    public void method29() {}
    public void method30() {}
    public void method31() {}

    // ❌ Law of Demeter 4단계 체인
    public String getCityName(User user) {
        return user.getAddress().getCity().getName().toUpperCase(); // ❌ 4단계 체인
    }

    // ❌ LoD 5단계
    public String getZipCode(Order order) {
        return order.getUser().getAddress().getCity().getZip().getCode(); // ❌ 5단계 체인
    }

    // stub inner types
    static class User {
        Address getAddress() { return new Address(); }
    }
    static class Address {
        City getCity() { return new City(); }
    }
    static class City {
        String getName() { return "Seoul"; }
        Zip getZip() { return new Zip(); }
    }
    static class Zip {
        String getCode() { return "12345"; }
    }
    static class Order {
        User getUser() { return new User(); }
    }
}
