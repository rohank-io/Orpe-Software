package com.orpe.consultants.repository;

import com.orpe.consultants.model.ImportData;
import com.orpe.consultants.dto.StockWiseEligibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ImportDataRepository extends JpaRepository<ImportData, Long>, JpaSpecificationExecutor<ImportData> {

    // Global default
    Page<ImportData> findAllByOrderByBeDateDesc(Pageable pageable);

    // Simple lookups (forced order)
    Page<ImportData> findByBeNoContainingIgnoreCaseOrderByBeDateDesc(String beNo, Pageable pageable);
    Page<ImportData> findByDbkPartNoContainingIgnoreCaseOrderByBeDateDesc(String dbkPartNo, Pageable pageable);
    Page<ImportData> findByItchsCodeContainingIgnoreCaseOrderByBeDateDesc(String itchsCode, Pageable pageable);

    // Date range (forced order)
    Page<ImportData> findByBeDateBetweenOrderByBeDateDesc(LocalDate from, LocalDate to, Pageable pageable);

    // By eligibility/status (forced order)
    Page<ImportData> findByStockWiseEligibilityOrderByBeDateDesc(StockWiseEligibility status, Pageable pageable);

    // By material FK (forced order)
    Page<ImportData> findByMaterial_BomPartNoOrderByBeDateDesc(String bomPartNo, Pageable pageable);

    // Combined filters (forced order)
    Page<ImportData> findByBeDateBetweenAndDbkPartNoContainingIgnoreCaseOrderByBeDateDesc(
            LocalDate from, LocalDate to, String dbkPartNo, Pageable pageable);

    Page<ImportData> findByBeNoContainingIgnoreCaseAndStockWiseEligibilityOrderByBeDateDesc(
            String beNo, StockWiseEligibility status, Pageable pageable);

    // Keyword search across multiple fields (forced order)
    @Query("""
        select i from ImportData i
        where (:q is null
           or lower(i.beNo) like lower(concat('%', :q, '%'))
           or lower(i.dbkPartNo) like lower(concat('%', :q, '%'))
           or lower(i.itchsCode) like lower(concat('%', :q, '%'))
           or lower(i.itemDescription) like lower(concat('%', :q, '%')))
        order by i.beDate desc
    """)
    Page<ImportData> searchAllOrderByBeDateDesc(String q, Pageable pageable);
}
