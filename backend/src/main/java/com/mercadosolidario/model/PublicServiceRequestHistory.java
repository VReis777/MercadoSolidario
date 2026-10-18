package com.mercadosolidario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "public_service_request_history")
@Getter
@Setter
@NoArgsConstructor
public class PublicServiceRequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_request_id", nullable = false)
    private PublicServiceRequest serviceRequest;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private ServiceRequestStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private ServiceRequestStatus newStatus;

    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Column(name = "observation", nullable = false, length = 2000)
    private String observation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
