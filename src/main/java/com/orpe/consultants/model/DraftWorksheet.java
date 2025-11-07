package com.orpe.consultants.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "tbl_draft_worksheet", indexes = {
    @Index(name = "idx_draft_ws_claim", columnList = "claim_ref_no, claim_year, be_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DraftWorksheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_worksheet_id", nullable = false, updatable = false)
    private Long draftWorksheetId;

    @Column(name = "claim_ref_no", length = 32)
    private String claimRefNo;

    @Column(name = "claim_year", length = 32)
    private String claimYear;

    @Column(name = "import_id")
    private Long importId;

    @Column(name = "be_no", length = 32)
    private String beNo;

    @Column(name = "be_date", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @NotNull
    private LocalDate beDate;

    @Column(name = "bom_part_no")
    private String bomPartNo;

    @Column(name = "dbk_part_no", length = 100)
    private String dbkPartNo;

    @Column(name = "item_description", length = 600)
    private String itemDescription;

    @Column(name = "uom", length = 16)
    private String uom;

    @Column(name = "imp_qty", precision = 18, scale = 6)
    private BigDecimal importQty;

    @Column(name = "assessable_value", precision = 18, scale = 2)
    private BigDecimal assessableValue;

    @Column(name = "cif_value", precision = 18, scale = 2)
    private BigDecimal cifValue;

    @Column(name = "per_qty_cif", precision = 18, scale = 6)
    private BigDecimal perQtyCif;

    @Column(name = "bcd", precision = 18, scale = 2)
    private BigDecimal bcd;

    @Column(name = "sws", precision = 18, scale = 2)
    private BigDecimal sws;

    @Column(name = "add_duty", precision = 18, scale = 2)
    private BigDecimal addDuty;

    @Column(name = "total_duty", precision = 18, scale = 2)
    private BigDecimal totalDuty;

    @Column(name = "duty_per_qty", precision = 18, scale = 6)
    private BigDecimal dutyPerQty;

    @Column(name = "used_qty_total", precision = 18, scale = 6)
    private BigDecimal usedQtyTotal;

    @Column(name = "duty_claimed_total", precision = 18, scale = 2)
    private BigDecimal dutyClaimedTotal;

    @Column(name = "cif_claimed_total", precision = 18, scale = 2)
    private BigDecimal cifClaimedTotal;

    @Column(name = "bcd_claimed_dgft", precision = 18, scale = 2)
    private BigDecimal bcdClaimed;

    @Column(name = "sws_claimed_dgft", precision = 18, scale = 2)
    private BigDecimal swsClaimed;

    @Column(name = "add_claimed_dgft", precision = 18, scale = 2)
    private BigDecimal addClaimed;

    @Column(name = "opening_balance_qty_def", precision = 18, scale = 6)
    private BigDecimal openingBalanceQtyDef;

    @Column(name = "qty_used_total_def", precision = 18, scale = 6)
    private BigDecimal qtyUsedDef;

    @Column(name = "closing_balance_def", precision = 18, scale = 6)
    private BigDecimal closingBalanceDef;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "draft_status", length = 16)
    private String draftStatus;     // e.g., "DRAFT", "SUBMITTED"

    // Optionally, relate export models as DraftWorksheetExportModels
    @OneToMany(mappedBy = "draftWorksheet", cascade = {}, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DraftWorksheetExportModels> exportModels;

}

