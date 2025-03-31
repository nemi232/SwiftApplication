package org.example.swiftapplication.model;

/**
 * Error message response model for API error responses
 */
public class ErrorResponse {
    private String message;

    /**
     * Default constructor for serialization
     */
    public ErrorResponse() {
    }

    /**
     * Constructor with error message
     */
    public ErrorResponse(String message) {
        this.message = message;
    }

    /**
     * Get the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the error message
     */
    public void setMessage(String message) {
        this.message = message;
    }
}