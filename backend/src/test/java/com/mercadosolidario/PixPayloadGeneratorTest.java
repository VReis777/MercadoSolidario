package com.mercadosolidario;

import com.mercadosolidario.service.PixPayloadGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PixPayloadGeneratorTest {

    private final PixPayloadGenerator generator = new PixPayloadGenerator();

    @Test
    void buildsPixBrCodeAndLocalQrSvgFromConfiguredValues() {
        String payload = generator.generate(
            "pix@example.org", "Mercado Solidário", "São Paulo", new BigDecimal("12.50"));
        String qrDataUri = generator.generateQrCodeDataUri(payload);

        assertThat(payload).startsWith("000201");
        assertThat(payload).contains("BR.GOV.BCB.PIX", "pix@example.org", "12.50", "SAO PAULO");
        int crcField = payload.lastIndexOf("6304");
        assertThat(payload.substring(crcField + 4))
            .isEqualTo(crc16(payload.substring(0, crcField + 4)));
        assertThat(qrDataUri).startsWith("data:image/svg+xml;base64,");
    }

    private String crc16(String payload) {
        int crc = 0xFFFF;
        for (byte value : payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            crc ^= (value & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
