package com.orpe.consultants.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_sb_qty_consumption",
indexes = {
    @Index(name = "idx_sb_no", columnList = "sb_no"),
    @Index(name = "idx_export_model_no", columnList = "export_model_no"),
    @Index(name = "idx_claim_id", columnList = "claim_id"),
    @Index(name = "idx_claim_ref_no", columnList = "claim_ref_no")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SbWiseQuantityConsumption {
	
	
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qty_consum_id", nullable = false, updatable = false)
    private Long sbQtyConsumptionId;
	
	
	@Column(name = "claim_ref_no", length = 100, nullable = false)
    @NotBlank
    @Size(max = 100)
    private String claimRefNo;

    @Column(name = "claim_year", length = 32, nullable = false)
    @NotBlank
    @Size(max = 32)
    private String claimYear;

	    
    @Column(name = "dbk_part_no", length = 100)
    private String dbkPartNo;
    
    @Column(name = "bom_part_no", length = 100)
    private String bomPartNo;

    @Column(name = "boe_no", length = 100)
    @Size(max = 100)
    private String boeNo;


    @Column(name = "used_qty", precision = 18, scale = 6, nullable = false)
    private BigDecimal usedQty;

	    
    @Column(name = "export_model_no", length = 100)
    @Size(max = 100)
    private String exportModelNo;

    @Column(name = "sb_no", length = 100)
    @Size(max = 100)
    private String sbNo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
	    
	    
	    
    @Column(name = "claim_id")
    private Long claimId;
    
    @PrePersist
    public void prePersistDefaults() {
        if (usedQty == null) usedQty = BigDecimal.ZERO;
        if (claimRefNo != null) claimRefNo = claimRefNo.trim();
        if (claimYear != null) claimYear = claimYear.trim();
        if (dbkPartNo != null) dbkPartNo = dbkPartNo.trim();
        if (exportModelNo != null) exportModelNo = exportModelNo.trim();
        if (sbNo != null) sbNo = sbNo.trim();
        if (boeNo != null) boeNo = boeNo.trim();
    }

    @PreUpdate
    public void preUpdateDefaults() {
        if (usedQty == null) usedQty = BigDecimal.ZERO;
    }
	    
	    

}
