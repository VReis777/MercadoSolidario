package com.mercadosolidario.service;

import com.mercadosolidario.dto.history.CampaignOverviewResponse;
import com.mercadosolidario.dto.history.CampaignRegistrationResponse;
import com.mercadosolidario.dto.history.CampaignRegistrationResponse.HistoryMatch;
import com.mercadosolidario.dto.history.HistoricalBeneficiaryResponse;
import com.mercadosolidario.model.Family;
import com.mercadosolidario.model.HistoricalBeneficiary;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.repository.HistoricalBeneficiaryRepository;
import com.mercadosolidario.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

import static com.mercadosolidario.util.SpreadsheetText.*;

/** Consultas da página de auditoria: histórico importado, cadastros da campanha e alertas automáticos. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignAuditService {

    private static final int MAX_PAGE_SIZE = 100;

    private final HistoricalBeneficiaryRepository historicalRepository;
    private final FamilyRepository familyRepository;
    private final ProductRepository productRepository;

    public int currentCampaignYear() {
        return Year.now().getValue();
    }

    public Page<HistoricalBeneficiaryResponse> searchHistory(Integer year, String search, String origin, int page, int size) {
        Specification<HistoricalBeneficiary> spec = Specification.where(null);
        if (year != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("campaignYear"), year));
        if (search != null && !search.isBlank()) {
            String cpf = digits(search).length() == 11 ? digits(search) : null;
            String name = normalize(search);
            spec = spec.and(cpf != null
                ? (root, q, cb) -> cb.equal(root.get("cpf"), cpf)
                : (root, q, cb) -> cb.like(root.get("normalizedName"), "%" + name + "%"));
        }
        if (origin != null && !origin.isBlank()) {
            String o = origin.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("origin")), o));
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size),
            Sort.by(Sort.Order.desc("campaignYear"), Sort.Order.asc("fullName")));
        Page<HistoricalBeneficiary> result = historicalRepository.findAll(spec, pageable);

        FamilyIndex current = familyIndex(familiesOf(currentCampaignYear()));
        return result.map(h -> new HistoricalBeneficiaryResponse(
            h.getId(), h.getCampaignYear(), h.getFullName(), maskCpf(h.getCpf()), maskPhone(h.getPhone()),
            h.getHouseholdSize(), h.getOrigin(), h.getSourceFile(), h.getSourceSheet(), h.getSourceRow(),
            (h.getCpf() != null && current.cpfs().contains(h.getCpf())) || current.names().contains(h.getNormalizedName())));
    }

    public Page<CampaignRegistrationResponse> registrations(Integer year, String search, FamilyStatus status,
                                                            boolean onlyWithHistory, int page, int size) {
        int campaign = year == null ? currentCampaignYear() : year;
        String name = search == null ? "" : normalize(search);
        String cpf = search != null && digits(search).length() == 11 ? digits(search) : null;
        List<Family> families = familiesOf(campaign).stream()
            .filter(f -> status == null || f.getStatus() == status)
            .filter(f -> name.isEmpty() || (cpf != null ? cpf.equals(f.getCpf()) : normalize(f.getResponsibleName()).contains(name)))
            .sorted(Comparator.comparing(Family::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        HistoryLookup history = historyLookup(families);
        List<CampaignRegistrationResponse> rows = families.stream()
            .map(f -> toRegistration(f, history))
            .filter(r -> !onlyWithHistory || r.historyMatch() != HistoryMatch.NENHUM)
            .toList();

        int pageSize = clampSize(size);
        int from = Math.min(Math.max(page, 0) * pageSize, rows.size());
        return new PageImpl<>(rows.subList(from, Math.min(from + pageSize, rows.size())),
            PageRequest.of(Math.max(page, 0), pageSize), rows.size());
    }

    public CampaignOverviewResponse overview(Integer year) {
        int campaign = year == null ? currentCampaignYear() : year;
        Map<Integer, Long> byYear = toYearMap(historicalRepository.countByYear());
        Map<Integer, Long> withoutCpf = toYearMap(historicalRepository.countWithoutCpfByYear());
        long inMoreThanOneYear = historicalRepository.countPeopleInMoreThanOneYear();

        List<Family> families = familiesOf(campaign);
        HistoryLookup history = historyLookup(families);
        long byCpf = 0, byName = 0, people = 0, waiting = 0;
        for (Family f : families) {
            HistoryMatch match = toRegistration(f, history).historyMatch();
            if (match == HistoryMatch.CPF) byCpf++;
            if (match == HistoryMatch.NOME) byName++;
            people += f.getHouseholdSize() == null ? 0 : f.getHouseholdSize();
            if (f.getStatus() == FamilyStatus.CADASTRADA) waiting++;
        }
        long activeProducts = productRepository.countByActiveTrue();
        long withoutStock = productRepository.countByActiveTrueAndCurrentQuantityLessThanEqual(0);

        List<String> alerts = new ArrayList<>();
        if (byYear.isEmpty()) {
            alerts.add("Histórico de campanhas anteriores ainda não importado: o cruzamento automático com cadastros fica indisponível.");
        }
        withoutCpf.forEach((y, n) -> alerts.add(n + " registro(s) de " + y
            + " sem CPF: o cruzamento é feito só pelo nome e precisa de conferência manual."));
        if (byCpf > 0) alerts.add(byCpf + " família(s) cadastrada(s) em " + campaign
            + " já foram contempladas antes (mesmo CPF): encaminhar para triagem antes de aprovar.");
        if (byName > 0) alerts.add(byName + " família(s) cadastrada(s) em " + campaign
            + " têm o mesmo nome de alguém contemplado antes: conferir se é a mesma pessoa.");
        if (inMoreThanOneYear > 0) alerts.add(inMoreThanOneYear + " pessoa(s) aparecem em mais de uma campanha anterior.");
        if (waiting > 0) alerts.add(waiting + " cadastro(s) de " + campaign + " ainda com status CADASTRADA (aguardando análise).");
        if (activeProducts == 0) alerts.add("Nenhum item cadastrado na lista da cesta.");
        else if (withoutStock > 0) alerts.add(withoutStock + " item(ns) da cesta sem estoque registrado.");

        return new CampaignOverviewResponse(campaign, byYear, withoutCpf, inMoreThanOneYear, families.size(),
            byCpf, byName, people, activeProducts, withoutStock, historicalRepository.findDistinctOrigins(), alerts);
    }

    private CampaignRegistrationResponse toRegistration(Family f, HistoryLookup history) {
        Set<Integer> cpfYears = f.getCpf() == null ? Set.of() : history.yearsByCpf().getOrDefault(f.getCpf(), Set.of());
        Set<Integer> nameYears = history.yearsByName().getOrDefault(normalize(f.getResponsibleName()), Set.of());
        HistoryMatch match = !cpfYears.isEmpty() ? HistoryMatch.CPF : !nameYears.isEmpty() ? HistoryMatch.NOME : HistoryMatch.NENHUM;
        TreeSet<Integer> years = new TreeSet<>(cpfYears);
        years.addAll(nameYears);
        return new CampaignRegistrationResponse(f.getId(), f.getResponsibleName(), maskCpf(f.getCpf()),
            f.getHouseholdSize(), f.getTotalIncome(), f.getOrigin() == null ? null : f.getOrigin().name(),
            f.getReferrerName(), f.getStatus() == null ? null : f.getStatus().name(), f.getCreatedAt(),
            match, List.copyOf(years));
    }

    private HistoryLookup historyLookup(List<Family> families) {
        Set<String> cpfs = families.stream().map(Family::getCpf).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> names = families.stream().map(f -> normalize(f.getResponsibleName())).collect(Collectors.toSet());
        Map<String, Set<Integer>> byCpf = new HashMap<>();
        Map<String, Set<Integer>> byName = new HashMap<>();
        if (!cpfs.isEmpty()) {
            historicalRepository.findByCpfIn(cpfs).forEach(h ->
                byCpf.computeIfAbsent(h.getCpf(), k -> new TreeSet<>()).add(h.getCampaignYear()));
        }
        if (!names.isEmpty()) {
            historicalRepository.findByNormalizedNameIn(names).forEach(h ->
                byName.computeIfAbsent(h.getNormalizedName(), k -> new TreeSet<>()).add(h.getCampaignYear()));
        }
        return new HistoryLookup(byCpf, byName);
    }

    private List<Family> familiesOf(int year) {
        return familyRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime.of(year, 1, 1, 0, 0), LocalDateTime.of(year + 1, 1, 1, 0, 0));
    }

    private static FamilyIndex familyIndex(List<Family> families) {
        return new FamilyIndex(
            families.stream().map(Family::getCpf).filter(Objects::nonNull).collect(Collectors.toSet()),
            families.stream().map(f -> normalize(f.getResponsibleName())).collect(Collectors.toSet()));
    }

    private static Map<Integer, Long> toYearMap(List<Object[]> rows) {
        Map<Integer, Long> map = new TreeMap<>();
        rows.forEach(r -> map.put(((Number) r[0]).intValue(), ((Number) r[1]).longValue()));
        return map;
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private record HistoryLookup(Map<String, Set<Integer>> yearsByCpf, Map<String, Set<Integer>> yearsByName) {}

    private record FamilyIndex(Set<String> cpfs, Set<String> names) {}
}
