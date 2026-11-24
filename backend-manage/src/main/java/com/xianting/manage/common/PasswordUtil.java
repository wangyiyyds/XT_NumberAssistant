package com.xianting.manage.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltHex = HexFormat.of().formatHex(salt);
        return saltHex + ":" + sha256(saltHex + rawPassword);
    }

    public static boolean matches(String rawPassword, String stored) {
        if (stored == null || !stored.contains(":")) return false;
        String saltHex = stored.substring(0, stored.indexOf(':'));
        String hash = stored.substring(stored.indexOf(':') + 1);
        return MessageDigest.isEqual(
                hash.getBytes(StandardCharsets.UTF_8),
                sha256(saltHex + rawPassword).getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
