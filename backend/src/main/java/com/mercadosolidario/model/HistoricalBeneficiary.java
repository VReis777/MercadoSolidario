package com.mercadosolidario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro histórico de uma família contemplada em campanha anterior, importado de planilha.
 * Fica separado de {@link Family} para não misturar contemplação passada com cadastro atual.
 */
@Entity
@Table(name = "historical_beneficiaries")
@Getter
@Setter
@NoArgsConstructor
public class HistoricalBeneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_year", nullable = false)
    private Integer campaignYear;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "normalized_name", nullable = false, length = 255)
    private String normalizedName;

    @Column(name = "cpf", length = 11)
    private String cpf;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "household_size")
    private Integer householdSize;

    @Column(name = "origin", length = 255)
    private String origin;

    @Column(name = "source_file", nullable = false, length = 255)
    private String sourceFile;

    @Column(name = "source_sheet", length = 100)
    private String sourceSheet;

    @Column(name = "source_row")
    private Integer sourceRow;

    @Column(name = "imported_by", length = 100)
    private String importedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((HistoricalBeneficiary) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
