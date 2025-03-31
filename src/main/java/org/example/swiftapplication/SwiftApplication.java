package org.example.swiftapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SwiftApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(SwiftApplication.class, args);
    }
}