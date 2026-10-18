package com.mercadosolidario.dto.stock;

import java.util.List;

public record StockSummaryResponse(
    long activeProducts,
    long lowStockProducts,
    List<ProductResponse> products
) {}
