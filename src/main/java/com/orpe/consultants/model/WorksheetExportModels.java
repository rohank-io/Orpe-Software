package com.orpe.consultants.model;


import java.math.BigDecimal;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "tbl_worksheet_model_split",
       indexes = {@Index(name = "idx_ws_col", columnList = "worksheet_id, col_no")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorksheetExportModels {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ws_model_id", nullable = false, updatable = false)
    private Long wsModelId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worksheet_id", nullable = false)
    private Worksheet worksheet;
    
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id", nullable = false)
    private BomExportModelQuantity bomExportModelData;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_no", nullable = false)
    private Models model;

    @Column(name = "col_no")
    @Builder.Default
    private Integer colNo = 1;


    @Column(name = "used_qty", precision = 18, scale = 6, nullable = false)
    @Builder.Default
    private BigDecimal emUsedQty = BigDecimal.ZERO;

    @Column(name = "duty_claimed", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal dutyClaimed = BigDecimal.ZERO;

    @Column(name = "cif_claimed", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal cifClaimed = BigDecimal.ZERO;

    
}

