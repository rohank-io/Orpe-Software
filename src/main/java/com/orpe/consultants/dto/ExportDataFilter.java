package com.orpe.consultants.dto;



import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

/**
 * DTO encapsulating filter/search criteria for ExportData queries.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportDataFilter {
    /**
     * SB Number filter.
     */
    private String sbNo;
    /**
     * Claim year filter.
     */
    private String claimYear;
    /**
     * Customer name filter.
     */
    private String customerName;
    /**
     * Port code filter.
     */
    private String portCode;
    /**
     * Model number filter.
     */
    private String modelNo;
    /**
     * Product type filter.
     */
    private String productType;
    /**
     * DBK SNO filter.
     */
    private String dbkSno;
    /**
     * Invoice number filter.
     */
    private String invoiceNo;
    /**
     * Item description filter.
     */
    private String modelDescription;
    /**
     * BE Date range start (inclusive).
     */
    private LocalDate sbDateFrom;
    /**
     * BE Date range end (inclusive).
     */
    private LocalDate sbDateTo;
    /**
     * Claim reference number filter.
     */
    private String claimRefNo;
    /**
     * Stock utilization filter, e.g. OPEN or CLOSED.
     */
    private String sbUtilization;
}
