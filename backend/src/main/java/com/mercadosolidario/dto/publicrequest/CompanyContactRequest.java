package com.mercadosolidario.dto.publicrequest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyContactRequest {

    @NotBlank(message = "Nome do responsável é obrigatório")
    @Size(max = 255)
    private String responsibleName;

    @NotBlank(message = "Nome da empresa é obrigatório")
    @Size(max = 255)
    private String companyName;

    @NotBlank(message = "Informe um e-mail de contato")
    @Email(message = "Informe um e-mail válido")
    @Size(max = 255)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 20)
    private String whatsapp;

    @NotBlank(message = "Escolha como a empresa quer participar")
    @Size(max = 50)
    private String interest;

    @Size(max = 2000)
    private String message;
}
