package com.orpe.consultants.repository;



import com.orpe.consultants.model.ExportData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExportDataRepository extends JpaRepository<ExportData, Long>, JpaSpecificationExecutor<ExportData> {

    // Global default ordered by sbDate desc
    Page<ExportData> findAllByOrderBySbDateDesc(Pageable pageable);

    // Simple lookups with forced order
    Page<ExportData> findBySbNoContainingIgnoreCaseOrderBySbDateDesc(String sbNo, Pageable pageable);

    Page<ExportData> findByInvoiceNoContainingIgnoreCaseOrderBySbDateDesc(String invoiceNo, Pageable pageable);

    Page<ExportData> findByPortCodeContainingIgnoreCaseOrderBySbDateDesc(String portCode, Pageable pageable);

    Page<ExportData> findByCustomerNameContainingIgnoreCaseOrderBySbDateDesc(String customerName, Pageable pageable);

    // Date range filter
    Page<ExportData> findBySbDateBetweenOrderBySbDateDesc(LocalDate from, LocalDate to, Pageable pageable);

    // By Claim Year
    Page<ExportData> findByClaimYearOrderBySbDateDesc(String claimYear, Pageable pageable);

    // Combined filters example
    Page<ExportData> findBySbDateBetweenAndPortCodeContainingIgnoreCaseOrderBySbDateDesc(LocalDate from, LocalDate to, String portCode, Pageable pageable);

    @Query("""
        select e from ExportData e
        where (:q is null
           or lower(e.sbNo) like lower(concat('%', :q, '%'))
           or lower(e.invoiceNo) like lower(concat('%', :q, '%'))
           or lower(e.portCode) like lower(concat('%', :q, '%'))
           or lower(e.customerName) like lower(concat('%', :q, '%'))
           or lower(e.dbkSno) like lower(concat('%', :q, '%'))
           or lower(e.modelDescription) like lower(concat('%', :q, '%'))
        )
        order by e.sbDate desc
    """)
    Page<ExportData> searchAllOrderBySbDateDesc(String q, Pageable pageable);
    
    
    Optional<ExportData> findByClaimRefNoAndModels_ModelNo(String claimRefNo, String modelNo);

}
