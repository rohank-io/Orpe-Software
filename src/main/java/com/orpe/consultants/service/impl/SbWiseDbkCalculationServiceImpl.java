package com.orpe.consultants.service.impl;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.*;



import org.springframework.stereotype.Service;

import com.orpe.consultants.dto.SbWiseConsumptionDetailDTO;
import com.orpe.consultants.dto.SbWiseDbkCalculationDTO;
import com.orpe.consultants.model.ExportData;
import com.orpe.consultants.model.ImportData;
import com.orpe.consultants.model.SbWiseDbkCalculation;
import com.orpe.consultants.model.SbWiseQuantityConsumption;
import com.orpe.consultants.model.User;
import com.orpe.consultants.model.Worksheet;
import com.orpe.consultants.model.WorksheetExportModels;
import com.orpe.consultants.repository.ExportDataRepository;
import com.orpe.consultants.repository.ImportDataRepository;
import com.orpe.consultants.repository.SbWiseDbkCalculationRepository;
import com.orpe.consultants.repository.WorksheetRepository;
import com.orpe.consultants.repository.SbWiseQuantityConsumptionRepository;
import com.orpe.consultants.service.SbWiseDbkCalculationService;
import com.orpe.consultants.service.WorksheetService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SbWiseDbkCalculationServiceImpl implements SbWiseDbkCalculationService {
	
	

	    private final ExportDataRepository exportDataRepository;
	    private final WorksheetRepository worksheetRepository;
	    private final ImportDataRepository importDataRepository;
	    private final SbWiseDbkCalculationRepository sbWiseDbkCalculationRepository;
	    private final SbWiseQuantityConsumptionRepository sbUsageRepo;

	    private final WorksheetService worksheetService; // your existing service for fetching worksheet meta if needed

	    /**
	     * Build DTO list for the given exportIds.
	     */
	    

	    private static final BigDecimal ONE = BigDecimal.ONE;
	    private static final BigDecimal ZERO = BigDecimal.ZERO;

	    /**
	     * =========================================================
	     * MAIN TABLE (PARENT ROWS)
	     * =========================================================
	     */
	    @Override
	    public List<SbWiseDbkCalculationDTO> calculateForExportIds(
	            List<Long> exportIds,
	            User performedBy) {

	        if (exportIds == null || exportIds.isEmpty())
	            return Collections.emptyList();

	        // FIFO order by SB Date
	        List<ExportData> exports =
	                exportDataRepository.findAllByExportIdInOrderBySbDateAsc(exportIds);

	        if (exports.isEmpty())
	            return Collections.emptyList();

	        log.info("=== DBK CALCULATION STARTED ===");

	        Map<String, BigDecimal> claimImportQtyMap = new HashMap<>();
	        Map<String, BigDecimal> claimTotalDutyMap = new HashMap<>();
	        Map<String, BigDecimal> claimTotalCifMap = new HashMap<>();

	        Set<String> claimKeys = exports.stream()
	                .filter(e -> e.getClaimRefNo() != null && e.getClaimYear() != null)
	                .map(e -> e.getClaimRefNo() + "||" + e.getClaimYear())
	                .collect(Collectors.toSet());

	        for (String key : claimKeys) {

	            String[] parts = key.split("\\|\\|");
	            String claimRef = parts[0];
	            String claimYear = parts[1];

	            List<ImportData> imports =
	                    importDataRepository.findByClaimRefNoAndClaimYear(claimRef, claimYear);

	            BigDecimal importQty = ZERO;
	            BigDecimal totalDuty = ZERO;
	            BigDecimal cif = ZERO;

	            for (ImportData imp : imports) {

	                importQty = importQty.add(safe(imp.getQuantity()));

	                totalDuty = totalDuty
	                        .add(safe(imp.getBcd()))
	                        .add(safe(imp.getSws()))
	                        .add(safe(imp.getAddDuty()));

	                cif = cif.add(safe(imp.getAssessableValue()));
	            }

	            claimImportQtyMap.put(key, importQty);
	            claimTotalDutyMap.put(key, totalDuty);
	            claimTotalCifMap.put(key, cif);
	        }

	        List<SbWiseDbkCalculationDTO> rows = new ArrayList<>();

	        for (ExportData ed : exports) {

	            SbWiseDbkCalculationDTO dto = new SbWiseDbkCalculationDTO();

	            dto.setExportId(ed.getExportId());
	            dto.setPortCode(ed.getPortCode());
	            dto.setShippingBillNo(ed.getSbNo());
	            dto.setShippingBillDate(ed.getSbDate());
	            dto.setLeoDate(ed.getLeoDate());
	            dto.setExportDescription(ed.getModelDescription());
	            dto.setQuantity(ed.getQuantity());
	            dto.setUnit(ed.getUnit());
	            dto.setDbkSno(ed.getDbkSno());
	            dto.setFobValue(ed.getFobInr());
	            dto.setPmvValue(ed.getPmvPerQty());
	            dto.setAirRate(ed.getRate());
	            dto.setAirAmount(ed.getAirAmount());
	            dto.setClaimRefNo(ed.getClaimRefNo());
	            dto.setClaimYear(ed.getClaimYear());

	            String claimKey =
	                    ed.getClaimRefNo() != null && ed.getClaimYear() != null
	                            ? ed.getClaimRefNo() + "||" + ed.getClaimYear()
	                            : null;

	            dto.setImportQuantity(
	                    claimKey == null ? ZERO :
	                            claimImportQtyMap.getOrDefault(claimKey, ZERO));

	            dto.setTotalDuty(
	                    claimKey == null ? ZERO :
	                            claimTotalDutyMap.getOrDefault(claimKey, ZERO));

	            dto.setTotalCifValue(
	                    claimKey == null ? ZERO :
	                            claimTotalCifMap.getOrDefault(claimKey, ZERO));

	            // 🔒 Parent consumption handled on UI
	            dto.setConsumptionPerExportQty(ZERO);

	            rows.add(dto);
	        }

	        log.info("=== DBK CALCULATION COMPLETED. Rows Generated = {} ===", rows.size());
	        return rows;
	    }


	    /**
	     * =========================================================
	     * TOGGLE ROWS (CHILD DATA – EXCEL MATCH)
	     * =========================================================
	     */
	    @Override
	    public List<SbWiseConsumptionDetailDTO> getConsumptionDetailsForExport(Long exportId) {

	        ExportData export = exportDataRepository.findById(exportId)
	                .orElseThrow(() -> new IllegalArgumentException("Export not found"));

	        // All Import rows of same claim year
	        List<ImportData> imports =
	                importDataRepository.findByClaimRefNoAndClaimYear(
	                        export.getClaimRefNo(),
	                        export.getClaimYear()
	                );

	        // ✅ ORDER BY BOM PART NO
	        imports.sort(
	                Comparator.comparing(
	                        (ImportData i) ->
	                                i.getMaterial() != null
	                                        ? i.getMaterial().getBomPartNo()
	                                        : "",
	                        String.CASE_INSENSITIVE_ORDER
	                )
	        );

	        // All SB-wise consumption rows
	        List<SbWiseQuantityConsumption> usages =
	                sbUsageRepo.findBySbNoAndClaimRefNoAndClaimYear(
	                        export.getSbNo(),
	                        export.getClaimRefNo(),
	                        export.getClaimYear()
	                );

	        List<SbWiseConsumptionDetailDTO> details = new ArrayList<>();

	        // 🔒 Prevent duplicate usedQty
	        Set<String> consumptionAssigned = new HashSet<>();

	        for (ImportData imp : imports) {

	            String bomPartNo =
	                    imp.getMaterial() != null
	                            ? imp.getMaterial().getBomPartNo()
	                            : null;

	            String key = importKey(imp.getBeNo(), bomPartNo);

	            BigDecimal usedQty = ZERO;

	            // ✅ Assign only ONCE per (BE + BOM)
	            if (!consumptionAssigned.contains(key)) {

	                usedQty =
	                        usages.stream()
	                                .filter(u ->
	                                        equals(u.getSbNo(), export.getSbNo()) &&
	                                        equals(u.getBoeNo(), imp.getBeNo()) &&
	                                        bomLikeMatch(u.getBomPartNo(), bomPartNo)
	                                )
	                                .map(SbWiseQuantityConsumption::getUsedQty)
	                                .reduce(ZERO, BigDecimal::add);

	                consumptionAssigned.add(key);
	            }

	            details.add(
	                    SbWiseConsumptionDetailDTO.builder()
	                            .exportId(exportId)
	                            .sbNo(export.getSbNo())
	                            .dbkPartNo(imp.getDbkPartNo())
	                            .bomPartNo(bomPartNo)
	                            .boeNo(imp.getBeNo())
	                            .consumptionForOneExportQty(usedQty)
	                            .importQuantity(imp.getQuantity())
	                            .assessableValue(imp.getAssessableValue())
	                            .cifValue(imp.getAssessableValue())
	                            .bcd(safe(imp.getBcd()))
	                            .sws(safe(imp.getSws()))
	                            .addDuty(safe(imp.getAddDuty()))
	                            .totalDuty(
	                                    safe(imp.getBcd())
	                                            .add(safe(imp.getSws()))
	                                            .add(safe(imp.getAddDuty()))
	                            )
	                            .build()
	            );
	        }

	        return details;
	    }

	    
	    private boolean bomLikeMatch(String a, String b) {
	        if (a == null || b == null) return false;

	        String x = a.trim().toUpperCase();
	        String y = b.trim().toUpperCase();

	        return x.contains(y) || y.contains(x);
	    }

	    private String importKey(String beNo, String bomPartNo) {
	        return normalize(beNo) + "|" + normalize(bomPartNo);
	    }

	    private String normalize(String v) {
	        return v == null ? "" : v.trim().toUpperCase();
	    }

	    private BigDecimal safe(BigDecimal v) {
	        return v != null ? v : BigDecimal.ZERO;
	    }

	    private boolean equals(String a, String b) {
	        if (a == null || b == null) return false;
	        return a.trim().equalsIgnoreCase(b.trim());
	    }






	    /**
	     * =========================================================
	     * TRUE FIFO – EXACT EXCEL BEHAVIOUR
	     * SB + BE NO + DBK PART NO
	     * =========================================================
	     */
	    private Map<String, Map<Long, BigDecimal>> buildFifoConsumptionMap(
	            List<ExportData> sbExports,
	            List<SbWiseQuantityConsumption> usages) {

	        Map<String, Map<Long, BigDecimal>> result = new HashMap<>();

	        // Group usages by (SB + BE + DBK PART)
	        Map<String, BigDecimal> totalUsedMap =
	                usages.stream().collect(
	                    Collectors.groupingBy(
	                        u -> key(u.getSbNo(), u.getBoeNo(), u.getDbkPartNo()),
	                        Collectors.reducing(
	                            ZERO,
	                            SbWiseQuantityConsumption::getUsedQty,
	                            BigDecimal::add
	                        )
	                    )
	                );

	        // FIFO allocation
	        for (Map.Entry<String, BigDecimal> entry : totalUsedMap.entrySet()) {

	            BigDecimal remaining = entry.getValue();
	            Map<Long, BigDecimal> perExport = new LinkedHashMap<>();

	            for (ExportData ed : sbExports) {

	                if (remaining.compareTo(ZERO) <= 0) {
	                    perExport.put(ed.getExportId(), ZERO);
	                    continue;
	                }

	                // FIFO → 1 unit per export
	                BigDecimal alloc = ONE.min(remaining);
	                perExport.put(ed.getExportId(), alloc);
	                remaining = remaining.subtract(alloc);
	            }

	            result.put(entry.getKey(), perExport);
	        }

	        return result;
	    }




	    

	    private String key(String sbNo, String beNo, String dbkPartNo) {
	        return normalize(sbNo) + "|" +
	               normalize(beNo) + "|" +
	               normalize(dbkPartNo);
	    }

	    

	


	    
	    
	    @Transactional
	    public List<Long> saveAll(List<SbWiseDbkCalculationDTO> rows, User performedBy) {
	        if (rows == null || rows.isEmpty()) return Collections.emptyList();

	        // map DTOs defensively
	        List<SbWiseDbkCalculation> entities = new ArrayList<>(rows.size());
	        for (SbWiseDbkCalculationDTO dto : rows) {
	            try {
	                SbWiseDbkCalculation e = mapDtoToEntity(dto);
	                // optionally set audit info: createdBy, createdAt using performedBy if entity supports it
	                entities.add(e);
	            } catch (Exception ex) {
	                log.warn("Skipping row due to mapping error: {} -> {}", dto, ex.getMessage());
	            }
	        }

	        if (entities.isEmpty()) return Collections.emptyList();

	        // persist batch of SbWiseDbkCalculation
	        List<SbWiseDbkCalculation> saved = sbWiseDbkCalculationRepository.saveAll(entities);

	        // collect returned ids to return to caller
	        List<Long> savedIds = saved.stream()
	                .map(SbWiseDbkCalculation::getDbkCalcId)
	                .filter(Objects::nonNull)
	                .collect(Collectors.toList());

	        // Group saved rows by exportId and compute aggregates
	        // Map<exportId, Aggregates>
	        Map<Long, Aggregates> aggregatesByExport = new HashMap<>();
	        for (SbWiseDbkCalculation s : saved) {
	            Long exportId = s.getExportId();
	            if (exportId == null) continue;
	            Aggregates agg = aggregatesByExport.computeIfAbsent(exportId, id -> new Aggregates());
	            agg.airAmount = agg.airAmount.add(nullSafeBig(s.getAirAmount()));
	            // totalDbk we treat as sum of dbkAmount from calculations
	            agg.totalDbk = agg.totalDbk.add(nullSafeBig(s.getDbkAmount()));
	            agg.sbr = agg.sbr.add(nullSafeBig(s.getSbr()));
	        }

	        if (!aggregatesByExport.isEmpty()) {
	            // fetch existing ExportData rows for the exportIds
	            List<Long> exportIds = new ArrayList<>(aggregatesByExport.keySet());
	            List<ExportData> exportDataList = exportDataRepository.findAllById(exportIds);

	            // update exported rows
	            for (ExportData ed : exportDataList) {
	                Aggregates agg = aggregatesByExport.get(ed.getExportId());
	                if (agg == null) continue; // no change
	                // Set sb_utilization to CLOSED and apply aggregate values
	                ed.setSbUtilization("CLOSED");
	                // Replace exported amounts with computed sums from the saved calculations
	                ed.setAirAmount(agg.airAmount);
	                ed.setTotalDbk(agg.totalDbk);
	                ed.setSbr(agg.sbr);
	            }

	            // persist the updated ExportData rows
	            exportDataRepository.saveAll(exportDataList);
	        }

	        return savedIds;
	    }

	    /** Map DTO -> entity (manual mapping, defensive) */
	    private SbWiseDbkCalculation mapDtoToEntity(SbWiseDbkCalculationDTO dto) {
	        SbWiseDbkCalculation e = new SbWiseDbkCalculation();

	        // if you want to update existing rows when dto.dbkCalcId != null,
	        // you can fetch from repo and update rather than create new. Here we create new entries.
	        // e.setDbkCalcId(dto.getDbkCalcId()); // usually not set for new entities

	        e.setExportId(dto.getExportId());

	        e.setPortCode(blankToNull(dto.getPortCode()));
	        e.setShippingBillNo(blankToNull(dto.getShippingBillNo()));

	        e.setClaimRefNo(blankToNull(dto.getClaimRefNo()));
	        e.setClaimYear(blankToNull(dto.getClaimYear()));

	        // dates
	        e.setShippingBillDate(dto.getShippingBillDate());
	        e.setLeoDate(dto.getLeoDate());

	        e.setExportDescription(blankToNull(dto.getExportDescription()));
	        e.setQuantity(nullSafeBig(dto.getQuantity()));
	        e.setUnit(blankToNull(dto.getUnit()));
	        e.setDbkSno(blankToNull(dto.getDbkSno()));
	        e.setFobValue(nullSafeBig(dto.getFobValue()));
	        e.setPmvValue(nullSafeBig(dto.getPmvValue()));
	        e.setImportQuantity(nullSafeBig(dto.getImportQuantity()));
	        e.setConsumptionPerExportQty(nullSafeBig(dto.getConsumptionPerExportQty()));
	        e.setTotalDuty(nullSafeBig(dto.getTotalDuty()));
	        e.setTotalCifValue(nullSafeBig(dto.getTotalCifValue()));
	        e.setDbkAmount(nullSafeBig(dto.getDbkAmount()));
	        e.setCifValue(nullSafeBig(dto.getCifValue()));
	        e.setAirRate(nullSafeBig(dto.getAirRate()));
	        e.setAirAmount(nullSafeBig(dto.getAirAmount()));
	        e.setSbr(nullSafeBig(dto.getSbr()));
	        e.setFourFifthOfBrodMainClaim(nullSafeBig(dto.getFourFifthOfBrodMainClaim()));
	        e.setDiffBrodAndAir(nullSafeBig(dto.getDiffBrodAndAir()));
	        e.setValueAddition(nullSafeBig(dto.getValueAddition()));

	        return e;
	    }

	    /* ---------- Helpers ---------- */

	    // Null-safe conversion for BigDecimal-like DTO fields
	    private static BigDecimal nullSafeBig(BigDecimal v) {
	        return v == null ? BigDecimal.ZERO : v;
	    }

	    // If DTO uses Strings for some fields, trim and return null when blank
	    private static String blankToNull(String s) {
	        if (s == null) return null;
	        String t = s.trim();
	        return t.isEmpty() ? null : t;
	    }

	    // Simple container to hold aggregated sums
	    private static class Aggregates {
	        BigDecimal airAmount = BigDecimal.ZERO;
	        BigDecimal totalDbk = BigDecimal.ZERO;
	        BigDecimal sbr = BigDecimal.ZERO;
	    }





	    /**
	     * Streams an Excel file with the calculated rows to response.
	     */
//	    public void exportDbkCalculationToExcel(List<Long> exportIds, User performedBy, HttpServletResponse response) throws IOException {
//	        List<DbkCalculationRowDTO> rows = calculateForExportIds(exportIds, performedBy);
//	        try (Workbook wb = new XSSFWorkbook()) {
//	            Sheet sheet = wb.createSheet("DBK Calculation");
//
//	            int r = 0;
//	            Row hdr = sheet.createRow(r++);
//	            int c = 0;
//	            hdr.createCell(c++).setCellValue("SL No");
//	            hdr.createCell(c++).setCellValue("Port Code");
//	            hdr.createCell(c++).setCellValue("Shipping Bill No");
//	            hdr.createCell(c++).setCellValue("Date");
//	            hdr.createCell(c++).setCellValue("LEO Date");
//	            hdr.createCell(c++).setCellValue("Description");
//	            hdr.createCell(c++).setCellValue("Quantity");
//	            hdr.createCell(c++).setCellValue("Unit");
//	            hdr.createCell(c++).setCellValue("DBK SNO");
//	            hdr.createCell(c++).setCellValue("FOB Value");
//	            hdr.createCell(c++).setCellValue("PMV Value");
//	            hdr.createCell(c++).setCellValue("Imp Quantity");
//	            hdr.createCell(c++).setCellValue("Consumption for Exp.Qty");
//	            // ... add other headers for fields 14..23
//	            hdr.createCell(c++).setCellValue("AIR Rate");
//	            hdr.createCell(c++).setCellValue("AIR Amount");
//
//	            int idx = 1;
//	            for (DbkCalculationRowDTO d : rows) {
//	                Row row = sheet.createRow(r++);
//	                int cc = 0;
//	                row.createCell(cc++).setCellValue(idx++);
//	                row.createCell(cc++).setCellValue(ns(d.getPortCode()));
//	                row.createCell(cc++).setCellValue(ns(d.getSbNo()));
//	                row.createCell(cc++).setCellValue(d.getSbDate() != null ? d.getSbDate().toString() : "");
//	                row.createCell(cc++).setCellValue(d.getLeoDate() != null ? d.getLeoDate().toString() : "");
//	                row.createCell(cc++).setCellValue(ns(d.getModelDescription()));
//	                row.createCell(cc++).setCellValue(d.getQuantity() != null ? d.getQuantity().doubleValue() : 0d);
//	                row.createCell(cc++).setCellValue(ns(d.getUnit()));
//	                row.createCell(cc++).setCellValue(ns(d.getDbkSno()));
//	                row.createCell(cc++).setCellValue(d.getFobValue() != null ? d.getFobValue().doubleValue() : 0d);
//	                row.createCell(cc++).setCellValue(d.getPmvValue() != null ? d.getPmvValue().doubleValue() : 0d);
//	                row.createCell(cc++).setCellValue(d.getImpQuantity() != null ? d.getImpQuantity().doubleValue() : 0d);
//	                row.createCell(cc++).setCellValue(d.getConsumptionForExpQty() != null ? d.getConsumptionForExpQty().doubleValue() : 0d);
//	                // TODO: other formula-based cells
//	                row.createCell(cc++).setCellValue(d.getAirRate() != null ? d.getAirRate().doubleValue() : 0d);
//	                row.createCell(cc++).setCellValue(d.getAirAmount() != null ? d.getAirAmount().doubleValue() : 0d);
//	            }
//
//	            // autosize
//	            for (int i = 0; i < 30; i++) sheet.autoSizeColumn(i);
//
//	            String filename = "dbk-calculation-" + LocalDate.now() + ".xlsx";
//	            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//	            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
//	            wb.write(response.getOutputStream());
//	        }
//	    }

	    private String ns(String s) {
	        return s == null ? "" : s;
	    }
	    
	    @Override
	    public List<Map<String, Object>> getAllDbkGroups() {
	        List<Object[]> rows = sbWiseDbkCalculationRepository.findAllDbkGroups();
	        List<Map<String, Object>> result = new ArrayList<>(rows.size());

	        for (Object[] r : rows) {
	            Map<String, Object> m = new HashMap<>();

	            m.put("claimRefNo", r[0] != null ? r[0].toString() : null);
	            m.put("claimYear", r[1] != null ? r[1].toString() : null);

	            Long countValue = 0L;
	            if (r[2] instanceof Number) {
	                countValue = ((Number) r[2]).longValue();
	            }
	            m.put("count", countValue);

	            // Convert LocalDate/Date safely
	            LocalDate sbDate = null;
	            if (r[3] instanceof java.sql.Date) {
	                sbDate = ((java.sql.Date) r[3]).toLocalDate();
	            } else if (r[3] instanceof LocalDate) {
	                sbDate = (LocalDate) r[3];
	            }
	            m.put("minSbDate", sbDate);

	            result.add(m);
	        }

	        return result;
	    }
	    
	    
	    
	    @Override
	    public List<SbWiseDbkCalculationDTO> findByClaimRefAndYear(String claimRefNo, String claimYear) {
	        if (claimRefNo == null || claimRefNo.isBlank() || claimYear == null || claimYear.isBlank()) {
	            return Collections.emptyList();
	        }
	        List<SbWiseDbkCalculation> list = sbWiseDbkCalculationRepository.findByClaimRefNoAndClaimYearOrderByShippingBillDateAsc(claimRefNo, claimYear);
	        return list.stream().map(this::toDto).toList();
	    }
	    
	    private SbWiseDbkCalculationDTO toDto(SbWiseDbkCalculation e) {
	        if (e == null) return null;
	        return SbWiseDbkCalculationDTO.builder()
	                .dbkCalcId(e.getDbkCalcId())
	                .exportId(e.getExportId())
	                .portCode(e.getPortCode())
	                .shippingBillNo(e.getShippingBillNo())
	                .shippingBillDate(e.getShippingBillDate())
	                .leoDate(e.getLeoDate())
	                .exportDescription(e.getExportDescription())
	                .quantity(e.getQuantity())
	                .unit(e.getUnit())
	                .dbkSno(e.getDbkSno())
	                .fobValue(e.getFobValue())
	                .pmvValue(e.getPmvValue())
	                .importQuantity(e.getImportQuantity())
	                .consumptionPerExportQty(e.getConsumptionPerExportQty())
	                .totalDuty(e.getTotalDuty())
	                .totalCifValue(e.getTotalCifValue())
	                .dbkAmount(e.getDbkAmount())
	                .cifValue(e.getCifValue())
	                .airRate(e.getAirRate())
	                .airAmount(e.getAirAmount())
	                .sbr(e.getSbr())
	                .fourFifthOfBrodMainClaim(e.getFourFifthOfBrodMainClaim())
	                .diffBrodAndAir(e.getDiffBrodAndAir())
	                .valueAddition(e.getValueAddition())
	                .claimRefNo(e.getClaimRefNo())  
	                .claimYear(e.getClaimYear())// optional claim fields if DTO has them
	                .build();
	    }

	


}
