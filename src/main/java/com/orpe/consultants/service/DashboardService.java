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

    public List<ChartPointDto> getChartPoints(String range, LocalDate fromDate, LocalDate toDate) {
        LocalDate start = (fromDate != null) ? fromDate : LocalDate.now().minusDays(6); // last 7 days default
        LocalDate end = (toDate != null) ? toDate : LocalDate.now();

        switch ((range == null) ? "week" : range.toLowerCase()) {
            case "month":
                return buildMonthlyPoints(start, end);
            case "year":
                return buildYearlyPoints(start, end);
            default:
                return buildDailyPoints(start, end);
        }
    }

    // daily buckets (each bucket = one LocalDate)
    private List<ChartPointDto> buildDailyPoints(LocalDate start, LocalDate end) {
        List<ChartPointDto> result = new ArrayList<>();

        LocalDate current = start;

        while (!current.isAfter(end)) {

            Long imports = nullSafe(importRepo.countImportsInRange(current, current));
            Long exports = nullSafe(exportRepo.countExportsInRange(current, current));
            Long sbCount = nullSafe(exportRepo.countSbInRange(current, current));

            result.add(new ChartPointDto(
                    current.toString(),
                    imports,
                    exports,
                    sbCount
            ));

            current = current.plusDays(1);
        }

        return result;
    }


    // monthly buckets (bucket = full calendar month)
    private List<ChartPointDto> buildMonthlyPoints(LocalDate start, LocalDate end) {
        List<ChartPointDto> result = new ArrayList<>();

        LocalDate monthStart = start.withDayOfMonth(1);

        while (!monthStart.isAfter(end)) {
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

            Long imports = nullSafe(importRepo.countImportsInRange(monthStart, monthEnd));
            Long exports = nullSafe(exportRepo.countExportsInRange(monthStart, monthEnd));
            Long sbCount = nullSafe(exportRepo.countSbInRange(monthStart, monthEnd));

            result.add(new ChartPointDto(
                    monthStart.getMonth().toString().substring(0, 3),
                    imports,
                    exports,
                    sbCount
            ));

            monthStart = monthStart.plusMonths(1);
        }

        return result;
    }


    // yearly buckets (bucket = calendar year)
    private List<ChartPointDto> buildYearlyPoints(LocalDate start, LocalDate end) {
        List<ChartPointDto> result = new ArrayList<>();

        int startYear = start.getYear();
        int endYear = end.getYear();

        for (int y = startYear; y <= endYear; y++) {
            LocalDate from = LocalDate.of(y, 1, 1);
            LocalDate to = LocalDate.of(y, 12, 31);

            Long imports = nullSafe(importRepo.countImportsInRange(from, to));
            Long exports = nullSafe(exportRepo.countExportsInRange(from, to));
            Long sbCount = nullSafe(exportRepo.countSbInRange(from, to));

            result.add(new ChartPointDto(
                    String.valueOf(y),
                    imports,
                    exports,
                    sbCount
            ));
        }

        return result;
    }

    // small helper to guard against repo returning null or throwing
    private Long safeCount(java.util.concurrent.Callable<Long> c) {
        try {
            Long v = c.call();
            return (v == null) ? 0L : v;
        } catch (Exception ex) {
            log.warn("count query failed: {}", ex.getMessage());
            return 0L;
        }
    }
}

