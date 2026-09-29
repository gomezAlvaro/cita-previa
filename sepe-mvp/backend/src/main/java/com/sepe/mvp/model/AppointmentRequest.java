package com.sepe.mvp.model;

public class AppointmentRequest {
    private String postalCode;

    public AppointmentRequest() {}

    public AppointmentRequest(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
}
