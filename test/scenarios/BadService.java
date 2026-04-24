package com.example.service;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * [시나리오 테스트] Refactor Agent 검증 대상
 *
 * 포함된 의도적 위반:
 *  - CC > 10 (Cyclomatic Complexity 11)
 *  - 파라미터 4개 (Parameter Object 필요)
 *  - catch(Exception) 광범위 포착 [High]
 *  - e.printStackTrace() [High]
 *  - try-finally close (try-with-resources 미적용) [High]
 */
public class BadService {

    // ❌ 파라미터 4개 초과 → Parameter Object 필요
    public String processOrder(Long userId, Long productId, int quantity, String address) {
        String result = "";

        // ❌ CC 과다: if/else if 체인 11개 분기
        if (quantity <= 0) {
            result = "INVALID_QTY";
        } else if (quantity == 1) {
            result = "SINGLE";
        } else if (quantity <= 5) {
            result = "SMALL";
        } else if (quantity <= 10) {
            result = "MEDIUM";
        } else if (quantity <= 20) {
            result = "LARGE";
        } else if (quantity <= 50) {
            result = "BULK";
        } else if (quantity <= 100) {
            result = "WHOLESALE";
        } else if (quantity <= 500) {
            result = "ENTERPRISE";
        } else if (quantity <= 1000) {
            result = "MEGA";
        } else if (quantity <= 5000) {
            result = "ULTRA";
        } else {
            result = "MAX";
        }

        return result;
    }

    // ❌ catch(Exception) 광범위 포착 + e.printStackTrace()
    public void readConfig(String path) {
        try {
            doRead(path);
        } catch (Exception e) {
            e.printStackTrace(); // ❌ log.error("...", e) 로 교체 필요
        }
    }

    // ❌ try-finally close → try-with-resources 강제 대상
    public void readFile(String path) throws Exception {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
            int data = fis.read();
        } finally {
            if (fis != null) {
                fis.close(); // ❌ try-with-resources 사용 필요
            }
        }
    }

    // ❌ Connection 수동 close
    public void queryDb(Connection conn) throws Exception {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement("SELECT 1");
            ps.execute();
        } finally {
            if (ps != null) ps.close(); // ❌ try-with-resources 사용 필요
        }
    }

    private void doRead(String path) throws Exception {
        // stub
    }
}
