package com.orpe.consultants.service.impl;


import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.*;

import org.springframework.stereotype.Service;

import com.orpe.consultants.dto.SbWiseDbkCalculationDTO;
import com.orpe.consultants.model.ExportData;
import com.orpe.consultants.model.User;
import com.orpe.consultants.repository.ExportDataRepository;
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
	    private final SbWiseQuantityConsumptionRepository sbUsageRepo;
	    private final WorksheetService worksheetService; // your existing service for fetching worksheet meta if needed

	    /**
	     * Build DTO list for the given exportIds.
	     */
	    public List<SbWiseDbkCalculationDTO> calculateForExportIds(List<Long> exportIds, User performedBy) {
	        if (exportIds == null || exportIds.isEmpty()) return Collections.emptyList();

	        // 1) Load export rows (preserve order by sbDate asc if you want)
	        List<ExportData> exports = exportDataRepository.findAllByExportIdInOrderBySbDateAsc(exportIds);

	        // 2) Prepare SB list and fetch consumption totals
	        List<String> sbNos = exports.stream()
	                .map(ExportData::getSbNo)
	                .filter(Objects::nonNull)
	                .distinct()
	                .toList();

	        Map<String, BigDecimal> sbConsumptionMap = new HashMap<>();
	        if (!sbNos.isEmpty()) {
	            List<SbWiseQuantityConsumptionRepository.SbUsageProjection> usages = sbUsageRepo.sumUsedQtyBySbNoIn(sbNos);
	            for (var p : usages) sbConsumptionMap.put(p.getSbNo(), p.getUsedTotal() == null ? BigDecimal.ZERO : p.getUsedTotal());
	        }

	        // 3) If impQuantity or other worksheet values depend on Worksheet entries,
	        //    call worksheetService to fetch/compute them here. For now we'll leave TODO.
	        Map<Long, BigDecimal> impQuantityMap = new HashMap<>(); // exportId -> impQuantity (populated later)

	        List<SbWiseDbkCalculationDTO> rows = new ArrayList<>(exports.size());
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
	            // FOB and PMV mapping — adjust if you want per-unit or per-row
	            dto.setFobValue(ed.getFobInr());
	            dto.setPmvValue(ed.getPmvPerQty());
	            // impQuantity: placeholder (populate using worksheetService)
	            dto.setImportQuantity( impQuantityMap.getOrDefault(ed.getExportId(), BigDecimal.ZERO) );
	            // consumption for exp qty: sum of usedQty for this SB
	            dto.setConsumptionPerExportQty(sbConsumptionMap.getOrDefault(ed.getSbNo(), BigDecimal.ZERO));
	            // copies
	            dto.setAirRate(ed.getAirGivenInSb()); // confirm mapping if you need rate instead
	            dto.setAirAmount(ed.getAirAmount());
	            // TODO: fill formula-driven fields here:
	            dto.setTotalDuty(null);
	            dto.setTotalCifValue(null);
	            dto.setDbkAmount(null);
	            dto.setCifValue(null);
	            dto.setSbr(null);
	            dto.setFourFifthOfBrodMainClaim(null);
	            dto.setDiffBrodAndAir(null);
	            dto.setValueAddition(null);

	            

	            rows.add(dto);
	        }

	        // IMPORTANT: If you require additional worksheet-based lookups per model / export,
	        // call the worksheetService and mutate the rows here.

	        return rows;
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
	


}
