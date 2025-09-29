package com.orpe.consultants.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomDataDTO {

    private Long bomId;
    private String claimRefNo;
    private String claimYear;
    private String materialDesc;
    private String bomPartNo;
    private String alternateBoePartNo;
    private String dbkPartNo;
    private String importedIndigenous;
    private String unit;
    private BigDecimal grandTotal;
    private BigDecimal netWeightKg;
    private LocalDateTime createdAt;
    private List<BomExportModelQuantityDTO> exportModels = new ArrayList<>();

}
