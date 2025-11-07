package com.orpe.consultants.model;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_draft_worksheet_model_split",
       indexes = {@Index(name = "idx_draft_ws_col", columnList = "draft_worksheet_id, col_no")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DraftWorksheetExportModels {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_ws_model_id", nullable = false, updatable = false)
    private Long draftWsModelId; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_worksheet_id")
    private DraftWorksheet draftWorksheet;

    @Column(name = "bom_export_model_id")
    private Long bomExportModelId; // Reference to BomExportModelQuantity, can be nullable for drafts

    @Column(name = "model_no")
    private String modelNo; // Can store model number directly for drafts

    @Column(name = "col_no")
    private Integer colNo;

    @Column(name = "used_qty", precision = 18, scale = 6)
    private BigDecimal emUsedQty;

    @Column(name = "duty_claimed", precision = 18, scale = 2)
    private BigDecimal dutyClaimed;

    @Column(name = "cif_claimed", precision = 18, scale = 2)
    private BigDecimal cifClaimed;
}

