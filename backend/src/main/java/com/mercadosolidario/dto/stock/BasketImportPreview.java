package com.mercadosolidario.dto.stock;

import java.util.List;

public record BasketImportPreview(
    String fileName,
    int totalItems,
    int newItems,
    int existingItems,
    int duplicateItems,
    List<Item> items,
    List<String> unmatchedBrands,
    List<String> warnings
) {
    public enum ItemStatus { NOVO, JA_CADASTRADO, DUPLICADO_NO_ARQUIVO }

    public record Item(String name, ItemStatus status, String preferredBrand) {}
}
