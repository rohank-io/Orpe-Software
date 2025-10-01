package com.orpe.consultants.model;

import jakarta.persistence.*;
import lombok.*;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "tbl_bom")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bom_id", nullable = false, updatable = false)
    private Long bomId;

    @Column(name = "claim_ref_no", length = 32, nullable = false)
    private String claimRefNo;

    @Column(name = "claim_year", length = 16, nullable = false)
    private String claimYear;

    @Column(name = "material_desc", length = 512)
    private String materialDesc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_part_no", nullable = false)
    private Material material;

    @Column(name = "alternate_boe_part_no", length = 50)
    private String alternateBoePartNo;

    @Column(name = "dbk_part_no", length = 50)
    private String dbkPartNo;

    @Column(name = "imported_indigenous", length = 16)
    private String importedIndigenous;

    @Column(name = "unit", length = 16)
    private String unit;

    @Column(name = "grand_total", precision = 18, scale = 6)
    private BigDecimal grandTotal;

    @Column(name = "net_weight_kg", precision = 18, scale = 6)
    private BigDecimal netWeightKg;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "bomData", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BomExportModelQuantity> exportModels;
}
