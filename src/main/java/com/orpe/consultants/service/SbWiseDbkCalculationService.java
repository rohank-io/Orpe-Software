package com.orpe.consultants.service;

import java.util.List;

import com.orpe.consultants.dto.SbWiseDbkCalculationDTO;
import com.orpe.consultants.model.User;

public interface SbWiseDbkCalculationService {
	
	List<SbWiseDbkCalculationDTO> calculateForExportIds(List<Long> exportIds, User performedBy);

}
