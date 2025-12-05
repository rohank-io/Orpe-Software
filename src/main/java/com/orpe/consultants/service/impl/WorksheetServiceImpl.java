package com.orpe.consultants.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.orpe.consultants.dto.BomDataDTO;
import com.orpe.consultants.dto.BomDataFilter;
import com.orpe.consultants.dto.ImportDataDTO;
import com.orpe.consultants.dto.ImportDataFilter;
import com.orpe.consultants.dto.StockWiseEligibility;
import com.orpe.consultants.dto.WorksheetDTO;
import com.orpe.consultants.dto.WorksheetDataFilter;
import com.orpe.consultants.dto.WorksheetExportModelsDTO;
import com.orpe.consultants.model.BomData;
import com.orpe.consultants.model.BomExportModelQuantity;
import com.orpe.consultants.model.DraftWorksheet;
import com.orpe.consultants.model.ExportData;
//import com.orpe.consultants.model.BomData;
import com.orpe.consultants.model.ImportData;
import com.orpe.consultants.model.Material;
import com.orpe.consultants.model.Models;
//import com.orpe.consultants.model.Models;
import com.orpe.consultants.model.Worksheet;
import com.orpe.consultants.model.WorksheetExportModels;
import com.orpe.consultants.repository.BomDataRepository;
import com.orpe.consultants.repository.BomExportModelReposioty;
import com.orpe.consultants.repository.DraftWorksheetRepository;
import com.orpe.consultants.repository.ExportDataRepository;
import com.orpe.consultants.repository.ImportDataRepository;
import com.orpe.consultants.repository.MaterialRepository;
import com.orpe.consultants.repository.ModelsRepository;
import com.orpe.consultants.repository.WorksheetExportModelsRepository;
import com.orpe.consultants.repository.WorksheetRepository;
import com.orpe.consultants.service.WorksheetService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WorksheetServiceImpl implements WorksheetService {
	
	@Autowired
	private Validator validator;
	
	private final WorksheetRepository worksheetRepository;
	private final ModelsRepository modelsRepository;
	private final MaterialRepository materialRepository;
	private final WorksheetExportModelsRepository exportModelsRepository;
	private final BomDataRepository bomDataRepository;
	private final BomExportModelReposioty bomExportModelRepository;
    private final ModelMapper modelMapper;
    private final ImportDataRepository importDataRepository;
    private final ExportDataRepository exportDataRepository;
    private final DraftWorksheetRepository draftWorksheetRepository;

    
    
    @Transactional
    public void saveBulkWorksheets(List<WorksheetDTO> worksheetDTOList) {
        for (WorksheetDTO worksheetDTO : worksheetDTOList) {

            // Feature: Validate DTO before processing to ensure data integrity.
            Set<ConstraintViolation<WorksheetDTO>> violations = validator.validate(worksheetDTO);
            if (!violations.isEmpty()) {
                violations.forEach(v -> 
                    System.err.println("DTO validation failed on " + v.getPropertyPath() + ": " + v.getMessage()));
                throw new ConstraintViolationException(violations);
            }

            Worksheet worksheet = toEntity(worksheetDTO);

            // Feature: Fetch and associate ImportData entity; update opening, used and closing balances.
            if (worksheetDTO.getImportId() != null) {
                ImportData importData = importDataRepository.findById(worksheetDTO.getImportId())
                    .orElseThrow(() -> new RuntimeException("ImportData not found with id " + worksheetDTO.getImportId()));
                worksheet.setImportData(importData);

                if (worksheet.getOpeningBalanceQtyDef() != null) {
                    importData.setQtyOpeningBalance(worksheet.getOpeningBalanceQtyDef());
                }
                if (worksheet.getQtyUsedDef() != null) {
                    importData.setQtyUsed(worksheet.getQtyUsedDef());
                }
                if (worksheet.getClosingBalanceDef() != null) {
                    importData.setClosingBalance(worksheet.getClosingBalanceDef());

                    // Feature: Automatically close StockWise Eligibility if closing balance is zero.
                    if (BigDecimal.ZERO.compareTo(worksheet.getClosingBalanceDef()) == 0) {
                        importData.setStockWiseEligibility(StockWiseEligibility.CLOSED);
                    }
                }
                importDataRepository.save(importData);
            } else {
                throw new RuntimeException("ImportDataId must not be null");
            }

            // Feature: Fetch and associate Material entity by BomPartNo if present.
            if (worksheetDTO.getBomPartNo() != null) {
                Material material = materialRepository.findById(worksheetDTO.getBomPartNo())
                    .orElseThrow(() -> new RuntimeException("Material not found with BomPartNO: " + worksheetDTO.getBomPartNo()));
                worksheet.setMaterial(material);
            }

            // Feature: Save worksheet entity to generate ID for export models association.
            Worksheet savedWorksheet = worksheetRepository.saveAndFlush(worksheet);

            // Feature: Process each export model DTO to persist and link to saved worksheet.
            if (worksheetDTO.getExportModels() != null) {
                System.out.println("Saving " + worksheetDTO.getExportModels().size() + " export models for worksheet " + worksheetDTO.getBeNo());

                for (WorksheetExportModelsDTO exportModelDTO : worksheetDTO.getExportModels()) {
                    try {
                        final Long bomIdToUse = (exportModelDTO.getBomExportModelId() != null && exportModelDTO.getBomExportModelId() == 0)
                                    ? 1L
                                    : exportModelDTO.getBomExportModelId();

                        // Feature: Create new export model entity and set properties from DTO.
                        WorksheetExportModels exportModelEntity = new WorksheetExportModels();
                        exportModelEntity.setBomExportModelData(bomExportModelRepository.findById(bomIdToUse)
                            .orElseThrow(() -> new RuntimeException("BomData not found")));
                        exportModelEntity.setModel(modelsRepository.findById(exportModelDTO.getModelNo())
                            .orElseThrow(() -> new RuntimeException("Model not found")));
                        exportModelEntity.setWorksheet(savedWorksheet);

                        // Feature: Set exported quantities and claimed duties as per DTO.
                        exportModelEntity.setEmUsedQty(exportModelDTO.getEmUsedQty());
                        exportModelEntity.setDutyClaimed(exportModelDTO.getDutyClaimed());
                        exportModelEntity.setCifClaimed(exportModelDTO.getCifClaimed());
                        
                        // Persist the export model entity.
                        exportModelsRepository.save(exportModelEntity);
                        System.out.println("Saved export model id: " + exportModelEntity.getWsModelId());

                        // Feature: Update corresponding BomExportModelQuantity status to "CLOSED" by bomIdToUse.
                        if (bomIdToUse != null) {
                            bomExportModelRepository.findById(bomIdToUse).ifPresent(bomExportModel -> {
                                bomExportModel.setStatus("CLOSED");
                                bomExportModelRepository.save(bomExportModel);
                                System.out.println("BomExportModelQuantity status set to CLOSED for ID: " + bomIdToUse);
                            });
                        }
                        
                        
                    } catch (Exception e) {
                        System.err.println("Failed to save export model for worksheet " + worksheetDTO.getBeNo() + ": " + e.getMessage());
                        // handle error gracefully or rethrow as needed
                    }
                }

            } else {
                System.out.println("No export models to save for worksheet " + worksheetDTO.getBeNo());
            }
            
         // ✅ To delete drafts After saving worksheet and its export models successfully:
            if (worksheetDTO.getDraftWorksheetId() != null) {
                try {
                    draftWorksheetRepository.deleteById(worksheetDTO.getDraftWorksheetId());
                    System.out.println("✅ Deleted draft worksheet ID: " + worksheetDTO.getDraftWorksheetId());
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to delete draft worksheet ID: "
                        + worksheetDTO.getDraftWorksheetId() + " - " + e.getMessage());
                }
            }

        }
    }
    
    
    
    @Override
    public WorksheetDTO save(WorksheetDTO dto) {
        Worksheet entity = toEntity(dto);
        Worksheet saved = worksheetRepository.save(entity);
        return toDto(saved);
    }

    @Override
    public Optional<WorksheetDTO> findById(Long bomId) {
        return worksheetRepository.findById(bomId)
                .map(this::toDto);
    }

    @Override
    public void deleteById(Long bomId) {
        bomDataRepository.deleteById(bomId);
    }
    
    @Override
    public List<WorksheetDTO> findAll() {
        List<Worksheet> all = worksheetRepository.findAll();
        List<WorksheetDTO> dtos = new ArrayList<>();
        for (Worksheet bd : all) {
            dtos.add(toDto(bd));
        }
        return dtos;
    }

    @Override
    public Page<WorksheetDTO> search(WorksheetDataFilter filter, Pageable pageable) {
      Specification<Worksheet> spec = buildSpecification(filter);
      Page<Worksheet> page = worksheetRepository.findAll(spec, pageable);
      return page.map(this::toDto);
    }
    
    private Specification<Worksheet> buildSpecification(WorksheetDataFilter filter) {
	    return (root, query, cb) -> {
	        List<Predicate> predicates = new ArrayList<>();

	        String field = filter.getFilterField();
	        String value = filter.getFilterValue();

	        if (field != null && !field.isBlank() && value != null && !value.isBlank()) {
	            List<String> stringFields = List.of(
	                "beNo",
	                "claimYear",
	                "clientName",
	                "supplierNameAddress",
	                "countryOfOrigin",
	                "altBoePartNo",
	                "dbkPartNo",
	                "itchsCode",
	                "portCode",
	                "claimRefNo",
	                "username"
	            );

	            if ("bomPartNo".equals(field)) {
	                // JOIN ImportData → Material
	                Join<ImportData, Material> materialJoin = root.join("material", JoinType.LEFT);

	                predicates.add(
	                    cb.like(
	                        cb.lower(materialJoin.get("bomPartNo")),
	                        "%" + value.toLowerCase() + "%"
	                    )
	                );
	            }
	            else if (stringFields.contains(field)) {
	                predicates.add(
	                    cb.like(
	                        cb.lower(root.get(field)),
	                        "%" + value.toLowerCase() + "%"
	                    )
	                );
	            }
	            else if ("stockWiseEligibility".equals(field)) {
	                predicates.add(cb.equal(root.get(field), value));
	            }
	        }

	     // Apply date range filtering on beDate independently regardless of filterField
	        if (filter.getFromDate() != null) {
	            predicates.add(cb.greaterThanOrEqualTo(root.get("beDate"), filter.getFromDate()));
	        }
	        if (filter.getToDate() != null) {
	            predicates.add(cb.lessThanOrEqualTo(root.get("beDate"), filter.getToDate()));
	        }
	        

	        return cb.and(predicates.toArray(new Predicate[0]));
	    };
	}


    
    public WorksheetDTO toDto(Worksheet entity) {
        WorksheetDTO dto = modelMapper.map(entity, WorksheetDTO.class);
        // Manually map related entity IDs if ModelMapper does not populate correctly
        if (entity.getImportData() != null) {
            dto.setImportId(entity.getImportData().getImportId());
        }
        if (entity.getMaterial() != null) {
            dto.setBomPartNo(entity.getMaterial().getBomPartNo());
        }
        return dto;
    }

    public Worksheet toEntity(WorksheetDTO dto) {
        return modelMapper.map(dto, Worksheet.class);
        // You will need service/repository lookups to populate related entities (importData, material) from ids if saving.
    }



	@Override
	public byte[] exportData(WorksheetDataFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public boolean validate(WorksheetDTO dto) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public long count(WorksheetDataFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	
	@Override
	public List<Map<String, Object>> getAllWorksheetGroups() {
	    // ensure repository method exists: findAllDraftGroupsByUserClaimRefAndYear()
	    List<Object[]> rows = worksheetRepository.findAllWorksheetsGroupsByUserClaimRefAndYear();
	    List<Map<String, Object>> result = new ArrayList<>(rows.size());

	    for (Object[] r : rows) {
	        Map<String, Object> m = new HashMap<>(4);
	        // Expected layout:
	        // r[0] = username (String)
	        // r[1] = claimRefNo (String)
	        // r[2] = claimYear  (String)
	        // r[3] = count      (Number)

	        m.put("username", r[0] != null ? r[0].toString() : null);
	        m.put("claimRefNo", r[1] != null ? r[1].toString() : null);
	        m.put("claimYear", r[2] != null ? r[2].toString() : null);

	        Long countValue = 0L;
	        if (r.length > 3 && r[3] instanceof Number) {
	            countValue = ((Number) r[3]).longValue();
	        }
	        m.put("count", countValue);

	        result.add(m);
	    }

	    return result;
	}





	@Transactional
	public List<WorksheetDTO> getWorksheetByUserAndClaimRefAndYear(
	        String username,
	        String claimRefNo,
	        String claimYear
	) {
	    // no extra filters → pass an empty filter object
	    WorksheetDataFilter filter = new WorksheetDataFilter();
	    return getWorksheetByUserAndClaimRefAndYear(username, claimRefNo, claimYear, filter);
	}

	@Transactional
	public List<WorksheetDTO> getWorksheetByUserAndClaimRefAndYear(
	        String username,
	        String claimRefNo,
	        String claimYear,
	        WorksheetDataFilter filter
	) {
	    if (username == null || username.isBlank() ||
	        claimRefNo == null || claimRefNo.isBlank() ||
	        claimYear == null || claimYear.isBlank()) {

	        log.debug("Invalid parameters for getWorksheetByUserAndClaimRefAndYear: username='{}', claimRefNo='{}', claimYear='{}'",
	                username, claimRefNo, claimYear);
	        return Collections.emptyList();
	    }

	    // Mandatory filters
	    Specification<Worksheet> baseSpec = (root, query, cb) -> cb.and(
	            cb.equal(root.get("username"), username),
	            cb.equal(root.get("claimRefNo"), claimRefNo),
	            cb.equal(root.get("claimYear"), claimYear)
	    );

	    // Modern, NON-DEPRECATED way
	    Specification<Worksheet> spec = Specification.allOf(
	            baseSpec,
	            buildSpecification(filter)
	    );

	    List<Worksheet> rows = worksheetRepository.findAll(
	            spec,
	            Sort.by(Sort.Direction.ASC, "beDate")
	    );

	    if (rows.isEmpty()) {
	        return Collections.emptyList();
	    }

	    rows.forEach(w -> {
	        if (w.getMaterial() != null) {
	            w.getMaterial().getBomPartNo();
	        }
	    });

	    return rows.stream()
	            .map(this::toDto)
	            .collect(Collectors.toList());
	}
	
	
	
	
	@Override
    @Transactional
    public WorksheetDTO getWorksheetWithExportModels(Long worksheetId) {
        Worksheet worksheet = worksheetRepository.findByWorksheetId(worksheetId)
                .orElseThrow(() -> new EntityNotFoundException("Worksheet not found: " + worksheetId));

        // exportModels should be initialized due to @EntityGraph
        return toDto(worksheet);
    }
	
	
	
	@Transactional
	public void updateWorksheet(WorksheetDTO worksheetDTO) {

	    // Validate DTO
	    Set<ConstraintViolation<WorksheetDTO>> violations = validator.validate(worksheetDTO);
	    if (!violations.isEmpty()) {
	        violations.forEach(v ->
	            System.err.println("Validation failed on " + v.getPropertyPath() + ": " + v.getMessage()));
	        throw new ConstraintViolationException(violations);
	    }

	    Worksheet existing = worksheetRepository.findById(worksheetDTO.getWorksheetId())
	            .orElseThrow(() -> new RuntimeException("Worksheet not found with ID " + worksheetDTO.getWorksheetId()));

	    // Map scalar properties from DTO (e.g., remark)
	    existing.setClaimRefNo(worksheetDTO.getClaimRefNo());
	    existing.setClaimYear(worksheetDTO.getClaimYear());
	    existing.setBeNo(worksheetDTO.getBeNo());
	    existing.setBeDate(worksheetDTO.getBeDate());
	    
	    existing.setAltBoePartNo(worksheetDTO.getAltBoePartNo());
	    existing.setDbkPartNo(worksheetDTO.getDbkPartNo());
	    existing.setItemDescription(worksheetDTO.getItemDescription());
	    existing.setUom(worksheetDTO.getUom());
	    existing.setImportQty(worksheetDTO.getImportQty());
	    existing.setAssessableValue(worksheetDTO.getAssessableValue());
	    existing.setCifValue(worksheetDTO.getCifValue());
	    existing.setPerQtyCif(worksheetDTO.getPerQtyCif());
	    existing.setBcd(worksheetDTO.getBcd());
	    existing.setSws(worksheetDTO.getSws());
	    existing.setAddDuty(worksheetDTO.getAddDuty());
	    existing.setTotalDuty(worksheetDTO.getTotalDuty());
	    existing.setDutyPerQty(worksheetDTO.getDutyPerQty());
	    existing.setUsedQtyTotal(worksheetDTO.getUsedQtyTotal());
	    existing.setDutyClaimedTotal(worksheetDTO.getDutyClaimedTotal());
	    existing.setCifClaimedTotal(worksheetDTO.getCifClaimedTotal());
	    existing.setBcdClaimed(worksheetDTO.getBcdClaimed());
	    existing.setSwsClaimed(worksheetDTO.getSwsClaimed());
	    existing.setAddClaimed(worksheetDTO.getAddClaimed());
	    existing.setOpeningBalanceQtyDef(worksheetDTO.getOpeningBalanceQtyDef());
	    existing.setQtyUsedDef(worksheetDTO.getQtyUsedDef());
	    existing.setClosingBalanceDef(worksheetDTO.getClosingBalanceDef());
	    existing.setCreatedAt(worksheetDTO.getCreatedAt());
	    existing.setRemark(worksheetDTO.getRemark());
	    existing.setUsername(worksheetDTO.getUsername());

	    // Update ImportData relations and balances if importId present
	    if (worksheetDTO.getImportId() != null) {
	        ImportData importData = importDataRepository.findById(worksheetDTO.getImportId())
	                .orElseThrow(() -> new RuntimeException("ImportData not found with id " + worksheetDTO.getImportId()));
	        existing.setImportData(importData);

	        if (existing.getOpeningBalanceQtyDef() != null)
	            importData.setQtyOpeningBalance(existing.getOpeningBalanceQtyDef());
	        if (existing.getQtyUsedDef() != null)
	            importData.setQtyUsed(existing.getQtyUsedDef());
	        if (existing.getClosingBalanceDef() != null) {
	            importData.setClosingBalance(existing.getClosingBalanceDef());

	            if (BigDecimal.ZERO.compareTo(existing.getClosingBalanceDef()) == 0) {
	                importData.setStockWiseEligibility(StockWiseEligibility.CLOSED);
	            }
	        }
	        importDataRepository.save(importData);
	    } else {
	        throw new RuntimeException("ImportDataId must not be null");
	    }

	    // Update Material if needed
	    if (worksheetDTO.getBomPartNo() != null) {
	        Material material = materialRepository.findById(worksheetDTO.getBomPartNo())
	                .orElseThrow(() -> new RuntimeException("Material not found with BomPartNO: " + worksheetDTO.getBomPartNo()));
	        existing.setMaterial(material);
	    }

	    // Handle export models update (existing, new, removed) without replacing collection reference
	    List<WorksheetExportModelsDTO> exportModelDTOs = worksheetDTO.getExportModels();
	    if (exportModelDTOs == null) exportModelDTOs = Collections.emptyList();

	    Map<Long, WorksheetExportModels> existingExportModelsMap = existing.getExportModels().stream()
	            .filter(em -> em.getWsModelId() != null)
	            .collect(Collectors.toMap(WorksheetExportModels::getWsModelId, Function.identity()));

	    // Prepare a list to track processed export models (whether updated or new)
	    List<WorksheetExportModels> modelsToRetain = new ArrayList<>();

	    for (WorksheetExportModelsDTO emDto : exportModelDTOs) {
	        WorksheetExportModels emEntity;

	        if (emDto.getWsModelId() != null && existingExportModelsMap.containsKey(emDto.getWsModelId())) {
	            emEntity = existingExportModelsMap.remove(emDto.getWsModelId());
	        } else {
	            emEntity = new WorksheetExportModels();
	            emEntity.setWorksheet(existing);
	        }

	        // Set bomExportModelData
	        final Long bomExportModelIdToUse = (emDto.getBomExportModelId() != null && emDto.getBomExportModelId() == 0)
	                                          ? 1L : emDto.getBomExportModelId();

	        BomExportModelQuantity bomExportData = bomExportModelRepository.findById(bomExportModelIdToUse)
	                .orElseThrow(() -> new RuntimeException("BomExportModelQuantity not found"));

	        emEntity.setBomExportModelData(bomExportData);

	        // Set Model entity by modelNo
	        Models model = modelsRepository.findById(emDto.getModelNo())
	                .orElseThrow(() -> new RuntimeException("Model not found with modelNo " + emDto.getModelNo()));
	        emEntity.setModel(model);

	        // Set scalar fields
	        emEntity.setEmUsedQty(emDto.getEmUsedQty());
	        emEntity.setDutyClaimed(emDto.getDutyClaimed());
	        emEntity.setCifClaimed(emDto.getCifClaimed());
	        emEntity.setColNo(emDto.getColNo() != null ? emDto.getColNo() : 1);

	        // Track this model to retain
	        modelsToRetain.add(emEntity);

	        // Update bomExportModel status to CLOSED
	        if (bomExportModelIdToUse != null) {
	            bomExportModelRepository.findById(bomExportModelIdToUse).ifPresent(bom -> {
	                bom.setStatus("CLOSED");
	                bomExportModelRepository.save(bom);
	            });
	        }
	    }

	    // Remove export models that were deleted (leftover in map)
	    existingExportModelsMap.values().forEach(emToRemove -> {
	        existing.getExportModels().remove(emToRemove);
	    });

	    // Add or update processed export models in-place
	    for (WorksheetExportModels modelToRetain : modelsToRetain) {
	        if (!existing.getExportModels().contains(modelToRetain)) {
	            existing.getExportModels().add(modelToRetain);
	        }
	    }

	    // Save worksheet (cascades to export models)
	    worksheetRepository.save(existing);

	    // Optionally delete draft worksheet if applicable
	    if (worksheetDTO.getDraftWorksheetId() != null) {
	        draftWorksheetRepository.deleteById(worksheetDTO.getDraftWorksheetId());
	    }
	}








	
	

}