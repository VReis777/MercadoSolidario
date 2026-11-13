package com.mercadosolidario.service;

import com.mercadosolidario.model.SitePageView;
import com.mercadosolidario.repository.SitePageViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Contador de acessos por pagina publica — sem cookies nem dados pessoais. */
@Service
@RequiredArgsConstructor
public class SiteStatsService {

    /** Somente rotas publicas conhecidas entram na contagem. */
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/", "/nossahistoria", "/conheca", "/doacao", "/rum", "/sou-empresa",
        "/rum/verificar");

    private final SitePageViewRepository repository;

    @Transactional
    public void recordView(String rawPath) {
        String path = normalize(rawPath);
        if (path == null) {
            return;
        }
        SitePageView view = repository.findById(path).orElseGet(() -> {
            SitePageView created = new SitePageView();
            created.setPath(path);
            created.setViews(0);
            return created;
        });
        view.setViews(view.getViews() + 1);
        view.setUpdatedAt(LocalDateTime.now());
        repository.save(view);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        List<SitePageView> pages = repository.findAllByOrderByViewsDesc();
        long total = pages.stream().mapToLong(SitePageView::getViews).sum();
        return Map.of(
            "total", total,
            "pages", pages.stream()
                .map(p -> Map.of("path", p.getPath(), "views", p.getViews()))
                .toList());
    }

    private String normalize(String rawPath) {
        if (rawPath == null) {
            return null;
        }
        String path = rawPath.split("[?#]")[0].trim();
        if (path.isEmpty()) {
            path = "/";
        }
        // /rum/verificar/ABC vira /rum/verificar (nao vaza codigo na estatistica)
        if (path.startsWith("/rum/verificar/")) {
            path = "/rum/verificar";
        }
        return PUBLIC_PATHS.contains(path) ? path : null;
    }
}
