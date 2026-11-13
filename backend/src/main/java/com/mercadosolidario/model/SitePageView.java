package com.mercadosolidario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "site_page_views")
@Getter
@Setter
public class SitePageView {

    @Id
    @Column(name = "path", length = 60)
    private String path;

    @Column(name = "views", nullable = false)
    private long views;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
