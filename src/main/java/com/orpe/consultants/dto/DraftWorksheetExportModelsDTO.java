package com.orpe.consultants.dto;

import java.math.BigDecimal;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DraftWorksheetExportModelsDTO {
    private Long draftWsModelId;
    private Long draftWorksheetId;
    private Long bomExportModelId;
    private String modelNo;
    private Integer colNo;
    private BigDecimal emUsedQty;
    private BigDecimal dutyClaimed;
    private BigDecimal cifClaimed;
}
