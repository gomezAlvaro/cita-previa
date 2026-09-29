package com.sepe.mvp.model;

/**
 * Result of probing the SEPE cita previa portal.
 * Distinguishes "portal works" from the different ways it can fail,
 * so the UI can tell the user what is actually going on.
 */
public enum PortalStatus {
    /** Portal answered correctly and quickly. */
    OK,
    /** Portal answered correctly but slowly (likely under load). */
    SLOW,
    /** Portal refused the request (HTTP 403/429): rate limiting or bot protection. */
    BLOCKED,
    /** Portal answered with an error (HTTP 4xx/5xx other than blocking). */
    DOWN,
    /** Portal could not be reached (timeout, DNS, connection error). */
    UNREACHABLE
}
