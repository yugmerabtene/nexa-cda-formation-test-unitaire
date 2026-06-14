package com.nexa.usermanager.dto;

import java.util.Map;

public class ErrorResponse {
    private int status;
    private String title;
    private String detail;
    private Map<String, String> errors;

    public ErrorResponse(int status, String title, String detail) {
        this.status = status;
        this.title = title;
        this.detail = detail;
    }

    public ErrorResponse(int status, String title, String detail, Map<String, String> errors) {
        this(status, title, detail);
        this.errors = errors;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Map<String, String> getErrors() { return errors; }
    public void setErrors(Map<String, String> errors) { this.errors = errors; }
}
