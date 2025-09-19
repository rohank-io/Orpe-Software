package com.orpe.consultants.repository;

import com.orpe.consultants.model.ShippingBill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingBillRepository extends JpaRepository<ShippingBill, Long> {

    // ==================== BASIC LOOKUPS ====================
    
    /**
     * Find shipping bill by unique SB number (uses idx_sb_no index)
     */
    Optional<ShippingBill> findBySbNo(String sbNo);
    
    /**
     * Check if SB number already exists
     */
    boolean existsBySbNo(String sbNo);

    // ==================== DATE-BASED QUERIES ====================
    
    /**
     * Find shipping bills within date range (uses idx_sb_date index)
     */
    List<ShippingBill> findBySbDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find shipping bills for specific date
     */
    List<ShippingBill> findBySbDate(LocalDate sbDate);
    
    /**
     * Find shipping bills by month and year (uses idx_sb_month_year index)
     */
    @Query("SELECT sb FROM ShippingBill sb WHERE sb.month = :month AND sb.year = :year")
    List<ShippingBill> findByMonthAndYear(@Param("month") String month, @Param("year") Integer year);

    /**
     * Find shipping bills by year only
     */
    List<ShippingBill> findByYear(Integer year);

    // ==================== FILTERING QUERIES ====================
    
    /**
     * Find by port code
     */
    List<ShippingBill> findByPortCode(String portCode);
    
    /**
     * Find by currency
     */
    List<ShippingBill> findByCurrency(String currency);
    
    /**
     * Find by invoice value range
     */
    @Query("SELECT sb FROM ShippingBill sb WHERE sb.invoiceValue BETWEEN :minValue AND :maxValue")
    List<ShippingBill> findByInvoiceValueRange(@Param("minValue") BigDecimal minValue, 
                                               @Param("maxValue") BigDecimal maxValue);

    /**
     * Find high-value invoices above threshold
     */
    List<ShippingBill> findByInvoiceValueGreaterThanEqual(BigDecimal minValue);

    // ==================== SEARCH QUERIES ====================
    
    /**
     * Search by buyer details (case-insensitive)
     */
    @Query("SELECT sb FROM ShippingBill sb WHERE LOWER(sb.buyerDetails) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ShippingBill> findByBuyerDetailsContainingIgnoreCase(@Param("searchTerm") String searchTerm);
    
    /**
     * Search by description (case-insensitive)
     */
    @Query("SELECT sb FROM ShippingBill sb WHERE LOWER(sb.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ShippingBill> findByDescriptionContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    /**
     * Search by HS code
     */
    List<ShippingBill> findByHsCdContaining(String hsCd);

    // ==================== PAGINATED QUERIES ====================
    
    /**
     * Find all with pagination and sorting
     */
    Page<ShippingBill> findAll(Pageable pageable);
    
    Page<ShippingBill> findAllByOrderBySbDateDesc(Pageable pageable);

    
    /**
     * Find by date range with pagination
     */
    Page<ShippingBill> findBySbDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * Find by port with pagination
     */
    Page<ShippingBill> findByPortCode(String portCode, Pageable pageable);

    // ==================== STATISTICAL QUERIES ====================
    
    /**
     * Get total invoice value for date range
     */
    @Query("SELECT COALESCE(SUM(sb.invoiceValue), 0) FROM ShippingBill sb WHERE sb.sbDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalInvoiceValueByDateRange(@Param("startDate") LocalDate startDate, 
                                              @Param("endDate") LocalDate endDate);

    /**
     * Get count by port
     */
    @Query("SELECT COUNT(sb) FROM ShippingBill sb WHERE sb.portCode = :portCode")
    Long countByPortCode(@Param("portCode") String portCode);
    
    

    /**
     * Get count by month and year
     */
    @Query("SELECT COUNT(sb) FROM ShippingBill sb WHERE sb.month = :month AND sb.year = :year")
    Long countByMonthAndYear(@Param("month") String month, @Param("year") Integer year);

    // ==================== RECENT DATA QUERIES ====================
    
    /**
     * Find recent shipping bills (uses created_at)
     */
    @Query("SELECT sb FROM ShippingBill sb WHERE sb.createdAt >= :fromDate ORDER BY sb.createdAt DESC")
    List<ShippingBill> findRecentShippingBills(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Find latest N records
     */
    List<ShippingBill> findTop10ByOrderByCreatedAtDesc();
    
    

    // ==================== DBK RELATED QUERIES ====================
    
    /**
     * Find by DBK serial number
     */
    List<ShippingBill> findByDbkSno(String dbkSno);
    
    /**
     * Find records with DBK applicability
     */
    List<ShippingBill> findByDbkApplicability(String dbkApplicability);
    
    /**
     * Find records with DBK amount greater than threshold
     */
    List<ShippingBill> findByDbkAmountGreaterThan(BigDecimal minDbkAmount);

    // ==================== CUSTOM COMPLEX QUERIES ====================
    
    /**
     * Find shipping bills with multiple criteria
     */
    @Query("SELECT sb FROM ShippingBill sb WHERE " +
           "(:portCode IS NULL OR sb.portCode = :portCode) AND " +
           "(:currency IS NULL OR sb.currency = :currency) AND " +
           "(:startDate IS NULL OR sb.sbDate >= :startDate) AND " +
           "(:endDate IS NULL OR sb.sbDate <= :endDate)")
    List<ShippingBill> findWithFilters(@Param("portCode") String portCode,
                                       @Param("currency") String currency,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /**
     * Get shipping bills summary by port
     */
    @Query("SELECT sb.portCode, COUNT(sb), SUM(sb.invoiceValue) FROM ShippingBill sb GROUP BY sb.portCode")
    List<Object[]> getPortWiseSummary();
    
    /**
     * Get monthly summary for reporting
     */
    @Query("SELECT sb.month, sb.year, COUNT(sb), SUM(sb.invoiceValue) FROM ShippingBill sb " +
           "GROUP BY sb.month, sb.year ORDER BY sb.year, sb.month")
    List<Object[]> getMonthlySummary();
    
    // ==================== UPDATE QUERIES ====================
    
    /**
     * Update BRC realization date for specific SB
     */
    @Query("UPDATE ShippingBill sb SET sb.brcRealisationDate = :brcDate WHERE sb.sbNo = :sbNo")
    int updateBrcRealisationDate(@Param("sbNo") String sbNo, @Param("brcDate") LocalDate brcDate);
}
