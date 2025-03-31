package org.example.swiftapplication.controller;

import org.example.swiftapplication.model.ErrorResponse;
import org.example.swiftapplication.util.SwiftCode;
import org.example.swiftapplication.repository.SwiftCodeRowMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
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
                // Extract the first 8 characters directly
                String bankId = swiftCode.length() >= 8 ? swiftCode.substring(0, 8) : swiftCode;
                logger.info("Fetching branches for bank ID: " + bankId);

                String branchSql = "SELECT * FROM swift_codes WHERE bank_identifier = ? AND swift_code != ? AND is_headquarter = false";
                List<SwiftCode> branches = jdbcTemplate.query(branchSql, swiftCodeRowMapper, bankId, swiftCode);
                logger.info("Found " + branches.size() + " branches");

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
}
