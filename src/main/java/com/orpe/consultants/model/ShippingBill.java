package com.orpe.consultants.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_bills", indexes = {
    @Index(name = "idx_sb_no", columnList = "sb_no"),
    @Index(name = "idx_sb_date", columnList = "sb_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingBill implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sb_no", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Shipping Bill Number is required")
    private String sbNo;

    @Column(name = "sb_date", nullable = false)
    @NotNull(message = "Shipping Bill Date is required")
    private LocalDate sbDate;

    @Column(name = "month", length = 20)
    private String month; // Textual month like "June", "July"

    @Column(name = "year")
    @Min(value = 1990, message = "Year must be after 1990")
    private Integer year;

    @Column(name = "port_code", length = 30)
    @NotBlank(message = "Port Code is required")
    private String portCode;

    @Column(name = "leo_date")
    private LocalDate leoDate;

    @Column(name = "brc_realisation_date")
    private LocalDate brcRealisationDate;

    @Column(name = "invoice_no_date", length = 100)
    private String invoiceNoDate;

    @Column(name = "buyer_details", columnDefinition = "TEXT")
    private String buyerDetails;

    @Column(name = "exchange_rate", precision = 10, scale = 4)
    @DecimalMin(value = "0.0", inclusive = false, message = "Exchange rate must be greater than 0")
    private BigDecimal exchangeRate;

    @Column(name = "invoice_value", precision = 15, scale = 2, nullable = false)
    @NotNull(message = "Invoice Value is required")
    @DecimalMin(value = "0.0", message = "Invoice value must be non-negative")
    private BigDecimal invoiceValue;

    @Column(name = "currency", nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    private String currency;

    @Column(name = "hs_cd", length = 20)
    private String hsCd;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "model_no", length = 50)
    private String modelNo;

    @Column(name = "quantity", precision = 12, scale = 3)
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "fob", precision = 15, scale = 2)
    @DecimalMin(value = "0.0", message = "FOB value must be non-negative")
    private BigDecimal fob;

    @Column(name = "pmv_per_unit", precision = 15, scale = 2)
    private BigDecimal pmvPerUnit;

    @Column(name = "pmv_actual", precision = 15, scale = 2)
    private BigDecimal pmvActual;

    @Column(name = "scheme_description", columnDefinition = "TEXT")
    private String schemeDescription;

    @Column(name = "dbk_sno", length = 50)
    private String dbkSno;

    @Column(name = "dbk_applicability", length = 20)
    private String dbkApplicability;

    @Column(name = "rate", precision = 8, scale = 4)
    private BigDecimal rate;

    @Column(name = "dbk_amt_sb", precision = 15, scale = 2)
    private BigDecimal dbkAmtSb;

    @Column(name = "dbk_amount", precision = 15, scale = 2)
    private BigDecimal dbkAmount;

    @Column(name = "difference_amount", precision = 15, scale = 2)
    private BigDecimal differenceAmount;

    @Column(name = "total_dbk", precision = 15, scale = 2)
    private BigDecimal totalDbk;

    @Column(name = "sbr_no", length = 50)
    private String sbrNo;

    @Column(name = "sb_utilization_amt", precision = 15, scale = 2)
    private BigDecimal sbUtilizationAmt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Utility methods for validation
    
}
