package com.orpe.consultants.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorksheetExportModelsDTO {
    private Long wsModelId;
    private Long worksheetId;
    private Long bomExportModelId;
    private String modelNo;
    private Integer colNo;
    private BigDecimal emUsedQty;
    private BigDecimal dutyClaimed;
    private BigDecimal cifClaimed;
}
