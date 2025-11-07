package com.orpe.consultants.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import com.orpe.consultants.model.ExportData;
//import com.orpe.consultants.model.BomData;
import com.orpe.consultants.model.ImportData;
import com.orpe.consultants.model.Material;
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

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
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
                        
                        // Feature: Update sbUtilization in ExportData to "CLOSED" by matching claimRefNo & modelNo.
                        String claimRefNo = worksheetDTO.getClaimRefNo();
                        String modelNo = exportModelDTO.getModelNo();
                        if (claimRefNo != null && modelNo != null) {
                        	List<ExportData> exportDataList = exportDataRepository.findByClaimRefNoAndModels_ModelNo(claimRefNo, modelNo);

                        	if (exportDataList != null && !exportDataList.isEmpty()) {
                        	    for (ExportData exportData : exportDataList) {
                        	        exportData.setSbUtilization("CLOSED");
                        	        exportDataRepository.save(exportData);
                        	    }
                        	    System.out.println("ExportData sbUtilization set to CLOSED for claimRefNo: "
                        	        + claimRefNo + ", modelNo: " + modelNo + " (" + exportDataList.size() + " records)");
                        	} else {
                        	    System.err.println("No ExportData found for claimRefNo=" + claimRefNo + ", modelNo=" + modelNo);
                        	}
                        	

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
	                "bomPartNo",
	                "dbkPartNo",
	                "itchsCode",
	                "portCode",
	                "claimRefNo"
	            );

	            if (stringFields.contains(field)) {
	                predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
	            } else if ("stockWiseEligibility".equals(field)) {
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

}