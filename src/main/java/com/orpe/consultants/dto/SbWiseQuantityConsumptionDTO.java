package com.orpe.consultants.dto;



import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SbWiseQuantityConsumptionDTO {

    private Long sbQtyConsumptionId;

    private String claimRefNo;

    private String claimYear;

    private String dbkPartNo;

    private String boeNo;

    private BigDecimal usedQty;

    private String exportModelNo;

    private String sbNo;

    private Long claimId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

