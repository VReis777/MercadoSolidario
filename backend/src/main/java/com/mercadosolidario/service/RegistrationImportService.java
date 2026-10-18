package com.mercadosolidario.service;

import com.mercadosolidario.dto.history.RegistrationImportPreview;
import com.mercadosolidario.dto.history.RegistrationImportPreview.RowStatus;
import com.mercadosolidario.dto.history.RegistrationImportResult;
import com.mercadosolidario.model.Family;
import com.mercadosolidario.model.HistoricalBeneficiary;
import com.mercadosolidario.model.enums.FamilyOrigin;
import com.mercadosolidario.model.enums.FamilyStatus;
import com.mercadosolidario.model.enums.RequestReason;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.repository.HistoricalBeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

import static com.mercadosolidario.util.SpreadsheetText.*;

/**
 * Importa cadastros da campanha atual a partir da planilha modelo, criando famílias com
 * status CADASTRADA. Gera prévia antes de gravar; duplicados e já cadastrados são ignorados.
 */
@Service
@RequiredArgsConstructor
public class RegistrationImportService {

    private static final int HEADER_SCAN_ROWS = 5;
    private static final Set<String> NO_SPOUSE = Set.of(
        "-", "--", "/", "0", "N/A", "NA", "NAO", "NÃO", "NAO TEM", "NÃO TEM", "NENHUM", "NENHUMA",
        "SEM", "SEM CONJUGE", "SEM CÔNJUGE", "SOLTEIRO", "SOLTEIRA", "SOLTEIRO(A)", "SOLTEIROA",
        "VIUVO", "VIUVA", "VIÚVO", "VIÚVA", "DIVORCIADO", "DIVORCIADA", "SEPARADO", "SEPARADA", "DESQUITADO", "DESQUITADA");
    private static final Set<String> NO_INCOME = Set.of(
        "0", "-", "NAO TEM", "NÃO TEM", "SEM RENDA", "NENHUMA", "NENHUM", "ZERO", "DESEMPREGADO", "DESEMPREGADA");

    private final FamilyRepository familyRepository;
    private final HistoricalBeneficiaryRepository historicalRepository;
    private final AuditService auditService;

