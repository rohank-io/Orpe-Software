package com.orpe.consultants.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

/**
 * DTO encapsulating filter/search criteria for ImportData queries.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportDataFilter {

    /**
     * Bill of Entry Number filter.
     */
    private String beNo;

    /**
     * Claim year filter.
     */
    private String claimYear;

    /**
     * Supplier name/address filter.
     */
    private String supplierNameAddress;

    /**
     * Country of origin filter.
     */
    private String countryOfOrigin;

    /**
     * Item description filter.
     */
    private String itemDescription;

    /**
     * BOM part number filter.
     */
    private String bomPartNo;

    /**
     * DBK part number filter.
     */
    private String dbkPartNo;

    /**
     * ITCHS code filter.
     */
    private String itchsCode;

    /**
     * Port code filter.
     */
    private String portCode;

    /**
     * BE Date range start (inclusive).
     */
    private LocalDate beDateFrom;

    /**
     * BE Date range end (inclusive).
     */
    private LocalDate beDate;

    /**
     * Claim reference number filter.
     */
    private String claimRefNo;

    /**
     * Stock wise eligibility filter, e.g. OPEN or CLOSED.
     */
    private String stockWiseEligibility;

    

}
