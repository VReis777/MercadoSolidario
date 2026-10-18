package com.mercadosolidario.service;

import com.mercadosolidario.dto.history.HistoricalImportPreview;
import com.mercadosolidario.dto.history.HistoricalImportPreview.RowStatus;
import com.mercadosolidario.dto.history.HistoricalImportResult;
import com.mercadosolidario.model.HistoricalBeneficiary;
import com.mercadosolidario.repository.FamilyRepository;
import com.mercadosolidario.repository.HistoricalBeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Year;
import java.util.*;

import static com.mercadosolidario.util.SpreadsheetText.*;

/**
 * Lê planilhas (.xlsx/.xls) de campanhas anteriores, gera uma prévia com duplicidades e
 * inconsistências e só grava após confirmação explícita. Nunca altera famílias atuais.
 */
@Service
@RequiredArgsConstructor
public class HistoricalBeneficiaryImportService {

    private static final int HEADER_SCAN_ROWS = 5;

    private final HistoricalBeneficiaryRepository historicalRepository;
    private final FamilyRepository familyRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public HistoricalImportPreview preview(MultipartFile file, int year) {
        return analyze(file, year).preview();
    }

    @Transactional
    public HistoricalImportResult confirm(MultipartFile file, int year) {
        Analysis analysis = analyze(file, year);
        String actor = currentUsername();
        List<HistoricalBeneficiary> toSave = analysis.parsed().stream()
            .filter(row -> row.status() == RowStatus.NOVO)
            .map(row -> toEntity(row, year, analysis.fileName(), actor))
            .toList();
        historicalRepository.saveAll(toSave);

        HistoricalImportPreview p = analysis.preview();
        HistoricalImportResult result = new HistoricalImportResult(
            analysis.fileName(), year, toSave.size(), p.duplicateInFile(), p.alreadyImported(), p.invalidRows());
        auditService.log("HISTORICAL_BENEFICIARIES_IMPORTED", "HistoricalBeneficiary", null,
            "{\"file\": \"" + analysis.fileName().replace("\"", "") + "\", \"year\": " + year
                + ", \"imported\": " + result.imported() + ", \"duplicates\": " + result.skippedDuplicates()
                + ", \"alreadyImported\": " + result.skippedAlreadyImported()
                + ", \"invalid\": " + result.skippedInvalid() + "}");
        return result;
    }

    private Analysis analyze(MultipartFile file, int year) {
        validate(file, year);
        String fileName = truncate(Objects.requireNonNullElse(file.getOriginalFilename(), "planilha"), 255);
        List<String> warnings = new ArrayList<>();
        List<ParsedRow> raw = readWorkbook(file, warnings);

        Map<String, String> existingThisYear = new HashMap<>();
        Set<String> existingCpfsThisYear = new HashSet<>();
        Map<String, Set<Integer>> otherYearsByKey = new HashMap<>();
        for (HistoricalBeneficiary h : historicalRepository.findAll()) {
            if (h.getCampaignYear() == year) {
                existingThisYear.putIfAbsent(h.getNormalizedName(), h.getCpf());
                if (h.getCpf() != null) existingCpfsThisYear.add(h.getCpf());
            } else {
                otherYearsByKey.computeIfAbsent("N" + h.getNormalizedName(), k -> new TreeSet<>()).add(h.getCampaignYear());
                if (h.getCpf() != null) {
                    otherYearsByKey.computeIfAbsent("C" + h.getCpf(), k -> new TreeSet<>()).add(h.getCampaignYear());
                }
            }
        }
        Set<String> familyCpfs = new HashSet<>();
        Set<String> familyNames = new HashSet<>();
        for (Object[] f : familyRepository.findAllNamesAndCpfs()) {
            familyNames.add(normalize((String) f[0]));
            if (f[1] != null) familyCpfs.add((String) f[1]);
        }

        Map<String, String> seenNames = new HashMap<>();
        Set<String> seenCpfs = new HashSet<>();
        List<ParsedRow> analyzed = new ArrayList<>();
        int newRows = 0, duplicates = 0, already = 0, invalid = 0, withoutCpf = 0, otherYears = 0, families = 0;

        for (ParsedRow row : raw) {
            List<String> messages = new ArrayList<>(row.messages());
            RowStatus status;
            if (row.normalizedName().length() < 3 || !row.normalizedName().matches(".*[A-Z].*")) {
                status = RowStatus.INVALIDO;
                messages.add("Linha sem nome válido.");
            } else if (matches(row, seenNames, seenCpfs)) {
                status = RowStatus.DUPLICADO_NO_ARQUIVO;
                messages.add("Mesma pessoa já aparece antes nesta planilha.");
            } else if (matches(row, existingThisYear, existingCpfsThisYear)) {
                status = RowStatus.JA_IMPORTADO;
                messages.add("Já existe no histórico de " + year + ".");
            } else {
                status = RowStatus.NOVO;
            }
            if (status != RowStatus.INVALIDO) {
                seenNames.putIfAbsent(row.normalizedName(), row.cpf());
                if (row.cpf() != null) seenCpfs.add(row.cpf());
            }
            if (status == RowStatus.NOVO) {
                newRows++;
                if (row.cpf() == null) withoutCpf++;
                Set<Integer> years = new TreeSet<>();
                years.addAll(otherYearsByKey.getOrDefault("N" + row.normalizedName(), Set.of()));
                if (row.cpf() != null) years.addAll(otherYearsByKey.getOrDefault("C" + row.cpf(), Set.of()));
                if (!years.isEmpty()) {
                    otherYears++;
                    messages.add("Também contemplada em: " + years + ".");
                }
                boolean familyMatch = (row.cpf() != null && familyCpfs.contains(row.cpf()))
                    || familyNames.contains(row.normalizedName());
                if (familyMatch) {
                    families++;
                    messages.add("Possui cadastro atual no sistema (" + (row.cpf() != null && familyCpfs.contains(row.cpf()) ? "CPF" : "nome") + ").");
                }
            } else if (status == RowStatus.DUPLICADO_NO_ARQUIVO) {
                duplicates++;
            } else if (status == RowStatus.JA_IMPORTADO) {
                already++;
            } else {
                invalid++;
            }
            analyzed.add(row.withStatus(status, messages));
        }

        List<HistoricalImportPreview.Row> previewRows = analyzed.stream()
            .map(r -> new HistoricalImportPreview.Row(r.sheet(), r.rowNumber(), r.name(), maskCpf(r.cpf()),
                maskPhone(r.phone()), r.householdSize(), r.origin(), r.status(), r.messages()))
            .toList();
        HistoricalImportPreview preview = new HistoricalImportPreview(fileName, year, analyzed.size(), newRows,
            duplicates, already, invalid, withoutCpf, otherYears, families, warnings, previewRows);
        return new Analysis(fileName, analyzed, preview);
    }

