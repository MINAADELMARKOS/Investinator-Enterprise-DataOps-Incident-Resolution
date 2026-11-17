package com.investinator.security;
public final class Redaction {
    private Redaction() {}
    public static String redact(String value) {
        return value.replaceAll("(?i)(password|secret|token|api[_-]?key)\\s*[=:]\\s*\\S+","[REDACTED]")
            .replaceAll("\\b[\\w.-]+@[\\w.-]+\\.\\w+\\b","[REDACTED]");
    }
}
