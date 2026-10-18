package com.mercadosolidario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "public_service_requests")
@Getter
@Setter
@NoArgsConstructor
public class PublicServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "protocol_hash", nullable = false, unique = true, length = 64)
    private String protocolHash;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "contact_value", length = 254)
    private String contactValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_preference", length = 10)
    private ServiceRequestContactPreference contactPreference;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "household_size", nullable = false)
    private Integer householdSize;

    @Column(name = "assistance_type", nullable = false, length = 100)
    private String assistanceType;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "consent_accepted", nullable = false)
    private Boolean consentAccepted;

    @Enumerated(EnumType.STRING)
    @Column(name = "welcome_preference", length = 10)
    private ServiceRequestWelcomePreference welcomePreference;

    @Column(name = "welcome_date")
    private java.time.LocalDate welcomeDate;

    @Column(name = "welcome_time", length = 5)
    private String welcomeTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ServiceRequestStatus status = ServiceRequestStatus.RECEBIDA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
