package com.orpe.consultants.service.impl;

import com.orpe.consultants.dto.ImportDataDTO;
import com.orpe.consultants.dto.ImportDataFilter;
import com.orpe.consultants.model.BomData;
import com.orpe.consultants.model.BomExportModelQuantity;
import com.orpe.consultants.model.ImportData;
import com.orpe.consultants.model.Material;
import com.orpe.consultants.repository.BomDataRepository;
import com.orpe.consultants.repository.ExportDataRepository;
import com.orpe.consultants.repository.ImportDataRepository;
import com.orpe.consultants.repository.MaterialRepository;
import com.orpe.consultants.service.ImportDataService;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportDataServiceImpl implements ImportDataService {

	private final MaterialRepository materialRepo;
	private final ImportDataRepository importRepo;
	private final ExportDataRepository exportDataRepository;
	private final BomDataRepository bomDataRepository;
	private final ModelMapper modelMapper;

	@Override
	public int saveBulk(List<ImportDataDTO> rows) {
		int saved = 0;
		Map<String, Material> cache = new HashMap<>();

		for (ImportDataDTO dto : rows) {
			Material mat = null;
			if (dto.getBomPartNo() != null && !dto.getBomPartNo().isBlank()) {
				String key = dto.getBomPartNo().trim();
				mat = cache.get(key);
				if (mat == null) {
					mat = materialRepo.findById(key)
							.orElseGet(() -> materialRepo.save(Material.builder().bomPartNo(key).build()));
					cache.put(key, mat);
				}
			}

			ImportData entity = dtoToEntity(dto);
			entity.setMaterial(mat);

			importRepo.save(entity);
			saved++;
		}
		return saved;
	}

	@Override
	public ImportDataDTO save(ImportDataDTO dto) {
		validate(dto);

		Material mat = null;
		if (dto.getBomPartNo() != null && !dto.getBomPartNo().isBlank()) {
			String key = dto.getBomPartNo().trim();
			mat = materialRepo.findById(key)
					.orElseGet(() -> materialRepo.save(Material.builder().bomPartNo(key).build()));
		}
		ImportData entity = dtoToEntity(dto);
		entity.setMaterial(mat);

		ImportData saved = importRepo.save(entity);
		return entityToDto(saved);
	}

	@Override
	public Optional<ImportDataDTO> findById(Long importId) {
		return importRepo.findById(importId).map(this::entityToDto);
	}

	@Override
	public void deleteById(Long importId) {
		importRepo.deleteById(importId);
	}

	@Override
	public List<ImportDataDTO> findAll() {
		return importRepo.findAll().stream().map(this::entityToDto).collect(Collectors.toList());
	}

	@Override
	public Page<ImportDataDTO> search(ImportDataFilter filter, Pageable pageable) {
		Specification<ImportData> spec = buildSpecification(filter);
		Page<ImportData> page = importRepo.findAll(spec, pageable);
		return page.map(this::entityToDto);
	}

//  @Override
//  public Page<ImportDataDTO> findWithPositiveClosingBalance(ImportDataFilter filter, Pageable pageable) {
//
//      // 1) Existing dynamic filters
//      Specification<ImportData> baseSpec = buildSpecification(filter);
//
//      // 2) closingBalance > 0
//      Specification<ImportData> positiveClosingSpec = (root, query, cb) ->
//              cb.greaterThan(root.get("closingBalance"), BigDecimal.ZERO);
//
//      // 3) FIFO: only the oldest record per (material.bomPartNo, altBoePartNo)
//              Specification<ImportData> fifoSpec = (root, query, cb) -> {
//
//            	    // Join material for BOM part no
//            	    Join<ImportData, Material> rootMaterial = root.join("material", JoinType.LEFT);
//
//            	    // Subquery for finding older records
//            	    Subquery<Long> sub = query.subquery(Long.class);
//            	    Root<ImportData> subRoot = sub.from(ImportData.class);
//            	    Join<ImportData, Material> subMaterial = subRoot.join("material", JoinType.LEFT);
//
//            	    sub.select(cb.literal(1L))
//            	       .where(
//            	           // SAME BOM PART NO (JOIN!)
//            	           cb.equal(subMaterial.get("bomPartNo"), rootMaterial.get("bomPartNo")),
//
//            	           // SAME ALT BOE (or altBoePartNo)
//            	           cb.equal(subRoot.get("altBoePartNo"), root.get("altBoePartNo")),
//
//            	           // OLDER BE DATE
//            	           cb.lessThan(subRoot.get("beDate"), root.get("beDate")),
//
//            	           // STILL positive closing balance
//            	           cb.greaterThan(subRoot.get("closingBalance"), BigDecimal.ZERO)
//            	       );
//
//            	    // FIFO: keep only rows where no older record exists
//            	    return cb.not(cb.exists(sub));
//            	};
//
//
//      // 4) Combine all specs using non-deprecated API
//      Specification<ImportData> finalSpec = Specification.allOf(
//              baseSpec,
//              positiveClosingSpec,
//              fifoSpec
//      );
//
//      // 5) Force sort by beDate ASC (oldest first)
//      Sort sort = Sort.by(Sort.Direction.ASC, "beDate")
//                      .and(pageable.getSort());
//
//      Pageable sortedPageable = PageRequest.of(
//              pageable.getPageNumber(),
//              pageable.getPageSize(),
//              sort
//      );
//
//      // 6) Run query
//      Page<ImportData> page = importRepo.findAll(finalSpec, sortedPageable);
//
//      return page.map(this::entityToDto);
//  }

	@Override
	public Page<ImportDataDTO> findWithPositiveClosingBalance(ImportDataFilter filter, Pageable pageable) {
	    // 1) Normal filters (beNo, client, etc.)
	    Specification<ImportData> baseSpec = buildSpecification(filter);

	    // 2) closingBalance > 0
	    Specification<ImportData> positiveClosingSpec = (root, query, cb) ->
	            cb.greaterThan(root.get("closingBalance"), BigDecimal.ZERO);

	    // 3) Combine
	    Specification<ImportData> finalSpec = Specification.allOf(baseSpec, positiveClosingSpec);

	    // 4) FIFO sorting:
	    //    - beDate ASC         (older first)
	    //    - closingBalance ASC (for same day, smaller lot first)
	    //    - importId ASC       (stable tie-breaker)
	    Sort sort = Sort.by(
	            Sort.Order.asc("beDate"),
	            Sort.Order.asc("closingBalance"),
	            Sort.Order.asc("importId")
	    );

	    Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

	    // 5) Fetch (page-level FIFO – good enough for your screen)
	    Page<ImportData> page = importRepo.findAll(finalSpec, sortedPageable);

	    // 6) BOM requirements (how much quantity needed per BOM part)
	    Map<String, BigDecimal> requiredByBomPartNo = computeRequiredBomQuantities();

	    // 7) FIFO against BOM requirements
	    List<ImportData> fifoSelected = applyFifoSelection(page.getContent(), requiredByBomPartNo);

	    // 8) NEW RULE: beDate must NOT exceed last export invoiceDate for that claim
	    Map<String, LocalDate> lastInvoiceByClaim = computeLastInvoiceDatePerClaim();
	    List<ImportData> finalFiltered = filterByLastInvoiceDate(fifoSelected, lastInvoiceByClaim);

	    // 9) Map to DTOs
	    List<ImportDataDTO> dtoList = finalFiltered.stream()
	            .map(this::entityToDto)
	            .toList();

	    // totalElements is a bit subjective now; using finalFiltered.size() to reflect
	    return new PageImpl<>(dtoList, sortedPageable, finalFiltered.size());
	}


	/**
	 * Build a map of required quantity per BOM part number based on BomData +
	 * BomExportModelQuantity with status = 'active'.
	 *
	 * Key: material.bomPartNo Value: SUM of quantity for all ACTIVE export models
	 * for that material.
	 */
	private Map<String, BigDecimal> computeRequiredBomQuantities() {
	    List<BomData> bomRows = bomDataRepository.findAll();
	    Map<String, BigDecimal> requiredMap = new HashMap<>();

	    for (BomData bom : bomRows) {
	        if (bom.getMaterial() == null || bom.getMaterial().getBomPartNo() == null) {
	            continue;
	        }
	        String bomPartNo = bom.getMaterial().getBomPartNo();

	        BigDecimal totalForThisBomRow = BigDecimal.ZERO;
	        boolean hasActiveExportModels = false;

	        // 1) If export models exist, prefer ACTIVE ones
	        if (bom.getExportModels() != null && !bom.getExportModels().isEmpty()) {
	            for (BomExportModelQuantity em : bom.getExportModels()) {
	                if (em == null) continue;
	                if (em.getStatus() == null || !em.getStatus().equalsIgnoreCase("ACTIVE")) continue;
	                if (em.getQuantity() == null) continue;

	                totalForThisBomRow = totalForThisBomRow.add(em.getQuantity());
	                hasActiveExportModels = true;
	            }
	        }

	        // 2) If no active export models, fall back to grandTotal
	        if (!hasActiveExportModels && bom.getGrandTotal() != null) {
	            totalForThisBomRow = totalForThisBomRow.add(bom.getGrandTotal());
	        }

	        if (totalForThisBomRow.compareTo(BigDecimal.ZERO) > 0) {
	            requiredMap.merge(bomPartNo, totalForThisBomRow, BigDecimal::add);
	        }
	    }

	    return requiredMap;
	}


	/**
	 * FIFO selection: - Inputs: all import rows for the claim, sorted by beDate
	 * ASC, closingBalance > 0 - For each material (bomPartNo), keep taking rows
	 * until cumulative closingBalance >= required BOM quantity for that material. -
	 * Rows beyond the required quantity are dropped.
	 */
	private List<ImportData> applyFifoSelection(
	        List<ImportData> allImports,
	        Map<String, BigDecimal> requiredByBomPartNo
	) {
	    // If absolutely no BOM requirement, keep all imports
	    if (requiredByBomPartNo == null || requiredByBomPartNo.isEmpty()) {
	        return allImports;
	    }

	    Map<String, BigDecimal> usedSoFar = new HashMap<>();
	    List<ImportData> result = new ArrayList<>();

	    for (ImportData imp : allImports) {
	        if (imp.getMaterial() == null || imp.getMaterial().getBomPartNo() == null) {
	            continue; // cannot map to BOM
	        }

	        String bomPartNo = imp.getMaterial().getBomPartNo();

	        BigDecimal required = requiredByBomPartNo.get(bomPartNo);
	        if (required == null) {
	            // this material not required by BOM → skip it
	            continue;
	        }

	        BigDecimal alreadyUsed = usedSoFar.getOrDefault(bomPartNo, BigDecimal.ZERO);

	        // Requirement for this material already satisfied → skip newer rows
	        if (alreadyUsed.compareTo(required) >= 0) {
	            continue;
	        }

	        // Include this import row
	        result.add(imp);

	        BigDecimal closing = imp.getClosingBalance() != null
	                ? imp.getClosingBalance()
	                : BigDecimal.ZERO;

	        usedSoFar.put(bomPartNo, alreadyUsed.add(closing));
	    }

	    return result;
	}
	
	// Key format: claimRefNo + '|' + claimYear
	private String buildClaimKey(String claimRefNo, String claimYear) {
	    return (claimRefNo == null ? "" : claimRefNo.trim()) + "|" +
	           (claimYear == null ? "" : claimYear.trim());
	}

	/**
	 * Build a map:
	 *   key   = claimRefNo + '|' + claimYear
	 *   value = MAX(invoiceDate) for that claim
	 */
	private Map<String, LocalDate> computeLastInvoiceDatePerClaim() {
	    Map<String, LocalDate> result = new HashMap<>();

	    // Custom query in ExportDataRepository
	    List<Object[]> rows = exportDataRepository.findMaxInvoiceDatesByClaim();

	    for (Object[] row : rows) {
	        String claimRefNo = (String) row[0];
	        String claimYear  = (String) row[1];
	        LocalDate maxDate = (LocalDate) row[2];

	        if (maxDate != null) {
	            String key = buildClaimKey(claimRefNo, claimYear);
	            result.put(key, maxDate);
	        }
	    }

	    return result;
	}
	
	/**
	 * Business rule:
	 * For each ImportData row:
	 *   - Look up the last export invoiceDate for its (claimRefNo, claimYear)
	 *   - If none exists → keep the row (we allow imports when no export yet)
	 *   - If beDate is AFTER that last invoiceDate → DROP the row
	 *   - Else keep it.
	 */
	private List<ImportData> filterByLastInvoiceDate(
	        List<ImportData> imports,
	        Map<String, LocalDate> lastInvoiceByClaim
	) {
	    if (imports == null || imports.isEmpty()) {
	        return imports;
	    }
	    if (lastInvoiceByClaim == null || lastInvoiceByClaim.isEmpty()) {
	        // no export data: keep everything
	        return imports;
	    }

	    List<ImportData> result = new ArrayList<>();

	    for (ImportData imp : imports) {
	        String key = buildClaimKey(imp.getClaimRefNo(), imp.getClaimYear());
	        LocalDate maxInvoiceDate = lastInvoiceByClaim.get(key);

	        // If no export exists for this claim → allow
	        if (maxInvoiceDate == null) {
	            result.add(imp);
	            continue;
	        }

	        LocalDate beDate = imp.getBeDate();

	        // If beDate is null or NOT after maxInvoiceDate → allow
	        if (beDate == null || !beDate.isAfter(maxInvoiceDate)) {
	            result.add(imp);
	        }
	        // else: drop (beDate > last invoice date)
	    }

	    return result;
	}




	@Override
	public byte[] exportData(ImportDataFilter filter) {
		// TODO: implement export logic (CSV or Excel)
		List<ImportData> list = importRepo.findAll(buildSpecification(filter));
		return new byte[0];
	}

	@Override
	public boolean validate(ImportDataDTO dto) {
		if (!StringUtils.hasText(dto.getBeNo())) {
			throw new IllegalArgumentException("BE No is required");
		}
		if (dto.getBeDate() == null) {
			throw new IllegalArgumentException("BE Date is required");
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

	@Override
	public long count(ImportDataFilter filter) {
		return importRepo.count(buildSpecification(filter));
	}

	private ImportData dtoToEntity(ImportDataDTO dto) {
		ImportData entity = modelMapper.map(dto, ImportData.class);
		entity.setBeMonth(trim(dto.getBeMonth()));
		return entity;
	}

	private ImportDataDTO entityToDto(ImportData entity) {
		ImportDataDTO dto = modelMapper.map(entity, ImportDataDTO.class);
		if (entity.getMaterial() != null) {
			dto.setBomPartNo(entity.getMaterial().getBomPartNo());
		}
		return dto;
	}

	private Specification<ImportData> buildSpecification(ImportDataFilter filter) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			String field = filter.getFilterField();
			String value = filter.getFilterValue();

			if (field != null && !field.isBlank() && value != null && !value.isBlank()) {
				List<String> stringFields = List.of("beNo", "claimYear", "clientName", "supplierNameAddress",
						"countryOfOrigin",
						// ⚠️ REMOVE "bomPartNo" from here
						"dbkPartNo", "itchsCode", "portCode", "claimRefNo", "altBoePartNo");

				if ("bomPartNo".equals(field)) {
					// join with material and filter on its bomPartNo
					Join<ImportData, Material> materialJoin = root.join("material", JoinType.LEFT);
					predicates.add(cb.like(cb.lower(materialJoin.get("bomPartNo")), "%" + value.toLowerCase() + "%"));
				} else if (stringFields.contains(field)) {
					predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
				} else if ("stockWiseEligibility".equals(field)) {
					predicates.add(cb.equal(root.get(field), value));
				}
			}

			// Date range on beDate
			if (filter.getFromDate() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("beDate"), filter.getFromDate()));
			}
			if (filter.getToDate() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("beDate"), filter.getToDate()));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private static String trim(String s) {
		return s == null ? null : s.trim();
	}

	private static String req(String s) {
		if (s == null || s.trim().isEmpty())
			throw new IllegalArgumentException("Required field missing");
		return s.trim();
	}

	private static BigDecimal reqBig(BigDecimal b) {
		if (b == null)
			throw new IllegalArgumentException("Required numeric field missing");
		return b;
	}

	private static BigDecimal nz(BigDecimal b) {
		return b == null ? BigDecimal.ZERO : b;
	}

	private static LocalDate reqDate(LocalDate d) {
		if (d == null)
			throw new IllegalArgumentException("beDate required");
		return d;
	}

	@Override
	@Transactional
	public List<ImportDataDTO> fetchImportDataWithExportModels(List<Long> importIds) {
		List<ImportData> imports = importRepo.findAllById(importIds);
		Set<String> partNos = imports.stream()
				.map(i -> (i.getMaterial() != null) ? i.getMaterial().getBomPartNo() : null).filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<BomData> bomList = bomDataRepository.findAllByMaterial_BomPartNoIn(partNos);

		// Group BomData by bomPartNo, then flatten aggregated export model lists per
		// bomPartNo
		Map<String, List<BomExportModelQuantity>> exportModelsByPartNo = bomList.stream()
				.collect(Collectors.groupingBy(bom -> bom.getMaterial().getBomPartNo(),
						Collectors.mapping(BomData::getExportModels, Collectors.toList())))
				.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().flatMap(List::stream)
						// Filter export models only with status "OPEN"
						.filter(expModel -> "OPEN".equalsIgnoreCase(expModel.getStatus()))
						.collect(Collectors.toList())));

		List<ImportDataDTO> details = new ArrayList<>();
		for (ImportData imp : imports) {
			String partNo = (imp.getMaterial() != null) ? imp.getMaterial().getBomPartNo() : null;
			List<BomExportModelQuantity> exModels = partNo != null
					? exportModelsByPartNo.getOrDefault(partNo, Collections.emptyList())
					: Collections.emptyList();

			ImportDataDTO dto = new ImportDataDTO();
			dto.setImportData(imp);
			dto.setExportModels(exModels);
			details.add(dto);
		}
		return details;
	}

