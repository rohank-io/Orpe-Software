package com.orpe.consultants.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orpe.consultants.dto.BomClaimDTO;
import com.orpe.consultants.dto.BomClaimFilter;


public interface BomClaimService{
	
	 /**
     * Save multiple BomClaimDTO rows in bulk.
     * @param rows list of claim data rows
     * @return number of rows saved
     */
    int saveBulk(List<BomClaimDTO> rows);

    /**
     * Save or update a single ImportData row.
     * @param dto import data DTO
     * @return saved ImportDataDTO with generated ID
     */
    BomClaimDTO save(BomClaimDTO dto);

    /**
     * Find an ImportData row by its primary key.
     * @param importId primary key
     * @return optional ImportDataDTO if found
     */
    Optional<BomClaimDTO> findById(Long claimId);

    /**
     * Delete an ImportData by its primary key.
     * @param importId primary key
     */
    void deleteById(Long BomClaimDTO);

    /**
     * Fetch all ImportData rows (not recommended for large sets).
     * @return list of ImportDataDTO
     */
    List<BomClaimDTO> findAll();

    /**
     * Search ImportData rows using filter criteria with pagination support.
     * @param filter search filter DTO (contains fields like beNo, claimYear, date ranges, etc.)
     * @param pageable pagination and sorting information
     * @return paged slice of ImportDataDTO matching filter
     */
    
    Page<BomClaimDTO> search(BomClaimFilter filter, Pageable pageable);
    
    List<BomClaimDTO> search(BomClaimFilter filter);
    
    

    /**
     * Export filtered ImportData rows in CSV or Excel format.
     * @param filter search filter
     * @return byte[] representing exported file (e.g., CSV data)
     */
    byte[] exportData(BomClaimFilter filter);

    /**
     * Validate ImportDataDTO fields before saving.
     * Throws exception or returns validation messages as needed.
     * @param dto import data DTO
     * @return true if valid, false or throws if invalid
     */
    boolean validate(BomClaimDTO dto);

    /**
     * Count total rows matching filter criteria.
     * @param filter filter criteria
     * @return count of matching rows
     */
    long count(BomClaimFilter filter);

}
