package com.drbep.tvplayer;

import java.net.URI;

final class DiagnosticRedactor {
    private static final int MAX_INPUT_LENGTH = 4096;
    private static final String TRUNCATED_SUFFIX = "...[truncated]";
    private static final String[] SENSITIVE_KEY_PARTS = {
            "access_token",
            "refresh_token",
            "authorization",
            "nv-authorizations",
            "device_id",
            "signature",
            "license",
            "token",
            "auth",
            "sig",
            "key",
            "kid"
    };

    private DiagnosticRedactor() {
    }

    static String sanitizeUrl(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = boundedTrim(value);
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            URI uri = URI.create(trimmed);
            if (uri.getScheme() != null && uri.getHost() != null) {
                URI clean = new URI(
                        uri.getScheme(),
                        uri.getRawAuthority(),
                        uri.getRawPath(),
                        null,
                        null
                );
                return clean.toString();
            }
        } catch (Exception ignored) {
            // Fall through to conservative text redaction below.
        }
        return redactSensitiveText(stripQueryAndFragment(trimmed));
    }

    static String redactSensitiveText(String value) {
        String input = boundedTrim(value);
        if (input.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(input.length());
        int index = 0;
        while (index < input.length()) {
            int bearerEnd = bearerPrefixEnd(input, index);
            if (bearerEnd > index) {
                int tokenEnd = bearerEnd;
                while (tokenEnd < input.length() && isBearerTokenCharacter(input.charAt(tokenEnd))) {
                    tokenEnd++;
                }
                if (tokenEnd > bearerEnd) {
                    result.append(input, index, bearerEnd).append("<redacted>");
                    index = tokenEnd;
                    continue;
                }
            }

            if (isKeyCharacter(input.charAt(index))
                    && (index == 0 || !isKeyCharacter(input.charAt(index - 1)))) {
                int keyEnd = index;
                while (keyEnd < input.length() && isKeyCharacter(input.charAt(keyEnd))) {
                    keyEnd++;
                }
                if (keyEnd < input.length()
                        && input.charAt(keyEnd) == '='
                        && containsSensitiveKeyPart(input, index, keyEnd)) {
                    int valueEnd = keyEnd + 1;
                    while (valueEnd < input.length() && !isParameterDelimiter(input.charAt(valueEnd))) {
                        valueEnd++;
                    }
                    if (valueEnd > keyEnd + 1) {
                        result.append(input, index, keyEnd).append("=<redacted>");
                        index = valueEnd;
                        continue;
                    }
                }
            }

            result.append(input.charAt(index));
            index++;
        }
        return result.toString();
    }

    private static String boundedTrim(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) <= ' ') {
            start++;
        }
        while (end > start && value.charAt(end - 1) <= ' ') {
            end--;
        }
        if (start == end) {
            return "";
        }
        int boundedEnd = Math.min(end, start + MAX_INPUT_LENGTH);
        String trimmed = value.substring(start, boundedEnd);
        return boundedEnd < end ? trimmed + TRUNCATED_SUFFIX : trimmed;
    }

    private static int bearerPrefixEnd(String value, int start) {
        if (start + 6 >= value.length() || !value.regionMatches(true, start, "Bearer", 0, 6)) {
            return -1;
        }
        int whitespaceEnd = start + 6;
        if (!Character.isWhitespace(value.charAt(whitespaceEnd))) {
            return -1;
        }
        while (whitespaceEnd < value.length() && Character.isWhitespace(value.charAt(whitespaceEnd))) {
            whitespaceEnd++;
        }
        return whitespaceEnd;
    }

    private static boolean containsSensitiveKeyPart(String value, int start, int end) {
        for (String part : SENSITIVE_KEY_PARTS) {
            int lastStart = end - part.length();
            for (int index = start; index <= lastStart; index++) {
                if (value.regionMatches(true, index, part, 0, part.length())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isKeyCharacter(char value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9')
                || value == '_'
                || value == '.'
                || value == '-';
    }

    private static boolean isBearerTokenCharacter(char value) {
        return isKeyCharacter(value)
                || value == '~'
                || value == '+'
                || value == '/'
                || value == '=';
    }

    private static boolean isParameterDelimiter(char value) {
        return value == '&' || Character.isWhitespace(value);
    }

    private static String stripQueryAndFragment(String value) {
        if (value == null) {
            return "";
        }
        int queryIndex = value.indexOf('?');
        int fragmentIndex = value.indexOf('#');
        int cutIndex = -1;
        if (queryIndex >= 0) {
            cutIndex = queryIndex;
        }
        if (fragmentIndex >= 0 && (cutIndex < 0 || fragmentIndex < cutIndex)) {
            cutIndex = fragmentIndex;
        }
        return cutIndex >= 0 ? value.substring(0, cutIndex) : value;
    }
}
