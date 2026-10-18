package com.mercadosolidario.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

/** Helpers para ler células de planilhas e normalizar textos usados em comparações. */
public final class SpreadsheetText {

    private static final DataFormatter FORMATTER = new DataFormatter(Locale.forLanguageTag("pt-BR"));

    private SpreadsheetText() {
    }

    /** Texto da célula; números inteiros grandes (CPF/telefone) saem sem notação científica. */
    public static String cellText(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        if (type == CellType.NUMERIC) {
            BigDecimal value = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
            return value.scale() <= 0 ? value.toPlainString() : FORMATTER.formatCellValue(cell).trim();
        }
        if (type == CellType.STRING) return cell.getStringCellValue().trim();
        return FORMATTER.formatCellValue(cell).trim();
    }

    /** Maiúsculas, sem acentos e com espaços simples — usado para comparar nomes. */
    public static String normalize(String value) {
        if (value == null) return "";
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9/ ]", " ").replaceAll("\\s+", " ").trim();
    }

    public static String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    /** CPF com 11 dígitos (completa zeros à esquerda perdidos pelo Excel) ou null se inválido. */
    public static String validCpfOrNull(String raw) {
        String digits = digits(raw);
        if (digits.isEmpty() || digits.length() > 11) return null;
        String padded = "0".repeat(11 - digits.length()) + digits;
        return CpfValidator.isValid(padded) ? padded : null;
    }

    /** Telefone só com dígitos, sem DDI 55 nem zero de operadora; null se curto demais. */
    public static String phoneOrNull(String raw) {
        String digits = digits(raw);
        if (digits.startsWith("55") && digits.length() >= 12) digits = digits.substring(2);
        while (digits.startsWith("0")) digits = digits.substring(1);
        if (digits.length() < 8) return null;
        return digits.length() > 11 ? digits.substring(digits.length() - 11) : digits;
    }

    public static String maskCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return null;
        return "***.***." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return null;
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }

    public static String truncate(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
