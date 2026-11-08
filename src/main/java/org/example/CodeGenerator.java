package org.example;

import java.security.SecureRandom;
import java.util.Base64;

public final class CodeGenerator {
    // 6 случайных байт = 8 base64-символов
    private static final int BYTE_COUNT = 6;

    private final SecureRandom random;

    public CodeGenerator() {
        random = new SecureRandom();
    }

    public String next() {
        var buffer = new byte[BYTE_COUNT];
        random.nextBytes(buffer);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(buffer);
    }
}
