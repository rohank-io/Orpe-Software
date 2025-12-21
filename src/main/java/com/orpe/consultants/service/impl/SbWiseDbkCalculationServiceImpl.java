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

import com.orpe.consultants.dto.SbWiseDbkCalculationDTO;
import com.orpe.consultants.model.ExportData;
import com.orpe.consultants.model.SbWiseDbkCalculation;
import com.orpe.consultants.model.User;
import com.orpe.consultants.model.Worksheet;
import com.orpe.consultants.model.WorksheetExportModels;
import com.orpe.consultants.repository.ExportDataRepository;
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
	    private final SbWiseDbkCalculationRepository sbWiseDbkCalculationRepository;
	    private final SbWiseQuantityConsumptionRepository sbUsageRepo;
	    private final WorksheetService worksheetService; // your existing service for fetching worksheet meta if needed

	    /**
	     * Build DTO list for the given exportIds.
	     */
	    

	    public List<SbWiseDbkCalculationDTO> calculateForExportIds(List<Long> exportIds, User performedBy) {

	        if (exportIds == null || exportIds.isEmpty()) return Collections.emptyList();

	        List<ExportData> exports = exportDataRepository.findAllByExportIdInOrderBySbDateAsc(exportIds);
	        if (exports == null || exports.isEmpty()) return Collections.emptyList();

	        log.info("=== DBK CALCULATION STARTED ===");

	        // 2) SB-wise consumption totals
	        Set<String> sbNos = exports.stream()
	                .map(ExportData::getSbNo)
	                .filter(Objects::nonNull)
	                .collect(Collectors.toSet());

	        Map<String, BigDecimal> sbConsumptionMap = new HashMap<>();
	        if (!sbNos.isEmpty()) {
	            List<SbWiseQuantityConsumptionRepository.SbUsageProjection> usages =
	                    sbUsageRepo.sumUsedQtyBySbNoIn(new ArrayList<>(sbNos));

	            if (usages != null) {
	                for (var p : usages) {
	                    BigDecimal used = p.getUsedTotal() == null ? BigDecimal.ZERO : p.getUsedTotal();
	                    sbConsumptionMap.put(p.getSbNo(), used);
	                    log.info("SB {} → Total Used Qty = {}", p.getSbNo(), used);
	                }
	            }
	        }

	        // Rows per SB
	        Map<String, Long> sbSelectedCounts = exports.stream()
	                .map(ExportData::getSbNo)
	                .filter(Objects::nonNull)
	                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

	        // Worksheet cache
	        Map<String, List<Worksheet>> worksheetCache = new HashMap<>();

	        List<SbWiseDbkCalculationDTO> rows = new ArrayList<>();

	        for (ExportData ed : exports) {

	            log.info("\n---- Processing ExportID {} | SB {} | Model {} ----",
	                    ed.getExportId(),
	                    ed.getSbNo(),
	                    (ed.getModels() != null ? ed.getModels().getModelNo() : "NULL")
	            );

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
	         // NEW: populate claim info (you added these columns to the entity)
	            dto.setClaimRefNo(ed.getClaimRefNo());
	            dto.setClaimYear(ed.getClaimYear());

	            BigDecimal sumImportQty = BigDecimal.ZERO;
	            BigDecimal sumTotalDuty = BigDecimal.ZERO;
	            BigDecimal sumTotalCif = BigDecimal.ZERO;

	            String claimRef = ed.getClaimRefNo();
	            String claimYear = ed.getClaimYear();

	            String modelNo = (ed.getModels() != null ? ed.getModels().getModelNo() : null);

	            if (claimRef != null && claimYear != null && modelNo != null) {

	                String cacheKey = claimRef + "||" + claimYear;

	                List<Worksheet> worksheets = worksheetCache.computeIfAbsent(cacheKey, k -> {
	                    log.info("Loading Worksheets for Claim {} | Year {}", claimRef, claimYear);
	                    return worksheetRepository.findByClaimRefNoAndClaimYear(claimRef, claimYear);
	                });

	                log.info("Found {} Worksheets for Claim {}", worksheets.size(), claimRef);

	                final String modelKey = modelNo;

	                for (Worksheet ws : worksheets) {

	                    List<WorksheetExportModels> ems = ws.getExportModels();
	                    if (ems == null || ems.isEmpty()) continue;

	                    boolean modelPresent = ems.stream()
	                            .anyMatch(em -> em != null
	                                    && em.getModel() != null
	                                    && modelKey.equalsIgnoreCase(em.getModel().getModelNo()));

	                    if (modelPresent) {

	                        // ✅ PRINT ONLY THE RECORDS THAT ARE TAKEN
	                        log.info("✔ USED WorksheetID={} | BE No={} | Model={} | ImportQty={} | TotalDuty={} | CIF={}",
	                                ws.getWorksheetId(),
	                                ws.getBeNo(),
	                                modelKey,
	                                ws.getImportQty(),
	                                ws.getTotalDuty(),
	                                ws.getCifClaimedTotal()
	                        );

	                        if (ws.getImportQty() != null)
	                            sumImportQty = sumImportQty.add(ws.getImportQty());

	                        if (ws.getTotalDuty() != null)
	                            sumTotalDuty = sumTotalDuty.add(ws.getTotalDuty());

	                        if (ws.getCifClaimedTotal() != null)
	                            sumTotalCif = sumTotalCif.add(ws.getCifClaimedTotal());
	                    }
	                }
	            }

	            log.info("→ FINAL Worksheet Totals for ExportID {}: ImportQty={} | TotalDuty={} | CIF={}",
	                    ed.getExportId(), sumImportQty, sumTotalDuty, sumTotalCif);

	            dto.setImportQuantity(sumImportQty);
	            dto.setTotalDuty(sumTotalDuty);
	            dto.setTotalCifValue(sumTotalCif);

	            // SB equal split
	            BigDecimal totalUsedForSb = sbConsumptionMap.getOrDefault(ed.getSbNo(), BigDecimal.ZERO);
	            Long occurrences = sbSelectedCounts.get(ed.getSbNo());
	            BigDecimal perRow = BigDecimal.ZERO;

	            if (occurrences != null && occurrences > 0) {
	                perRow = totalUsedForSb.divide(BigDecimal.valueOf(occurrences), 6, RoundingMode.HALF_UP);
	            }

	            log.info("SB {} | TotalUsed={} | Rows={} | PerRow={}",
	                    ed.getSbNo(), totalUsedForSb, occurrences, perRow);

	            dto.setConsumptionPerExportQty(perRow);

	            dto.setAirRate(ed.getRate());
	            dto.setAirAmount(ed.getAirAmount());

	            rows.add(dto);
	        }

	        log.info("\n=== DBK CALCULATION COMPLETED. Rows generated = {} ===", rows.size());
	        return rows;
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
