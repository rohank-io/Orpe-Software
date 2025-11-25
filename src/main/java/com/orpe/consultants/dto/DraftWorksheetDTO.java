package com.orpe.consultants.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DraftWorksheetDTO {
    private Long draftWorksheetId;
    private String claimRefNo;
    private String claimYear;
    private Long importId;
    private String beNo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate beDate;

    private String bomPartNo;
    private String altBoePartNo;
    private String dbkPartNo;
    private String itemDescription;
    private String uom;
    private BigDecimal importQty;
    private BigDecimal assessableValue;
    private BigDecimal cifValue;
    private BigDecimal perQtyCif;
    private BigDecimal bcd;
    private BigDecimal sws;
    private BigDecimal addDuty;
    private BigDecimal totalDuty;
    private BigDecimal dutyPerQty;
    private BigDecimal usedQtyTotal;
    private BigDecimal dutyClaimedTotal;
    private BigDecimal cifClaimedTotal;
    private BigDecimal bcdClaimed;
    private BigDecimal swsClaimed;
    private BigDecimal addClaimed;
    private BigDecimal openingBalanceQtyDef;
    private BigDecimal qtyUsedDef;
    private BigDecimal closingBalanceDef;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String draftStatus; // e.g., "DRAFT", "SUBMITTED"
    
    
    
    private String username;

    private List<DraftWorksheetExportModelsDTO> exportModels;
}
