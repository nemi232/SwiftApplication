package org.example.swiftapplication.util;

import java.util.ArrayList;
import java.util.List;

public class SwiftCode {
    private String swiftCode;
    private String bankName;
    private String address;
    private String countryISO2;
    private String countryName;
    private boolean isHeadquarter;
    private String bankIdentifier;
    private List<SwiftCode> branches = new ArrayList<>();

    // Constructors
    public SwiftCode() {
    }

    // Getters and Setters
    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCountryISO2() {
        return countryISO2;
    }

    public void setCountryISO2(String countryISO2) {
        this.countryISO2 = countryISO2;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public boolean isHeadquarter() {
        return isHeadquarter;
    }

    public void setHeadquarter(boolean headquarter) {
        isHeadquarter = headquarter;
    }

    public String getBankIdentifier() {
        return bankIdentifier;
    }

    public void setBankIdentifier(String bankIdentifier) {
        this.bankIdentifier = bankIdentifier;
    }

    public List<SwiftCode> getBranches() {
        return branches;
    }

    public void setBranches(List<SwiftCode> branches) {
        this.branches = branches;
    }

    public void addBranch(SwiftCode branch) {
        if (branches == null) {
            branches = new ArrayList<>();
        }
        branches.add(branch);
    }
}