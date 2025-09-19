package com.orpe.consultants.utils;

import com.orpe.consultants.model.ShippingBill;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelShippingBillExtractor {

    public static ShippingBill extractFromExcel(InputStream inputStream) throws Exception {
        ShippingBill bill = new ShippingBill();
        Workbook workbook = new XSSFWorkbook(inputStream);

        try {
            // Search through all sheets dynamically
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                extractDataFromSheet(sheet, bill);
            }
        } finally {
            workbook.close();
        }

        return bill;
    }

    private static void extractDataFromSheet(Sheet sheet, ShippingBill bill) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            for (int colIndex = 0; colIndex < row.getLastCellNum(); colIndex++) {
                Cell cell = row.getCell(colIndex);
                if (cell == null) continue;

                String cellValue = getCellValueAsString(cell).trim();
                
                // Dynamic extraction based on field labels
                extractFieldData(sheet, rowIndex, colIndex, cellValue, bill);
            }
        }
    }

    private static void extractFieldData(Sheet sheet, int rowIndex, int colIndex, String cellValue, ShippingBill bill) {
        
        // Extract SB No - look for "SB No" label and get adjacent value
        if ("SB No".equalsIgnoreCase(cellValue) && bill.getSbNo() == null) {
            String sbNo = findAdjacentValue(sheet, rowIndex, colIndex, "\\d+");
            bill.setSbNo(sbNo);
        }

        // Extract SB Date - look for "SB Date" label and get adjacent date
        if ("SB Date".equalsIgnoreCase(cellValue) && bill.getSbDate() == null) {
            LocalDate sbDate = findAdjacentDate(sheet, rowIndex, colIndex);
            bill.setSbDate(sbDate);
        }

        // Extract Port Code - look for "Port Code" label and get adjacent code
        if ("Port Code".equalsIgnoreCase(cellValue) && bill.getPortCode() == null) {
            String portCode = findAdjacentValue(sheet, rowIndex, colIndex, "[A-Z]{5}\\d?");
            bill.setPortCode(portCode);
        }

        // Extract LEO Date - look for "LEO Date" or "6.LEO Date." label
        if ((cellValue.contains("LEO Date") || cellValue.equals("6.LEO Date.")) && bill.getLeoDate() == null) {
            LocalDate leoDate = findAdjacentDate(sheet, rowIndex, colIndex);
            bill.setLeoDate(leoDate);
        }

        // Extract BRC Realisation Date
        if (cellValue.contains("BRC Realisation Date") && bill.getBrcRealisationDate() == null) {
            LocalDate brcDate = findAdjacentDate(sheet, rowIndex, colIndex);
            bill.setBrcRealisationDate(brcDate);
        }

        // Extract Invoice No & Date - look for "INVOICE No. & Dt." or similar patterns
        if (cellValue.contains("INVOICE No") && bill.getInvoiceNoDate() == null) {
            String invoiceNoDate = findAdjacentValue(sheet, rowIndex, colIndex, "[A-Z]{2}\\d+\\s+\\d{2}/\\d{2}/\\d{4}");
            bill.setInvoiceNoDate(invoiceNoDate);
        }

        // Extract Buyer Details - look for "BUYER'S NAME" or buyer company names
        if (cellValue.contains("BUYER'S NAME") && bill.getBuyerDetails() == null) {
            String buyerDetails = extractMultilineBuyerDetails(sheet, rowIndex, colIndex);
            bill.setBuyerDetails(buyerDetails);
        }

        // Extract Exchange Rate - look for "EXCHANGE RATE" label
        if (cellValue.contains("EXCHANGE RATE") && bill.getExchangeRate() == null) {
            BigDecimal exchangeRate = findAdjacentNumericValue(sheet, rowIndex, colIndex);
            bill.setExchangeRate(exchangeRate);
        }

        // Extract Invoice Value - look for "INVOICE VALUE" label
        if (cellValue.contains("INVOICE VALUE") && bill.getInvoiceValue() == null) {
            BigDecimal invoiceValue = findAdjacentNumericValue(sheet, rowIndex, colIndex);
            bill.setInvoiceValue(invoiceValue);
        }

        // Extract Currency - look for "CURRENCY" label or common currency codes
        if ((cellValue.contains("CURRENCY") || cellValue.matches("USD|EUR|INR|GBP")) && bill.getCurrency() == null) {
            String currency = findAdjacentValue(sheet, rowIndex, colIndex, "USD|EUR|INR|GBP|AUD|CAD");
            if (currency == null && cellValue.matches("USD|EUR|INR|GBP")) {
                currency = cellValue;
            }
            bill.setCurrency(currency);
        }

        // Extract HS Code - look for "HS CD" or "HS CODE" label
        if (cellValue.contains("HS CD") && bill.getHsCd() == null) {
            String hsCode = findAdjacentValue(sheet, rowIndex, colIndex, "\\d{8}");
            bill.setHsCd(hsCode);
        }

        // Extract Description - look for "DESCRIPTION" label
        if (cellValue.contains("DESCRIPTION") && bill.getDescription() == null) {
            String description = extractMultilineDescription(sheet, rowIndex, colIndex);
            bill.setDescription(description);
        }

        // Extract Model No - look for model patterns in descriptions or dedicated fields
        if (bill.getModelNo() == null && (cellValue.matches(".*[A-Z]\\d+[A-Z]?\\d+.*") || cellValue.contains("FQ-"))) {
            String modelNo = extractModelNumber(cellValue);
            bill.setModelNo(modelNo);
        }

        // Extract Quantity - look for "QUANTITY" label
        if (cellValue.contains("QUANTITY") && bill.getQuantity() == null) {
            BigDecimal quantity = findAdjacentNumericValue(sheet, rowIndex, colIndex);
            bill.setQuantity(quantity);
        }

        // Extract Unit - look for "UQC" or "UNIT" label
        if ((cellValue.contains("UQC") || cellValue.contains("UNIT")) && bill.getUnit() == null) {
            String unit = findAdjacentValue(sheet, rowIndex, colIndex, "PCS|KGS|NOS|MTR|LTR");
            bill.setUnit(unit);
        }

        // Extract FOB - look for "FOB" label
        if (cellValue.contains("FOB") && bill.getFob() == null) {
            BigDecimal fob = findAdjacentNumericValue(sheet, rowIndex, colIndex);
            bill.setFob(fob);
        }

        // Extract PMV - look for "PMV" label
        if (cellValue.contains("PMV") && bill.getPmvPerUnit() == null) {
            BigDecimal pmv = findAdjacentNumericValue(sheet, rowIndex, colIndex);
            bill.setPmvPerUnit(pmv);
        }

        // Extract Scheme Description - look for "SCHEME DESCRIPTION" label
        if (cellValue.contains("SCHEME DESCRIPTION") && bill.getSchemeDescription() == null) {
            String scheme = findAdjacentValue(sheet, rowIndex, colIndex, "Drawback|MEIS|ROSL|RODTEP");
            bill.setSchemeDescription(scheme);
        }

        // Extract DBK SNO - look for "DBK SNO" label
        if (cellValue.contains("DBK SNO") && bill.getDbkSno() == null) {
            String dbkSno = findAdjacentValue(sheet, rowIndex, colIndex, "\\d+[A-Z]?");
            bill.setDbkSno(dbkSno);
        }

        // Extract Rate - look for "RATE" label in context
        if (cellValue.equals("RATE") && bill.getRate() == null) {
            BigDecimal rate = findAdjacentNumericValue(sheet, rowIndex, colIndex);
            bill.setRate(rate);
        }

        // Extract DBK Amount - look for "DBK AMT" label
        if (cellValue.contains("DBK AMT") && bill.getDbkAmtSb() == null) {
            BigDecimal dbkAmt = findAdjacentNumericValue(sheet, rowIndex, colIndex);
            bill.setDbkAmtSb(dbkAmt);
        }
    }

    // Helper method to find adjacent value based on pattern
    private static String findAdjacentValue(Sheet sheet, int rowIndex, int colIndex, String pattern) {
        Pattern regex = Pattern.compile(pattern);
        
        // Check adjacent cells (right, down, next row same column)
        int[][] directions = {{0, 1}, {0, 2}, {1, 0}, {1, 1}, {0, -1}};
        
        for (int[] dir : directions) {
            try {
                Row targetRow = sheet.getRow(rowIndex + dir[0]);
                if (targetRow != null) {
                    Cell targetCell = targetRow.getCell(colIndex + dir[1]);
                    if (targetCell != null) {
                        String value = getCellValueAsString(targetCell).trim();
                        Matcher matcher = regex.matcher(value);
                        if (matcher.find()) {
                            return matcher.group();
                        }
                    }
                }
            } catch (Exception e) {
                // Continue to next direction
            }
        }
        return null;
    }

    // Helper method to find adjacent date
    private static LocalDate findAdjacentDate(Sheet sheet, int rowIndex, int colIndex) {
        int[][] directions = {{0, 1}, {0, 2}, {1, 0}, {1, 1}, {0, -1}};
        
        for (int[] dir : directions) {
            try {
                Row targetRow = sheet.getRow(rowIndex + dir[0]);
                if (targetRow != null) {
                    Cell targetCell = targetRow.getCell(colIndex + dir[1]);
                    if (targetCell != null) {
                        if (targetCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(targetCell)) {
                            return targetCell.getLocalDateTimeCellValue().toLocalDate();
                        }
                        String value = getCellValueAsString(targetCell).trim();
                        LocalDate date = parseFlexibleDate(value);
                        if (date != null) {
                            return date;
                        }
                    }
                }
            } catch (Exception e) {
                // Continue to next direction
            }
        }
        return null;
    }

    // Helper method to find adjacent numeric value
    private static BigDecimal findAdjacentNumericValue(Sheet sheet, int rowIndex, int colIndex) {
        int[][] directions = {{0, 1}, {0, 2}, {1, 0}, {1, 1}, {0, -1}};
        
        for (int[] dir : directions) {
            try {
                Row targetRow = sheet.getRow(rowIndex + dir[0]);
                if (targetRow != null) {
                    Cell targetCell = targetRow.getCell(colIndex + dir[1]);
                    if (targetCell != null) {
                        if (targetCell.getCellType() == CellType.NUMERIC) {
                            return BigDecimal.valueOf(targetCell.getNumericCellValue());
                        }
                        String value = getCellValueAsString(targetCell).trim();
                        BigDecimal numericValue = parseNumericValue(value);
                        if (numericValue != null) {
                            return numericValue;
                        }
                    }
                }
            } catch (Exception e) {
                // Continue to next direction
            }
        }
        return null;
    }

    // Helper method to extract multiline buyer details
    private static String extractMultilineBuyerDetails(Sheet sheet, int rowIndex, int colIndex) {
        StringBuilder buyer = new StringBuilder();
        
        for (int i = 0; i < 5; i++) {
            try {
                Row targetRow = sheet.getRow(rowIndex + i);
                if (targetRow != null) {
                    for (int j = 0; j < 5; j++) {
                        Cell targetCell = targetRow.getCell(colIndex + j);
                        if (targetCell != null) {
                            String value = getCellValueAsString(targetCell).trim();
                            if (value.length() > 3 && !value.matches("\\d+") && 
                                !value.contains("BUYER'S NAME")) {
                                if (buyer.length() > 0) {
                                    buyer.append(" ");
                                }
                                buyer.append(value);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                break;
            }
        }
        return buyer.length() > 0 ? buyer.toString() : null;
    }

    // Helper method to extract multiline description
    private static String extractMultilineDescription(Sheet sheet, int rowIndex, int colIndex) {
        StringBuilder desc = new StringBuilder();
        
        for (int i = 0; i < 4; i++) {
            try {
                Row targetRow = sheet.getRow(rowIndex + i);
                if (targetRow != null) {
                    for (int j = 0; j < 4; j++) {
                        Cell targetCell = targetRow.getCell(colIndex + j);
                        if (targetCell != null) {
                            String value = getCellValueAsString(targetCell).trim();
                            if (value.length() > 5 && !value.contains("DESCRIPTION")) {
                                if (desc.length() > 0) {
                                    desc.append(" ");
                                }
                                desc.append(value);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                break;
            }
        }
        return desc.length() > 0 ? desc.toString() : null;
    }

    // Helper method to extract model number from text
    private static String extractModelNumber(String text) {
        Pattern[] patterns = {
            Pattern.compile("FQ-[A-Z]?\\d+"),
            Pattern.compile("A5E\\d+"),
            Pattern.compile("[A-Z]{2,4}\\d{4,}")
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    // Utility methods
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double numericValue = cell.getNumericCellValue();
                if (numericValue == (long) numericValue) {
                    return String.valueOf((long) numericValue);
                } else {
                    return String.valueOf(numericValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getCellFormula();
                }
            default:
                return "";
        }
    }

    private static LocalDate parseFlexibleDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (Exception e) {
                // Try next formatter
            }
        }
        return null;
    }

    private static BigDecimal parseNumericValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        
        try {
            // Remove common non-numeric characters but keep decimal points and minus signs
            String cleaned = value.replaceAll("[^\\d.-]", "");
            if (cleaned.isEmpty()) return null;
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return null;
        }
    }
}
