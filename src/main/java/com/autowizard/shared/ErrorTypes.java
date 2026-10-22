package com.autowizard.shared;

import java.net.URI;

/** The {@code type} URIs of the problem details returned by the API (RFC 7807). */
public final class ErrorTypes {

    private ErrorTypes() {
    }

    private static final String BASE_URL = "https://autowizard.example/errors/";

    public static final URI NOT_FOUND = URI.create(BASE_URL + "not-found");
    public static final URI CONFLICT = URI.create(BASE_URL + "conflict");
    public static final URI BUSINESS_RULE = URI.create(BASE_URL + "business-rule");
    public static final URI UNSUPPORTED_FILE = URI.create(BASE_URL + "unsupported-file");
    public static final URI VALIDATION = URI.create(BASE_URL + "validation-error");
    public static final URI UNAUTHORIZED = URI.create(BASE_URL + "unauthorized");
    public static final URI FORBIDDEN = URI.create(BASE_URL + "forbidden");
    public static final URI INTERNAL = URI.create(BASE_URL + "internal-server-error");
}
