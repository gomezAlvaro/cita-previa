package com.sepe.mvp.model;

/**
 * A province suggested for the user's postal code.
 * {@code nearby} is false for the province of the postal code itself.
 * {@code portalStatus} is the state of the (single, national) SEPE portal
 * at the time of the check; it does not say whether appointments are free.
 */
public record SearchResult(
        String provinceCode,
        String provinceName,
        boolean nearby,
        PortalStatus portalStatus,
        String bookingUrl) {}
