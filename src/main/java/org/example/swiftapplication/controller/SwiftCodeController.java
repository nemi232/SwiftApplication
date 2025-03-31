package org.example.swiftapplication.controller;

import org.example.swiftapplication.model.ErrorResponse;
import org.example.swiftapplication.util.SwiftCode;
import org.example.swiftapplication.repository.SwiftCodeRowMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Controller for SWIFT code API endpoints
 */
@RestController
@RequestMapping("/v1/swift-codes")
public class SwiftCodeController {

    private static final Logger logger = Logger.getLogger(SwiftCodeController.class.getName());

    private final JdbcTemplate jdbcTemplate;
    private final SwiftCodeRowMapper swiftCodeRowMapper;

    /**
     * Constructor with required dependencies
     */
    @Autowired
    public SwiftCodeController(JdbcTemplate jdbcTemplate, SwiftCodeRowMapper swiftCodeRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.swiftCodeRowMapper = swiftCodeRowMapper;
    }
    @GetMapping("/test-db")
    public ResponseEntity<?> testDbConnection() {
        try {
            String sql = "SELECT COUNT(*) FROM swift_codes";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return ResponseEntity.ok("Database connection successful. Found " + count + " SWIFT codes.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database connection error: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> listSwiftCodes() {
        try {
            String sql = "SELECT swift_code FROM swift_codes LIMIT 50";
            List<String> codes = jdbcTemplate.queryForList(sql, String.class);
            return ResponseEntity.ok(codes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }



    /**
     * Endpoint 1: Retrieve details of a single SWIFT code
     * GET: /v1/swift-codes/{swift-code}
     *
     * @param swiftCode The SWIFT code to retrieve
     * @return Details of the SWIFT code, including branches if it's a headquarters
     */
    @GetMapping("/{swiftCode}")
    public ResponseEntity<?> getSwiftCode(@PathVariable String swiftCode) {
        try {
            logger.info("Fetching SWIFT code: " + swiftCode);

            // Check if the SWIFT code exists
            String sql = "SELECT * FROM swift_codes WHERE swift_code = ?";
            logger.info("Executing SQL: " + sql + " with parameter: " + swiftCode);

            List<SwiftCode> swiftCodes = jdbcTemplate.query(sql, swiftCodeRowMapper, swiftCode);
            logger.info("Query returned " + swiftCodes.size() + " results");

            if (swiftCodes.isEmpty()) {
                logger.warning("SWIFT code not found: " + swiftCode);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("SWIFT code not found"));
            }

            SwiftCode swiftCodeData = swiftCodes.get(0);
            logger.info("Found SWIFT code: " + swiftCodeData.getSwiftCode() +
                    ", Bank: " + swiftCodeData.getBankName());

            // If it's a headquarters code, fetch all associated branches
            if (swiftCodeData.isHeadquarter()) {
                // Extract the first 8 characters (bank identifier)
                String bankId = swiftCode.substring(0, Math.min(swiftCode.length(), 8));

                // Just query for all codes with the same bankId that aren't headquarters
                String simpleBranchSql = "SELECT * FROM swift_codes WHERE LEFT(swift_code, 8) = ? AND swift_code != ? AND swift_code NOT LIKE '%XXX'";
                List<SwiftCode> branches = jdbcTemplate.query(simpleBranchSql, swiftCodeRowMapper, bankId, swiftCode);
                logger.info("Found " + branches.size() + " branches using simplified query");

                swiftCodeData.setBranches(branches);
            }

            return ResponseEntity.ok(swiftCodeData);
        } catch (Exception e) {
            logger.severe("Error retrieving SWIFT code: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error retrieving SWIFT code: " + e.getMessage()));
        }

    }

    /**
     * Endpoint 2: Return all SWIFT codes for a specific country
     * GET: /v1/swift-codes/country/{countryISO2code}
     */
    @GetMapping("/country/{countryISO2}")
    public ResponseEntity<?> getSwiftCodesByCountry(@PathVariable String countryISO2) {
        try {
            logger.info("Fetching SWIFT codes for country: " + countryISO2);

            // Convert to uppercase for consistency
            String iso2 = countryISO2.toUpperCase();

            // First check if the country exists
            String countrySql = "SELECT DISTINCT country_name FROM swift_codes WHERE country_iso2 = ?";
            List<String> countryNames = jdbcTemplate.queryForList(countrySql, String.class, iso2);

            if (countryNames.isEmpty()) {
                logger.warning("Country not found: " + iso2);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Country not found"));
            }

            // Get all SWIFT codes for the country
            String sql = "SELECT * FROM swift_codes WHERE country_iso2 = ?";
            List<SwiftCode> swiftCodes = jdbcTemplate.query(sql, swiftCodeRowMapper, iso2);

            logger.info("Found " + swiftCodes.size() + " SWIFT codes for country: " + iso2);

            // Create response structure
            Map<String, Object> response = new HashMap<>();
            response.put("countryISO2", iso2);
            response.put("countryName", countryNames.get(0));
            response.put("swiftCodes", swiftCodes);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Error retrieving SWIFT codes for country: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error retrieving SWIFT codes: " + e.getMessage()));
        }
    }

    /**
     * Endpoint 3: Add a new SWIFT code
     * POST: /v1/swift-codes
     */
    @PostMapping
    public ResponseEntity<?> addSwiftCode(@RequestBody SwiftCode swiftCode) {
        try {
            logger.info("Adding new SWIFT code: " + swiftCode.getSwiftCode());

            // Validate required fields
            if (swiftCode.getSwiftCode() == null || swiftCode.getSwiftCode().isEmpty() ||
                    swiftCode.getBankName() == null || swiftCode.getBankName().isEmpty() ||
                    swiftCode.getCountryISO2() == null || swiftCode.getCountryISO2().isEmpty() ||
                    swiftCode.getCountryName() == null || swiftCode.getCountryName().isEmpty()) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Missing required fields"));
            }

            // Convert country codes and names to uppercase
            swiftCode.setCountryISO2(swiftCode.getCountryISO2().toUpperCase());
            swiftCode.setCountryName(swiftCode.getCountryName().toUpperCase());

            // Ensure ISO2 is max 2 characters
            if (swiftCode.getCountryISO2().length() > 2) {
                swiftCode.setCountryISO2(swiftCode.getCountryISO2().substring(0, 2));
                logger.warning("Truncated country ISO2 to 2 characters: " + swiftCode.getCountryISO2());
            }

            // Generate bank identifier from SWIFT code (first 8 characters)
            String bankIdentifier = "";
            if (swiftCode.getSwiftCode().length() >= 8) {
                bankIdentifier = swiftCode.getSwiftCode().substring(0, 8);
            } else {
                bankIdentifier = swiftCode.getSwiftCode();
            }

            // Check if SWIFT code already exists
            String checkSql = "SELECT COUNT(*) FROM swift_codes WHERE swift_code = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, swiftCode.getSwiftCode());

            if (count > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse("SWIFT code already exists"));
            }

            // Insert the new SWIFT code
            String sql = "INSERT INTO swift_codes (swift_code, bank_name, address, country_name, country_iso2, is_headquarter, bank_identifier) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(sql,
                    swiftCode.getSwiftCode(),
                    swiftCode.getBankName(),
                    swiftCode.getAddress() != null ? swiftCode.getAddress() : "",
                    swiftCode.getCountryName(),
                    swiftCode.getCountryISO2(),
                    swiftCode.isHeadquarter(),
                    bankIdentifier
            );

            Map<String, String> response = new HashMap<>();
            response.put("message", "SWIFT code added successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.severe("Error adding SWIFT code: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error adding SWIFT code: " + e.getMessage()));
        }
    }

    /**
     * Endpoint 4: Delete a SWIFT code
     * DELETE: /v1/swift-codes/{swift-code}
     */
    @DeleteMapping("/{swiftCode}")
    public ResponseEntity<?> deleteSwiftCode(@PathVariable String swiftCode) {
        try {
            logger.info("Deleting SWIFT code: " + swiftCode);

            // Check if the SWIFT code exists
            String checkSql = "SELECT COUNT(*) FROM swift_codes WHERE swift_code = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, swiftCode);

            if (count == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("SWIFT code not found"));
            }

            // Delete the SWIFT code
            String sql = "DELETE FROM swift_codes WHERE swift_code = ?";
            int rowsAffected = jdbcTemplate.update(sql, swiftCode);

            logger.info("Deleted " + rowsAffected + " rows");

            Map<String, String> response = new HashMap<>();
            response.put("message", "SWIFT code deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Error deleting SWIFT code: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error deleting SWIFT code: " + e.getMessage()));
        }
    }
}
