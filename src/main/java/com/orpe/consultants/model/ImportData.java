package com.orpe.consultants.model;




import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.orpe.consultants.dto.StockWiseEligibility;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
 name = "tbl_import_data",
 indexes = {
     @Index(name = "idx_import_be_no", columnList = "be_no"),
     @Index(name = "idx_import_dbk_part_no", columnList = "dbk_part_no"),
     @Index(name = "idx_import_bom_part_no", columnList = "bom_part_no"),
     @Index(name = "idx_import_itchs_code", columnList = "itchs_code")
 }
)
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class ImportData {

 // PK
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 @Column(name = "import_id", nullable = false, updatable = false)
 private Long importId;

 // Header
 @Column(name = "be_no", length = 100, nullable = false)
 @NotBlank @Size(max = 100)
 private String beNo;

 @Column(name = "be_date", nullable = false)
 @NotNull
 private LocalDate beDate;

 // Stored as DATE in DB; using LocalDate to match the column while caller can treat as month marker
 @Column(name = "be_month")
 private String beMonth;

 @Column(name = "be_year")
 private Integer beYear;

 @Column(name = "claim_ref_no", length = 100, nullable = false)
 @NotBlank @Size(max = 100)
 private String claimRefNo;

 @Column(name = "claim_year", length = 32, nullable = false)
 @NotBlank @Size(max = 32)
 private String claimYear;

 @Column(name = "port_code", length = 100)
 @Size(max = 100)
 private String portCode;

 @Column(name = "country_of_origin", length = 600)
 @Size(max = 600)
 private String countryOfOrigin;

 @Column(name = "supplier_name_address", length = 600)
 @Size(max = 600)
 private String supplierNameAddress;

 // Item
 @Column(name = "itchs_code", length = 100)
 @Size(max = 100)
 private String itchsCode;

 @Column(name = "item_description", length = 600, nullable = false)
 @NotBlank @Size(max = 600)
 private String itemDescription;

 @ManyToOne(fetch = FetchType.LAZY, optional = true)
 @JoinColumn(
     name = "bom_part_no",
     referencedColumnName = "bom_part_no",
     foreignKey = @ForeignKey(name = "fk_import_material")
 )
 private Material material; // maps materials.bom_part_no

 @Column(name = "alt_boe_part_no", length = 100)
 @Size(max = 100)
 private String altBoePartNo;

 @Column(name = "dbk_part_no", length = 100)
 @Size(max = 100)
 private String dbkPartNo;

 @Column(name = "quantity", precision = 18, scale = 6, nullable = false)
 @NotNull
 private BigDecimal quantity;

 @Column(name = "uom", length = 100, nullable = false)
 @NotBlank @Size(max = 100)
 private String uom;

 @Column(name = "assessable_value", precision = 18, scale = 2, nullable = false)
 @NotNull
 private BigDecimal assessableValue;

 // Duties
 @Column(name = "bcd_rate", precision = 10, scale = 6)
 private BigDecimal bcdRate;

 @Column(name = "bcd", precision = 18, scale = 2)
 private BigDecimal bcd;

 @Column(name = "sws_rate", precision = 10, scale = 6)
 private BigDecimal swsRate;

 @Column(name = "sws", precision = 18, scale = 2)
 private BigDecimal sws;

 @Column(name = "add_rate", precision = 10, scale = 6)
 private BigDecimal addRate;

 @Column(name = "add_duty", precision = 18, scale = 2)
 private BigDecimal addDuty;

 @Column(name = "igst_rate", precision = 10, scale = 6)
 private BigDecimal igstRate;

 @Column(name = "igst", precision = 18, scale = 2)
 private BigDecimal igst;

 @Column(name = "total_duty", precision = 18, scale = 2)
 private BigDecimal totalDuty;

 // Notification & eligibility
 @Column(name = "notn_no", length = 100)
 @Size(max = 100)
 private String notnNo;

 @Column(name = "notn_eligibility", length = 100)
 @Size(max = 100)
 private String notnEligibility;

 // Stock balances
 @Column(name = "qty_opening_balance", precision = 18, scale = 6)
 private BigDecimal qtyOpeningBalance;

 @Column(name = "qty_used", precision = 18, scale = 6)
 private BigDecimal qtyUsed;

 @Column(name = "closing_balance", precision = 18, scale = 6)
 private BigDecimal closingBalance;

 @Enumerated(EnumType.STRING)
 @Column(name = "stock_wise_eligibility", columnDefinition = "ENUM('OPEN','CLOSED')")
 private StockWiseEligibility stockWiseEligibility;

 @Column(name = "duty_claimed_amt", precision = 18, scale = 6)
 private BigDecimal dutyClaimedAmt;

 // Audit
 @CreationTimestamp
 @Column(name = "created_at", nullable = false, updatable = false)
 private LocalDateTime createdAt;

 @UpdateTimestamp
 @Column(name = "updated_at", nullable = false)
 private LocalDateTime updatedAt;
}
