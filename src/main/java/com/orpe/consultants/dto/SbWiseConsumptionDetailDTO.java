package com.orpe.consultants.dto;

import lombok.Getter;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SbWiseConsumptionDetailDTO {

    private Long exportId;

    private String sbNo;
    private String exportModelNo;
    private String bomPartNo;
    private String dbkPartNo;
    private String boeNo;

    private BigDecimal consumptionForOneExportQty;

    private BigDecimal importQuantity;
    private BigDecimal assessableValue;
    private BigDecimal cifValue;

    private BigDecimal bcd;
    private BigDecimal sws;
    private BigDecimal addDuty;
    private BigDecimal totalDuty;

    private BigDecimal dbkAmount;
}

