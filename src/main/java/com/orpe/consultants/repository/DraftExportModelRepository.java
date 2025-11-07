package com.orpe.consultants.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.orpe.consultants.model.DraftWorksheet;
import com.orpe.consultants.model.DraftWorksheetExportModels;

public interface DraftExportModelRepository extends JpaRepository<DraftWorksheetExportModels, Long>, JpaSpecificationExecutor<DraftWorksheetExportModels> {

	void deleteByDraftWorksheet(DraftWorksheet draftWorksheet);
}
