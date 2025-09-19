package com.orpe.consultants.service;



import com.orpe.consultants.dto.ImportDataDTO;
import com.orpe.consultants.dto.ImportDataFilter;  // Optional DTO for search/filter criteria
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ImportDataService {

    /**
     * Save multiple ImportDataDTO rows in bulk.
     * @param rows list of import data rows
     * @return number of rows saved
     */
    int saveBulk(List<ImportDataDTO> rows);

    /**
     * Save or update a single ImportData row.
     * @param dto import data DTO
     * @return saved ImportDataDTO with generated ID
     */
    ImportDataDTO save(ImportDataDTO dto);

    /**
     * Find an ImportData row by its primary key.
     * @param importId primary key
     * @return optional ImportDataDTO if found
     */
    Optional<ImportDataDTO> findById(Long importId);

    /**
     * Delete an ImportData by its primary key.
     * @param importId primary key
     */
    void deleteById(Long importId);

    /**
     * Fetch all ImportData rows (not recommended for large sets).
     * @return list of ImportDataDTO
     */
    List<ImportDataDTO> findAll();

    /**
     * Search ImportData rows using filter criteria with pagination support.
     * @param filter search filter DTO (contains fields like beNo, claimYear, date ranges, etc.)
     * @param pageable pagination and sorting information
     * @return paged slice of ImportDataDTO matching filter
     */
    Page<ImportDataDTO> search(ImportDataFilter filter, Pageable pageable);

    /**
     * Export filtered ImportData rows in CSV or Excel format.
     * @param filter search filter
     * @return byte[] representing exported file (e.g., CSV data)
     */
    byte[] exportData(ImportDataFilter filter);

    /**
     * Validate ImportDataDTO fields before saving.
     * Throws exception or returns validation messages as needed.
     * @param dto import data DTO
     * @return true if valid, false or throws if invalid
     */
    boolean validate(ImportDataDTO dto);

    /**
     * Count total rows matching filter criteria.
     * @param filter filter criteria
     * @return count of matching rows
     */
    long count(ImportDataFilter filter);

}
