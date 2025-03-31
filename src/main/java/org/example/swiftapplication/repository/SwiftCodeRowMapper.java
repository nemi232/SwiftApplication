package org.example.swiftapplication.repository;

import org.example.swiftapplication.util.SwiftCode;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Row mapper for converting database rows to SwiftCode objects
 */
@Component
public class SwiftCodeRowMapper implements RowMapper<SwiftCode> {

    @Override
    public SwiftCode mapRow(ResultSet rs, int rowNum) throws SQLException {
        SwiftCode swiftCode = new SwiftCode();
        swiftCode.setSwiftCode(rs.getString("swift_code"));
        swiftCode.setBankName(rs.getString("bank_name"));
        swiftCode.setAddress(rs.getString("address"));
        swiftCode.setCountryName(rs.getString("country_name"));
        swiftCode.setCountryISO2(rs.getString("country_iso2"));
        swiftCode.setHeadquarter(rs.getBoolean("is_headquarter"));
        swiftCode.setBankIdentifier(rs.getString("bank_identifier"));
        return swiftCode;
    }
}