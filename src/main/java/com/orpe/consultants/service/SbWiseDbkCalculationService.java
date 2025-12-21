package com.orpe.consultants.service;

import java.util.List;
import java.util.Map;

import com.orpe.consultants.dto.SbWiseDbkCalculationDTO;
import com.orpe.consultants.model.User;

public interface SbWiseDbkCalculationService {
	
	List<SbWiseDbkCalculationDTO> calculateForExportIds(List<Long> exportIds, User performedBy);
	List<Long> saveAll(List<SbWiseDbkCalculationDTO> rows, User performedBy);
	List<Map<String, Object>> getAllDbkGroups();
	
	List<SbWiseDbkCalculationDTO> findByClaimRefAndYear(String claimRefNo, String claimYear);
}
