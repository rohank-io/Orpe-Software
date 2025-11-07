package com.orpe.consultants.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orpe.consultants.dto.DraftWorksheetDTO;
import com.orpe.consultants.dto.WorksheetDTO;
import com.orpe.consultants.dto.WorksheetDataFilter;

public interface DraftWorksheetService {
	
	void saveBulkWorksheets(List<DraftWorksheetDTO> worksheetDTOList);
	
	void updateBulkDrafts(List<DraftWorksheetDTO> draftDTOs);
	
	/**
     * Save or update a single WorksheetDTO.
     *
     * @param dto worksheet DTO to save
     * @return saved WorksheetDTO with generated ID
     */
	DraftWorksheetDTO save(DraftWorksheetDTO dto);

    /**
     * Find a worksheet by ID.
     *
     * @param id primary key
     * @return Optional containing WorksheetDTO if found
     */
    Optional<DraftWorksheetDTO> findById(Long id);

    /**
     * Retrieve all worksheets.
     *
     * @return list of all WorksheetDTOs
     */
    List<DraftWorksheetDTO> findAll();

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
    Page<DraftWorksheetDTO> search(WorksheetDataFilter filter, Pageable pageable);

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
    boolean validate(DraftWorksheetDTO dto);

    /**
     * Count total worksheets matching the filter.
     *
     * @param filter search filter criteria
     * @return count of matching rows
     */
    long count(WorksheetDataFilter filter);

}
