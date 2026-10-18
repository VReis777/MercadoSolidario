package com.mercadosolidario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadosolidario.model.Family;
import com.mercadosolidario.model.HistoricalBeneficiary;
import com.mercadosolidario.model.Product;
import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.RequestReason;
import com.mercadosolidario.repository.AuditLogRepository;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.repository.HistoricalBeneficiaryRepository;
import com.mercadosolidario.repository.ProductRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dados 100% fictícios: nomes inventados e CPFs de exemplo válidos apenas pelo dígito verificador. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CampaignAuditTest {

    private static final String CPF_A = "52998224725";
    private static final String CPF_B = "11144477735";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private HistoricalBeneficiaryRepository historicalRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @BeforeEach
    void clean() {
        historicalRepository.deleteAll();
    }

    @Test
    void anonymousCannotReadHistory() throws Exception {
        mockMvc.perform(get("/api/historical-beneficiaries")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/campaign/overview")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "diacono", roles = "DIACONO")
    void diaconoCanReadButCannotImport() throws Exception {
        mockMvc.perform(get("/api/historical-beneficiaries")).andExpect(status().isOk());
        mockMvc.perform(get("/api/campaign/overview")).andExpect(status().isOk());
        mockMvc.perform(get("/api/campaign/registrations")).andExpect(status().isOk());
        mockMvc.perform(multipart("/api/historical-beneficiaries/import/preview").file(listagemFile()).param("year", "2025"))
            .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/products/import").file(basketFile()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void previewDetectsDuplicatesAndInvalidCpfWithoutSaving() throws Exception {
        JsonNode preview = json(mockMvc.perform(multipart("/api/historical-beneficiaries/import/preview")
                .file(listagemFile()).param("year", "2025"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(preview.get("totalRows").asInt()).isEqualTo(3);
        assertThat(preview.get("newRows").asInt()).isEqualTo(2);
        assertThat(preview.get("duplicateInFile").asInt()).isEqualTo(1);
        assertThat(preview.get("withoutCpf").asInt()).isEqualTo(1);
        JsonNode first = preview.get("rows").get(0);
        assertThat(first.get("origin").asText()).isEqualTo("Projeto Exemplo");
        assertThat(first.get("cpfMasked").asText()).isEqualTo("***.***.247-25");
        assertThat(first.get("phoneMasked").asText()).endsWith("1234");
        assertThat(historicalRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void confirmSavesOnlyNewRowsAndIsIdempotent() throws Exception {
        mockMvc.perform(multipart("/api/historical-beneficiaries/import").file(listagemFile()).param("year", "2025"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imported").value(2))
            .andExpect(jsonPath("$.skippedDuplicates").value(1));

        mockMvc.perform(multipart("/api/historical-beneficiaries/import").file(listagemFile()).param("year", "2025"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imported").value(0))
            .andExpect(jsonPath("$.skippedAlreadyImported").value(2));

        assertThat(historicalRepository.count()).isEqualTo(2);
        assertThat(auditLogRepository.findByAction("HISTORICAL_BENEFICIARIES_IMPORTED")).hasSize(2);

        mockMvc.perform(get("/api/historical-beneficiaries").param("year", "2025").param("search", "maria"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].cpfMasked").value("***.***.247-25"));
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void readsRegistrationFormHeadersAndHeaderlessSheet() throws Exception {
        JsonNode preview = json(mockMvc.perform(multipart("/api/historical-beneficiaries/import/preview")
                .file(registrationFormFile()).param("year", "2024"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(preview.get("totalRows").asInt()).isEqualTo(3);
        assertThat(preview.get("newRows").asInt()).isEqualTo(2);
        assertThat(preview.get("duplicateInFile").asInt()).isEqualTo(1);
        JsonNode first = preview.get("rows").get(0);
        assertThat(first.get("householdSize").asInt()).isEqualTo(5);
        assertThat(first.get("origin").asText()).isEqualTo("Projeto Exemplo");
        assertThat(preview.get("sheetWarnings").toString()).contains("sem cabeçalho");
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void rejectsFutureYearAndNonExcelFile() throws Exception {
        mockMvc.perform(multipart("/api/historical-beneficiaries/import/preview")
                .file(listagemFile()).param("year", String.valueOf(Year.now().getValue() + 1)))
            .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/historical-beneficiaries/import/preview")
                .file(new MockMultipartFile("file", "lista.csv", "text/csv", "a;b".getBytes())).param("year", "2025"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void registrationsAreCrossCheckedWithHistory() throws Exception {
        mockMvc.perform(multipart("/api/historical-beneficiaries/import").file(listagemFile()).param("year", "2025"))
            .andExpect(status().isOk());
        saveFamily("Maria Exemplo da Silva", CPF_A);
        saveFamily("Pessoa Sem Historico", CPF_B);

        mockMvc.perform(get("/api/campaign/registrations").param("onlyWithHistory", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].historyMatch").value("CPF"))
            .andExpect(jsonPath("$.content[0].historyYears[0]").value(2025))
            .andExpect(jsonPath("$.content[0].cpfMasked").value("***.***.247-25"));

        mockMvc.perform(get("/api/campaign/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.registeredWithHistoryByCpf").value(1))
            .andExpect(jsonPath("$.historicalByYear.2025").value(2))
            .andExpect(jsonPath("$.historicalWithoutCpfByYear.2025").value(1));
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void basketImportCreatesZeroStockItemsWithPreferredBrands() throws Exception {
        Product existing = new Product();
        existing.setName("Arroz");
        existing.setUnit("kg");
        productRepository.save(existing);

        JsonNode preview = json(mockMvc.perform(multipart("/api/products/import/preview").file(basketFile()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(preview.get("newItems").asInt()).isEqualTo(2);
        assertThat(preview.get("existingItems").asInt()).isEqualTo(1);
        assertThat(preview.get("duplicateItems").asInt()).isEqualTo(1);
        assertThat(preview.get("unmatchedBrands").toString()).contains("ROSQUINHA MABEL");

        mockMvc.perform(multipart("/api/products/import").file(basketFile()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(2));

        Product sugar = productRepository.findByNameContainingIgnoreCase("AÇUCAR").get(0);
        assertThat(sugar.getUnit()).isEqualTo("un");
        assertThat(sugar.getCurrentQuantity()).isZero();
        assertThat(sugar.getNotes()).isEqualTo("Marca preferida: NEVE/UNIÃO");
        assertThat(productRepository.findByNameContainingIgnoreCase("Arroz").get(0).getUnit()).isEqualTo("kg");
    }

    @Test
    @WithMockUser(username = "diacono", roles = "DIACONO")
    void diaconoCannotImportRegistrations() throws Exception {
        mockMvc.perform(multipart("/api/campaign/registrations/import/preview").file(registrationsFile()))
            .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/campaign/registrations/import").file(registrationsFile()))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/campaign/registrations/template")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void registrationsPreviewValidatesRequiredFields() throws Exception {
        JsonNode preview = json(mockMvc.perform(multipart("/api/campaign/registrations/import/preview")
                .file(registrationsFile()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(preview.get("totalRows").asInt()).isEqualTo(6);
        assertThat(preview.get("newRows").asInt()).isEqualTo(2);
        assertThat(preview.get("duplicateInFile").asInt()).isEqualTo(1);
        assertThat(preview.get("invalidRows").asInt()).isEqualTo(3);
        assertThat(preview.get("rows").get(2).get("messages").toString()).contains("Renda é obrigatória");
        assertThat(familyRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "rh", roles = "RH_TESOURARIA")
    void registrationsConfirmCreatesFamiliesAndFlagsHistory() throws Exception {
        HistoricalBeneficiary past = new HistoricalBeneficiary();
        past.setCampaignYear(2025);
        past.setFullName("Maria Exemplo da Silva");
        past.setNormalizedName("MARIA EXEMPLO DA SILVA");
        past.setCpf(CPF_A);
        past.setSourceFile("teste.xlsx");
        historicalRepository.save(past);

        mockMvc.perform(multipart("/api/campaign/registrations/import").file(registrationsFile()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imported").value(2))
            .andExpect(jsonPath("$.skippedDuplicates").value(1))
            .andExpect(jsonPath("$.skippedInvalid").value(3));

        Family maria = familyRepository.findByCpf(CPF_A).orElseThrow();
        assertThat(maria.getPhone()).isEqualTo("24990001234");
        assertThat(maria.getAddress()).isEqualTo("Rua Fictícia 1");
        assertThat(maria.getSpouseName()).isNull();
        assertThat(maria.getTotalIncome()).isEqualByComparingTo("0");
        assertThat(maria.getOrigin()).isEqualTo(FamilyOrigin.INDICACAO);
        assertThat(maria.getReferrerName()).isEqualTo("Projeto Exemplo");
        assertThat(maria.getStatus().name()).isEqualTo("CADASTRADA");
        assertThat(maria.getPreviousBeneficiary()).isTrue();
        assertThat(maria.getPreviousBeneficiaryYears()).isEqualTo("2025");

        Family joao = familyRepository.findByCpf(CPF_B).orElseThrow();
        assertThat(joao.getSpouseName()).isEqualTo("Carlos Ficticio");
        assertThat(joao.getTotalIncome()).isEqualByComparingTo("1500.50");

        // Repetir não duplica
        mockMvc.perform(multipart("/api/campaign/registrations/import").file(registrationsFile()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imported").value(0))
            .andExpect(jsonPath("$.skippedAlreadyRegistered").value(2));
        assertThat(familyRepository.count()).isEqualTo(2);
    }

    /** Modelo de cadastro da campanha: 10 colunas; linhas 3-4 com erros propositalmente. */
    private MockMultipartFile registrationsFile() throws Exception {
        return workbook("cadastros.xlsx", wb -> {
            Sheet s = wb.createSheet("Cadastros");
            row(s, 0, "Nome", "CPF", "Telefone", "Endereço", "Cônjuge",
                "Grupo Familiar", "Renda", "Maior Necessidade", "Observação", "Indicado por Grupo/Apoio");
            row(s, 1, "Maria Exemplo da Silva", CPF_A, "(24) 99000-1234", "Rua Fictícia 1", "não tem",
                "4", "0", "Alimentação", "", "Projeto Exemplo");
            row(s, 2, "Maria Exemplo da Silva", CPF_A, "(24) 99000-1234", "Rua Fictícia 1", "não tem",
                "4", "0", "", "", "Projeto Exemplo");
            row(s, 3, "Sem Renda Ficticia", "390.533.447-05", "24991112222", "Rua X 10", "não tem",
                "2", "", "", "", "");
            row(s, 4, "Sem Cpf Ficticio", "", "24991112222", "Rua X 10", "não tem",
                "2", "800", "", "", "");
            row(s, 5, "João Teste Ficticio", CPF_B, "21 98888-7777", "Av Teste 50", "Carlos Ficticio",
                "3", "1.500,50", "Desemprego", "Mora de favor", "");
            row(s, 6, "Total de famílias:", "5", "", "", "", "", "", "", "", "");
        });
    }

    private void saveFamily(String name, String cpf) {
        Family family = new Family();
        family.setResponsibleName(name);
        family.setCpf(cpf);
        family.setHouseholdSize(4);
        family.setTotalIncome(new BigDecimal("900.00"));
        family.setRequestReason(RequestReason.BAIXA_RENDA);
        family.setOrigin(FamilyOrigin.INDICACAO);
        familyRepository.save(family);
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    /** Formato da "LISTAGEM FINAL": colunas A-D sem título, NOME/CPF/TELEFONE/ENDEREÇO e origem sem título. */
    private MockMultipartFile listagemFile() throws Exception {
        return workbook("listagem.xlsx", wb -> {
            Sheet s = wb.createSheet("FINAL");
            row(s, 0, "", "", "", "", "NOME", "CPF", "TELEFONE", "ENDEREÇO");
            Row r1 = row(s, 1, "", "", "", "", "Maria Exemplo da Silva ", "", "ZAP 024990001234", "Rua Fictícia 1", "Projeto Exemplo");
            r1.createCell(0).setCellValue(1);
            r1.createCell(5).setCellValue(Double.parseDouble(CPF_A));
            Row r2 = row(s, 2, "", "", "", "", "Maria Exemplo da Silva", "", "", "", "Projeto Exemplo");
            r2.createCell(5).setCellValue(Double.parseDouble(CPF_A));
            row(s, 3, "", "", "", "", "João Teste Ficticio", "123", "21 99999-0000", "", "Outros");
        });
    }

    private MockMultipartFile registrationFormFile() throws Exception {
        return workbook("inscricao.xlsx", wb -> {
            Sheet s = wb.createSheet("contemplada");
            row(s, 0, "Carimbo de data/hora", "Nome Completo", "Telefone celular", "Telefone fixo",
                "Quantas pessoas no total moram no domicílio?", "Quem lhe enviou o formulário de inscrição?");
            row(s, 1, "", "Ana Ficticia Souza", "24 99999-1111", "", "5", "Projeto Exemplo");
            row(s, 2, "", "Bruno Ficticio Lima", "", "24 2222-3333", "3", "Uma amiga");
            Sheet s2 = wb.createSheet("CONTEMPLADA DIA 22");
            row(s2, 0, "Ana Ficticia Souza", "24999991111", "2 crianças");
        });
    }

    private MockMultipartFile basketFile() throws Exception {
        return workbook("comanda.xlsx", wb -> {
            Sheet items = wb.createSheet("Página2");
            row(items, 0, "AÇUCAR");
            row(items, 1, "ARROZ");
            row(items, 2, "FAROFA");
            row(items, 3, "FAROFA");
            Sheet brands = wb.createSheet("Página3");
            row(brands, 0, "ITENS");
            row(brands, 1, "AÇUCAR NEVE/UNIÃO");
            row(brands, 2, "ROSQUINHA MABEL");
        });
    }

    private interface WorkbookFiller {
        void fill(XSSFWorkbook wb) throws Exception;
    }

    private MockMultipartFile workbook(String name, WorkbookFiller filler) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            filler.fill(wb);
            wb.write(out);
            return new MockMultipartFile("file", name,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    private Row row(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            if (!values[i].isEmpty()) row.createCell(i).setCellValue(values[i]);
        }
        return row;
    }
}
