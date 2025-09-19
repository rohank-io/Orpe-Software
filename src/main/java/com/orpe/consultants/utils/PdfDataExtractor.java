package com.orpe.consultants.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfDataExtractor {

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
        DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)
    };

    private static LocalDate parseDateFlexible(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr.trim().toUpperCase(), formatter);
            } catch (Exception e) {
                // Continue trying other formats
            }
        }
        return null;
    }

    private static String extractSingleLineValue(String text, String label) {
        // Matches label followed by newline and captures next line content
        Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(label) + "\\s*\\n\\s*([^\n]+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    public static String extractSbNo(String text) {
        return extractSingleLineValue(text, "SB No");
    }

    public static LocalDate extractSbDate(String text) {
        String dateStr = extractSingleLineValue(text, "SB Date");
        return parseDateFlexible(dateStr);
    }

    public static String extractPortCode(String text) {
        return extractSingleLineValue(text, "Port Code");
    }

    public static LocalDate extractLeoDate(String text) {
        String dateStr = extractSingleLineValue(text, "LEO Date");
        return parseDateFlexible(dateStr);
    }

    public static LocalDate extractBrcRealisationDate(String text) {
        String dateStr = extractSingleLineValue(text, "BRC Realisation Date");
        return parseDateFlexible(dateStr);
    }

    public static String extractInvoiceNoDate(String text) {
        return extractSingleLineValue(text, "INVOICE No. & Dt.");
    }

    public static BigDecimal extractExchangeRate(String text) {
        String val = extractSingleLineValue(text, "EXCHANGE RATE");
        return parseBigDecimal(val);
    }

    public static BigDecimal extractInvoiceValue(String text) {
        String val = extractSingleLineValue(text, "INVOICE VALUE");
        return parseBigDecimal(val);
    }

    public static String extractCurrency(String text) {
        return extractSingleLineValue(text, "CURRENCY");
    }

    public static String extractHsCd(String text) {
        return extractSingleLineValue(text, "HS CD");
    }

    public static String extractDescription(String text) {
        // If description might be multiline, consider more complex regex here
        return extractSingleLineValue(text, "DESCRIPTION");
    }

    public static String extractModelNo(String text) {
        return extractSingleLineValue(text, "MODEL no.");
    }

    public static BigDecimal extractQuantity(String text) {
        String val = extractSingleLineValue(text, "QUANTITY");
        return parseBigDecimal(val);
    }

    public static String extractUnit(String text) {
        return extractSingleLineValue(text, "UNIT");
    }

    public static BigDecimal extractFob(String text) {
        String val = extractSingleLineValue(text, "FOB");
        return parseBigDecimal(val);
    }

    public static BigDecimal extractPmvPerUnit(String text) {
        String val = extractSingleLineValue(text, "PMV \\(per qty\\)");
        return parseBigDecimal(val);
    }

    public static String extractSchemeDescription(String text) {
        return extractSingleLineValue(text, "SCHEME DESCRIPTION");
    }

    public static String extractDbkSno(String text) {
        return extractSingleLineValue(text, "DBK SNO.");
    }

    public static BigDecimal extractRate(String text) {
        String val = extractSingleLineValue(text, "RATE");
        return parseBigDecimal(val);
    }

    public static BigDecimal extractDbkAmtSb(String text) {
        String val = extractSingleLineValue(text, "DBK AMT \\(AIR\\) Given in SB");
        return parseBigDecimal(val);
    }

    public static String extractBuyerDetails(String text) {
        // Capture multiline text from BUYER'S NAME & ADDRESS up to next known label
        Pattern pattern = Pattern.compile("(?i)BUYER'S NAME & ADDRESS\\s*(.*?)\\s*(EXCHANGE RATE|INVOICE VALUE|\\n[A-Z ]{3,})", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String buyerRaw = matcher.group(1);
            return buyerRaw.replaceAll("\\s+", " ").trim();
        }
        return null;
    }


    private static BigDecimal parseBigDecimal(String val) {
        try {
            if (val == null || val.isEmpty()) return null;
            // Remove non-numeric except dots and commas, commas replaced with empty for decimal support
            String cleaned = val.replaceAll("[^\\d.,-]", "").replace(",", "");
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return null;
        }
    }
}
