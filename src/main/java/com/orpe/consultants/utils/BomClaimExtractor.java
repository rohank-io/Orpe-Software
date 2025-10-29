package com.orpe.consultants.utils;

import com.orpe.consultants.dto.BomClaimDTO;
import com.orpe.consultants.exception.ExcelParseException;


import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.*;

/**
 * ✅ Production-ready Excel extractor for BOM CLAIM data.
 * Supports header auto-detection, locale-aware numeric parsing,
 * formula evaluation, validation, and structured error reporting.
 */
@Service
public class BomClaimExtractor {

    private static final Logger log = LoggerFactory.getLogger(BomClaimExtractor.class);

    // === CONFIG ===
    private static final long MAX_FILE_BYTES = 8L * 1024 * 1024; // 8 MB
    private static final int MAX_ROWS_TO_PARSE = 200_000;
    private static final int MAX_HEADER_SCAN_ROW = 10;
    private static final List<String> PREFERRED_SHEETS = List.of("BOMCLAIM", "BOM CLAIM", "CLAIM DATA", "BOM");
    private static final Set<String> ACCEPTED_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "application/octet-stream"
    );

    // === Header aliases ===
    private static final String[] CLAIM_REF_ALIASES = {"CLAIM REF NO", "CLAIMREF", "CLAIM REF", "CLAIM REF#"};
    private static final String[] CLAIM_YEAR_ALIASES = {"CLAIM YEAR", "YEAR"};
    private static final String[] MATERIAL_DESC_ALIASES = {"MATERIAL DESCRIPTION", "DESCRIPTION", "MATERIAL"};
    private static final String[] BOM_PART_NO_ALIASES = {"BOM PART NO", "PART NO", "BOM PART"};
    private static final String[] ALTERNATE_BOE_ALIASES = {"ALTERNATE BOE PART NO", "ALT BOE PART NO", "ALTERNATE PART NO"};
    private static final String[] DBK_PART_NO_ALIASES = {"DBK PART NO", "DBK PART"};
    private static final String[] IMPORTED_INDIGENOUS_ALIASES = {"WHETHER IMPORTED INDIGENOUS", "IMPORTED INDIGENOUS", "IMPORTED/INDIGENOUS"};
    private static final String[] UNIT_ALIASES = {"UNIT", "UOM"};
    private static final String[] BOE_NO_ALIASES = {"BOE NO", "BOE"};
    private static final String[] USED_QTY_ALIASES = {"USED QTY", "QTY USED", "USEDQTY", "QTY"};
    private static final String[] EXPORT_MODEL_NO_ALIASES = {"EXPORT MODEL NO", "EXPORT MODEL"};
    private static final String[] SB_NO_ALIASES = {"SB NO", "SB"};

    /**
     * Parses uploaded Excel file and returns structured ParseResult.
     */
    public ParseResult<BomClaimDTO> parseImportSheet(MultipartFile file, Locale locale) {
        Objects.requireNonNull(file, "file must not be null");
        if (locale == null) locale = Locale.getDefault();

        String originalFilename = file.getOriginalFilename();
        String baseFilename = deriveBaseFilename(originalFilename);

        // === Validation ===
        if (file.getSize() <= 0) {
            throw new ExcelParseException("Uploaded file is empty: " + originalFilename);
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ExcelParseException("File too large (" + file.getSize() + " bytes). Max allowed: " + MAX_FILE_BYTES);
        }
        String contentType = file.getContentType();
        if (contentType != null && !ACCEPTED_CONTENT_TYPES.contains(contentType)) {
            log.warn("Suspicious content type '{}'; continuing...", contentType);
        }

        List<BomClaimDTO> allRows = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        // === Workbook processing ===
        try (InputStream inRaw = file.getInputStream();
             BufferedInputStream in = new BufferedInputStream(inRaw);
             Workbook workbook = WorkbookFactory.create(in)) {

            DataFormatter dataFormatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
                Sheet sheet = workbook.getSheetAt(si);
                if (sheet == null) continue;
                String sheetName = sheet.getSheetName();
                if (!isBomSheetName(sheetName)) continue;

                ParseResult<BomClaimDTO> partial =
                        parseSingleSheet(sheet, baseFilename, dataFormatter, evaluator, locale);
                allRows.addAll(partial.getRows());
                errors.addAll(partial.getErrors());

                if (allRows.size() > MAX_ROWS_TO_PARSE) {
                    throw new ExcelParseException("Too many rows (" + allRows.size() + ")");
                }
            }
        }  catch (IOException e) {
            throw new ExcelParseException("Failed to read Excel: " + e.getMessage(), e);
        }

        return new ParseResult<>(allRows, errors);
    }

    // === Internal helpers ===

    private String deriveBaseFilename(String originalFilename) {
        if (originalFilename == null) return "";
        int dot = originalFilename.lastIndexOf('.');
        return dot > 0 ? originalFilename.substring(0, dot) : originalFilename;
    }

    private boolean isBomSheetName(String sheetName) {
        if (sheetName == null) return false;
        String upper = sheetName.toUpperCase(Locale.ROOT).trim();
        for (String s : PREFERRED_SHEETS) if (upper.equals(s)) return true;
        return upper.contains("BOM") || upper.contains("CLAIM");
    }

    private ParseResult<BomClaimDTO> parseSingleSheet(Sheet sheet, String clientName,
                                                      DataFormatter fmt, FormulaEvaluator eval, Locale locale) {
        List<BomClaimDTO> rows = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        int headerRowIndex = detectHeaderRow(sheet, fmt, eval);
        if (headerRowIndex < 0) {
            errors.add(new ParseError(sheet.getSheetName(), -1, null, "Header row not found"));
            return new ParseResult<>(rows, errors);
        }

        Row header = sheet.getRow(headerRowIndex);
        Map<String, Integer> idx = buildHeaderMap(header, fmt, eval);

        for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
            if (rows.size() > MAX_ROWS_TO_PARSE) break;
            Row row = sheet.getRow(r);
            if (row == null) continue;

            try {
                String claimRefNo = safeGetString(row, firstCol(idx, CLAIM_REF_ALIASES), fmt, eval);
                if (isBlank(claimRefNo)) continue;

                BomClaimDTO dto = BomClaimDTO.builder()
                        .claimRefNo(claimRefNo)
                        .claimYear(safeGetString(row, firstCol(idx, CLAIM_YEAR_ALIASES), fmt, eval))
                        .materialDescription(safeGetString(row, firstCol(idx, MATERIAL_DESC_ALIASES), fmt, eval))
                        .bomPartNo(safeGetString(row, firstCol(idx, BOM_PART_NO_ALIASES), fmt, eval))
                        .altBoePartNo(safeGetString(row, firstCol(idx, ALTERNATE_BOE_ALIASES), fmt, eval))
                        .dbkPartNo(safeGetString(row, firstCol(idx, DBK_PART_NO_ALIASES), fmt, eval))
                        .importedOrIndigenous(safeGetString(row, firstCol(idx, IMPORTED_INDIGENOUS_ALIASES), fmt, eval))
                        .unit(safeGetString(row, firstCol(idx, UNIT_ALIASES), fmt, eval))
                        .boeNo(safeGetString(row, firstCol(idx, BOE_NO_ALIASES), fmt, eval))
                        .usedQty(safeGetDecimal(row, firstCol(idx, USED_QTY_ALIASES), fmt, eval, locale))
                        .exportModelNo(safeGetString(row, firstCol(idx, EXPORT_MODEL_NO_ALIASES), fmt, eval))
                        .sbNo(safeGetString(row, firstCol(idx, SB_NO_ALIASES), fmt, eval))
                        .clientName(clientName)
                        .build();

                rows.add(dto);

            } catch (Exception ex) {
                errors.add(new ParseError(sheet.getSheetName(), r, null, ex.getMessage()));
                log.warn("Parse error at row {}: {}", r, ex.getMessage());
            }
        }
        return new ParseResult<>(rows, errors);
    }

    // === Header detection ===
    private int detectHeaderRow(Sheet sheet, DataFormatter fmt, FormulaEvaluator eval) {
        int last = Math.min(sheet.getLastRowNum(), MAX_HEADER_SCAN_ROW);
        for (int r = 0; r <= last; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Map<String, Integer> candidate = buildHeaderMap(row, fmt, eval);
            if (firstCol(candidate, CLAIM_REF_ALIASES) != null ||
                firstCol(candidate, BOM_PART_NO_ALIASES) != null ||
                firstCol(candidate, USED_QTY_ALIASES) != null) {
                return r;
            }
        }
        return -1;
    }

    private Map<String, Integer> buildHeaderMap(Row header, DataFormatter fmt, FormulaEvaluator eval) {
        Map<String, Integer> idx = new HashMap<>();
        short last = header.getLastCellNum();
        for (int c = 0; c < last; c++) {
            Cell cell = header.getCell(c, MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) continue;
            String norm = normalize(fmt.formatCellValue(cell, eval));
            if (!norm.isBlank()) idx.putIfAbsent(norm, c);
        }
        return idx;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFKD);
        t = t.replaceAll("\\p{M}", "");
        t = t.replace("&", " AND ");
        t = t.replace(".", " ");
        t = t.replaceAll("[^A-Za-z0-9]+", " ");
        return t.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static Integer firstCol(Map<String, Integer> idx, String... aliases) {
        for (String a : aliases) {
            Integer i = idx.get(normalize(a));
            if (i != null) return i;
        }
        return null;
    }

    private static String safeGetString(Row r, Integer c, DataFormatter f, FormulaEvaluator e) {
        if (c == null) return "";
        Cell cell = r.getCell(c, MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        try {
            return f.formatCellValue(cell, e).trim();
        } catch (Exception ex) {
            return "";
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private BigDecimal safeGetDecimal(Row r, Integer c, DataFormatter f, FormulaEvaluator evaluator, Locale locale) {
        if (c == null) return null;
        Cell cell = r.getCell(c, MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        try {
            CellType type = cell.getCellType();
            if (type == CellType.FORMULA) {
                CellValue cv = evaluator.evaluate(cell);
                if (cv == null) return null;
                if (cv.getCellType() == CellType.NUMERIC)
                    return BigDecimal.valueOf(cv.getNumberValue());
                if (cv.getCellType() == CellType.STRING)
                    return parseNumber(cv.getStringValue(), locale);
                if (cv.getCellType() == CellType.BOOLEAN)
                    return cv.getBooleanValue() ? BigDecimal.ONE : BigDecimal.ZERO;
                return null;
            } else if (type == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (type == CellType.STRING) {
                return parseNumber(cell.getStringCellValue(), locale);
            } else if (type == CellType.BOOLEAN) {
                return cell.getBooleanCellValue() ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        } catch (Exception ex) {
            log.warn("Decimal parse failed row={} col={}: {}", r.getRowNum(), c, ex.getMessage());
        }
        return null;
    }

    private static BigDecimal parseNumber(String raw, Locale locale) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("NA") || s.equalsIgnoreCase("-")) return null;
        boolean negative = s.startsWith("(") && s.endsWith(")");
        if (negative) s = s.substring(1, s.length() - 1);

        s = s.replace("\u2212", "-").replace("−", "-");
        String simple = s.replaceAll(",", "");
        if (simple.startsWith(".")) simple = "0" + simple;
        try {
            return negative ? new BigDecimal(simple).negate() : new BigDecimal(simple);
        } catch (NumberFormatException ignore) {
            // locale fallback
        }

        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(symbols);
        df.setParseBigDecimal(true);
        try {
            BigDecimal bd = (BigDecimal) df.parse(s);
            return negative ? bd.negate() : bd;
        } catch (ParseException ex) {
            String cleaned = s.replaceAll("[^0-9.\\-]", "");
            if (cleaned.isEmpty()) return null;
            try {
                BigDecimal bd = new BigDecimal(cleaned);
                return negative ? bd.negate() : bd;
            } catch (NumberFormatException e) {
                log.warn("Cannot parse numeric '{}'", raw);
                return null;
            }
        }
    }
}
