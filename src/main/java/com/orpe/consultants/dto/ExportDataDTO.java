package com.orpe.consultants.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportDataDTO {

    private Long exportId;

    // SB No
    private String sbNo;

    // SB Date
    private LocalDate sbDate;

    // Port Code
    private String portCode;

    // CUSTOMER NAME
    private String customerName;

    // LEO Date.
    private LocalDate leoDate;

    // Claim Ref. no.
    private String claimRefNo;

    // Claim Year
    private String claimYear;

    // SCHEME DESCRIPTION
    private String schemeDescription;

    // DBK SNO.
    private String dbkSno;

    // DBK Applicability
    private String dbkApplicability; // String instead of enum for flexibility in DTO

    // RATE
    private BigDecimal rate;

    // AIR Given in SB
    private BigDecimal airGivenInSb;

    // AIR AMOUNT
    private BigDecimal airAmount;

    // Difference
    private BigDecimal difference;

    // Total DBK
    private BigDecimal totalDbk;

    // SBR
    private BigDecimal sbr;

    // ARO No.
    private String aroNo;

    // ARO DATE
    private LocalDate aroDate;

    // ARO File NO.
    private String aroFileNo;

    // ARO File DATE
    private LocalDate aroFileDate;

    // BRC no.
    private String brcNo;

    // Net Realised Value
    private BigDecimal netRealisedValue;

    // CURRENCY (related to net realised)
    private String netRealisedCurrency;

    // SB utilization
    private String sbUtilization; // String instead of enum for DTO

    // INVOICE No.
    private String invoiceNo;

    // Invoice Date
    private LocalDate invoiceDate;

    // MODEL no.
    private String modelNo;

    // PRODUCT TYPE
    private String productType;

    // HS CD
    private String hsCode;

    // DESCRIPTION (Model Description)
    private String modelDescription;

    // QUANTITY
    private BigDecimal quantity;

    // UNIT
    private String unit;

    // INVOICE VALUE (IN FCC)
    private BigDecimal invoiceValueFcc;

    // CURRENCY Code
    private String currencyCode;

    // FOB (INR)
    private BigDecimal fobInr;

    // PMV (per qty)
    private BigDecimal pmvPerQty;

    // PMV Actual
    private BigDecimal pmvActual;

    // Created At
    private LocalDateTime createdAt;

    // Updated At
    private LocalDateTime updatedAt;
    
    private String clientName;
}
