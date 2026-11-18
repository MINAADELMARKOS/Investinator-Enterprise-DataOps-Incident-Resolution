package com.investinator.security;
public final class Redaction {
    private Redaction() {}
    public static Object object(Object value) {
        if(value==null||value instanceof Number||value instanceof Boolean)return value;
        if(value instanceof String text)return redact(text);
        if(value instanceof java.util.Map<?,?> map){var safe=new java.util.LinkedHashMap<String,Object>();map.forEach((key,item)->safe.put(key.toString(),key.toString().matches("(?i).*(password|secret|api[_-]?key|private[_-]?key|access[_-]?key|encryption[_-]?key|authorization|jaas).*|(?:.*[_-])?(?:token|state_key)" )?"[REDACTED]":object(item)));return safe;}
        if(value instanceof java.util.Collection<?> list)return list.stream().map(Redaction::object).toList();
        return object(com.investinator.domain.Json.decode(com.investinator.domain.Json.encode(value)));
    }
    public static String redact(String value) {
        value=value.replaceAll("(?s)-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----.*?-----END (?:RSA |EC |OPENSSH )?PRIVATE KEY-----","[REDACTED PRIVATE KEY]");
        value=value.replaceAll("(?i)(\"[^\"\\n]{0,100}(?:password|secret|token|api[_-]?key|private[_-]?key|access[_-]?key|state[_-]?key)[^\"\\n]{0,100}\"\\s*:\\s*)\"(?:\\\\.|[^\"\\\\])*\"","$1\"[REDACTED]\"");
        return value.replaceAll("(?i)Bearer\\s+[^\\s\"\']+","Bearer [REDACTED]").replaceAll("\\bsk-[A-Za-z0-9_-]{12,}","[REDACTED]").replaceAll("(?i)(password|secret|token|api[_-]?key)\\s*[=:]\\s*\\S+","[REDACTED]")
            .replaceAll("\\b[\\w.-]+@[\\w.-]+\\.\\w+\\b","[REDACTED]");
    }
}