    /** Planilha modelo com as colunas esperadas e instruções de preenchimento. */
    public byte[] buildTemplate(int year) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Cadastros " + year);
            String[] headers = {"Nome", "CPF", "Telefone", "Endereço", "Cônjuge",
                "Grupo Familiar", "Renda", "Maior Necessidade", "Observação", "Indicado por Grupo/Apoio"};
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }

            Sheet help = wb.createSheet("Instruções");
            String[] lines = {
                "Modelo de cadastro — Mercado Solidário " + year,
                "Preencha somente a aba 'Cadastros " + year + "', uma família por linha, a partir da linha 2.",
                "Obrigatórios: Nome, CPF, Telefone, Endereço, Cônjuge, Grupo Familiar e Renda.",
                "Cônjuge: se não houver, escreva 'não tem'.",
                "Grupo Familiar: número total de pessoas que moram na casa (incluindo o responsável).",
                "Renda: valor total mensal da família; se não houver renda, escreva 0.",
                "CPF: escreva os 11 dígitos; o formato 000.000.000-00 também é aceito.",
                "Não altere a linha 1 (cabeçalho) e não apague esta aba de instruções.",
                "Linhas com erro são listadas na prévia e não são cadastradas; corrija e importe de novo.",
            };
            for (int i = 0; i < lines.length; i++) {
                org.apache.poi.ss.usermodel.Row row = help.createRow(i);
                row.createCell(0).setCellValue(lines[i]);
            }
            help.setColumnWidth(0, 100 * 256);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar a planilha modelo.");
        }
    }

    @Transactional(readOnly = true)
    public RegistrationImportPreview preview(MultipartFile file) {
        return analyze(file).preview();
    }

    @Transactional
    public RegistrationImportResult confirm(MultipartFile file) {
        Analysis analysis = analyze(file);
        String actor = currentUsername();
        List<Family> toSave = analysis.parsed().stream()
            .filter(row -> row.status() == RowStatus.NOVO)
            .map(row -> toEntity(row))
            .toList();
        familyRepository.saveAll(toSave);

        RegistrationImportPreview p = analysis.preview();
        RegistrationImportResult result = new RegistrationImportResult(
            analysis.fileName(), p.campaignYear(), toSave.size(),
            p.duplicateInFile(), p.alreadyRegistered(), p.invalidRows());
        auditService.log("FAMILIES_IMPORTED", "Family", null,
            "{\"file\": \"" + analysis.fileName().replace("\"", "") + "\", \"imported\": " + result.imported()
                + ", \"duplicates\": " + result.skippedDuplicates()
                + ", \"alreadyRegistered\": " + result.skippedAlreadyRegistered()
                + ", \"invalid\": " + result.skippedInvalid() + ", \"by\": \"" + (actor == null ? "" : actor) + "\"}");
        return result;
    }

    private Analysis analyze(MultipartFile file) {
        validate(file);
        String fileName = truncate(Objects.requireNonNullElse(file.getOriginalFilename(), "planilha"), 255);
        List<String> warnings = new ArrayList<>();
        List<ParsedRow> raw = readWorkbook(file, warnings);

        Set<String> existingCpfs = new HashSet<>();
        Set<String> existingNames = new HashSet<>();
        for (Object[] f : familyRepository.findAllNamesAndCpfs()) {
            existingNames.add(normalize((String) f[0]));
            if (f[1] != null) existingCpfs.add((String) f[1]);
        }
        Map<String, Set<Integer>> historyByKey = new HashMap<>();
        for (HistoricalBeneficiary h : historicalRepository.findAll()) {
            historyByKey.computeIfAbsent("N" + h.getNormalizedName(), k -> new TreeSet<>()).add(h.getCampaignYear());
            if (h.getCpf() != null) {
                historyByKey.computeIfAbsent("C" + h.getCpf(), k -> new TreeSet<>()).add(h.getCampaignYear());
            }
        }

        Set<String> seenCpfs = new HashSet<>();
        Set<String> seenNames = new HashSet<>();
        List<ParsedRow> analyzed = new ArrayList<>();
        int newRows = 0, duplicates = 0, already = 0, invalid = 0, withHistory = 0;

        for (ParsedRow row : raw) {
            List<String> messages = new ArrayList<>(row.messages());
            RowStatus status;
            if (!messages.isEmpty() || row.normalizedName().isEmpty()) {
                status = RowStatus.INVALIDO;
                if (messages.isEmpty()) messages.add("Linha sem nome válido.");
            } else if (seenCpfs.contains(row.cpf()) || seenNames.contains(row.normalizedName())) {
                status = RowStatus.DUPLICADO_NO_ARQUIVO;
                messages.add("Mesma pessoa já aparece antes nesta planilha.");
            } else if (existingCpfs.contains(row.cpf())) {
                status = RowStatus.JA_CADASTRADO;
                messages.add("CPF já possui cadastro no sistema.");
            } else if (existingNames.contains(row.normalizedName())) {
                status = RowStatus.JA_CADASTRADO;
                messages.add("Já existe cadastro com este nome; conferir se é a mesma pessoa antes de recadastrar.");
            } else {
                status = RowStatus.NOVO;
            }
            if (status == RowStatus.NOVO || status == RowStatus.JA_CADASTRADO) {
                if (row.cpf() != null) seenCpfs.add(row.cpf());
                seenNames.add(row.normalizedName());
            }
            if (status == RowStatus.NOVO) {
                newRows++;
                Set<Integer> years = new TreeSet<>();
                years.addAll(historyByKey.getOrDefault("N" + row.normalizedName(), Set.of()));
                years.addAll(historyByKey.getOrDefault("C" + row.cpf(), Set.of()));
                if (!years.isEmpty()) {
                    withHistory++;
                    messages.add("Consta como contemplada em " + years + " — a triagem deve conferir antes de aprovar.");
                }
            } else if (status == RowStatus.DUPLICADO_NO_ARQUIVO) {
                duplicates++;
            } else if (status == RowStatus.JA_CADASTRADO) {
                already++;
            } else {
                invalid++;
            }
            analyzed.add(row.withStatus(status, messages));
        }

        List<RegistrationImportPreview.Row> previewRows = analyzed.stream()
            .map(r -> new RegistrationImportPreview.Row(r.sheet(), r.rowNumber(), r.name(), maskCpf(r.cpf()),
                maskPhone(r.phone()), r.spouseName(), r.householdSize(), r.totalIncome(), r.referrerName(),
                r.status(), r.messages()))
            .toList();
        RegistrationImportPreview preview = new RegistrationImportPreview(fileName, Year.now().getValue(),
            analyzed.size(), newRows, duplicates, already, invalid, withHistory, warnings, previewRows);
        return new Analysis(fileName, analyzed, preview);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Selecione uma planilha.");
        String name = Objects.requireNonNullElse(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new IllegalArgumentException("Formato não suportado. Envie um arquivo .xlsx ou .xls (Excel).");
        }
    }

    private List<ParsedRow> readWorkbook(MultipartFile file, List<String> warnings) {
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            List<ParsedRow> rows = new ArrayList<>();
            for (Sheet sheet : workbook) {
                if (normalize(sheet.getSheetName()).startsWith("INSTRUC")) continue;
                readSheet(sheet, rows, warnings);
            }
            if (rows.isEmpty()) warnings.add("Nenhuma linha de cadastro foi encontrada na planilha.");
            return rows;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível ler a planilha. Verifique se o arquivo está íntegro e use o modelo fornecido.");
        }
    }

    private void readSheet(Sheet sheet, List<ParsedRow> out, List<String> warnings) {
        Columns cols = detectHeader(sheet);
        if (cols == null) {
            if (sheet.getPhysicalNumberOfRows() > 0) {
                warnings.add("Aba \"" + sheet.getSheetName() + "\" ignorada: cabeçalho não encontrado (use a planilha modelo).");
            }
            return;
        }
        for (int r = cols.headerRow() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String name = text(row, cols.name());
            String cpfRaw = text(row, cols.cpf());
            String phoneRaw = text(row, cols.phone());
            String addressRaw = text(row, cols.address());
            String spouseRaw = text(row, cols.spouse());
            String householdRaw = text(row, cols.household());
            String incomeRaw = text(row, cols.income());
            if (name.isBlank() && cpfRaw.isBlank() && phoneRaw.isBlank() && addressRaw.isBlank()
                && householdRaw.isBlank() && incomeRaw.isBlank()) continue;

            List<String> messages = new ArrayList<>();

            String cpf = validCpfOrNull(cpfRaw);
            if (cpf == null) messages.add(cpfRaw.isBlank() ? "CPF é obrigatório." : "CPF inválido — corrija na planilha.");
            String phone = phoneOrNull(phoneRaw);
            if (phone == null) messages.add(phoneRaw.isBlank() ? "Telefone é obrigatório." : "Telefone inválido.");
            String address = truncate(addressRaw, 500);
            if (address == null) messages.add("Endereço é obrigatório.");
            String cleanName = truncate(name.replaceAll("\\s+", " "), 255);
            if (cleanName == null || normalize(name).length() < 3) messages.add("Nome é obrigatório.");

            String spouse = spouseRaw.trim().replaceAll("\\s+", " ");
            String spouseName = spouse.isBlank() ? null : (NO_SPOUSE.contains(normalize(spouse)) ? null : truncate(spouse, 255));
            if (spouse.isBlank()) messages.add("Cônjuge é obrigatório; escreva \"não tem\" se não houver.");

            Integer household = parseHousehold(householdRaw);
            if (household == null) messages.add("Grupo Familiar é obrigatório (número total de pessoas).");

            BigDecimal income = parseIncome(incomeRaw);
            if (income == null) messages.add("Renda é obrigatória; escreva 0 se não houver renda.");

            String necessity = truncate(text(row, cols.necessity()), 500);
            String observation = truncate(text(row, cols.observation()), 1000);
            String referrer = truncate(text(row, cols.referrer()), 255);

            out.add(new ParsedRow(sheet.getSheetName(), r + 1, cleanName == null ? "" : cleanName,
                normalize(name), cpf, phone, address, spouseName, household, income,
                necessity, observation, referrer, null, messages));
        }
    }

    private Columns detectHeader(Sheet sheet) {
        int start = Math.max(sheet.getFirstRowNum(), 0);
        for (int r = start; r < start + HEADER_SCAN_ROWS && r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            int name = -1, cpf = -1, phone = -1, address = -1, spouse = -1, household = -1,
                income = -1, necessity = -1, observation = -1, referrer = -1;
            for (Cell cell : row) {
                String h = normalize(cellText(cell));
                int i = cell.getColumnIndex();
                if (name < 0 && (h.equals("NOME") || h.startsWith("NOME COMPLETO") || h.equals("NOME DO RESPONSAVEL"))) name = i;
                else if (cpf < 0 && h.contains("CPF")) cpf = i;
                else if (phone < 0 && (h.startsWith("TELEFONE") || h.startsWith("CELULAR") || h.startsWith("WHATSAPP"))) phone = i;
                else if (address < 0 && h.startsWith("ENDERECO")) address = i;
                else if (spouse < 0 && (h.startsWith("CONJUGE") || h.startsWith("ESPOSO") || h.startsWith("ESPOSA") || h.startsWith("MARIDO"))) spouse = i;
                else if (household < 0 && (h.startsWith("GRUPO FAMILIAR") || h.startsWith("QUANTAS PESSOAS") || h.startsWith("PESSOAS"))) household = i;
                else if (income < 0 && h.startsWith("RENDA")) income = i;
                else if (necessity < 0 && h.contains("NECESSIDADE")) necessity = i;
                else if (observation < 0 && (h.startsWith("OBSERVACAO") || h.equals("OBS"))) observation = i;
                else if (referrer < 0 && (h.startsWith("INDICAD") || h.startsWith("QUEM LHE ENVIOU") || h.contains("APOIO") || h.equals("ORIGEM"))) referrer = i;
            }
            if (name >= 0 && cpf >= 0) return new Columns(r, name, cpf, phone, address, spouse, household, income, necessity, observation, referrer);
        }
        return null;
    }

    private static String text(Row row, int col) {
        return col < 0 ? "" : cellText(row.getCell(col));
    }

    private static Integer parseHousehold(String value) {
        String d = digits(value);
        if (d.isEmpty() || d.length() > 2) return null;
        int n = Integer.parseInt(d);
        return n > 0 ? n : null;
    }

    /** Aceita "1500", "1.500,00", "R$ 800" e textos como "não tem"/"sem renda" (= 0). */
    private static BigDecimal parseIncome(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) return null;
        if (NO_INCOME.contains(normalize(trimmed))) return BigDecimal.ZERO;
        String cleaned = trimmed.replaceAll("[^0-9,.-]", "");
        if (cleaned.isEmpty()) return null;
        // pt-BR: ponto como milhar, vírgula como decimal
        if (cleaned.contains(",") && cleaned.contains(".")) {
            cleaned = cleaned.replace(".", "").replace(",", ".");
        } else if (cleaned.contains(",")) {
            cleaned = cleaned.replace(",", ".");
        }
        try {
            BigDecimal value = new BigDecimal(cleaned);
            return value.signum() < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static RequestReason mapReason(String necessity) {
        String n = normalize(necessity);
        if (n.isEmpty()) return RequestReason.OUTRO;
        if (n.contains("DESEMPREG")) return RequestReason.DESEMPREGO;
        if (n.contains("DOENC") || n.contains("SAUDE") || n.contains("DOENTE") || n.contains("REMEDIO")) return RequestReason.DOENCA_FAMILIA;
        if (n.contains("HABITAC") || n.contains("MORADIA") || n.contains("ALUGUEL") || n.contains("CASA")) return RequestReason.SITUACAO_HABITACIONAL;
        if (n.contains("PERDA") && n.contains("RENDA")) return RequestReason.PERDA_RENDA;
        if (n.contains("RENDA") || n.contains("SALARIO")) return RequestReason.BAIXA_RENDA;
        if (n.contains("EMERGENCIA") || n.contains("URGENTE") || n.contains("URGENTE")) return RequestReason.EMERGENCIA_FINANCEIRA;
        if (n.contains("FILHO") || n.contains("CRIANC") || n.contains("NUMEROSA")) return RequestReason.FAMILIA_NUMEROSA;
        return RequestReason.OUTRO;
    }

    private Family toEntity(ParsedRow row) {
        Family f = new Family();
        f.setResponsibleName(row.name());
        f.setCpf(row.cpf());
        f.setPhone(row.phone());
        f.setAddress(row.address());
        f.setSpouseName(row.spouseName());
        f.setHouseholdSize(row.householdSize());
        f.setTotalIncome(row.totalIncome());
        f.setRequestReason(mapReason(row.necessity()));
        f.setOrigin(row.referrerName() == null ? FamilyOrigin.CADASTRO_PROPRIO : FamilyOrigin.INDICACAO);
        f.setReferrerName(row.referrerName());
        StringBuilder obs = new StringBuilder();
        if (row.observation() != null) obs.append(row.observation());
        if (row.necessity() != null) {
            if (obs.length() > 0) obs.append(" | ");
            obs.append("Maior necessidade: ").append(row.necessity());
        }
        f.setObservations(obs.length() == 0 ? null : truncate(obs.toString(), 2000));
        f.setStatus(FamilyStatus.CADASTRADA);
        f.setDependents(0);
        // Sinaliza histórico de contemplação para a triagem.
        Set<Integer> years = new TreeSet<>();
        // a análise já marcou nos messages; aqui recalcula-se de forma determinística
        // (não confia na mensagem para gravar o flag)
        List<HistoricalBeneficiary> matches = historicalRepository
            .findByCpfOrNormalizedName(row.cpf(), row.normalizedName());
        matches.forEach(h -> years.add(h.getCampaignYear()));
        if (!years.isEmpty()) {
            f.setPreviousBeneficiary(true);
            f.setPreviousBeneficiaryYears(years.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        }
        return f;
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private record Columns(int headerRow, int name, int cpf, int phone, int address, int spouse,
                           int household, int income, int necessity, int observation, int referrer) {}

    private record ParsedRow(String sheet, int rowNumber, String name, String normalizedName, String cpf,
                             String phone, String address, String spouseName, Integer householdSize,
                             BigDecimal totalIncome, String necessity, String observation, String referrerName,
                             RowStatus status, List<String> messages) {
        ParsedRow withStatus(RowStatus newStatus, List<String> newMessages) {
            return new ParsedRow(sheet, rowNumber, name, normalizedName, cpf, phone, address, spouseName,
                householdSize, totalIncome, necessity, observation, referrerName,
                newStatus, List.copyOf(newMessages));
        }
    }

    private record Analysis(String fileName, List<ParsedRow> parsed, RegistrationImportPreview preview) {}
}
