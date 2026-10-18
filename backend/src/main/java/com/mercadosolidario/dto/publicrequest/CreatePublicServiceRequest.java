package com.mercadosolidario.dto.publicrequest;

import com.mercadosolidario.model.ServiceRequestContactPreference;
import com.mercadosolidario.model.ServiceRequestWelcomePreference;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePublicServiceRequest {

    @Size(max = 255)
    private String fullName;

    @Size(max = 254)
    private String contactValue;

    private ServiceRequestContactPreference contactPreference;

    @NotBlank
    @Size(max = 255)
    private String location;

    @NotNull
    @Positive(message = "A quantidade deve ser maior que zero")
    private Integer householdSize;

    @NotBlank
    @Size(max = 100)
    private String assistanceType;

    @Size(max = 1000)
    private String message;

    @NotNull
    @AssertTrue(message = "O consentimento é obrigatório")
    private Boolean consentAccepted;

    private ServiceRequestWelcomePreference welcomePreference;

    private java.time.LocalDate welcomeDate;

    @Size(max = 5)
    private String welcomeTime;

    @AssertTrue(message = "Para conversar presencialmente, escolha um dos dias disponíveis e um horário aproximado")
    public boolean isWelcomeValid() {
        if (welcomePreference == null) {
            return welcomeDate == null && welcomeTime == null;
        }
        if (welcomePreference == ServiceRequestWelcomePreference.PHONE) {
            return welcomeDate == null && welcomeTime == null;
        }
        return welcomeDate != null
            && (welcomeTime == null || welcomeTime.matches("^([01]\\d|2[0-3]):[0-5]\\d$"));
    }

    @AssertTrue(message = "O contato deve corresponder à preferência informada")
    public boolean isContactValid() {
        boolean hasValue = contactValue != null && !contactValue.isBlank();
        boolean hasPreference = contactPreference != null;
        if (!hasValue && !hasPreference) {
            return true;
        }
        if (!hasValue || !hasPreference) {
            return false;
        }
        return switch (contactPreference) {
            case EMAIL -> contactValue.length() <= 254
                && contactValue.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
            case PHONE -> contactValue.matches("(?=.*\\S)[+()0-9 .-]{7,30}");
        };
    }
}
