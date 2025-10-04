package com.orpe.consultants.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "tbl_worksheet", indexes = {
    @Index(name = "idx_ws_claim", columnList = "claim_ref_no, claim_year, be_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Worksheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "worksheet_id", nullable = false, updatable = false)
    private Long worksheetId;

    @Column(name = "claim_ref_no", length = 16, nullable = false)
    @NotBlank
    @Size(max = 16)
    private String claimRefNo;

    @Column(name = "claim_year", length = 32, nullable = false)
    @NotBlank
    @Size(max = 32)
    private String claimYear;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "import_id",
        referencedColumnName = "import_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_worksheet_importdata")
    )
    private ImportData importData;



    @Column(name = "be_no", length = 32, nullable = false)
    @NotBlank
    @Size(max = 32)
    private String beNo;

    @Column(name = "be_date", nullable = false)
    @NotNull
    private LocalDate beDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bom_part_no", nullable = false)
    private Material material;

    @Column(name = "dbk_part_no", length = 100)
    @Size(max = 100)
    private String dbkPartNo;

    @Column(name = "item_description", length = 600, nullable = false)
    @NotBlank
    @Size(max = 600)
    private String itemDescription;

    @Column(name = "uom", length = 16, nullable = false)
    @NotBlank
    @Size(max = 16)
    private String uom;

    @Column(name = "imp_qty", precision = 18, scale = 6, nullable = false)
    @NotNull
    private BigDecimal impQty;

    @Column(name = "assessable_value", precision = 18, scale = 2, nullable = false)
    @NotNull
    private BigDecimal assessableValue;

    @Column(name = "per_qty_cif", precision = 18, scale = 6, nullable = false)
    @NotNull
    private BigDecimal perQtyCif;

    @Column(name = "bcd", precision = 18, scale = 2, nullable = false)
    @NotNull
    private BigDecimal bcd;

    @Column(name = "sws", precision = 18, scale = 2, nullable = false)
    @NotNull
    private BigDecimal sws;

    @Column(name = "add_duty", precision = 18, scale = 2, nullable = false)
    @NotNull
    private BigDecimal addDuty;

    @Column(name = "total_duty", precision = 18, scale = 2, nullable = false)
    @NotNull
    private BigDecimal totalDuty;

    @Column(name = "duty_per_qty", precision = 18, scale = 6, nullable = false)
    @NotNull
    private BigDecimal dutyPerQty;

    @Column(name = "used_qty_total", precision = 18, scale = 6, nullable = false)
    @NotNull
    @Builder.Default
    private BigDecimal usedQtyTotal = BigDecimal.ZERO;

    @Column(name = "duty_claimed_total", precision = 18, scale = 2, nullable = false)
    @NotNull
    @Builder.Default
    private BigDecimal dutyClaimedTotal = BigDecimal.ZERO;

    @Column(name = "cif_claimed_total", precision = 18, scale = 2, nullable = false)
    @NotNull
    @Builder.Default
    private BigDecimal cifClaimedTotal = BigDecimal.ZERO;

    @Column(name = "bcd_claimed_dgft", precision = 18, scale = 2, nullable = false)
    @NotNull
    @Builder.Default
    private BigDecimal bcdClaimedDgft = BigDecimal.ZERO;

    @Column(name = "sws_claimed_dgft", precision = 18, scale = 2, nullable = false)
    @NotNull
    @Builder.Default
    private BigDecimal swsClaimedDgft = BigDecimal.ZERO;

    @Column(name = "add_claimed_dgft", precision = 18, scale = 2, nullable = false)
    @NotNull
    @Builder.Default
    private BigDecimal addClaimedDgft = BigDecimal.ZERO;

    @Column(name = "opening_balance_qty_def", precision = 18, scale = 6)
    private BigDecimal openingBalanceQtyDef;

    @Column(name = "qty_used_total_def", precision = 18, scale = 6)
    private BigDecimal qtyUsedDef;

    @Column(name = "closing_balance_def", precision = 18, scale = 6)
    private BigDecimal closingBalanceDef;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
