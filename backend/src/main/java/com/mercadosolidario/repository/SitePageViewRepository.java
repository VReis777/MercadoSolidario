package com.mercadosolidario.repository;

import com.mercadosolidario.model.SitePageView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SitePageViewRepository extends JpaRepository<SitePageView, String> {

    List<SitePageView> findAllByOrderByViewsDesc();
}
