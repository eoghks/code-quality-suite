# 예외·결합도 규칙 (refactor/exceptions-coupling)

> §11~12: 런타임 안전(예외·Resource)·응집도·결합도

---

## 11. 런타임 안전 (예외·Resource) (v0.2.0+)

### 11.1 예외 처리 금지 패턴

| 패턴 | 심각도 | 대응 |
|---|---|---|
| `catch (Exception e)` / `catch (Throwable t)` | **High** | 구체 예외로 분리 (`IOException`, `SQLException` 등) |
| `catch (RuntimeException e)` 상위 포착 | **High** | 구체 런타임 예외로 분리 |
| `e.printStackTrace()` | **High** | `log.error("...", e)` 로 교체 |
| catch 블록 공백 (swallow) | **High** | 최소 로깅 + 복구 로직 또는 재던지기 |
| `throw new RuntimeException(...)` | **Medium** | 도메인 의미 담은 구체 예외 클래스 정의 |

### 11.2 Resource 관리 강제

**try-with-resources 강제** — `AutoCloseable` 구현체는 예외 없이 사용.

```java
// ❌ 수동 close (finally 누락 시 리소스 누수)
Connection conn = dataSource.getConnection();
try { ... } finally { conn.close(); }

// ✅ try-with-resources
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(SQL)) { ... }
```

**감지 대상 AutoCloseable (주요):**
JDBC: `Connection` · `Statement` · `PreparedStatement` · `ResultSet`
I/O: `InputStream` · `OutputStream` · `Reader` · `Writer` · `FileInputStream` · `FileOutputStream`
기타: `Scanner` · `BufferedReader` · `RandomAccessFile`

**예외:** Spring `JdbcTemplate` · `TransactionTemplate` 등은 프레임워크가 close 관리 → 보고 제외.

---

## 12. 응집도·결합도 (v0.2.0+)

### 12.1 클래스·파일 크기 제한

| 단위 | 임계값 | 심각도 |
|---|---|---|
| 클래스 | 400줄 초과 | Medium |
| 파일 | 500줄 초과 | Medium |
| God Class (메서드 30개 초과 AND 필드 15개 초과) | 모두 초과 | **High** |

초과 시: 단일 책임 원칙 위반 신호 → 도메인 분리 / Service 레이어 분리.

### 12.2 Law of Demeter (LoD)

메서드 체이닝 **3단계 초과** 경고.

```java
// ❌ 4단계 체인 — LoD 위반
String city = user.getAddress().getCity().getName().toUpperCase();
// ✅ 위임 메서드 제공
String city = user.getCityName();
```

**예외:** Builder 체이닝, Stream API, Fluent API → 보고 제외.

### 12.3 순환 의존 (참고)

패키지 간 순환 참조는 **Low 보고** (import 분석으로 감지, 확신도 낮으므로 경고 수준).

### 12.4 God Class 분해 가이드

**High 판정 시 제안 순서:**
1. 책임별 메서드 그룹화 → 별도 Service/Component 분리
2. 데이터만 담는 필드들 → 내부 Value Object 추출
3. 분리 후 원본 클래스는 Facade 역할 유지 (외부 호출자 영향 최소화)
