package com.orpe.consultants.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orpe.consultants.dto.WorksheetDTO;
import com.orpe.consultants.dto.WorksheetDataFilter;
import com.orpe.consultants.model.DraftWorksheet;
import com.orpe.consultants.model.Worksheet;

public interface WorksheetService {
	
	void saveBulkWorksheets(List<WorksheetDTO> worksheetDTOList);
	
	/**
     * Save or update a single WorksheetDTO.
     *
     * @param dto worksheet DTO to save
     * @return saved WorksheetDTO with generated ID
     */
    WorksheetDTO save(WorksheetDTO dto);

    /**
     * Find a worksheet by ID.
     *
     * @param id primary key
     * @return Optional containing WorksheetDTO if found
     */
    Optional<WorksheetDTO> findById(Long id);

    /**
     * Retrieve all worksheets.
     *
     * @return list of all WorksheetDTOs
     */
    List<WorksheetDTO> findAll();

    /**
     * Delete a worksheet by ID.
     *
     * @param id primary key
     */
    void deleteById(Long id);

    /**
     * Search worksheets by filter with pagination.
     *
     * @param filter criteria to search worksheets
     * @param pageable paging and sorting information
     * @return paginated result of WorksheetDTOs
     */
    Page<WorksheetDTO> search(WorksheetDataFilter filter, Pageable pageable);

    /**
     * Export worksheets matching filter criteria to byte array (CSV, Excel, etc).
     *
     * @param filter worksheet filter
     * @return exported file as byte array
     */
    byte[] exportData(WorksheetDataFilter filter);

    /**
     * Validate WorksheetDTO fields before saving.
     *
     * @param dto worksheet DTO to validate
     * @return true if valid, otherwise false or throws validation exception
     */
    boolean validate(WorksheetDTO dto);

    /**
     * Count total worksheets matching the filter.
     *
     * @param filter search filter criteria
     * @return count of matching rows
     */
    long count(WorksheetDataFilter filter);
    
    
    List<Map<String, Object>> getAllWorksheetGroups();
    
    
    /**
     * Get all worksheets for a specific user + claimRef + claimYear
     * without additional filters.
     */
    List<WorksheetDTO> getWorksheetByUserAndClaimRefAndYear(
            String username,
            String claimRefNo,
            String claimYear
    );

    /**
     * Get all worksheets for a specific user + claimRef + claimYear
     * with additional filter conditions (field, value, date range, etc.).
     */
    List<WorksheetDTO> getWorksheetByUserAndClaimRefAndYear(
            String username,
            String claimRefNo,
            String claimYear,
            WorksheetDataFilter filter
    );
    
    WorksheetDTO getWorksheetWithExportModels(Long worksheetId);

    void updateWorksheet(WorksheetDTO worksheetDto);

}
