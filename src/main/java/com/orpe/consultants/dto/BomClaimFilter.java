package com.orpe.consultants.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomClaimFilter {
	
	 private Long claimId;

	    
	    private String claimRefNo;

	    
	    private String claimYear;

	    
	    private String materialDescription;

	    
	    private String bomPartNo;

	    
	    private String altBoePartNo;

	    
	    private String dbkPartNo;

	    
	    private String importedOrIndigenous;

	   
	    private String unit;

	    
	    private String boeNo;

	    
	    private BigDecimal usedQty;

	   
	    private String exportModelNo;

	   
	    private String sbNo;

	   
	    private String clientName;

	    /**
	     * The ID of the related ImportData entity.
	     * We use a simple ID to avoid circular dependencies and lazy-loading issues.
	     */
	    
	    private String filterField;      // e.g., beNo, claimYear, beDate, etc.
	    private String filterValue;      // Value to filter on filterField (for non-date)
	    private LocalDate fromDate;      // From date (optional)
	    private LocalDate toDate;
	   

}
