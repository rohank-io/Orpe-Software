package com.orpe.consultants.service;

import com.orpe.consultants.dto.ChartPointDto;
import com.orpe.consultants.dto.DashboardMetricsDto;
import com.orpe.consultants.repository.ExportDataRepository;
import com.orpe.consultants.repository.ImportDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ExportDataRepository exportRepo;
    private final ImportDataRepository importRepo;
    
    private Long nullSafe(Long v) {
        return (v == null) ? 0L : v;
    }


    /**
     * Summary metrics: uses LocalDate boundaries (matching sbDate / beDate which are LocalDate).
     * If toDate is null, we default to today.
     */
    public DashboardMetricsDto getDashboardMetrics(LocalDate fromDate, LocalDate toDate) {
        LocalDate effectiveTo = (toDate != null) ? toDate : LocalDate.now();

        // fetchDashboardMetrics must accept LocalDate params and compare against sbDate / beDate
        DashboardMetricsDto metrics = exportRepo.fetchDashboardMetrics(fromDate, effectiveTo);

        if (metrics == null) {
            metrics = new DashboardMetricsDto();
        }

        // Defensive: fill import/export counts if repository DTO didn't include them
        if (metrics.getTotalImportCount() == null) {
            Long imp = importRepo.countImportsInRange(fromDate, effectiveTo);
            metrics.setTotalImportCount(imp == null ? 0L : imp);
        }
        if (metrics.getTotalExportCount() == null) {
            Long exp = exportRepo.countExportsInRange(fromDate, effectiveTo);
            metrics.setTotalExportCount(exp == null ? 0L : exp);
        }

        // ensure other numeric fields are non-null (optional but safe for Thymeleaf)
        if (metrics.getSbCount() == null) metrics.setSbCount(0L);
        if (metrics.getBoeCount() == null) metrics.setBoeCount(0L);
        if (metrics.getClaimedDbkAmount() == null) metrics.setClaimedDbkAmount(java.math.BigDecimal.ZERO);
        if (metrics.getTotalAirAmount() == null) metrics.setTotalAirAmount(java.math.BigDecimal.ZERO);
        if (metrics.getTotalSbrAmount() == null) metrics.setTotalSbrAmount(java.math.BigDecimal.ZERO);
        if (metrics.getTotalWorksheetCount() == null) metrics.setTotalWorksheetCount(0L);

        return metrics;
    }

    // ---------------- Chart code (uses LocalDate buckets and LocalDate repo params) --------------

    
}

