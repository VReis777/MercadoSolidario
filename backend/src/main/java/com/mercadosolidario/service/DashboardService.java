package com.mercadosolidario.service;

import com.mercadosolidario.dto.dashboard.DashboardSummaryResponse;
import com.mercadosolidario.dto.dashboard.DashboardSummaryResponse.*;
import com.mercadosolidario.model.Distribution;
import com.mercadosolidario.model.Product;
import com.mercadosolidario.model.StockMovement;
import com.mercadosolidario.model.enums.*;
import com.mercadosolidario.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final long MAX_RANGE_DAYS = 366;

    private final FamilyRepository familyRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final DonationRepository donationRepository;
    private final DistributionRepository distributionRepository;
    private final DistributionItemRepository distributionItemRepository;
    private final PriorityConfigurationRepository priorityConfigurationRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(LocalDate requestedFrom, LocalDate requestedTo, boolean financialAccess) {
        LocalDate to = requestedTo == null && requestedFrom == null ? LocalDate.now() : requestedTo;
        LocalDate from = requestedFrom == null && requestedTo == null ? to.minusDays(29) : requestedFrom;
        validatePeriod(from, to);

        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        Map<String, Long> familyStatuses = new LinkedHashMap<>();
        Arrays.stream(FamilyStatus.values()).forEach(status -> familyStatuses.put(status.name(), 0L));
        familyRepository.countGroupedByStatus().forEach(row ->
            familyStatuses.put(((FamilyStatus) row[0]).name(), ((Number) row[1]).longValue()));
        long familiesInAnalysis = familyStatuses.getOrDefault(FamilyStatus.EM_ANALISE.name(), 0L);
        FamilyIndicators familyIndicators = new FamilyIndicators(
            familyStatuses.values().stream().mapToLong(Long::longValue).sum(),
            familyStatuses,
            familyRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(fromDateTime, toExclusive),
            familiesInAnalysis
        );

        List<Product> lowStock = productRepository.findLowStockProducts(PageRequest.of(0, 8));
        StockIndicators stockIndicators = new StockIndicators(
            productRepository.countByActiveTrue(),
            productRepository.countByActiveFalse(),
            productRepository.countLowStockProducts(),
            movementCounts(fromDateTime, toExclusive),
            lowStock.stream().map(product -> new LowStockProduct(
                product.getId(),
                product.getName(),
                product.getCurrentQuantity() == null ? 0 : product.getCurrentQuantity(),
                product.getUnit(),
                product.getMinimumQuantity() == null ? 0 : product.getMinimumQuantity()
            )).toList()
        );

        DonationIndicators donationIndicators = financialAccess
            ? donations(fromDateTime, toExclusive)
            : null;

        DistributionIndicators distributionIndicators = distributions(fromDateTime, toExclusive);
        List<OperationalAlert> alerts = alerts(
            productRepository.countLowStockProducts(),
            familiesInAnalysis,
            financialAccess ? donationRepository.countByStatus(DonationStatus.AGUARDANDO_PAGAMENTO) : 0
        );

        return new DashboardSummaryResponse(
            from,
            to,
            familyIndicators,
            stockIndicators,
            donationIndicators,
            distributionIndicators,
            priorityConfigurationRepository.findFirstByActiveTrueOrderByVersionNumberDesc().isPresent(),
            alerts,
            recentStockMovements(),
            recentDistributions()
        );
    }

    private Map<String, Long> movementCounts(LocalDateTime from, LocalDateTime toExclusive) {
        Map<String, Long> counts = Arrays.stream(MovementType.values())
            .collect(Collectors.toMap(MovementType::name, ignored -> 0L, (a, b) -> a, LinkedHashMap::new));
        movementRepository.countByTypeInPeriod(from, toExclusive)
            .forEach(row -> counts.put(((MovementType) row[0]).name(), ((Number) row[1]).longValue()));
        return counts;
    }

    private DonationIndicators donations(LocalDateTime from, LocalDateTime toExclusive) {
        Map<DonationStatus, Long> statusCounts = new EnumMap<>(DonationStatus.class);
        donationRepository.countGroupedByStatusInPeriod(from, toExclusive).forEach(row ->
            statusCounts.put((DonationStatus) row[0], ((Number) row[1]).longValue()));
        long productDonations = donationRepository.countByTypeAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            DonationType.PRODUTO, DonationStatus.CONFIRMADA_MANUALMENTE, from, toExclusive);
        return new DonationIndicators(
            donationRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, toExclusive),
            statusCounts.getOrDefault(DonationStatus.AGUARDANDO_PAGAMENTO, 0L),
            statusCounts.getOrDefault(DonationStatus.CONFIRMADA_MANUALMENTE, 0L),
            statusCounts.getOrDefault(DonationStatus.CANCELADA, 0L),
            statusCounts.getOrDefault(DonationStatus.EXPIRADA, 0L),
            donationRepository.sumConfirmedFinancialAmount(
                DonationType.FINANCEIRA,
                List.of(DonationStatus.PAGA, DonationStatus.CONFIRMADA_MANUALMENTE),
                from,
                toExclusive
            ),
            productDonations,
            movementRepository.countDonationEntries(
                MovementType.ENTRADA_DOACAO, "Doação de produto registrada", from, toExclusive)
        );
    }

    private DistributionIndicators distributions(LocalDateTime from, LocalDateTime toExclusive) {
        long confirmed = distributionRepository.countByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            DistributionStatus.CONFIRMADA, from, toExclusive);
        long cancelled = distributionRepository.countByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            DistributionStatus.CANCELADA, from, toExclusive);
        List<TopDistributedProduct> topProducts = distributionItemRepository.topProducts(
                DistributionStatus.CONFIRMADA, from, toExclusive, PageRequest.of(0, 5))
            .stream()
            .map(row -> new TopDistributedProduct(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((Number) row[2]).longValue(),
                (String) row[3]
            ))
            .toList();
        return new DistributionIndicators(
            confirmed,
            cancelled,
            distributionRepository.countDistinctFamiliesServed(DistributionStatus.CONFIRMADA, from, toExclusive),
            distributionItemRepository.sumDistributedQuantity(DistributionStatus.CONFIRMADA, from, toExclusive),
            topProducts
        );
    }

    private List<OperationalAlert> alerts(
            long lowStockCount,
            long familiesInAnalysis,
            long pendingDonations) {
        List<OperationalAlert> alerts = new ArrayList<>();
        if (lowStockCount > 0) {
            alerts.add(new OperationalAlert(
                "LOW_STOCK",
                "Estoque",
                lowStockCount + " produto(s) estão no nível mínimo ou abaixo dele.",
                null,
                "Consultar estoque",
                "/stock",
                "ADMIN, RH_TESOURARIA ou DIACONO"
            ));
        }
        if (familiesInAnalysis > 0) {
            alerts.add(new OperationalAlert(
                "FAMILIES_IN_ANALYSIS",
                "Famílias",
                familiesInAnalysis + " cadastro(s) aguardam análise.",
                null,
                "Consultar famílias",
                "/families",
                "ADMIN, RH_TESOURARIA ou DIACONO"
            ));
        }
        if (pendingDonations > 0) {
            alerts.add(new OperationalAlert(
                "DONATIONS_PENDING",
                "Doações",
                pendingDonations + " doação(ões) aguardam pagamento.",
                null,
                "Revisar doações",
                "/donations",
                "ADMIN ou RH_TESOURARIA"
            ));
        }
        return List.copyOf(alerts);
    }

    private List<RecentStockMovement> recentStockMovements() {
        return movementRepository.findRecent(PageRequest.of(0, 5)).stream()
            .map(movement -> recentMovement(movement))
            .toList();
    }

    private RecentStockMovement recentMovement(StockMovement movement) {
        Product product = movement.getProduct();
        return new RecentStockMovement(
            movement.getId(),
            product.getId(),
            product.getName(),
            movement.getType().name(),
            movement.getQuantity(),
            movement.getNewBalance(),
            product.getUnit(),
            movement.getCreatedAt()
        );
    }

    private List<RecentDistribution> recentDistributions() {
        List<Distribution> recent = distributionRepository.findRecent(PageRequest.of(0, 5));
        if (recent.isEmpty()) return List.of();
        Map<Long, Long> itemCounts = distributionItemRepository.countByDistributionIds(
            recent.stream().map(Distribution::getId).toList()
        ).stream().collect(Collectors.toMap(
            row -> ((Number) row[0]).longValue(),
            row -> ((Number) row[1]).longValue()
        ));
        return recent.stream().map(distribution -> new RecentDistribution(
            distribution.getId(),
            itemCounts.getOrDefault(distribution.getId(), 0L),
            distribution.getCompletedAt() == null ? distribution.getDistributionDate() : distribution.getCompletedAt()
        )).toList();
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Informe as duas datas do período.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }
        if (from.plusDays(MAX_RANGE_DAYS - 1).isBefore(to)) {
            throw new IllegalArgumentException("O período máximo permitido é de 366 dias.");
        }
    }
}
