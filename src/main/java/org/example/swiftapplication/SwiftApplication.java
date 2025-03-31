package org.example.swiftapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Main application class for the SWIFT Code API
 */
@SpringBootApplication
public class SwiftApplication extends SpringBootServletInitializer {

    /**
     * Main method to start the Spring Boot application
     */
    public static void main(String[] args) {
        SpringApplication.run(SwiftApplication.class, args);
    }
}