package com.mercadosolidario.service;

import com.mercadosolidario.dto.stock.BasketImportPreview;
import com.mercadosolidario.dto.stock.BasketImportPreview.Item;
import com.mercadosolidario.dto.stock.BasketImportPreview.ItemStatus;
import com.mercadosolidario.dto.stock.BasketImportResult;
import com.mercadosolidario.dto.stock.ProductRequest;
import com.mercadosolidario.model.Product;
import com.mercadosolidario.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

import static com.mercadosolidario.util.SpreadsheetText.*;

/**
 * Importa a lista de itens da cesta (coluna A) como produtos com estoque zero e unidade "un".
 * Uma aba iniciada por "ITENS" é tratada como lista de marcas preferidas, gravadas em observação.
 * Produtos já cadastrados nunca são alterados.
 */
@Service
@RequiredArgsConstructor
public class BasketItemImportService {

    static final String DEFAULT_UNIT = "un";

    private final ProductRepository productRepository;
    private final StockService stockService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public BasketImportPreview preview(MultipartFile file) {
        return analyze(file);
    }

    @Transactional
    public BasketImportResult confirm(MultipartFile file) {
        BasketImportPreview preview = analyze(file);
        int created = 0;
        for (Item item : preview.items()) {
            if (item.status() != ItemStatus.NOVO) continue;
            ProductRequest request = new ProductRequest();
            request.setName(item.name());
            request.setUnit(DEFAULT_UNIT);
            request.setMinimumQuantity(0);
            request.setNotes(item.preferredBrand() == null ? null : "Marca preferida: " + item.preferredBrand());
            stockService.createProduct(request);
            created++;
        }
        BasketImportResult result = new BasketImportResult(
            preview.fileName(), created, preview.existingItems(), preview.duplicateItems());
        auditService.log("BASKET_ITEMS_IMPORTED", "Product", null,
            "{\"file\": \"" + preview.fileName().replace("\"", "") + "\", \"created\": " + created
                + ", \"existing\": " + result.skippedExisting() + ", \"duplicates\": " + result.skippedDuplicates() + "}");
        return result;
    }

    private BasketImportPreview analyze(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Selecione uma planilha.");
        String fileName = truncate(Objects.requireNonNullElse(file.getOriginalFilename(), "planilha"), 255);
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
            throw new IllegalArgumentException("Formato não suportado. Envie um arquivo .xlsx ou .xls (Excel).");
        }

        List<String> itemLines = new ArrayList<>();
        List<String> brandLines = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            for (Sheet sheet : workbook) {
                List<String> values = new ArrayList<>();
                for (Row row : sheet) {
                    String value = cellText(row.getCell(0)).replaceAll("\\s+", " ").trim();
                    if (!value.isEmpty()) values.add(value);
                }
                if (values.isEmpty()) continue;
                if (normalize(values.get(0)).equals("ITENS")) {
                    brandLines.addAll(values.subList(1, values.size()));
                    warnings.add("Aba \"" + sheet.getSheetName() + "\" lida como lista de marcas preferidas.");
                } else {
                    itemLines.addAll(values);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível ler a planilha. Verifique se o arquivo está íntegro.");
        }
        if (itemLines.isEmpty()) throw new IllegalArgumentException("Nenhum item encontrado na coluna A da planilha.");

        Set<String> existing = new HashSet<>();
        for (Product p : productRepository.findAll()) existing.add(normalize(p.getName()));

        Map<String, String> brandByItem = new HashMap<>();
        List<String> unmatched = new ArrayList<>();
        List<String> knownItems = new ArrayList<>(existing);
        itemLines.forEach(line -> knownItems.add(normalize(line)));
        for (String brandLine : brandLines) {
            String norm = normalize(brandLine);
            String bestItem = knownItems.stream()
                .filter(item -> norm.equals(item) || norm.startsWith(item + " "))
                .max(Comparator.comparingInt(String::length))
                .orElse(null);
            if (bestItem == null) {
                unmatched.add(brandLine);
            } else if (!norm.equals(bestItem)) {
                brandByItem.putIfAbsent(bestItem, brandRemainder(brandLine, norm, bestItem));
            }
        }

        Set<String> seen = new HashSet<>();
        List<Item> items = new ArrayList<>();
        int newItems = 0, existingItems = 0, duplicates = 0;
        for (String line : itemLines) {
            String norm = normalize(line);
            ItemStatus status;
            if (!seen.add(norm)) {
                status = ItemStatus.DUPLICADO_NO_ARQUIVO;
                duplicates++;
            } else if (existing.contains(norm)) {
                status = ItemStatus.JA_CADASTRADO;
                existingItems++;
            } else {
                status = ItemStatus.NOVO;
                newItems++;
            }
            items.add(new Item(truncate(line, 255), status, brandByItem.get(norm)));
        }
        return new BasketImportPreview(fileName, items.size(), newItems, existingItems, duplicates, items, unmatched, warnings);
    }

    /** Parte da linha de marca após o nome do item, preservando acentos quando possível. */
    private static String brandRemainder(String original, String normalized, String item) {
        String[] originalWords = original.trim().split("\\s+");
        int itemWords = item.split(" ").length;
        if (originalWords.length == normalized.split(" ").length && originalWords.length > itemWords) {
            return truncate(String.join(" ", Arrays.copyOfRange(originalWords, itemWords, originalWords.length)), 200);
        }
        return truncate(normalized.substring(item.length()).trim(), 200);
    }
}
