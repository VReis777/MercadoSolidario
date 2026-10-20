package com.mercadosolidario.service;

import com.mercadosolidario.dto.stock.StockMovementRequest;
import com.mercadosolidario.model.Product;
import com.mercadosolidario.model.enums.MovementType;
import com.mercadosolidario.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockExcelService {

    public record StockImportChange(Long productId, String name, String unit,
                                    int currentQuantity, int newQuantity, int delta) {}

    public record StockImportResult(int changed, List<String> warnings) {}

    private final ProductRepository productRepository;
    private final StockService stockService;

    @Transactional(readOnly = true)
    public byte[] exportWorkbook() {
        List<Product> products = productRepository.findAll().stream()
            .filter(p -> Boolean.TRUE.equals(p.getActive()))
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .toList();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Estoque");
            Row header = sheet.createRow(0);
            String[] columns = {"ID", "Produto", "Unidade", "Quantidade atual", "Nova quantidade"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }
            int rowIndex = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(product.getId());
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getUnit());
                row.createCell(3).setCellValue(product.getCurrentQuantity() == null ? 0 : product.getCurrentQuantity());
                // coluna 4 ("Nova quantidade") fica em branco para a equipe preencher
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível gerar a planilha de estoque.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<StockImportChange> preview(MultipartFile file) {
        return parse(file).changes;
    }

    @Transactional
    public StockImportResult confirm(MultipartFile file) {
        Parsed parsed = parse(file);
        for (StockImportChange change : parsed.changes) {
            StockMovementRequest request = new StockMovementRequest();
            request.setProductId(change.productId);
            request.setType(MovementType.AJUSTE);
            request.setAdjustmentDirection(change.delta > 0
                ? StockMovementRequest.AdjustmentDirection.ENTRADA
                : StockMovementRequest.AdjustmentDirection.SAIDA);
            request.setQuantity(Math.abs(change.delta));
            request.setReason("Importação de planilha de estoque");
            stockService.registerMovement(request);
        }
        return new StockImportResult(parsed.changes.size(), parsed.warnings);
    }

    private Parsed parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Envie a planilha exportada preenchida.");
        }
        List<StockImportChange> changes = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowIndex = 0;
            for (Row row : sheet) {
                rowIndex++;
                if (rowIndex == 1) continue; // cabeçalho
                Long productId = readId(row.getCell(0));
                if (productId == null) continue;
                Integer newQuantity = readInteger(row.getCell(4));
                if (newQuantity == null) continue; // sem nova quantidade, ignora
                if (newQuantity < 0) {
                    warnings.add("Linha " + rowIndex + ": quantidade negativa ignorada.");
                    continue;
                }
                Product product = productRepository.findById(productId).orElse(null);
                if (product == null || !Boolean.TRUE.equals(product.getActive())) {
                    warnings.add("Linha " + rowIndex + ": produto ID " + productId + " não encontrado ou inativo.");
                    continue;
                }
                int current = product.getCurrentQuantity() == null ? 0 : product.getCurrentQuantity();
                if (newQuantity == current) continue;
                changes.add(new StockImportChange(product.getId(), product.getName(),
                    product.getUnit(), current, newQuantity, newQuantity - current));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler a planilha. Use o arquivo exportado pelo sistema.");
        }
        if (changes.isEmpty() && warnings.isEmpty()) {
            warnings.add("Nenhuma alteração encontrada — preencha a coluna \"Nova quantidade\".");
        }
        return new Parsed(changes, warnings);
    }

    private Long readId(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return (long) cell.getNumericCellValue();
        try {
            String text = cell.getStringCellValue().trim();
            return text.isEmpty() ? null : Long.parseLong(text);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer readInteger(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        try {
            String text = cell.getStringCellValue().trim();
            return text.isEmpty() ? null : Integer.parseInt(text.replace(",", ".").split("\\.")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private record Parsed(List<StockImportChange> changes, List<String> warnings) {}
}