//******Method To Export The Data In Excel File Before Worksheet Calculation
	@Override
	@Transactional
	public void exportImportDataToExcel(List<Long> importIds, HttpServletResponse response) throws IOException {

	    if (importIds == null || importIds.isEmpty()) {
	        throw new IllegalArgumentException("importIds must not be null or empty");
	    }

	    List<ImportData> rows = importRepo.findAllById(importIds);

	    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
	        Sheet sheet = workbook.createSheet("Import Data");

	        int rowIdx = 0;
	        int col;

	        // ===== HEADER ROW =====
	        Row header = sheet.createRow(rowIdx++);
	        col = 0;

	        header.createCell(col++).setCellValue("BE No");
	        header.createCell(col++).setCellValue("BE Date");
	        header.createCell(col++).setCellValue("Month");
	        header.createCell(col++).setCellValue("Year");
	        // Client Name REMOVED
	        header.createCell(col++).setCellValue("Claim Ref No");
	        header.createCell(col++).setCellValue("Claim Year");
	        header.createCell(col++).setCellValue("Port Code");
	        header.createCell(col++).setCellValue("Country of Origin");
	        header.createCell(col++).setCellValue("Supplier");
	        header.createCell(col++).setCellValue("ITCHS");
	        header.createCell(col++).setCellValue("Item Description");
	        header.createCell(col++).setCellValue("BOM Part");
	        header.createCell(col++).setCellValue("Alt BOE Part");
	        header.createCell(col++).setCellValue("DBK Part");
	        header.createCell(col++).setCellValue("Qty");
	        header.createCell(col++).setCellValue("UOM");
	        header.createCell(col++).setCellValue("Assessable");
	        header.createCell(col++).setCellValue("BCD Rate (%)");
	        header.createCell(col++).setCellValue("BCD");
	        header.createCell(col++).setCellValue("SWS Rate (%)");
	        header.createCell(col++).setCellValue("SWS");
	        header.createCell(col++).setCellValue("Add Rate (%)");
	        header.createCell(col++).setCellValue("Add Duty");
	        header.createCell(col++).setCellValue("IGST Rate (%)");
	        header.createCell(col++).setCellValue("IGST");
	        header.createCell(col++).setCellValue("Total Duty");
	        header.createCell(col++).setCellValue("Notification No");
	        header.createCell(col++).setCellValue("Eligibility");
	        header.createCell(col++).setCellValue("Opening");
	        header.createCell(col++).setCellValue("Used");
	        header.createCell(col++).setCellValue("Closing");
	        
	        header.createCell(col++).setCellValue("Duty Claimed");
	        // Import ID REMOVED

	        // ===== DATA ROWS =====
	        for (ImportData d : rows) {
	            Row excelRow = sheet.createRow(rowIdx++);
	            col = 0;

	            excelRow.createCell(col++).setCellValue(ns(d.getBeNo()));
	            excelRow.createCell(col++).setCellValue(d.getBeDate() != null ? d.getBeDate().toString() : "");
	            excelRow.createCell(col++).setCellValue(ns(d.getBeMonth()));
	            excelRow.createCell(col++).setCellValue((d.getBeYear()));
	            excelRow.createCell(col++).setCellValue(ns(d.getClaimRefNo()));
	            excelRow.createCell(col++).setCellValue(ns(d.getClaimYear()));
	            excelRow.createCell(col++).setCellValue(ns(d.getPortCode()));
	            excelRow.createCell(col++).setCellValue(ns(d.getCountryOfOrigin()));
	            excelRow.createCell(col++).setCellValue(ns(d.getSupplierNameAddress()));
	            excelRow.createCell(col++).setCellValue(ns(d.getItchsCode()));
	            excelRow.createCell(col++).setCellValue(ns(d.getItemDescription()));

	            excelRow.createCell(col++)
	                .setCellValue(d.getMaterial() != null ? ns(d.getMaterial().getBomPartNo()) : "");

	            excelRow.createCell(col++).setCellValue(ns(d.getAltBoePartNo()));
	            excelRow.createCell(col++).setCellValue(ns(d.getDbkPartNo()));
	            excelRow.createCell(col++)
	                .setCellValue(d.getQuantity() != null ? d.getQuantity().doubleValue() : 0d);

	            excelRow.createCell(col++).setCellValue(ns(d.getUom()));
	            excelRow.createCell(col++)
	                .setCellValue(d.getAssessableValue() != null ? d.getAssessableValue().doubleValue() : 0d);

	            excelRow.createCell(col++)
	                .setCellValue(d.getBcdRate() != null ? d.getBcdRate().doubleValue() : 0d);

	            excelRow.createCell(col++)
	                .setCellValue(d.getBcd() != null ? d.getBcd().doubleValue() : 0d);

	            excelRow.createCell(col++)
	                .setCellValue(d.getSwsRate() != null ? d.getSwsRate().doubleValue() : 0d);

	            excelRow.createCell(col++)
	                .setCellValue(d.getSws() != null ? d.getSws().doubleValue() : 0d);

	            excelRow.createCell(col++)
	                .setCellValue(d.getAddRate() != null ? d.getAddRate().doubleValue() : 0d);

	            excelRow.createCell(col++)
	                .setCellValue(d.getAddDuty() != null ? d.getAddDuty().doubleValue() : 0d);

	            excelRow.createCell(col++)
	                .setCellValue(d.getIgstRate() != null ? d.getIgstRate().doubleValue() : 0d);

	            excelRow.createCell(col++)
	                .setCellValue(d.getIgst() != null ? d.getIgst().doubleValue() : 0d);

	            excelRow.createCell(col++)
	                .setCellValue(d.getTotalDuty() != null ? d.getTotalDuty().doubleValue() : 0d);

	            excelRow.createCell(col++).setCellValue(ns(d.getNotnNo()));
	            excelRow.createCell(col++).setCellValue(ns(d.getNotnEligibility()));

	            excelRow.createCell(col++)
	                .setCellValue(d.getQtyOpeningBalance() != null ? d.getQtyOpeningBalance().doubleValue() : 0d);

	            excelRow.createCell(col++)
	                .setCellValue(d.getQtyUsed() != null ? d.getQtyUsed().doubleValue() : 0d);

	            excelRow.createCell(col++)
	                .setCellValue(d.getClosingBalance() != null ? d.getClosingBalance().doubleValue() : 0d);

	            
	            excelRow.createCell(col++)
	                .setCellValue(d.getDutyClaimedAmt() != null ? d.getDutyClaimedAmt().doubleValue() : 0d);

	            // Import ID intentionally NOT exported
	        }

	        // Auto-size columns
	        for (int i = 0; i < col; i++) {
	            sheet.autoSizeColumn(i);
	        }

	        String filename = "PreWorksheet-boeRecords-" + java.time.LocalDate.now() + ".xlsx";
	        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

	        workbook.write(response.getOutputStream());
	    }
	}


	private String ns(String s) {
		return (s == null) ? "" : s;
	}

}
