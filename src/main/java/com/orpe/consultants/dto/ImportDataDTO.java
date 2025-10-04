package com.orpe.consultants.dto;



import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.orpe.consultants.model.BomExportModelQuantity;
import com.orpe.consultants.model.ImportData;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportDataDTO {
	private ImportData importData;
    // Existing fields:
	private Long importId;
    private String beNo;
    private LocalDate beDate;
    private String beMonth;
    private Integer beYear;
    private String claimRefNo;
    private String claimYear;
    private String portCode;
    private String countryOfOrigin;
    private String supplierNameAddress;
    private String itchsCode;
    private String itemDescription;
    private String bomPartNo;
    private String altBoePartNo;
    private String dbkPartNo;
    private BigDecimal quantity;
    private String uom;
    private BigDecimal assessableValue;
    private BigDecimal bcdRate;
    
    private BigDecimal bcd;
    private BigDecimal swsRate;
    private BigDecimal sws;
    
    private BigDecimal addRate;
    private BigDecimal addDuty;
    private BigDecimal igstRate;
    private BigDecimal igst;
    private BigDecimal totalDuty;
    private String notnNo;
    private String notnEligibility;
    private BigDecimal qtyOpeningBalance;
    private BigDecimal qtyUsed;
    private BigDecimal closingBalance;
    private StockWiseEligibility stockWiseEligibility; 
    private BigDecimal dutyClaimedAmt;
    // TODO new fields for full extraction:
    
    private String clientName;
    
   
    
    
    private List<BomExportModelQuantity> exportModels;
    
     // To match Entity type or convert accordingly
        // To match Entity type or convert accordingly
    // Use enum or map properly

    // Optional aliases from sheet structure
}

