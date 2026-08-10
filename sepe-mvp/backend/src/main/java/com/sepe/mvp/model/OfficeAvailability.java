package com.sepe.mvp.model;

public class OfficeAvailability {
    private String officeName;
    private String province;
    private String address;
    private boolean hasAppointments;
    private String distance;
    private String bookingUrl;

    public OfficeAvailability() {}

    public OfficeAvailability(String officeName, String province, String address, boolean hasAppointments, String distance, String bookingUrl) {
        this.officeName = officeName;
        this.province = province;
        this.address = address;
        this.hasAppointments = hasAppointments;
        this.distance = distance;
        this.bookingUrl = bookingUrl;
    }

    public String getOfficeName() { return officeName; }
    public void setOfficeName(String officeName) { this.officeName = officeName; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public boolean isHasAppointments() { return hasAppointments; }
    public void setHasAppointments(boolean hasAppointments) { this.hasAppointments = hasAppointments; }
    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }
    public String getBookingUrl() { return bookingUrl; }
    public void setBookingUrl(String bookingUrl) { this.bookingUrl = bookingUrl; }
}
