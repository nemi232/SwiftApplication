package org.example.swiftapplication.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Swift Code Processor
 *
 * This utility processes SWIFT codes from an Excel file and stores them in a MySQL database.
 * It identifies headquarters (codes ending with "XXX") and branches, and associates branches
 * with their headquarters based on the first 8 characters of the SWIFT code.
 */
@Component
public class SwiftCodeProcessor {

    private static final Logger logger = Logger.getLogger(SwiftCodeProcessor.class.getName());

    private final DataSource dataSource;

    @Autowired
    public SwiftCodeProcessor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Process the Excel file and store data in database
     * @param filePath Path to the Excel file
     * @return Count of processed records
     * @throws Exception If an error occurs
     */
    public int processExcelFile(String filePath) throws Exception {
        logger.info("Starting to process Excel file: " + filePath);

        // Check if file exists
        File file = new File(filePath);
        if (!file.exists()) {
            // Try different paths
            String workingDir = System.getProperty("user.dir");
            String alternativePath = workingDir + File.separator + filePath;
            file = new File(alternativePath);

            if (!file.exists()) {
                // Try resources directory
                alternativePath = "src/main/resources/" + filePath;
                file = new File(alternativePath);

                if (!file.exists()) {
                    throw new FileNotFoundException("Excel file not found at: " + filePath +
                            " or " + alternativePath +
                            ". Current directory: " + workingDir);
                }
            }
            filePath = file.getAbsolutePath();
        }

        logger.info("Found Excel file at: " + file.getAbsolutePath());

        // Create a connection
        Connection connection = dataSource.getConnection();

        try {
            // Create tables if they don't exist
            createTables(connection);

            // Parse Excel file
            List<SwiftCodeData> swiftCodesData = parseExcelFile(filePath);
            logger.info("Found " + swiftCodesData.size() + " SWIFT codes");

            // Store data in database
            int count = storeDataInDatabase(connection, swiftCodesData);
            logger.info("Successfully stored " + count + " SWIFT codes in the database");

            return count;
        } finally {
            // Close connection
            connection.close();
        }
    }

    // Model class for SWIFT code data
    public static class SwiftCodeData {
        private String swiftCode;
        private String bankName;
        private String address;
        private String countryName;
        private String countryISO2;
        private boolean isHeadquarter;

        public SwiftCodeData(String swiftCode, String bankName, String address,
                             String countryName, String countryISO2, boolean isHeadquarter) {
            this.swiftCode = swiftCode;
            this.bankName = bankName;
            this.address = address;
            // Store country data in uppercase as per requirements
            this.countryName = countryName != null ? countryName.toUpperCase() : "";

            // Ensure country ISO2 is exactly 2 characters
            if (countryISO2 != null) {
                if (countryISO2.length() > 2) {
                    this.countryISO2 = countryISO2.substring(0, 2).toUpperCase();
                    logger.warning("Truncated country ISO2 code for " + swiftCode +
                            " from '" + countryISO2 + "' to '" + this.countryISO2 + "'");
                } else {
                    this.countryISO2 = countryISO2.toUpperCase();
                }
            } else {
                this.countryISO2 = "";
            }

            this.isHeadquarter = isHeadquarter;
        }

        // Get the first 8 characters to identify bank headquarters-branch relationship
        public String getBankIdentifier() {
            if (swiftCode != null && swiftCode.length() >= 8) {
                return swiftCode.substring(0, 8);
            }
            return "";
        }

        @Override
        public String toString() {
            return "SwiftCode: " + swiftCode +
                    "\nBank Name: " + bankName +
                    "\nAddress: " + address +
                    "\nCountry: " + countryName + " (" + countryISO2 + ")" +
                    "\nHeadquarter: " + (isHeadquarter ? "Yes" : "No");
        }
    }