    private static boolean matches(ParsedRow row, Map<String, String> names, Set<String> cpfs) {
        if (row.cpf() != null && cpfs.contains(row.cpf())) return true;
        if (!names.containsKey(row.normalizedName())) return false;
        String otherCpf = names.get(row.normalizedName());
        return row.cpf() == null || otherCpf == null || otherCpf.equals(row.cpf());
    }

    private void validate(MultipartFile file, int year) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Selecione uma planilha.");
        String name = Objects.requireNonNullElse(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new IllegalArgumentException("Formato não suportado. Envie um arquivo .xlsx ou .xls (Excel).");
        }
        if (year < 2000 || year > Year.now().getValue()) {
            throw new IllegalArgumentException("Ano da campanha inválido.");
        }
    }

    private List<ParsedRow> readWorkbook(MultipartFile file, List<String> warnings) {
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            List<ParsedRow> rows = new ArrayList<>();
            for (Sheet sheet : workbook) {
                readSheet(sheet, rows, warnings);
            }
            if (rows.isEmpty()) warnings.add("Nenhuma linha com nome foi encontrada na planilha.");
            return rows;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível ler a planilha. Verifique se o arquivo está íntegro.");
        }
    }

    private void readSheet(Sheet sheet, List<ParsedRow> out, List<String> warnings) {
        Columns cols = detectHeader(sheet);
        int firstDataRow;
        if (cols != null) {
            firstDataRow = cols.headerRow() + 1;
            if (cols.origin() < 0) cols = cols.withOrigin(findUnlabeledColumn(sheet, cols));
        } else if (looksHeaderless(sheet)) {
            cols = new Columns(-1, 0, -1, 1, -1, -1, -1, -1);
            firstDataRow = sheet.getFirstRowNum();
            warnings.add("Aba \"" + sheet.getSheetName() + "\" sem cabeçalho: considerei coluna A = nome e B = telefone.");
        } else {
            if (sheet.getPhysicalNumberOfRows() > 0) {
                warnings.add("Aba \"" + sheet.getSheetName() + "\" ignorada: cabeçalho com coluna NOME não encontrado.");
            }
            return;
        }

        for (int r = firstDataRow; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String name = text(row, cols.name());
            String cpfRaw = text(row, cols.cpf());
            String phoneRaw = text(row, cols.phone());
            if (name.isBlank() && cpfRaw.isBlank() && phoneRaw.isBlank()) continue;

            List<String> messages = new ArrayList<>();
            String cpf = validCpfOrNull(cpfRaw);
            if (!cpfRaw.isBlank() && cpf == null) messages.add("CPF inválido na planilha — será guardado sem CPF.");
            String phone = phoneOrNull(phoneRaw);
            if (phone == null) phone = phoneOrNull(text(row, cols.phone2()));
            Integer household = parseSmallInt(text(row, cols.household()));
            String cleanName = truncate(name.replaceAll("\\s+", " "), 255);
            out.add(new ParsedRow(sheet.getSheetName(), r + 1, cleanName == null ? "" : cleanName,
                normalize(name), cpf, phone, truncate(text(row, cols.address()), 500), household,
                truncate(text(row, cols.origin()), 255), null, messages));
        }
    }

    private Columns detectHeader(Sheet sheet) {
        int start = Math.max(sheet.getFirstRowNum(), 0);
        for (int r = start; r < start + HEADER_SCAN_ROWS && r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            int name = -1, cpf = -1, phone = -1, phone2 = -1, address = -1, household = -1, origin = -1;
            for (Cell cell : row) {
                String h = normalize(cellText(cell));
                int i = cell.getColumnIndex();
                if (name < 0 && (h.equals("NOME") || h.startsWith("NOME COMPLETO") || h.equals("NOME DO RESPONSAVEL"))) name = i;
                else if (cpf < 0 && h.contains("CPF")) cpf = i;
                else if (phone2 < 0 && h.contains("TELEFONE FIXO")) phone2 = i;
                else if (phone < 0 && (h.startsWith("TELEFONE") || h.startsWith("CELULAR") || h.startsWith("WHATSAPP"))) phone = i;
                else if (address < 0 && h.startsWith("ENDERECO")) address = i;
                else if (household < 0 && h.startsWith("QUANTAS PESSOAS")) household = i;
                else if (origin < 0 && (h.startsWith("QUEM LHE ENVIOU") || h.contains("INDICAD") || h.equals("ORIGEM"))) origin = i;
            }
            if (name >= 0) return new Columns(r, name, cpf, phone, phone2, address, household, origin);
        }
        return null;
    }

    /** Primeira coluna sem título, à direita das colunas reconhecidas, que tenha dados (ex.: origem/projeto). */
    private int findUnlabeledColumn(Sheet sheet, Columns cols) {
        int max = Collections.max(List.of(cols.name(), cols.cpf(), cols.phone(), cols.phone2(), cols.address(), cols.household()));
        Row header = sheet.getRow(cols.headerRow());
        for (int c = max + 1; c <= max + 3; c++) {
            if (header != null && !text(header, c).isBlank()) continue;
            for (int r = cols.headerRow() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row != null && !text(row, c).isBlank() && text(row, c).matches(".*[A-Za-zÀ-ú].*")) return c;
            }
        }
        return -1;
    }

    private boolean looksHeaderless(Sheet sheet) {
        Row first = sheet.getRow(Math.max(sheet.getFirstRowNum(), 0));
        if (first == null) return false;
        String a = text(first, 0);
        return a.matches("[A-Za-zÀ-ú' ]{5,}") && first.getLastCellNum() <= 5;
    }

    private static String text(Row row, int col) {
        return col < 0 ? "" : cellText(row.getCell(col));
    }

    private static Integer parseSmallInt(String value) {
        String d = digits(value);
        if (d.isEmpty() || d.length() > 2) return null;
        int n = Integer.parseInt(d);
        return n > 0 ? n : null;
    }

    private HistoricalBeneficiary toEntity(ParsedRow row, int year, String fileName, String actor) {
        HistoricalBeneficiary h = new HistoricalBeneficiary();
        h.setCampaignYear(year);
        h.setFullName(row.name());
        h.setNormalizedName(truncate(row.normalizedName(), 255));
        h.setCpf(row.cpf());
        h.setPhone(row.phone());
        h.setAddress(row.address());
        h.setHouseholdSize(row.householdSize());
        h.setOrigin(row.origin());
        h.setSourceFile(fileName);
        h.setSourceSheet(truncate(row.sheet(), 100));
        h.setSourceRow(row.rowNumber());
        h.setImportedBy(truncate(actor, 100));
        return h;
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private record Columns(int headerRow, int name, int cpf, int phone, int phone2, int address, int household, int origin) {
        Columns withOrigin(int newOrigin) {
            return new Columns(headerRow, name, cpf, phone, phone2, address, household, newOrigin);
        }
    }

    private record ParsedRow(String sheet, int rowNumber, String name, String normalizedName, String cpf, String phone,
                             String address, Integer householdSize, String origin, RowStatus status, List<String> messages) {
        ParsedRow withStatus(RowStatus newStatus, List<String> newMessages) {
            return new ParsedRow(sheet, rowNumber, name, normalizedName, cpf, phone, address, householdSize, origin,
                newStatus, List.copyOf(newMessages));
        }
    }

    private record Analysis(String fileName, List<ParsedRow> parsed, HistoricalImportPreview preview) {}
}
