package com.orpe.consultants.dto;



import java.math.BigDecimal;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DashboardMetricsDto {

    private Long sbCount;
    private Long boeCount;
    private BigDecimal claimedDbkAmount;
    private BigDecimal totalAirAmount;
    private BigDecimal totalSbrAmount;
    private Long totalWorksheetCount;

    private Long totalImportCount;
    private Long totalExportCount;

    public DashboardMetricsDto(Long sbCount,
            Long boeCount,
            BigDecimal claimedDbkAmount,
            BigDecimal totalAirAmount,
            BigDecimal totalSbrAmount,
            Long totalWorksheetCount) {
this.sbCount = sbCount;
this.boeCount = boeCount;
this.claimedDbkAmount = (claimedDbkAmount == null ? BigDecimal.ZERO : claimedDbkAmount);
this.totalAirAmount = (totalAirAmount == null ? BigDecimal.ZERO : totalAirAmount);
this.totalSbrAmount = (totalSbrAmount == null ? BigDecimal.ZERO : totalSbrAmount);
this.totalWorksheetCount = (totalWorksheetCount == null ? 0L : totalWorksheetCount);
}
}

