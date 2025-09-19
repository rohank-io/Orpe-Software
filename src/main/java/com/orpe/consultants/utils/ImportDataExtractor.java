package com.orpe.consultants.utils;

import com.orpe.consultants.dto.ImportDataDTO;
import com.orpe.consultants.dto.StockWiseEligibility;
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
public class ImportDataExtractor {

    private static final List<String> PREFERRED_SHEETS = List.of("IMPORT", "IMPORT DETAILS");

    private static final DateTimeFormatter[] DATE_PATTERNS = new DateTimeFormatter[] {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    public List<ImportDataDTO> parseImportSheet(MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = pickSheet(wb);

            DataFormatter fmt = new DataFormatter();
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();

            // Header map
            Row header = sheet.getRow(0);
            if (header == null) throw new IllegalArgumentException("Header row missing");
            Map<String,Integer> idx = new HashMap<>();
            for (int c = 0; c < header.getLastCellNum(); c++) {
                String key = normalize(fmt.formatCellValue(header.getCell(c), eval));
                if (!key.isBlank()) idx.putIfAbsent(key, c);
            }

            List<ImportDataDTO> rows = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String beNo = getString(row, col(idx, "BE NO"), fmt, eval).trim();
                if (beNo.isEmpty()) continue; // skip empty rows

                ImportDataDTO dto = ImportDataDTO.builder()
                    .beNo(beNo)
                    .beDate(getDate(row, col(idx, "BE DATE"), fmt, eval))
                    .beMonth(getString(row, col(idx, "MONTH"), fmt, eval))
                    .beYear(getInteger(row, col(idx, "YEAR"), fmt, eval))
                    .claimRefNo(getString(row, col(idx, "CLAIM REF NO"), fmt, eval))
                    .claimYear(getString(row, col(idx, "CLAIM YEAR"), fmt, eval))
                    .portCode(getString(row, col(idx, "PORT CODE"), fmt, eval))
                    .countryOfOrigin(getString(row, col(idx, "COUNTRY OF ORIGIN"), fmt, eval))
                    .supplierNameAddress(getString(row, col(idx, "SUPPLIER NAME & ADDRESS"), fmt, eval))
                    .itchsCode(getString(row, col(idx, "ITCHS CODE","HS CODE","HS CD"), fmt, eval))
                    .itemDescription(getString(row, col(idx, "ITEM DESCRIPTION","ITEM DESCRIPTION."), fmt, eval))
                    .bomPartNo(getString(row, col(idx, "BOM PART NO"), fmt, eval))
                    .altBoePartNo(getString(row, col(idx, "ALTERNATE BOE PART NO"), fmt, eval))
                    .dbkPartNo(getString(row, col(idx, "DBK PART NO"), fmt, eval))
                    .quantity(getDecimal(row, col(idx, "QUANTITY","QTY"), fmt, eval))
                    .uom(getString(row, col(idx, "UOM","UNIT"), fmt, eval))
                    .assessableValue(getDecimal(row, col(idx, "ASSESSABLE VALUE","ASSESSABLE"), fmt, eval))
                    .bcdRate(getPercent(row, col(idx, "BCD RATE"), fmt, eval))
                    .bcd(getDecimal(row, col(idx, "BCD"), fmt, eval))
                    .swsRate(getPercent(row, col(idx, "SWS RATE"), fmt, eval))
                    .sws(getDecimal(row, col(idx, "SWS"), fmt, eval))
                    .addRate(getPercent(row, col(idx, "ADD RATE"), fmt, eval))
                    .addDuty(getDecimal(row, col(idx, "ADD","ADD DUTY","ADDL DUTY"), fmt, eval))
                    .igstRate(getPercent(row, col(idx, "IGST RATE"), fmt, eval))
                    .igst(getDecimal(row, col(idx, "IGST"), fmt, eval))
                    .totalDuty(getDecimal(row, col(idx, "TOTAL DUTY","TOTAL"), fmt, eval))
                    .notnNo(getString(row, col(idx,
                        "NOTN NO",
                        "NOTN NO(SHOULD AUTO FETCH FROM BOE)",
                        "NOTN NOSHOULD AUTO FETCH FROM BOE",
                        "NOTIFICATION NO"), fmt, eval))
                    .notnEligibility(getString(row, col(idx, "NOTN ELIGIBILITY","ELIGIBILITY"), fmt, eval))
                    .qtyOpeningBalance(getDecimal(row, col(idx, "QTY (OPENING BALANCE)","QTY OPENING BALANCE","OPENING BALANCE"), fmt, eval))
                    .qtyUsed(getDecimal(row, col(idx, "QTY USED","USED QTY"), fmt, eval))
                    .closingBalance(getDecimal(row, col(idx, "CLOSING BALANCE","QTY CLOSING BALANCE"), fmt, eval))
                    .stockWiseEligibility(parseEligibility(getString(row, col(idx, "STOCK WISE ELIGIBILITY","STOCK ELIGIBILITY"), fmt, eval)))
                    .dutyClaimedAmt(getDecimal(row, col(idx, "DUTY CLAIMED AMT","DUTY CLAIMED"), fmt, eval))
                    .build();

                rows.add(dto);
            }

            return rows.stream()
                .sorted(Comparator.comparing(ImportDataDTO::getBeDate,
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
            if (wb.getSheetName(i).toLowerCase(Locale.ROOT).contains("import")) {
                return wb.getSheetAt(i);
            }
        }
        throw new IllegalArgumentException("No 'Import' or 'Import Details' sheet found");
    }

    private static String normalize(String s) {
        if (s == null) return "";
        s = s.replace("&", " AND ");
        s = s.replace(".", " ");
        s = s.replaceAll("[^A-Za-z0-9]+", " ");
        return s.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private static Integer col(Map<String,Integer> idx, String... aliases) {
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

 // Keep this for general amounts (no percent scaling)
    private static BigDecimal getDecimal(Row r, Integer c, DataFormatter f, FormulaEvaluator e) {
        if (c == null) return null; // or BigDecimal.ZERO to preserve prior behavior
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
                        return parsePlainNumber(cv.getStringValue());
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
                return parsePlainNumber(cell.getStringCellValue());
            } else if (cell.getCellType() == CellType.BOOLEAN) {
                return cell.getBooleanCellValue() ? BigDecimal.ONE : BigDecimal.ZERO;
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    // Returns 7.5 for a 7.5% cell and 15 for 15%
    private static BigDecimal getPercent(Row r, Integer c, DataFormatter f, FormulaEvaluator e) {
        if (c == null) return null;
        Cell cell = r.getCell(c);
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.FORMULA) {
                CellValue cv = e.evaluate(cell);
                if (cv == null) return null;
                if (cv.getCellType() == CellType.NUMERIC) {
                    BigDecimal v = BigDecimal.valueOf(cv.getNumberValue()); // Excel fraction
                    String fmt = cell.getCellStyle() != null ? cell.getCellStyle().getDataFormatString() : null;
                    if (fmt != null && fmt.contains("%")) return v.multiply(BigDecimal.valueOf(100));
                    // Fallback: known rate column but no % format
                    return v.compareTo(BigDecimal.ONE) <= 0 ? v.multiply(BigDecimal.valueOf(100)) : v;
                } else if (cv.getCellType() == CellType.STRING) {
                    return parsePercentText(cv.getStringValue());
                }
                return null;
            }
            if (cell.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) return null;
                BigDecimal v = BigDecimal.valueOf(cell.getNumericCellValue());
                String fmt = cell.getCellStyle() != null ? cell.getCellStyle().getDataFormatString() : null;
                if (fmt != null && fmt.contains("%")) return v.multiply(BigDecimal.valueOf(100));
                return v.compareTo(BigDecimal.ONE) <= 0 ? v.multiply(BigDecimal.valueOf(100)) : v;
            } else if (cell.getCellType() == CellType.STRING) {
                return parsePercentText(cell.getStringCellValue());
            } else if (cell.getCellType() == CellType.BOOLEAN) {
                return cell.getBooleanCellValue() ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    // Text helpers
    private static BigDecimal parsePlainNumber(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("S") || s.equalsIgnoreCase("NA") || s.equals("-")) return null;
        boolean neg = s.startsWith("(") && s.endsWith(")");
        if (neg) s = s.substring(1, s.length() - 1);
        s = s.replace(",", "").replace("−", "-");
        if (s.endsWith("%")) s = s.substring(0, s.length() - 1).trim(); // just in case
        if (s.startsWith(".")) s = "0" + s;
        BigDecimal val = new BigDecimal(s);
        return neg ? val.negate() : val;
    }

    private static BigDecimal parsePercentText(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("S") || s.equalsIgnoreCase("NA") || s.equals("-")) return null;
        boolean neg = s.startsWith("(") && s.endsWith(")");
        if (neg) s = s.substring(1, s.length() - 1);
        s = s.replace(",", "").replace("−", "-").replace("%", "").trim();
        if (s.startsWith(".")) s = "0" + s;
        BigDecimal val = new BigDecimal(s); // already 7.5 or 15, not 0.075
        return neg ? val.negate() : val;
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

    private static Integer getInteger(Row r, Integer c, DataFormatter f, FormulaEvaluator e) {
        String txt = getString(r, c, f, e).trim();
        if (txt.isEmpty()) return null;
        try { return Integer.parseInt(txt.replaceAll("\\D","")); } catch (Exception ex) { return null; }
    }

    private static StockWiseEligibility parseEligibility(String s) {
        if (s == null) return null;
        s = s.trim().toUpperCase();
        if (s.startsWith("OPEN")) return StockWiseEligibility.OPEN;
        if (s.startsWith("CLOSE")) return StockWiseEligibility.CLOSED;
        return null;
    }
}
