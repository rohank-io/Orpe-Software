package com.orpe.consultants.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SbWiseDbkCalculationDTO {

    private Long dbkCalcId;

    private String portCode;

    private String shippingBillNo;

    private LocalDate shippingBillDate;

    private LocalDate leoDate;

    private String exportDescription;

    private BigDecimal quantity;

    private String unit;

    private String dbkSno;

    private BigDecimal fobValue;

    private BigDecimal pmvValue;

    private BigDecimal importQuantity;

    private BigDecimal consumptionPerExportQty;

    private BigDecimal totalDuty;

    private BigDecimal totalCifValue;

    private BigDecimal dbkAmount;

    private BigDecimal cifValue;

    private BigDecimal airRate;

    private BigDecimal airAmount;

    private BigDecimal sbr;

    private BigDecimal fourFifthOfBrodMainClaim;

    private BigDecimal diffBrodAndAir;

    private BigDecimal valueAddition;
}
