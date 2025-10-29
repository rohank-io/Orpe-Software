package com.orpe.consultants.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;



@Entity
@Table(name = "tbl_bom_claim_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long claimId;

    @Column(name = "claim_ref_no", length = 100, nullable = false)
    @NotBlank
    private String claimRefNo;

    @Column(name = "claim_year", length = 32, nullable = false)
    @NotBlank
    private String claimYear;

    @Column(name = "material_description", length = 600)
    private String materialDescription;

    @Column(name = "bom_part_no", length = 100)
    private String bomPartNo;

    @Column(name = "alt_boe_part_no", length = 100)
    private String altBoePartNo;

    @Column(name = "dbk_part_no", length = 100)
    private String dbkPartNo;

    @Column(name = "imported_or_indigenous", length = 50)
    private String importedOrIndigenous;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "boe_no", length = 100)
    private String boeNo;

    @Column(name = "used_qty", precision = 18, scale = 6)
    private BigDecimal usedQty;

    @Column(name = "export_model_no", length = 100)
    private String exportModelNo;

    @Column(name = "sb_no", length = 100)
    private String sbNo;

    @Column(name = "client_name", length = 100)
    private String clientName;

    // Relationship to ImportData (join on claimRefNo)
    // @ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "import_id", referencedColumnName = "import_id")
    // private ImportData importData;

    // Relationship to ExportData (join on sbNo or claimRefNo)
    //  @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "export_id", referencedColumnName = "export_id")
    //private ExportData exportData;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}

