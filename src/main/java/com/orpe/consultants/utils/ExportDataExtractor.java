package com.orpe.consultants.utils;

import com.orpe.consultants.dto.ExportDataDTO;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Service
public class ExportDataExtractor {
    private static final List<String> PREFERRED_SHEETS = List.of("EXPORT", "EXPORT DETAILS");
    private static final DateTimeFormatter[] DATE_PATTERNS = new DateTimeFormatter[] {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    public List<ExportDataDTO> parseExportSheet(MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = pickSheet(wb);
            DataFormatter fmt = new DataFormatter();
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();

            // Header row mapping (Normalize keys)
            Row header = sheet.getRow(0);
            if (header == null) throw new IllegalArgumentException("Header row missing");
            Map<String, Integer> idx = new HashMap<>();
            for (int c = 0; c < header.getLastCellNum(); c++) {
                String key = normalize(fmt.formatCellValue(header.getCell(c), eval));
                if (!key.isBlank()) idx.putIfAbsent(key, c);
            }

            List<ExportDataDTO> rows = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String sbNo = getString(row, col(idx, "SB NO"), fmt, eval);
                if (sbNo.isEmpty()) continue; // skip empty or header rows

                ExportDataDTO dto = ExportDataDTO.builder()
                        .sbNo(sbNo)
                        .sbDate(getDate(row, col(idx, "SB DATE"), fmt, eval))
                        .portCode(getString(row, col(idx, "PORT CODE"), fmt, eval))
                        .customerName(getString(row, col(idx, "CUSTOMER NAME"), fmt, eval))
                        .leoDate(getDate(row, col(idx, "LEO DATE"), fmt, eval))
                        .claimRefNo(getString(row, col(idx, "CLAIM REF NO"), fmt, eval))
                        .claimYear(getString(row, col(idx, "CLAIM YEAR"), fmt, eval))
                        .schemeDescription(getString(row, col(idx, "SCHEME DESCRIPTION"), fmt, eval))
                        .dbkSno(getString(row, col(idx, "DBK SNO"), fmt, eval))
                        .dbkApplicability(getString(row, col(idx, "DBK APPLICABILITY"), fmt, eval))
                        .rate(getDecimal(row, col(idx, "RATE"), fmt, eval))
                        .airGivenInSb(getDecimal(row, col(idx, "AIR GIVEN IN SB"), fmt, eval))
                        .airAmount(getDecimal(row, col(idx, "AIR AMOUNT"), fmt, eval))
                        .difference(getDecimal(row, col(idx, "DIFFERENCE"), fmt, eval))
                        .totalDbk(getDecimal(row, col(idx, "TOTAL DBK"), fmt, eval))
                        .sbr(getDecimal(row, col(idx, "SBR"), fmt, eval))
                        .aroNo(getString(row, col(idx, "ARO NO"), fmt, eval))
                        .aroDate(getDate(row, col(idx, "ARO DATE"), fmt, eval))
                        .aroFileNo(getString(row, col(idx, "ARO FILE NO"), fmt, eval))
                        .aroFileDate(getDate(row, col(idx, "ARO FILE DATE"), fmt, eval))
                        .brcNo(getString(row, col(idx, "BRC NO"), fmt, eval))
                        .netRealisedValue(getDecimal(row, col(idx, "NET REALISED VALUE"), fmt, eval))
                        .netRealisedCurrency(getString(row, col(idx, "CURRENCY"), fmt, eval))
                        .sbUtilization(getString(row, col(idx, "SB UTILIZATION"), fmt, eval))
                        .invoiceNo(getString(row, col(idx, "INVOICE NO"), fmt, eval))
                        .invoiceDate(getDate(row, col(idx, "INVOICE DATE"), fmt, eval))
                        .modelNo(getString(row, col(idx, "MODEL NO"), fmt, eval))
                        .productType(getString(row, col(idx, "PRODUCT TYPE"), fmt, eval))
                        .hsCode(getString(row, col(idx, "HS CD"), fmt, eval))
                        .modelDescription(getString(row, col(idx, "DESCRIPTION"), fmt, eval))
                        .quantity(getDecimal(row, col(idx, "QUANTITY"), fmt, eval))
                        .unit(getString(row, col(idx, "UNIT"), fmt, eval))
                        .invoiceValueFcc(getDecimal(row, col(idx, "INVOICE VALUE FCC", "INVOICE VALUE (IN FCC)"), fmt, eval))
                        .currencyCode(getString(row, col(idx, "CURRENCY CODE", "CURRENCY"), fmt, eval))
                        .fobInr(getDecimal(row, col(idx, "FOB INR", "FOB (INR)"), fmt, eval))
                        .pmvPerQty(getDecimal(row, col(idx, "PMV PER QTY", "PMV (PER QTY)"), fmt, eval))
                        .pmvActual(getDecimal(row, col(idx, "PMV ACTUAL"), fmt, eval))
                        
                        .build();

                rows.add(dto);
            }

            return rows.stream()
                    .sorted(Comparator.comparing(ExportDataDTO::getSbDate,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .collect(toList());
        }
    }

    private static Sheet pickSheet(Workbook wb) {
        for (String name : PREFERRED_SHEETS) {
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                if (wb.getSheetName(i).equalsIgnoreCase(name)) return wb.getSheetAt(i);
            }
        }
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            if (wb.getSheetName(i).toLowerCase(Locale.ROOT).contains("export")) {
                return wb.getSheetAt(i);
            }
        }
        throw new IllegalArgumentException("No 'Export' or 'Export Details' sheet found");
    }

    private static String normalize(String s) {
        if (s == null) return "";
        s = s.replace("&", " AND ");
        s = s.replace(".", " ");
        s = s.replaceAll("[^A-Za-z0-9]+", " ");
        return s.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private static Integer col(Map<String, Integer> idx, String... aliases) {
        for (String a : aliases) {
            Integer c = idx.get(normalize(a));
            if (c != null) return c;
        }
        return null;
    }

    private static String getString(Row r, Integer c, DataFormatter f, FormulaEvaluator e) {
        if (c == null) return "";
        Cell cell = r.getCell(c);
        return cell == null ? "" : f.formatCellValue(cell, e).trim();
    }

    private static BigDecimal getDecimal(Row r, Integer c, DataFormatter f, FormulaEvaluator e) {
        if (c == null) return null;
        Cell cell = r.getCell(c);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.FORMULA) {
                CellValue cv = e.evaluate(cell);
                if (cv == null) return null;
                switch (cv.getCellType()) {
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) return null;
                        return BigDecimal.valueOf(cv.getNumberValue());
                    case STRING:
                        return new BigDecimal(cv.getStringValue().trim());
                    case BOOLEAN:
                        return cv.getBooleanValue() ? BigDecimal.ONE : BigDecimal.ZERO;
                    default:
                        return null;
                }
            }
            if (cell.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) return null;
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                return new BigDecimal(cell.getStringCellValue().trim());
            } else if (cell.getCellType() == CellType.BOOLEAN) {
                return cell.getBooleanCellValue() ? BigDecimal.ONE : BigDecimal.ZERO;
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static LocalDate getDate(Row r, Integer c, DataFormatter f, FormulaEvaluator e) {
        if (c == null) return null;
        Cell cell = r.getCell(c);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String s = f.formatCellValue(cell, e).trim();
            if (s.isEmpty()) return null;
            for (DateTimeFormatter p : DATE_PATTERNS) {
                try { return LocalDate.parse(s, p); } catch (Exception ignore) {}
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}
