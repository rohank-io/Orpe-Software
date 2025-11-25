package com.orpe.consultants.service;



import java.util.List;

import com.orpe.consultants.dto.BomClaimDTO;
import com.orpe.consultants.dto.BomClaimFilter;
import com.orpe.consultants.dto.SbWiseQuantityConsumptionDTO;

public interface SbWiseQtyConsumptionService {
	
	int saveBulk(List<SbWiseQuantityConsumptionDTO> rows);
	
	List<SbWiseQuantityConsumptionDTO> search(BomClaimFilter filter);
	
	List<SbWiseQuantityConsumptionDTO> getGroupedByClaimRefNoAndClaimYear();
	
	
	 List<SbWiseQuantityConsumptionDTO> getDetailsByClaimRefNoAndClaimYear(String claimRefNo, String claimYear);
    
	
	

}
