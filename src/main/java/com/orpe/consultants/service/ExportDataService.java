package com.orpe.consultants.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orpe.consultants.dto.ExportDataDTO;
import com.orpe.consultants.dto.ExportDataFilter;
import com.orpe.consultants.dto.ImportDataDTO;
import com.orpe.consultants.dto.ImportDataFilter;

public interface ExportDataService {
	
	 /**
     * Save multiple ExportDataDTO rows in bulk.
     * @param rows list of import data rows
     * @return number of rows saved
     */
    int saveBulk(List<ExportDataDTO> rows);

    /**
     * Save or update a single ExportData row.
     * @param dto export data DTO
     * @return saved ExportDataDTO with generated ID
     */
    ExportDataDTO save(ExportDataDTO dto);

    /**
     * Find an ExportData row by its primary key.
     * @param exportId primary key
     * @return optional ExportDataDTO if found
     */
    Optional<ExportDataDTO> findById(Long exportId);

    /**
     * Delete an ExportData by its primary key.
     * @param exportId primary key
     */
    void deleteById(Long exportId);

    /**
     * Fetch all ImportData rows (not recommended for large sets).
     * @return list of ImportDataDTO
     */
    List<ExportDataDTO> findAll();

    /**
     * Search ExportData rows using filter criteria with pagination support.
     * @param filter search filter DTO (contains fields like beNo, claimYear, date ranges, etc.)
     * @param pageable pagination and sorting information
     * @return paged slice of ExportDataDTO matching filter
     */
    Page<ExportDataDTO> search(ExportDataFilter filter, Pageable pageable);

    /**
     * Export filtered ExportData rows in CSV or Excel format.
     * @param filter search filter
     * @return byte[] representing exported file (e.g., CSV data)
     */
    byte[] exportData(ExportDataFilter filter);

    /**
     * Validate ExportDataDTO fields before saving.
     * Throws exception or returns validation messages as needed.
     * @param dto export data DTO
     * @return true if valid, false or throws if invalid
     */
    boolean validate(ExportDataDTO dto);

    /**
     * Count total rows matching filter criteria.
     * @param filter filter criteria
     * @return count of matching rows
     */
    long count(ExportDataFilter filter);

}
