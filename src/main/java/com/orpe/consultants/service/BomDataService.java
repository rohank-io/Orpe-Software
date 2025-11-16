package com.orpe.consultants.service;

import com.orpe.consultants.dto.BomDataDTO;
import com.orpe.consultants.dto.BomDataFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BomDataService {

    /**
     * Save multiple BOM rows in bulk.
     * @param rows list of BOM rows
     * @return number of rows saved
     */
    int saveBulk(List<BomDataDTO> rows);

    /**
     * Save or update a single BOM row.
     * @param dto BOM data DTO
     * @return saved BomDataDTO with generated ID
     */
    BomDataDTO save(BomDataDTO dto);

    /**
     * Find a BOM row by its primary key.
     * @param bomId primary key
     * @return optional BomDataDTO if found
     */
    Optional<BomDataDTO> findById(Long bomId);

    /**
     * Delete a BOM row by its primary key.
     * @param bomId primary key
     */
    void deleteById(Long bomId);

    /**
     * Fetch all BOM rows (may be expensive for large sets).
     * @return list of BomDataDTO
     */
    List<BomDataDTO> findAll();
    
    List<BomDataDTO> findAllForExport();

    /**
     * Search BOM rows using filter criteria with pagination support.
     * @param filter search filter DTO (contains fields like claimRefNo, bomPartNo, year, etc.)
     * @param pageable pagination and sorting information
     * @return paged slice of BomDataDTO matching filter
     */
    Page<BomDataDTO> search(BomDataFilter filter, Pageable pageable);

    /**
     * Export filtered BOM rows in CSV or Excel format.
     * @param filter search filter
     * @return byte[] representing exported file (e.g., CSV, Excel data)
     */
    byte[] exportData(BomDataFilter filter);

    /**
     * Validate BomDataDTO fields before saving.
     * Throws exception or returns validation messages as needed.
     * @param dto BOM data DTO
     * @return true if valid, false or throws if invalid
     */
    boolean validate(BomDataDTO dto);

    /**
     * Count total rows matching filter criteria.
     * @param filter filter criteria
     * @return count of matching rows
     */
    long count(BomDataFilter filter);
}
