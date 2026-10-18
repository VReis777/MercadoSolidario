package com.mercadosolidario.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;

public record DashboardSummaryResponse(
    LocalDate from,
    LocalDate to,
    FamilyIndicators families,
    StockIndicators stock,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    DonationIndicators donations,
    DistributionIndicators distributions,
    boolean priorityConfigured,
    List<OperationalAlert> alerts,
    List<RecentStockMovement> recentStockMovements,
    List<RecentDistribution> recentDistributions
) {
    public record FamilyIndicators(
        long total,
        Map<String, Long> byStatus,
        long createdInPeriod,
        long inAnalysis
    ) {}

    public record StockIndicators(
        long activeProducts,
        long inactiveProducts,
        long lowStockProducts,
        Map<String, Long> movementsByType,
        List<LowStockProduct> lowStock
    ) {}

    public record DonationIndicators(
        long created,
        long awaitingPayment,
        long manuallyConfirmed,
        long cancelled,
        long expired,
        BigDecimal confirmedFinancialAmount,
        long productDonations,
        long donatedProductEntries
    ) {}

    public record DistributionIndicators(
        long confirmed,
        long cancelled,
        long distinctFamiliesServed,
        long itemsDistributed,
        List<TopDistributedProduct> topProducts
    ) {}

    public record LowStockProduct(Long productId, String name, Integer quantity, String unit, Integer minimum) {}

    public record TopDistributedProduct(Long productId, String productName, long quantity, String unit) {}

    public record OperationalAlert(
        String code,
        String source,
        String message,
        LocalDateTime occurredAt,
        String actionLabel,
        String actionUrl,
        String requiredRole
    ) {}

    public record RecentStockMovement(
        Long id,
        Long productId,
        String productName,
        String type,
        Integer quantity,
        Integer newBalance,
        String unit,
        LocalDateTime occurredAt
    ) {}

    public record RecentDistribution(
        Long id,
        long itemCount,
        LocalDateTime occurredAt
    ) {}
}
