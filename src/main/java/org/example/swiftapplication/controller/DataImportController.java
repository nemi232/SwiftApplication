package org.example.swiftapplication.controller;

import org.example.swiftapplication.util.SwiftCodeProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

/**
 * Controller for manually triggering data importer
 */
@RestController
public class DataImportController {

    private static final Logger logger = Logger.getLogger(DataImportController.class.getName());

    private final SwiftCodeProcessor swiftCodeProcessor;

    @Autowired
    public DataImportController(SwiftCodeProcessor swiftCodeProcessor) {
        this.swiftCodeProcessor = swiftCodeProcessor;
    }

    /**
     * Endpoint to trigger data import from Excel file
     * @param filePath Optional path to the Excel file
     * @return Result of the import operation
     */
    @GetMapping("/import-data")
    public ResponseEntity<String> importData(
            @RequestParam(required = false, defaultValue = "Interns_2025_SWIFT_CODES.xlsx") String filePath) {
        try {
            logger.info("Data import requested, file: " + filePath);
            int count = swiftCodeProcessor.processExcelFile(filePath);
            return ResponseEntity.ok("Successfully imported " + count + " SWIFT codes");
        } catch (Exception e) {
            logger.severe("Error importing data: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error importing data: " + e.getMessage());
        }
    }
}
