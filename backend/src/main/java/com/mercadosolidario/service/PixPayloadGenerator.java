package com.mercadosolidario.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@Component
public class PixPayloadGenerator {

    public String generate(String pixKey, String merchantName, String merchantCity, BigDecimal amount) {
        String merchantAccount = field("00", "BR.GOV.BCB.PIX") + field("01", pixKey);
        String additionalData = field("05", "***");
        String payload = field("00", "01")
            + field("26", merchantAccount)
            + field("52", "0000")
            + field("53", "986")
            + field("54", amount.setScale(2).toPlainString())
            + field("58", "BR")
            + field("59", normalize(merchantName, 25))
            + field("60", normalize(merchantCity, 15))
            + field("62", additionalData)
            + "6304";
        return payload + crc16(payload);
    }

    public String generateQrCodeDataUri(String payload) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                payload,
                BarcodeFormat.QR_CODE,
                320,
                320,
                Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            );
            StringBuilder path = new StringBuilder();
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    if (matrix.get(x, y)) {
                        path.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
                    }
                }
            }
            String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 "
                + matrix.getWidth() + " " + matrix.getHeight() + "\" shape-rendering=\"crispEdges\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"white\"/>"
                + "<path d=\"" + path + "\" fill=\"black\"/></svg>";
            return "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        } catch (WriterException exception) {
            throw new IllegalStateException("Não foi possível gerar o QR Code PIX.", exception);
        }
    }

    private String field(String id, String value) {
        int length = value.getBytes(StandardCharsets.UTF_8).length;
        if (length > 99) {
            throw new IllegalArgumentException("Campo PIX excede o limite do formato BR Code.");
        }
        return id + String.format(Locale.ROOT, "%02d", length) + value;
    }

    private String normalize(String value, int maxLength) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9 $%*+\\-./:]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return normalized.substring(0, Math.min(normalized.length(), maxLength));
    }

    private String crc16(String payload) {
        int crc = 0xFFFF;
        for (byte value : payload.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (value & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format(Locale.ROOT, "%04X", crc);
    }
}
