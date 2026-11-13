package com.mercadosolidario.controller;

import com.mercadosolidario.service.SiteStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Contador publico de acessos (somente registro; consulta e restrita a admin/RH). */
@RestController
@RequestMapping("/api/public/site-stats")
@RequiredArgsConstructor
public class PublicSiteStatsController {

    private final SiteStatsService statsService;

    @PostMapping("/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordView(@RequestBody(required = false) Map<String, String> body) {
        statsService.recordView(body != null ? body.get("path") : null);
    }
}
