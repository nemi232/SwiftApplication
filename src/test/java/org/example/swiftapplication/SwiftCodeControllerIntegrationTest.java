package org.example.swiftapplication;

import org.example.swiftapplication.model.ErrorResponse;
import org.example.swiftapplication.util.SwiftCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

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

    /**
     * Test getting SWIFT codes by country
     */
    @Test
    public void testGetSwiftCodesByCountry() {
        try {
            setupTestData();

            String url = "http://localhost:" + port + "/v1/swift-codes/country/TC";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> responseBody = response.getBody();
            assertNotNull(responseBody);
            assertEquals("TC", responseBody.get("countryISO2"));
            assertEquals("TEST COUNTRY", responseBody.get("countryName"));

            List<?> swiftCodes = (List<?>) responseBody.get("swiftCodes");
            assertNotNull(swiftCodes);
            assertEquals(2, swiftCodes.size());
        } finally {
            cleanupTestData();
        }
    }

    /**
     * Test getting SWIFT codes for non-existent country
     */
    @Test
    public void testGetSwiftCodesByNonExistentCountry() {
        String url = "http://localhost:" + port + "/v1/swift-codes/country/XX";
        ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * Test adding a new SWIFT code
     */
    @Test
    public void testAddSwiftCode() {
        try {
            SwiftCode newCode = new SwiftCode();
            newCode.setSwiftCode("TESTNEWABC");
            newCode.setBankName("New Test Bank");
            newCode.setAddress("789 New St");
            newCode.setCountryName("NEW TEST COUNTRY");
            newCode.setCountryISO2("NT");
            newCode.setHeadquarter(true);

            String url = "http://localhost:" + port + "/v1/swift-codes";
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    newCode,
                    Map.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            Map<String, String> responseBody = response.getBody();
            assertNotNull(responseBody);
            assertEquals("SWIFT code added successfully", responseBody.get("message"));

            // Verify the code was added
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM swift_codes WHERE swift_code = ?",
                    Integer.class,
                    "TESTNEWABC"
            );
            assertEquals(1, count);
        } finally {
            jdbcTemplate.update("DELETE FROM swift_codes WHERE swift_code = 'TESTNEWABC'");
        }
    }

    /**
     * Test adding a duplicate SWIFT code
     */
    @Test
    public void testAddDuplicateSwiftCode() {
        try {
            setupTestData();

            SwiftCode duplicateCode = new SwiftCode();
            duplicateCode.setSwiftCode("TESTBANKXXX");  // Already exists
            duplicateCode.setBankName("Duplicate Bank");
            duplicateCode.setAddress("Duplicate Address");
            duplicateCode.setCountryName("DUPLICATE COUNTRY");
            duplicateCode.setCountryISO2("DC");
            duplicateCode.setHeadquarter(true);

            String url = "http://localhost:" + port + "/v1/swift-codes";
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    duplicateCode,
                    Map.class
            );

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        } finally {
            cleanupTestData();
        }
    }

    /**
     * Test deleting a SWIFT code
     */
    @Test
    public void testDeleteSwiftCode() {
        try {
            setupTestData();

            String url = "http://localhost:" + port + "/v1/swift-codes/TESTBANK001";
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    new ParameterizedTypeReference<Map<String, String>>() {}
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, String> responseBody = response.getBody();
            assertNotNull(responseBody);
            assertEquals("SWIFT code deleted successfully", responseBody.get("message"));

            // Verify the code was deleted
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM swift_codes WHERE swift_code = ?",
                    Integer.class,
                    "TESTBANK001"
            );
            assertEquals(0, count);
        } finally {
            cleanupTestData();
        }
    }

    /**
     * Test deleting a non-existent SWIFT code
     */
    @Test
    public void testDeleteNonExistentSwiftCode() {
        String url = "http://localhost:" + port + "/v1/swift-codes/NONEXISTENT";
        ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                null,
                Object.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}