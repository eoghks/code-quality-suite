# 역직렬화·XXE 규칙 (A08 — Integrity Failures / X02)

> `security-audit-agent` 가 로드하는 카테고리 파일.
> **Unsafe Deserialization, JWT 서명 누락, XXE** 감지.

---

## A08.1 Unsafe Deserialization

| 패턴 | 심각도 | 대응 |
|---|---|---|
| `ObjectMapper.enableDefaultTyping()` (Jackson) | **Critical** | 제거. polymorphic type 은 `@JsonTypeInfo(use = NAME)` + `PolymorphicTypeValidator` |
| `new ObjectInputStream(...)` | **Critical** | `ObjectInputFilter` 화이트리스트 설정 필수 |
| `XMLDecoder` (`java.beans.XMLDecoder`) | **Critical** | JAXB 등 안전 파서로 대체 |
| `new Yaml()` (SnakeYAML 기본 constructor) | **Critical** | `new Yaml(new SafeConstructor(...))` |
| `SerializationUtils.deserialize` (Apache Commons) | **High** | 신뢰 경계 밖 데이터 사용 금지 |

---

## A08.2 JWT 서명 검증 누락 **[Critical]**

```java
// ❌ 서명 미검증
Jwts.parser().parseClaimsJwt(token);

// ✅ 서명 검증
Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
```

---

## X02. XXE (XML External Entity) **[Critical]**

XML 파서 생성 시 외부 엔티티 비활성화 설정 누락.

```java
// ❌ 외부 엔티티 허용 (기본값)
DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();

// ✅ 외부 엔티티 차단
DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
f.setFeature("http://xml.org/sax/features/external-general-entities", false);
f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
f.setXIncludeAware(false);
f.setExpandEntityReferences(false);
```

**감지 대상 클래스:** `DocumentBuilderFactory` · `SAXParserFactory` · `XMLInputFactory` · `TransformerFactory` · `SchemaFactory` · `XPathFactory`.
