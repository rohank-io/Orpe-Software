package com.orpe.consultants.service;

import java.util.List;

import com.orpe.consultants.dto.WorksheetDTO;

public interface WorksheetService {
	
	void saveBulkWorksheets(List<WorksheetDTO> worksheetDTOList);

}
