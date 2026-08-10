package com.sepe.mvp.model;

public class ProvinceStatus {
    private String code;
    private String name;
    private String status;
    private long responseTimeMs;
    private long timestamp;
    private String bookingUrl;

    public ProvinceStatus() {}

    public ProvinceStatus(String code, String name, String status, long responseTimeMs, long timestamp, String bookingUrl) {
        this.code = code;
        this.name = name;
        this.status = status;
        this.responseTimeMs = responseTimeMs;
        this.timestamp = timestamp;
        this.bookingUrl = bookingUrl;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getBookingUrl() { return bookingUrl; }
    public void setBookingUrl(String bookingUrl) { this.bookingUrl = bookingUrl; }
}
