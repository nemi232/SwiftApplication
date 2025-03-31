package org.example.swiftapplication;

import org.example.swiftapplication.util.SwiftCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SwiftCodeController
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SwiftCodeControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Set up test data
     */
    private void setupTestData() {
        // Create test headquarters
        jdbcTemplate.update(
                "INSERT INTO swift_codes (swift_code, bank_name, address, country_name, country_iso2, is_headquarter, bank_identifier) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "TESTBANKXXX", "Test Bank", "123 Test St", "TEST COUNTRY", "TC", true, "TESTBANK"
        );

        // Create test branch
        jdbcTemplate.update(
                "INSERT INTO swift_codes (swift_code, bank_name, address, country_name, country_iso2, is_headquarter, bank_identifier) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "TESTBANK001", "Test Bank Branch", "456 Branch St", "TEST COUNTRY", "TC", false, "TESTBANK"
        );
    }

    /**
     * Clean up test data
     */
    private void cleanupTestData() {
        jdbcTemplate.update("DELETE FROM swift_codes WHERE swift_code LIKE 'TESTBANK%'");
    }

    /**
     * Test getting a headquarters SWIFT code
     */
    @Test
    public void testGetHeadquarterSwiftCode() {
        try {
            setupTestData();

            String url = "http://localhost:" + port + "/v1/swift-codes/TESTBANKXXX";
            ResponseEntity<SwiftCode> response = restTemplate.getForEntity(url, SwiftCode.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            SwiftCode swiftCode = response.getBody();
            assertNotNull(swiftCode);
            assertEquals("TESTBANKXXX", swiftCode.getSwiftCode());
            assertEquals("Test Bank", swiftCode.getBankName());
            assertTrue(swiftCode.isHeadquarter());
            assertEquals(1, swiftCode.getBranches().size());
            assertEquals("TESTBANK001", swiftCode.getBranches().get(0).getSwiftCode());
        } finally {
            cleanupTestData();
        }
    }

    /**
     * Test getting a branch SWIFT code
     */
    @Test
    public void testGetBranchSwiftCode() {
        try {
            setupTestData();

            String url = "http://localhost:" + port + "/v1/swift-codes/TESTBANK001";
            ResponseEntity<SwiftCode> response = restTemplate.getForEntity(url, SwiftCode.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            SwiftCode swiftCode = response.getBody();
            assertNotNull(swiftCode);
            assertEquals("TESTBANK001", swiftCode.getSwiftCode());
            assertEquals("Test Bank Branch", swiftCode.getBankName());
            assertFalse(swiftCode.isHeadquarter());
            assertTrue(swiftCode.getBranches() == null || swiftCode.getBranches().isEmpty());
        } finally {
            cleanupTestData();
        }
    }

    /**
     * Test getting a non-existent SWIFT code
     */
    @Test
    public void testGetNonExistentSwiftCode() {
        String url = "http://localhost:" + port + "/v1/swift-codes/NONEXISTENT";
        ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}