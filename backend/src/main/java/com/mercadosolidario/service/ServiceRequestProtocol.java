package com.mercadosolidario.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class ServiceRequestProtocol {
    private static final HexFormat HEX = HexFormat.of();

    private ServiceRequestProtocol() {
    }

    public static String normalize(String protocol) {
        return protocol == null ? "" : protocol.trim().toLowerCase(Locale.ROOT);
    }

    public static String hash(String protocol) {
        String normalized = normalize(protocol);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Protocolo inválido");
        }
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256")
                .digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    public static boolean constantTimeEquals(String first, String second) {
        return first != null && second != null
            && MessageDigest.isEqual(first.getBytes(StandardCharsets.UTF_8),
                                     second.getBytes(StandardCharsets.UTF_8));
    }
}
