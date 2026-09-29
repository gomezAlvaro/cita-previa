package com.sepe.mvp.model;

/** One probe of the SEPE portal: status, how long it took, and when it happened. */
public record PortalCheck(PortalStatus status, long responseTimeMs, long timestamp) {}
