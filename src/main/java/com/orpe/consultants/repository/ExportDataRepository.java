package com.orpe.consultants.repository;

import com.orpe.consultants.model.ExportData;
import com.orpe.consultants.dto.DashboardMetricsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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
	Page<ExportData> findBySbDateBetweenAndPortCodeContainingIgnoreCaseOrderBySbDateDesc(LocalDate from, LocalDate to,
			String portCode, Pageable pageable);

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

	List<ExportData> findByClaimRefNoAndModels_ModelNo(String claimRefNo, String modelNo);

	@Query("""
			    SELECT new com.orpe.consultants.dto.DashboardMetricsDto(
			        COUNT( e.sbNo),
			        (SELECT COUNT( i.altBoePartNo)
			           FROM ImportData i
			           WHERE (:fromDate IS NULL OR i.beDate >= :fromDate)
			             AND (:toDate   IS NULL OR i.beDate <= :toDate)
			        ),
			        COALESCE(SUM(e.totalDbk), 0),
			        COALESCE(SUM(e.airAmount), 0),
			        COALESCE(SUM(e.sbr), 0),
			        (SELECT COUNT(DISTINCT w.claimYear)
			      FROM Worksheet w
			      WHERE (:fromDate IS NULL OR w.beDate >= :fromDate)
			        AND (:toDate   IS NULL OR w.beDate <= :toDate)
			   )
			    )
			    FROM ExportData e
			    WHERE (:fromDate IS NULL OR e.sbDate >= :fromDate)
			      AND (:toDate   IS NULL OR e.sbDate <= :toDate)
			""")
	DashboardMetricsDto fetchDashboardMetrics(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

	@Query("""
			    SELECT COUNT(e)
			    FROM ExportData e
			    WHERE (:fromDate IS NULL OR e.sbDate >= :fromDate)
			      AND (:toDate   IS NULL OR e.sbDate <= :toDate)
			""")
	Long countExportsInRange(LocalDate fromDate, LocalDate toDate);

	@Query("""
			    SELECT COUNT(DISTINCT e.sbNo)
			    FROM ExportData e
			    WHERE (:fromDate IS NULL OR e.sbDate >= :fromDate)
			      AND (:toDate   IS NULL OR e.sbDate <= :toDate)
			""")
	Long countSbInRange(LocalDate fromDate, LocalDate toDate);

	@Query("""
			SELECT e.claimRefNo,
			       e.claimYear,
			       MAX(e.invoiceDate)
			FROM ExportData e
			GROUP BY e.claimRefNo, e.claimYear
			""")
	List<Object[]> findMaxInvoiceDatesByClaim();
	
	
	 // loads the requested exports ordered by sbDate ASC (oldest first)
    List<ExportData> findAllByExportIdInOrderBySbDateAsc(List<Long> exportIds);

    // fallback if you prefer unsorted
    List<ExportData> findAllByExportIdIn(List<Long> exportIds);

}
