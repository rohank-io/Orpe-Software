package com.orpe.consultants.dto;



import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for representing BomClaim data.
 * This object is used for transferring data between the service layer and the
 * presentation layer (e.g., in REST API requests and responses).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomClaimDTO {

    private Long claimId;

    @NotBlank(message = "Claim reference number cannot be blank")
    @Size(max = 100, message = "Claim reference number must be less than 100 characters")
    private String claimRefNo;

    @NotBlank(message = "Claim year cannot be blank")
    @Size(max = 32, message = "Claim year must be less than 32 characters")
    private String claimYear;

    @Size(max = 600, message = "Material description must be less than 600 characters")
    private String materialDescription;

    @Size(max = 100, message = "BOM part number must be less than 100 characters")
    private String bomPartNo;

    @Size(max = 100, message = "Alt BOE part number must be less than 100 characters")
    private String altBoePartNo;

    @Size(max = 100, message = "DBK part number must be less than 100 characters")
    private String dbkPartNo;

    @Size(max = 50, message = "Imported/Indigenous must be less than 50 characters")
    private String importedOrIndigenous;

    @Size(max = 50, message = "Unit must be less than 50 characters")
    private String unit;

    @Size(max = 100, message = "BOE number must be less than 100 characters")
    private String boeNo;

    @Digits(integer = 12, fraction = 6, message = "Used quantity format is invalid (max 12 integer, 6 fraction digits)")
    private BigDecimal usedQty;

    @Size(max = 100, message = "Export model number must be less than 100 characters")
    private String exportModelNo;

    @Size(max = 100, message = "SB number must be less than 100 characters")
    private String sbNo;

    @Size(max = 100, message = "Client name must be less than 100 characters")
    private String clientName;

    /**
     * The ID of the related ImportData entity.
     * We use a simple ID to avoid circular dependencies and lazy-loading issues.
     */
    // private Long importId;

    /**
     * The ID of the related ExportData entity.
     */
    // private Long exportId;

    // Timestamps are often included in responses but are read-only
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}