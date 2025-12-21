	package com.orpe.consultants.service.impl;

import com.orpe.consultants.dto.ExportDataDTO;
import com.orpe.consultants.dto.ExportDataFilter;
import com.orpe.consultants.model.ExportData;
import com.orpe.consultants.model.ImportData;
import com.orpe.consultants.model.Material;
import com.orpe.consultants.model.Models;
import com.orpe.consultants.repository.ExportDataRepository;
import com.orpe.consultants.repository.ImportDataRepository;
import com.orpe.consultants.repository.ModelsRepository; // if you have one
import com.orpe.consultants.service.ExportDataService;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class ExportDataServiceImpl implements ExportDataService {

    private final ExportDataRepository exportRepo;
    private final ImportDataRepository importRepo;
    private final ModelsRepository modelsRepo; // optional if model management needed
    private final ModelMapper modelMapper;

    @Override
    public int saveBulk(List<ExportDataDTO> dtos) {
        int saved = 0;
        Map<String, Models> modelCache = new HashMap<>();
        for (ExportDataDTO dto : dtos) {
            Models model = null;
            if (dto.getModelNo() != null && !dto.getModelNo().isBlank()) {
                String key = dto.getModelNo().trim();
                model = modelCache.get(key);
                if (model == null) {
                    model = modelsRepo.findById(key).orElseGet(() -> 
                        modelsRepo.save(Models.builder().modelNo(key).build()));
                    modelCache.put(key, model);
                }
            }
            ExportData entity = dtoToEntity(dto);
            entity.setModels(model);
            exportRepo.save(entity);
            saved++;
        }
        return saved;
    }

    @Override
    public ExportDataDTO save(ExportDataDTO dto) {
        validate(dto);
        Models model = null;
        if (dto.getModelNo() != null && !dto.getModelNo().isBlank()) {
            String key = dto.getModelNo().trim();
            model = modelsRepo.findById(key)
                .orElseGet(() -> modelsRepo.save(Models.builder().modelNo(key).build()));
        }
        ExportData entity = dtoToEntity(dto);
        entity.setModels(model);
        ExportData saved = exportRepo.save(entity);
        return entityToDto(saved);
    }

    @Override
    public Optional<ExportDataDTO> findById(Long exportId) {
        return exportRepo.findById(exportId).map(this::entityToDto);
    }

    @Override
    public void deleteById(Long id) {
        exportRepo.deleteById(id);
    }

    @Override
    public List<ExportDataDTO> findAll() {
        return exportRepo.findAll().stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ExportDataDTO> search(ExportDataFilter filter, Pageable pageable) {
        Specification<ExportData> spec = buildSpecification(filter);
        Page<ExportData> page = exportRepo.findAll(spec, pageable);
        return page.map(this::entityToDto);
    }

    @Override
    public long count(ExportDataFilter filter) {
        return exportRepo.count(buildSpecification(filter));
    }

    @Override
    public boolean validate(ExportDataDTO dto) {
        if (!StringUtils.hasText(dto.getSbNo())) {
            throw new IllegalArgumentException("SB No is required");
        }
        if (dto.getSbDate() == null) {
            throw new IllegalArgumentException("SB Date is required");
        }
        if (!StringUtils.hasText(dto.getClaimRefNo())) {
            throw new IllegalArgumentException("Claim Ref No is required");
        }
        if (!StringUtils.hasText(dto.getClaimYear())) {
            throw new IllegalArgumentException("Claim Year is required");
        }
        if (dto.getQuantity() == null || dto.getQuantity().signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        return true;
    }

    private ExportData dtoToEntity(ExportDataDTO dto) {
        ExportData entity = modelMapper.map(dto, ExportData.class);
        if (dto.getSbNo() != null) {
            entity.setSbNo(dto.getSbNo().trim());
        }
        return entity;
    }

    private ExportDataDTO entityToDto(ExportData entity) {
        ExportDataDTO dto = modelMapper.map(entity, ExportDataDTO.class);
        if (entity.getModels() != null) {
            dto.setModelNo(entity.getModels().getModelNo());
        }
        return dto;
    }

    private Specification<ExportData> buildSpecification(ExportDataFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(filter.getSbNo())) {
                predicates.add(cb.like(cb.lower(root.get("sbNo")), "%" + filter.getSbNo().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(filter.getClaimYear())) {
                predicates.add(cb.equal(root.get("claimYear"), filter.getClaimYear()));
            }
            if (StringUtils.hasText(filter.getCustomerName())) {
                predicates.add(cb.like(cb.lower(root.get("customerName")), "%" + filter.getCustomerName().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(filter.getPortCode())) {
                predicates.add(cb.like(cb.lower(root.get("portCode")), "%" + filter.getPortCode().toLowerCase() + "%"));
            }
            if (filter.getSbDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sbDate"), filter.getSbDateFrom()));
            }
            if (filter.getSbDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("sbDate"), filter.getSbDateTo()));
            }
            if (StringUtils.hasText(filter.getClaimRefNo())) {
                predicates.add(cb.like(cb.lower(root.get("claimRefNo")), "%" + filter.getClaimRefNo().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(filter.getModelNo())) {
                predicates.add(cb.like(cb.lower(root.get("models").get("modelNo")), "%" + filter.getModelNo().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(filter.getDbkSno())) {
                predicates.add(cb.like(cb.lower(root.get("dbkSno")), "%" + filter.getDbkSno().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(filter.getSbUtilization())) {
                predicates.add(cb.equal(root.get("sbUtilization"), filter.getSbUtilization()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

	@Override
	public byte[] exportData(ExportDataFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	 @Override
	    public void writeExportExcel(List<Long> exportIds, OutputStream os)
	            throws IOException {

	        Workbook wb = new XSSFWorkbook();

	        /* ================= EXPORT DATA ================= */

	        List<ExportData> exports = exportRepo.findAllById(exportIds)
	                .stream()
	                .sorted(Comparator.comparing(ExportData::getSbDate))
	                .toList();

	        if (exports.isEmpty()) {
	            wb.write(os);
	            wb.close();
	            return;
	        }

	        LocalDate anchorSbDate = exports.get(0).getSbDate();

	        Set<String> claimRefNos = exports.stream()
	                .map(ExportData::getClaimRefNo)
	                .filter(Objects::nonNull)
	                .collect(Collectors.toSet());

	        writeExportSheet(wb, exports);

	        /* ================= DBK SHEETS ================= */

	        writeDbkSheets(wb, anchorSbDate, claimRefNos);

	        wb.write(os);
	        wb.close();
	    }

	    /* ============================================================
	                            EXPORT SHEET
	       ============================================================ */

	    private void writeExportSheet(Workbook wb, List<ExportData> data) {

	        Sheet sheet = wb.createSheet("Export Data");

	        CellStyle headerStyle = headerStyle(wb);
	        CellStyle totalStyle = totalStyle(wb);
	        CellStyle wrapStyle = wrapStyle(wb);

	        int rowIdx = 0;

	        Row h1 = sheet.createRow(rowIdx++);
	        Row h2 = sheet.createRow(rowIdx++);

	        createMerged(sheet, h1, h2, 0, "Sr. No", headerStyle);
	        createMerged(sheet, h1, h2, 1, "Invoice No. & Date", headerStyle);
	        createMerged(sheet, h1, h2, 2, "SB No. & Date", headerStyle);
	        createMerged(sheet, h1, h2, 3, "LEO Date", headerStyle);
	        createMerged(sheet, h1, h2, 4, "Description of Export product", headerStyle);
	        createMerged(sheet, h1, h2, 5, "Qty", headerStyle);
	        createMerged(sheet, h1, h2, 6, "FOB", headerStyle);
	        createMerged(sheet, h1, h2, 7, "PMV (Per QTY)", headerStyle);
	        createMerged(sheet, h1, h2, 8, "PMV", headerStyle);
	        createMerged(sheet, h1, h2, 9, "HSC", headerStyle);

	        merge(sheet, h1, 10, 12, "TOTAL", headerStyle);
	        h2.createCell(10).setCellValue("Qty");
	        h2.createCell(11).setCellValue("FOB");
	        h2.createCell(12).setCellValue("PMV");

	        merge(sheet, h1, 13, 14, "", headerStyle);
	        h2.createCell(13).setCellValue("DBK per unit");
	        h2.createCell(14).setCellValue("Total DBK");

	        createMerged(sheet, h1, h2, 15, "1.5% of FOB", headerStyle);
	        createMerged(sheet, h1, h2, 16, "AIR", headerStyle);
	        createMerged(sheet, h1, h2, 17, "Total", headerStyle);
	        createMerged(sheet, h1, h2, 18, "SBR", headerStyle);

	        BigDecimal tQty = BigDecimal.ZERO;
	        BigDecimal tFob = BigDecimal.ZERO;
	        BigDecimal tPmvPerQty = BigDecimal.ZERO;
	        BigDecimal tPmv = BigDecimal.ZERO;
	        BigDecimal tDbk = BigDecimal.ZERO;
	        BigDecimal tAir = BigDecimal.ZERO;
	        BigDecimal tSbr = BigDecimal.ZERO;
	        BigDecimal tFob15 = BigDecimal.ZERO;

	        int sr = 1;
	        for (ExportData e : data) {

	            Row r = sheet.createRow(rowIdx++);

	            r.createCell(0).setCellValue(sr++);

	            Cell invCell = r.createCell(1);
	            invCell.setCellValue(e.getInvoiceNo() + "\n" + fmt(e.getInvoiceDate()));
	            invCell.setCellStyle(wrapStyle);

	            Cell sbCell = r.createCell(2);
	            sbCell.setCellValue(e.getSbNo() + "\n" + fmt(e.getSbDate()));
	            sbCell.setCellStyle(wrapStyle);

	            r.createCell(3).setCellValue(fmt(e.getLeoDate()));
	            r.createCell(4).setCellValue(e.getModelDescription());
	            r.createCell(5).setCellValue(n(e.getQuantity()).doubleValue());
	            r.createCell(6).setCellValue(n(e.getFobInr()).doubleValue());
	            r.createCell(7).setCellValue(n(e.getPmvPerQty()).doubleValue());
	            r.createCell(8).setCellValue(n(e.getPmvActual()).doubleValue());
	            r.createCell(9).setCellValue(e.getHsCode());

	            BigDecimal fob15 = n(e.getFobInr()).multiply(BigDecimal.valueOf(0.015));

	            r.createCell(10).setCellValue(n(e.getQuantity()).doubleValue());
	            r.createCell(11).setCellValue(n(e.getFobInr()).doubleValue());
	            r.createCell(12).setCellValue(n(e.getPmvActual()).doubleValue());
	            r.createCell(13).setCellValue(0);
	            r.createCell(14).setCellValue(n(e.getTotalDbk()).doubleValue());
	            r.createCell(15).setCellValue(fob15.doubleValue());
	            r.createCell(16).setCellValue(n(e.getAirAmount()).doubleValue());
	            r.createCell(17).setCellValue(n(e.getTotalDbk()).doubleValue());
	            r.createCell(18).setCellValue(n(e.getSbr()).doubleValue());

	            tQty = tQty.add(n(e.getQuantity()));
	            tFob = tFob.add(n(e.getFobInr()));
	            tPmvPerQty = tPmvPerQty.add(n(e.getPmvPerQty()));
	            tPmv = tPmv.add(n(e.getPmvActual()));
	            tDbk = tDbk.add(n(e.getTotalDbk()));
	            tAir = tAir.add(n(e.getAirAmount()));
	            tSbr = tSbr.add(n(e.getSbr()));
	            tFob15 = tFob15.add(fob15);
	        }

	        Row total = sheet.createRow(rowIdx);
	        total.createCell(4).setCellValue("TOTAL");
	        total.getCell(4).setCellStyle(totalStyle);

	        total.createCell(5).setCellValue(tQty.doubleValue());
	        total.createCell(6).setCellValue(tFob.doubleValue());
	        total.createCell(7).setCellValue(tPmvPerQty.doubleValue());
	        total.createCell(8).setCellValue(tPmv.doubleValue());
	        total.createCell(14).setCellValue(tDbk.doubleValue());
	        total.createCell(15).setCellValue(tFob15.doubleValue());
	        total.createCell(16).setCellValue(tAir.doubleValue());
	        total.createCell(17).setCellValue(tDbk.doubleValue());
	        total.createCell(18).setCellValue(tSbr.doubleValue());

	        for (int i = 0; i <= 18; i++) sheet.autoSizeColumn(i);
	    }

	    /* ============================================================
	                             DBK SHEETS
	       ============================================================ */

	    private void writeDbkSheets(
	            Workbook wb,
	            LocalDate anchorSbDate,
	            Set<String> claimRefNos) {

	        LocalDate end = anchorSbDate.minusDays(1);
	        int dbkIndex = 1;

	        while (true) {
	            LocalDate start = end.minusMonths(3).plusDays(1);

	            List<ImportData> imports =
	                    importRepo.findByBeDateBetweenAndClaimRefNoInOrderByBeDateAsc(
	                            start, end, claimRefNos);

	            if (imports.isEmpty()) break;

	            writeSingleDbkSheet(wb, imports, dbkIndex++);
	            end = start.minusDays(1);
	        }
	    }

	    private void writeSingleDbkSheet(
	            Workbook wb,
	            List<ImportData> data,
	            int idx) {

	        Sheet sheet = wb.createSheet("DBK " + idx);
	        CellStyle header = headerStyle(wb);

	        int r = 0;
	        Row h1 = sheet.createRow(r++);
	        Row h2 = sheet.createRow(r++);

	        String[] baseHeaders = {
	                "Sr. No", "Description", "Part No", "Sr. no in DBK – I",
	                "B/E No.", "B/E Date", "Name of the Customs", "Unit",
	                "Qty Imp", "Assessable Value", "Rate of Duty",
	                "C'try from which Imp", "Is assessment final", "ITCHS No"
	        };

	        for (int i = 0; i < baseHeaders.length; i++) {
	            h1.createCell(i).setCellValue(baseHeaders[i]);
	            h1.getCell(i).setCellStyle(header);
	            sheet.addMergedRegion(new CellRangeAddress(0, 1, i, i));
	        }

	        merge(sheet, h1, 14, 20, "Basic Duty + Addl. Customs duty", header);

	        String[] sub = {
	                "Basic", "CVD", "Edu. Cess on CVD",
	                "Edu. Cess on Basic / SW Surcharge",
	                "Antidm DUTY", "Addl. Duty / IGST", "Total"
	        };

	        for (int i = 0; i < sub.length; i++) {
	            h2.createCell(14 + i).setCellValue(sub[i]);
	            h2.getCell(14 + i).setCellStyle(header);
	        }

	        h1.createCell(21).setCellValue("Remarks");
	        h1.getCell(21).setCellStyle(header);
	        sheet.addMergedRegion(new CellRangeAddress(0, 1, 21, 21));

	        int sr = 1;
	        for (ImportData d : data) {

	            BigDecimal basic = n(d.getBcd());

	            BigDecimal swsAmt = basic
	                    .multiply(n(d.getSwsRate()))
	                    .divide(BigDecimal.valueOf(100));

	            BigDecimal igstAmt =
	                    n(d.getAssessableValue())
	                            .add(basic)
	                            .add(swsAmt)
	                            .add(n(d.getAddDuty()))
	                            .multiply(n(d.getIgstRate()))
	                            .divide(BigDecimal.valueOf(100));

	            BigDecimal total =
	                    basic.add(swsAmt)
	                            .add(n(d.getAddDuty()))
	                            .add(igstAmt);

	            Row row = sheet.createRow(r++);

	            row.createCell(0).setCellValue(sr++);
	            row.createCell(1).setCellValue(d.getItemDescription());
	            row.createCell(2).setCellValue(
	                    d.getMaterial() != null ? d.getMaterial().getBomPartNo() : "");
	            row.createCell(3).setCellValue(0);
	            row.createCell(4).setCellValue(d.getBeNo());
	            row.createCell(5).setCellValue(fmt(d.getBeDate()));
	            row.createCell(6).setCellValue(d.getPortCode());
	            row.createCell(7).setCellValue(d.getUom());
	            row.createCell(8).setCellValue(n(d.getQuantity()).doubleValue());
	            row.createCell(9).setCellValue(n(d.getAssessableValue()).doubleValue());
	            row.createCell(10).setCellValue(rateStr(d));
	            row.createCell(11).setCellValue(d.getCountryOfOrigin());
	            row.createCell(12).setCellValue("YES");
	            row.createCell(13).setCellValue(d.getItchsCode());

	            row.createCell(14).setCellValue(basic.doubleValue());
	            row.createCell(15).setCellValue(0);
	            row.createCell(16).setCellValue(0);
	            row.createCell(17).setCellValue(swsAmt.doubleValue());
	            row.createCell(18).setCellValue(n(d.getAddDuty()).doubleValue());
	            row.createCell(19).setCellValue(igstAmt.doubleValue());
	            row.createCell(20).setCellValue(total.doubleValue());
	            row.createCell(21).setCellValue("NIL");
	        }

	        for (int i = 0; i <= 21; i++) sheet.autoSizeColumn(i);
	    }

	    /* ============================================================
	                               HELPERS
	       ============================================================ */

	    private CellStyle wrapStyle(Workbook wb) {
	        CellStyle cs = wb.createCellStyle();
	        cs.setWrapText(true);
	        cs.setVerticalAlignment(VerticalAlignment.CENTER);
	        return cs;
	    }

	    private CellStyle headerStyle(Workbook wb) {
	        CellStyle cs = wb.createCellStyle();
	        cs.setAlignment(HorizontalAlignment.CENTER);
	        cs.setVerticalAlignment(VerticalAlignment.CENTER);
	        cs.setBorderBottom(BorderStyle.THIN);
	        cs.setBorderTop(BorderStyle.THIN);
	        cs.setBorderLeft(BorderStyle.THIN);
	        cs.setBorderRight(BorderStyle.THIN);
	        Font f = wb.createFont();
	        f.setBold(true);
	        cs.setFont(f);
	        return cs;
	    }

	    private CellStyle totalStyle(Workbook wb) {
	        CellStyle cs = wb.createCellStyle();
	        Font f = wb.createFont();
	        f.setBold(true);
	        cs.setFont(f);
	        return cs;
	    }

	    private void createMerged(
	            Sheet s, Row r1, Row r2, int col, String val, CellStyle st) {
	        r1.createCell(col).setCellValue(val);
	        r1.getCell(col).setCellStyle(st);
	        s.addMergedRegion(new CellRangeAddress(
	                r1.getRowNum(), r2.getRowNum(), col, col));
	    }

	    private void merge(
	            Sheet s, Row r, int c1, int c2, String val, CellStyle st) {
	        r.createCell(c1).setCellValue(val);
	        r.getCell(c1).setCellStyle(st);
	        s.addMergedRegion(new CellRangeAddress(
	                r.getRowNum(), r.getRowNum(), c1, c2));
	    }

	    private BigDecimal n(BigDecimal v) {
	        return v == null ? BigDecimal.ZERO : v;
	    }

	    private String fmt(LocalDate d) {
	        return d == null ? "" :
	                d.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
	    }

	    private String rateStr(ImportData d) {
	        return Stream.of(d.getBcdRate(), d.getSwsRate(), d.getIgstRate())
	                .filter(Objects::nonNull)
	                .map(BigDecimal::stripTrailingZeros)
	                .map(BigDecimal::toPlainString)
	                .collect(Collectors.joining("+"));
	    }
}
