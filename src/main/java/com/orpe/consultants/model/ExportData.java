package com.orpe.consultants.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;



@Entity
@Table(
	    name = "tbl_export_data",
	    indexes = {
	        @Index(name = "idx_sb_no", columnList = "sb_no"),
	        @Index(name = "idx_invoice_no", columnList = "invoice_no"),
	        @Index(name = "idx_port_code", columnList = "port_code"),
	        @Index(name = "idx_customer_name", columnList = "customer_name")
	    }
	)
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class ExportData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "export_id", nullable = false)
    private Long exportId;

    // SB No
    @Column(name = "sb_no", nullable = false, length = 250)
    private String sbNo;

    // SB Date
    @Column(name = "sb_date", nullable = false)
    private LocalDate sbDate;

    // Port Code
    @Column(name = "port_code", length = 50)
    private String portCode;

    // CUSTOMER NAME
    @Column(name = "customer_name", length = 250)
    private String customerName;

    // LEO Date.
    @Column(name = "leo_date")
    private LocalDate leoDate;

    // Claim Ref. no.
    @Column(name = "claim_ref_no", nullable = false, length = 50)
    private String claimRefNo;

    // Claim Year
    @Column(name = "claim_year", nullable = false, length = 32)
    private String claimYear;

    // SCHEME DESCRIPTION
    @Column(name = "scheme_description", length = 500)
    private String schemeDescription;

    // DBK SNO.
    @Column(name = "dbk_sno", length = 250)
    private String dbkSno;

    // DBK Applicability (now String)
    @Column(name = "dbk_applicability", length = 50)
    private String dbkApplicability;

    // RATE
    @Column(name = "rate", precision = 10, scale = 6)
    private BigDecimal rate;

    // AIR Given in SB
    @Column(name = "air_given_in_sb", precision = 18, scale = 3)
    private BigDecimal airGivenInSb;

    // AIR AMOUNT
    @Column(name = "air_amount", precision = 18, scale = 3)
    private BigDecimal airAmount;

    // Difference
    @Column(name = "difference", precision = 18, scale = 12)
    private BigDecimal difference;

    // Total DBK
    @Column(name = "total_dbk", precision = 18, scale = 6)
    private BigDecimal totalDbk;

    // SBR
    @Column(name = "sbr", precision = 18, scale = 6)
    private BigDecimal sbr;

    // ARO No.
    @Column(name = "aro_no", length = 250)
    private String aroNo;

    // ARO DATE
    @Column(name = "aro_date")
    private LocalDate aroDate;

    // ARO File NO.
    @Column(name = "aro_file_no", length = 250)
    private String aroFileNo;

    // ARO File DATE
    @Column(name = "aro_file_date")
    private LocalDate aroFileDate;

    // BRC no.
    @Column(name = "brc_no", length = 250)
    private String brcNo;

    // Net Realised Value
    @Column(name = "net_realised_value", precision = 18, scale = 6)
    private BigDecimal netRealisedValue;

    // CURRENCY (related to net realised)
    @Column(name = "net_realised_currency", length = 50)
    private String netRealisedCurrency;

    // SB utilization (now String)
    @Column(name = "sb_utilization", length = 50)
    private String sbUtilization;

    // INVOICE No.
    @Column(name = "invoice_no", nullable = false, length = 250)
    private String invoiceNo;

    // Invoice Date
    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "model_no",
        referencedColumnName = "model_no",
        foreignKey = @ForeignKey(name = "fk_export_material")
    )
    private Models models;

    // PRODUCT TYPE
    @Column(name = "product_type", length = 250)
    private String productType;

    // HS CD
    @Column(name = "hs_code", length = 100)
    private String hsCode;

    // DESCRIPTION (Model Description)
    @Column(name = "model_description", length = 1000)
    private String modelDescription;

    // QUANTITY
    @Column(name = "quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    // UNIT
    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    // INVOICE VALUE (IN FCC)
    @Column(name = "invoice_value_fcc", nullable = false, precision = 18, scale = 6)
    private BigDecimal invoiceValueFcc;

    // CURRENCY Code
    @Column(name = "currency_code", nullable = false, length = 50)
    private String currencyCode;

    // FOB (INR)
    @Column(name = "fob_inr", precision = 18, scale = 2)
    private BigDecimal fobInr;

    // PMV (per qty)
    @Column(name = "pmv_per_qty", precision = 18, scale = 2)
    private BigDecimal pmvPerQty;

    // PMV Actual
    @Column(name = "pmv_actual", precision = 18, scale = 2)
    private BigDecimal pmvActual;

    // Created At
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "username", nullable = false, length = 50)
    private String username;
}
