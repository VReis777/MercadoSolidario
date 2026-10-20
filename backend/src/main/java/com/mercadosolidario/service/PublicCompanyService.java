package com.mercadosolidario.service;

import com.mercadosolidario.dto.publicrequest.CompanyContactRequest;
import com.mercadosolidario.dto.publicrequest.SiteInfoResponse;
import com.mercadosolidario.dto.publicrequest.SiteInfoUpdateRequest;
import com.mercadosolidario.model.Product;
import com.mercadosolidario.model.Sponsor;
import com.mercadosolidario.model.SystemSetting;
import com.mercadosolidario.model.enums.SponsorType;
import com.mercadosolidario.repository.ProductRepository;
import com.mercadosolidario.repository.SponsorRepository;
import com.mercadosolidario.repository.SystemSettingRepository;
import com.mercadosolidario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicCompanyService {

    private static final String INSTAGRAM_URL_KEY = "INSTAGRAM_URL";
    private static final String WHATSAPP_GROUP_URL_KEY = "WHATSAPP_GROUP_URL";
    private static final String DEFAULT_INSTAGRAM =
        "https://www.instagram.com/mercadosolidariopetropolis";
    private static final Map<String, String> INTEREST_LABELS = Map.of(
        "PATROCINAR", "Quero patrocinar uma ação",
        "DOAR_MAIS", "Quero doar valores ou produtos",
        "DOAR_ITENS", "Quero doar itens da lista de compras",
        "SER_PARCEIRO", "Quero ser parceiro do projeto");

    private final SponsorRepository sponsorRepository;
    private final ProductRepository productRepository;
    private final SystemSettingRepository settingRepository;
    private final UserRepository userRepository;
    private final InvitationEmailSender emailSender;
    private final AuditService auditService;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Transactional(readOnly = true)
    public SiteInfoResponse siteInfo() {
        String instagram = settingRepository.findByKey(INSTAGRAM_URL_KEY)
            .map(SystemSetting::getValue).filter(value -> !value.isBlank())
            .orElse(DEFAULT_INSTAGRAM);
        String whatsapp = settingRepository.findByKey(WHATSAPP_GROUP_URL_KEY)
            .map(SystemSetting::getValue).filter(value -> !value.isBlank())
            .orElse(null);
        return new SiteInfoResponse(instagram, whatsapp);
    }

    @Transactional
    public SiteInfoResponse updateSiteInfo(SiteInfoUpdateRequest request) {
        saveSetting(WHATSAPP_GROUP_URL_KEY,
            request.getWhatsappGroupUrl() == null ? "" : request.getWhatsappGroupUrl(),
            "Link do grupo de WhatsApp exibido na página de empresas");
        if (request.getInstagramUrl() != null) {
            saveSetting(INSTAGRAM_URL_KEY, request.getInstagramUrl(),
                "Link do Instagram exibido nas páginas públicas");
        }
        auditService.log("SITE_INFO_UPDATED", "system_setting", null,
            "whatsappGroupUrl=" + (request.getWhatsappGroupUrl() != null ? "definido" : "mantido"));
        return siteInfo();
    }

    @Transactional
    public boolean registerContact(CompanyContactRequest request) {
        String interestLabel = INTEREST_LABELS.get(request.getInterest().trim());
        if (interestLabel == null) {
            throw new IllegalArgumentException("Escolha como a empresa quer participar");
        }
        Sponsor sponsor = new Sponsor();
        sponsor.setName(request.getResponsibleName().trim());
        sponsor.setType(SponsorType.EMPRESA);
        sponsor.setCompanyName(request.getCompanyName().trim());
        sponsor.setPhone(trimToNull(request.getPhone()));
        sponsor.setWhatsapp(trimToNull(request.getWhatsapp()));
        sponsor.setEmail(request.getEmail().trim());
        sponsor.setObservations(("Interesse: " + interestLabel
            + (request.getMessage() != null && !request.getMessage().isBlank()
                ? " | Mensagem: " + request.getMessage().trim() : "")));
        sponsor.setActive(true);
        sponsorRepository.save(sponsor);
        auditService.log("COMPANY_CONTACT", "Sponsor", sponsor.getId(),
            "Empresa interessada registrada pela página pública.");

        return notifyTeam(request, interestLabel);
    }

    private boolean notifyTeam(CompanyContactRequest request, String interestLabel) {
        Set<String> recipients = new LinkedHashSet<>();
        userRepository.findActiveByRoleNames(List.of("ADMIN", "RH_TESOURARIA")).stream()
            .map(user -> user.getEmail())
            .filter(email -> email != null && !email.isBlank())
            .forEach(recipients::add);
        if (fromAddress != null && !fromAddress.isBlank()) {
            recipients.add(fromAddress);
        }
        if (recipients.isEmpty()) {
            return false;
        }
        boolean itemDonation = "DOAR_ITENS".equals(request.getInterest().trim());
        try {
            StringBuilder items = new StringBuilder();
            if (itemDonation) {
                items.append("\nITENS NECESSÁRIOS NO MOMENTO (lista do estoque):\n");
                List<Product> products = productRepository.findByActiveTrue();
                if (products.isEmpty()) {
                    items.append("- nenhum item cadastrado ainda\n");
                } else {
                    products.stream()
                        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                        .forEach(p -> items.append("- ").append(p.getName())
                            .append(" (").append(p.getUnit()).append(") — em estoque: ")
                            .append(p.getCurrentQuantity() == null ? 0 : p.getCurrentQuantity())
                            .append('\n'));
                }
            }
            emailSender.sendToMany(recipients.toArray(String[]::new),
                itemDonation
                    ? "URGENTE: empresa quer doar itens - Mercado Solidário"
                    : "Empresa interessada em apoiar - Mercado Solidário",
                """
                %sUma empresa pediu contato pela página "Sou empresa".

                Empresa: %s
                Responsável: %s
                E-mail: %s
                Telefone: %s
                WhatsApp: %s
                Como quer participar: %s

                Mensagem:
                %s
                %s
                O registro já está na lista de Patrocinadores e Parceiros do sistema.
                """.formatted(
                    itemDonation
                        ? "ATENÇÃO: a empresa quer doar ITENS. Entrem em contato com urgência " +
                          "(e-mail/WhatsApp) ANTES da compra, para combinar quais itens e " +
                          "quantidades a lista realmente precisa — evitando itens fora da lista " +
                          "ou em excesso.\n\n"
                        : "",
                    request.getCompanyName(),
                    request.getResponsibleName(),
                    request.getEmail(),
                    blank(request.getPhone()),
                    blank(request.getWhatsapp()),
                    interestLabel,
                    request.getMessage() != null && !request.getMessage().isBlank()
                        ? request.getMessage() : "(sem mensagem)",
                    items));
            return true;
        } catch (RuntimeException exception) {
            log.warn("Company contact saved but team notification e-mail was not sent.");
            return false;
        }
    }

    private void saveSetting(String key, String value, String description) {
        if (value == null) {
            return;
        }
        SystemSetting setting = settingRepository.findByKey(key).orElseGet(() -> {
            SystemSetting created = new SystemSetting();
            created.setKey(key);
            return created;
        });
        setting.setValue(value.trim());
        setting.setDescription(description);
        settingRepository.save(setting);
    }

    private String blank(String value) {
        return value != null && !value.isBlank() ? value : "não informado";
    }

    private String trimToNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
