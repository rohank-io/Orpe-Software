package com.orpe.consultants.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orpe.consultants.dto.StockWiseEligibility;
import com.orpe.consultants.dto.WorksheetDTO;
import com.orpe.consultants.dto.WorksheetExportModelsDTO;
//import com.orpe.consultants.model.BomData;
import com.orpe.consultants.model.ImportData;
import com.orpe.consultants.model.Material;
//import com.orpe.consultants.model.Models;
import com.orpe.consultants.model.Worksheet;
import com.orpe.consultants.model.WorksheetExportModels;
import com.orpe.consultants.repository.BomDataRepository;
import com.orpe.consultants.repository.BomExportModelReposioty;
import com.orpe.consultants.repository.ExportDataRepository;
import com.orpe.consultants.repository.ImportDataRepository;
import com.orpe.consultants.repository.MaterialRepository;
import com.orpe.consultants.repository.ModelsRepository;
import com.orpe.consultants.repository.WorksheetExportModelsRepository;
import com.orpe.consultants.repository.WorksheetRepository;
import com.orpe.consultants.service.WorksheetService;

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
            Worksheet savedWorksheet = worksheetRepository.save(worksheet);

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
                            exportDataRepository.findByClaimRefNoAndModels_ModelNo(claimRefNo, modelNo).ifPresent(exportData -> {
                                exportData.setSbUtilization("CLOSED");
                                exportDataRepository.save(exportData);
                                System.out.println("ExportData sbUtilization set to CLOSED for claimRefNo: " + claimRefNo + ", modelNo: " + modelNo);
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
        }
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

}