    /**
     * Creates necessary database tables if they don't exist
     */
    private void createTables(Connection connection) throws Exception {
        Statement statement = connection.createStatement();

        // Create swift_codes table
        String createTableSQL = "CREATE TABLE IF NOT EXISTS swift_codes (" +
                "swift_code VARCHAR(20) PRIMARY KEY," +
                "bank_name VARCHAR(255) NOT NULL," +
                "address VARCHAR(255)," +
                "country_name VARCHAR(255) NOT NULL," +
                "country_iso2 VARCHAR(2) NOT NULL," +
                "is_headquarter BOOLEAN NOT NULL," +
                "bank_identifier VARCHAR(8) NOT NULL," +
                "INDEX (country_iso2)," +
                "INDEX (bank_identifier)" +
                ")";

        statement.execute(createTableSQL);
        statement.close();

        logger.info("Database tables created/verified successfully.");
    }

    /**
     * Parses Excel file to extract SWIFT codes data
     */
    /**
     * Parses Excel file to extract SWIFT codes data
     */
    private List<SwiftCodeData> parseExcelFile(String filePath) throws Exception {
        List<SwiftCodeData> swiftCodesData = new ArrayList<>();

        // Create a FileInputStream for the Excel file
        FileInputStream fileInputStream = new FileInputStream(filePath);

        // Create a workbook instance for XLSX file
        Workbook workbook = new XSSFWorkbook(fileInputStream);

        // Get the first sheet from the workbook
        Sheet sheet = workbook.getSheetAt(0);

        // Obtain an iterator over rows
        Iterator<Row> rowIterator = sheet.iterator();

        // Skip the header row if present
        if (rowIterator.hasNext()) {
            Row headerRow = rowIterator.next();
            // Print header row to help identify column structure
            StringBuilder headerInfo = new StringBuilder("Header row: ");
            for (int i = 0; i < 10; i++) { // Check first 10 columns
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    headerInfo.append(i).append(":").append(getCellValueAsString(cell)).append(", ");
                }
            }
            logger.info(headerInfo.toString());
        }

        int rowCount = 0;

        // Iterate over each row
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowCount++;

