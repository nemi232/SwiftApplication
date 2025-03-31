-- Database schema for SWIFT codes

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS swift_codes_db;
USE swift_codes_db;

-- Create swift_codes table for storing SWIFT code data
CREATE TABLE IF NOT EXISTS swift_codes (
    -- Primary identifier
                                           swift_code VARCHAR(20) PRIMARY KEY,

    -- Bank information
    bank_name VARCHAR(255) NOT NULL,
    address VARCHAR(255),

    -- Country information (stored as uppercase as per requirements)
    country_name VARCHAR(255) NOT NULL,
    country_iso2 VARCHAR(2) NOT NULL,

    -- Headquarters and branch relationship
    is_headquarter BOOLEAN NOT NULL,
    bank_identifier VARCHAR(8) NOT NULL COMMENT 'First 8 characters of SWIFT code for branch-HQ association',

    -- Created and updated timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Indexes for efficient querying
    INDEX idx_country_iso2 (country_iso2),
    INDEX idx_bank_identifier (bank_identifier),
    INDEX idx_is_headquarter (is_headquarter)
    );