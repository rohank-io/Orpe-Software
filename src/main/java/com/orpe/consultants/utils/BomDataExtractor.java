package com.orpe.consultants.utils;

import com.orpe.consultants.dto.BomDataDTO;
import com.orpe.consultants.dto.ExportModelQuantityDTO;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BomDataExtractor {

    private static final List<String> PREFERRED_SHEETS = List.of("BOM", "BOM DETAILS", "BILL OF MATERIAL");

    public List<BomDataDTO> parseBomSheet(MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            List<BomDataDTO> allRows = new ArrayList<>();

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                System.out.println("Checking sheet: " + sheetName);
                if (isBomSheetName(sheetName)) {
                    System.out.println("Parsing sheet: " + sheetName);
                    List<BomDataDTO> sheetRows = parseBomSheetFromSingleSheet(sheet);
                    System.out.println("Rows parsed from sheet " + sheetName + " = " + sheetRows.size());
                    allRows.addAll(sheetRows);
                }
            }

            return allRows;
        }
    }

    private boolean isBomSheetName(String sheetName) {
        if (sheetName == null) return false;
        String upperName = sheetName.toUpperCase(Locale.ROOT);
        for (String name : PREFERRED_SHEETS) {
            if (upperName.equals(name)) return true; // exact match
        }
        // Check if 'BOM' is anywhere in the name
        return upperName.contains("BOM");
    }


    private List<BomDataDTO> parseBomSheetFromSingleSheet(Sheet sheet) {
        DataFormatter fmt = new DataFormatter();
        FormulaEvaluator eval = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();

        Row header = sheet.getRow(0);
        if (header == null)
            throw new IllegalArgumentException("Header row missing in sheet: " + sheet.getSheetName());

        Map<String, Integer> idx = new HashMap<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String norm = normalize(fmt.formatCellValue(header.getCell(c), eval));
            if (!norm.isBlank())
                idx.putIfAbsent(norm, c);
        }

        List<BomDataDTO> rows = new ArrayList<>();
        // Second row (index 1) contains actual model numbers for dynamic export model columns
        Row modelNumberRow = sheet.getRow(1);

        for (int r = 2; r <= sheet.getLastRowNum(); r++) { // data rows start from index 2 now
            Row row = sheet.getRow(r);
            if (row == null)
                continue;

            String claimRefNo = getString(row, col(idx, "CLAIM REF NO"), fmt, eval);
            String bomPartNo = getString(row, col(idx, "BOM PART NO"), fmt, eval);
            if (claimRefNo.isBlank() && bomPartNo.isBlank())
                continue;

            BomDataDTO.BomDataDTOBuilder builder = BomDataDTO.builder().claimRefNo(claimRefNo)
                    .claimYear(getString(row, col(idx, "CLAIM YEAR"), fmt, eval))
                    .materialDesc(getString(row, col(idx, "MATERIAL DESCRIPTION"), fmt, eval)).bomPartNo(bomPartNo)
                    .alternateBoePartNo(getString(row, col(idx, "ALTERNATE BOE PART NO"), fmt, eval))
                    .dbkPartNo(getString(row, col(idx, "DBK PART NO"), fmt, eval))
                    .importedIndigenous(getString(row, col(idx, "WHETHER IMPORTED INDIGENOUS"), fmt, eval))
                    .unit(getString(row, col(idx, "UNIT", "UOM"), fmt, eval))
                    .grandTotal(getDecimal(row, col(idx, "GRAND TOTAL"), fmt, eval))
                    .netWeightKg(getDecimal(row,
                            col(idx, "NET WEIGHT OF THE MATERIAL IN KGS", "NET WEIGHT KG", "NET WEIGHT"), fmt,
                            eval));

            List<ExportModelQuantityDTO> exportModels = new ArrayList<>();

            for (Map.Entry<String, Integer> colEntry : idx.entrySet()) {
                String normalizedHeader = colEntry.getKey();
                if (normalizedHeader.startsWith("EXPORT MODEL")) {
                    int colIndex = colEntry.getValue();

                    String modelNo = "";
                    if (modelNumberRow != null) {
                        Cell modelCell = modelNumberRow.getCell(colIndex);
                        if (modelCell != null) {
                            modelNo = fmt.formatCellValue(modelCell).trim();
                        }
                    }
                    if (modelNo.isBlank())
                        continue;

                    BigDecimal qty = getDecimal(row, colIndex, fmt, eval);
                    if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0) {
                        exportModels.add(ExportModelQuantityDTO.builder().modelNo(modelNo).quantity(qty).build());
                    }
                }
            }

            builder.exportModels(exportModels);
            rows.add(builder.build());
        }
        return rows;
    }

    private static String normalize(String s) {
        if (s == null)
            return "";
        s = s.replace("&", " AND ");
        s = s.replace(".", " ");
        s = s.replaceAll("[^A-Za-z0-9]+", " ");
        return s.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private static Integer col(Map<String, Integer> idx, String... aliases) {
        for (String a : aliases) {
            Integer c = idx.get(normalize(a));
            if (c != null)
                return c;
        }
        return null;
    }

    private static String getString(Row r, Integer c, DataFormatter f, FormulaEvaluator e) {
        if (c == null)
            return "";
        Cell cell = r.getCell(c);
        return cell == null ? "" : f.formatCellValue(cell, e).trim();
    }

    private static BigDecimal getDecimal(Row r, Integer c, DataFormatter f, FormulaEvaluator er) {
        if (c == null)
            return null;
        Cell cell = r.getCell(c);
        if (cell == null)
            return null;
        try {
            if (cell.getCellType() == CellType.FORMULA) {
                CellValue cv = er.evaluate(cell);
                if (cv == null)
                    return null;
                if (cv.getCellType() == CellType.NUMERIC)
                    return BigDecimal.valueOf(cv.getNumberValue());
                if (cv.getCellType() == CellType.STRING)
                    return parsePlainNumber(cv.getStringValue());
                if (cv.getCellType() == CellType.BOOLEAN)
                    return cv.getBooleanValue() ? BigDecimal.ONE : BigDecimal.ZERO;
                return null;
            }
            if (cell.getCellType() == CellType.NUMERIC)
                return BigDecimal.valueOf(cell.getNumericCellValue());
            if (cell.getCellType() == CellType.STRING)
                return parsePlainNumber(cell.getStringCellValue());
            if (cell.getCellType() == CellType.BOOLEAN)
                return cell.getBooleanCellValue() ? BigDecimal.ONE : BigDecimal.ZERO;
        } catch (Exception ex) {
            // Log or ignore
        }
        return null;
    }

    private static BigDecimal parsePlainNumber(String raw) {
        if (raw == null)
            return null;
        String s = raw.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("NA") || s.equalsIgnoreCase("-") || s.equalsIgnoreCase("S"))
            return null;
        boolean neg = s.startsWith("(") && s.endsWith(")");
        if (neg)
            s = s.substring(1, s.length() - 1);
        s = s.replace(",", "").replace("−", "-");
        if (s.startsWith("."))
            s = "0" + s;
        BigDecimal val = new BigDecimal(s);
        return neg ? val.negate() : val;
    }
}
