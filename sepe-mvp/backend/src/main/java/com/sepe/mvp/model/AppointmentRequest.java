package com.sepe.mvp.model;

public class AppointmentRequest {
    private String dni;
    private String postalCode;
    private String appointmentType;

    public AppointmentRequest() {}

    public AppointmentRequest(String dni, String postalCode, String appointmentType) {
        this.dni = dni;
        this.postalCode = postalCode;
        this.appointmentType = appointmentType;
    }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }
}