            try {
                // Adjusted column mapping based on observed data pattern
                // Column 0: Country code (e.g., AL, AW)
                // Column 1: SWIFT code (e.g., PYALALT2XXX, BDCCAWAWXXX)
                // Column 2: Address (e.g., BIC11)
                // Column 3: Bank Name (e.g., PAYSERA ALBANIA, BANCO DI CARIBE (ARUBA) N.V)
                // Column 4: Country ISO2 (e.g., PA, VO)

                String countryName = getCellValueAsString(row.getCell(6));
                String swiftCode = getCellValueAsString(row.getCell(1));
                String address = getCellValueAsString(row.getCell(4));
                String bankName = getCellValueAsString(row.getCell(3));
                String countryISO2 = getCellValueAsString(row.getCell(0));

                // Use the country code as country name if no separate country name column exists
                // You might need to adjust this logic based on your actual data

                // Debug output for the first few rows
                if (rowCount <= 5) {
                    logger.info("Row " + rowCount + ": Country=" + countryName +
                            ", Swift=" + swiftCode +
                            ", Address=" + address +
                            ", Bank=" + bankName +
                            ", ISO2=" + countryISO2);
                }

                // Skip if essential fields are missing
                if (swiftCode == null || swiftCode.isEmpty() ||
                        bankName == null || bankName.isEmpty()) {
                    logger.warning("Skipping row " + rowCount + " due to missing critical data");
                    continue;
                }

                // Ensure country ISO2 is valid
                if (countryISO2 == null || countryISO2.isEmpty()) {
                    if (countryName != null && !countryName.isEmpty() && countryName.length() <= 2) {
                        // Use country code as ISO2 if it's suitable
                        countryISO2 = countryName;
                    } else {
                        // Set a placeholder
                        countryISO2 = "XX";
                        logger.warning("Using placeholder ISO2 code for row " + rowCount);
                    }
                }

                // Apply the code identification rule:
                // Codes ending with XXX represent headquarters, otherwise branches
                boolean isHeadquarter = swiftCode.endsWith("XXX");

                SwiftCodeData swiftCodeData = new SwiftCodeData(
                        swiftCode,
                        bankName,
                        address != null ? address : "",
                        countryName != null ? countryName.toUpperCase() : "UNKNOWN",
                        countryISO2,
                        isHeadquarter
                );

                swiftCodesData.add(swiftCodeData);
            } catch (Exception e) {
                logger.warning("Error processing row " + rowCount + ": " + e.getMessage());
            }
        }

        // Close resources
        fileInputStream.close();
        workbook.close();

        logger.info("Successfully parsed " + swiftCodesData.size() + " SWIFT codes from Excel");
        return swiftCodesData;
    }

    /**
     * Stores the parsed data in the database
     */
    private int storeDataInDatabase(Connection connection, List<SwiftCodeData> swiftCodesData) throws Exception {
        // Disable auto-commit for better performance with batch operations
        connection.setAutoCommit(false);

        String sql = "INSERT INTO swift_codes (swift_code, bank_name, address, country_name, country_iso2, is_headquarter, bank_identifier) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "bank_name = VALUES(bank_name), " +
                "address = VALUES(address), " +
                "country_name = VALUES(country_name), " +
                "country_iso2 = VALUES(country_iso2), " +
                "is_headquarter = VALUES(is_headquarter), " +
                "bank_identifier = VALUES(bank_identifier)";

        PreparedStatement statement = connection.prepareStatement(sql);

        int batchSize = 100;
        int count = 0;
        int totalProcessed = 0;

        for (SwiftCodeData data : swiftCodesData) {
            statement.setString(1, data.swiftCode);
            statement.setString(2, data.bankName);
            statement.setString(3, data.address);
            statement.setString(4, data.countryName);
            statement.setString(5, data.countryISO2);
            statement.setBoolean(6, data.isHeadquarter);
            statement.setString(7, data.getBankIdentifier());

            statement.addBatch();
            count++;

            // Execute batch when it reaches the batch size
            if (count % batchSize == 0) {
                statement.executeBatch();
                connection.commit();
                totalProcessed += count;
                count = 0;
                logger.info("Processed " + totalProcessed + " records...");
            }
        }

        // Execute any remaining records in the batch
        if (count > 0) {
            statement.executeBatch();
            connection.commit();
            totalProcessed += count;
        }

        statement.close();
        logger.info("Total records processed: " + totalProcessed);
        return totalProcessed;
    }

    /**
     * Helper method to get cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            switch (cell.getCellType()) {
                case STRING:
                    String stringValue = cell.getStringCellValue();
                    if (stringValue != null) {
                        stringValue = stringValue.trim();
                        // Debug if this might be an ISO2 code
                        if (stringValue.length() > 2 && cell.getColumnIndex() == 4) {
                            logger.fine("Found long ISO2 code: " + stringValue);
                        }
                        return stringValue;
                    }
                    return "";
                case NUMERIC:
                    double numValue = cell.getNumericCellValue();
                    if (numValue == Math.floor(numValue)) {
                        return String.valueOf((int) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        String formulaStringValue = cell.getStringCellValue();
                        if (formulaStringValue != null) {
                            return formulaStringValue.trim();
                        }
                        return "";
                    } catch (Exception e) {
                        try {
                            double formulaValue = cell.getNumericCellValue();
                            if (formulaValue == Math.floor(formulaValue)) {
                                return String.valueOf((int) formulaValue);
                            } else {
                                return String.valueOf(formulaValue);
                            }
                        } catch (Exception ex) {
                            return "";
                        }
                    }
                case BLANK:
                    return "";
                default:
                    return "";
            }
        } catch (Exception e) {
            logger.warning("Error reading cell value in column " + cell.getColumnIndex() +
                    ": " + e.getMessage());
            return "";
        }
    }
}