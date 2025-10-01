package com.orpe.consultants.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;



@Entity
@Table(name = "tbl_bom_export_model")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomExportModelQuantity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id", nullable = false)
    private BomData bomData;

    @Column(name = "model_no", length = 50, nullable = false)
    private String modelNo;

    @Column(name = "quantity", precision = 18, scale = 6, nullable = false)
    private BigDecimal quantity;
}
