package com.orpe.consultants.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.orpe.consultants.dto.DraftWorksheetDTO;
import com.orpe.consultants.dto.DraftWorksheetExportModelsDTO;
import com.orpe.consultants.dto.WorksheetDTO;
import com.orpe.consultants.dto.WorksheetDataFilter;
import com.orpe.consultants.model.DraftWorksheet;
import com.orpe.consultants.model.DraftWorksheetExportModels;
import com.orpe.consultants.model.Worksheet;
import com.orpe.consultants.repository.BomDataRepository;
import com.orpe.consultants.repository.BomExportModelReposioty;
import com.orpe.consultants.repository.DraftExportModelRepository;
import com.orpe.consultants.repository.DraftWorksheetRepository;
import com.orpe.consultants.repository.ExportDataRepository;
import com.orpe.consultants.repository.ImportDataRepository;
import com.orpe.consultants.repository.MaterialRepository;
import com.orpe.consultants.repository.ModelsRepository;
import com.orpe.consultants.repository.WorksheetExportModelsRepository;
import com.orpe.consultants.repository.WorksheetRepository;
import com.orpe.consultants.service.DraftWorksheetService;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class DraftWorksheetServiceImpl implements DraftWorksheetService {
	
	@Autowired
	private Validator validator;
	private final ModelMapper modelMapper;
	private final ImportDataRepository importDataRepository;
	private final DraftWorksheetRepository  draftWorksheetRepository;
	private final DraftExportModelRepository draftExportModelsRepository;
	
	
	   
	
	@Transactional
	public void saveBulkWorksheets(List<DraftWorksheetDTO> draftWorksheetDTOList) {
	    for (DraftWorksheetDTO draftDTO : draftWorksheetDTOList) {
	        Set<ConstraintViolation<DraftWorksheetDTO>> violations = validator.validate(draftDTO);
	        if (!violations.isEmpty()) {
	            violations.forEach(v ->
	                System.err.println("Draft DTO validation failed on " + v.getPropertyPath() + ": " + v.getMessage()));
	            throw new ConstraintViolationException(violations);
	        }

	        DraftWorksheet draftWorksheet = modelMapper.map(draftDTO, DraftWorksheet.class);

	        if (draftDTO.getImportId() != null) {
	            draftWorksheet.setImportId(draftDTO.getImportId());
	        }


	        System.out.println("Saving draft: " + draftWorksheet.getClaimRefNo());

	        DraftWorksheet savedDraft = draftWorksheetRepository.saveAndFlush(draftWorksheet);
	        System.out.println("Saved draft ID: " + savedDraft.getDraftWorksheetId());

	        draftExportModelsRepository.deleteByDraftWorksheet(savedDraft);

	        if (draftDTO.getExportModels() != null) {
	            for (DraftWorksheetExportModelsDTO emDTO : draftDTO.getExportModels()) {
	                DraftWorksheetExportModels draftExportModel = new DraftWorksheetExportModels();
	                draftExportModel.setDraftWorksheet(savedDraft);
	                draftExportModel.setBomExportModelId(emDTO.getBomExportModelId());
	                draftExportModel.setModelNo(emDTO.getModelNo());
	                draftExportModel.setColNo(emDTO.getColNo());
	                draftExportModel.setEmUsedQty(emDTO.getEmUsedQty());
	                draftExportModel.setDutyClaimed(emDTO.getDutyClaimed());
	                draftExportModel.setCifClaimed(emDTO.getCifClaimed());
	                draftExportModelsRepository.save(draftExportModel);
	            }
	        }
	    }
	}
	
	
	@Transactional
	public void updateBulkDrafts(List<DraftWorksheetDTO> draftDTOs) {
	    for (DraftWorksheetDTO dto : draftDTOs) {

	        // Find existing DraftWorksheet entity
	        DraftWorksheet entity = draftWorksheetRepository.findById(dto.getDraftWorksheetId())
	            .orElseThrow(() -> new RuntimeException("Draft not found with ID " + dto.getDraftWorksheetId()));

	        // ====== MAIN WORKSHEET FIELDS ======
	        entity.setClaimRefNo(dto.getClaimRefNo());
	        entity.setClaimYear(dto.getClaimYear());
	        entity.setImportId(dto.getImportId());
	        entity.setBeNo(dto.getBeNo());
	        entity.setBeDate(dto.getBeDate());
	        entity.setBomPartNo(dto.getBomPartNo());
	        entity.setDbkPartNo(dto.getDbkPartNo());
	        entity.setItemDescription(dto.getItemDescription());
	        entity.setUom(dto.getUom());
	        entity.setImportQty(dto.getImportQty());
	        entity.setAssessableValue(dto.getAssessableValue());
	        entity.setCifValue(dto.getCifValue());
	        entity.setPerQtyCif(dto.getPerQtyCif());
	        entity.setBcd(dto.getBcd());
	        entity.setSws(dto.getSws());
	        entity.setAddDuty(dto.getAddDuty());
	        entity.setTotalDuty(dto.getTotalDuty());
	        entity.setDutyPerQty(dto.getDutyPerQty());
	        entity.setUsedQtyTotal(dto.getUsedQtyTotal());
	        entity.setDutyClaimedTotal(dto.getDutyClaimedTotal());
	        entity.setCifClaimedTotal(dto.getCifClaimedTotal());
	        entity.setBcdClaimed(dto.getBcdClaimed());
	        entity.setSwsClaimed(dto.getSwsClaimed());
	        entity.setAddClaimed(dto.getAddClaimed());
	        entity.setOpeningBalanceQtyDef(dto.getOpeningBalanceQtyDef());
	        entity.setQtyUsedDef(dto.getQtyUsedDef());
	        entity.setClosingBalanceDef(dto.getClosingBalanceDef());
	        entity.setDraftStatus(dto.getDraftStatus());
	        entity.setUpdatedAt(LocalDateTime.now());

	        // ====== EXPORT MODELS UPDATE ======
	        if (dto.getExportModels() != null && !dto.getExportModels().isEmpty()) {

	            for (DraftWorksheetExportModelsDTO emDto : dto.getExportModels()) {
	            	 // Skip empty export model DTOs
	                if (emDto.getModelNo() == null && emDto.getBomExportModelId() == null && 
	                    emDto.getEmUsedQty() == null && emDto.getDutyClaimed() == null &&
	                    emDto.getCifClaimed() == null) {
	                    continue; // skip this empty one
	                }

	                DraftWorksheetExportModels em;

	                // If export model already exists — update
	                if (emDto.getDraftWsModelId() != null) {
	                    em = draftExportModelsRepository.findById(emDto.getDraftWsModelId())
	                        .orElseThrow(() -> new RuntimeException(
	                            "Export model not found with ID " + emDto.getDraftWsModelId()
	                        ));
	                } 
	                // Otherwise create new one
	                else {
	                    em = new DraftWorksheetExportModels();
	                    em.setDraftWorksheet(entity);
	                }

	                // --- Update Export Model fields ---
	                em.setBomExportModelId(emDto.getBomExportModelId());
	                em.setModelNo(emDto.getModelNo());
	                em.setColNo(emDto.getColNo());
	                em.setEmUsedQty(emDto.getEmUsedQty());
	                em.setDutyClaimed(emDto.getDutyClaimed());
	                em.setCifClaimed(emDto.getCifClaimed());

	                draftExportModelsRepository.save(em);
	            }
	        }

	        draftWorksheetRepository.save(entity);
	    }
	}





	

	@Override
	public DraftWorksheetDTO save(DraftWorksheetDTO dto) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<DraftWorksheetDTO> findById(Long id) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public List<DraftWorksheetDTO> findAll() {
		List<DraftWorksheet> all = draftWorksheetRepository.findAll();
        List<DraftWorksheetDTO> dtos = new ArrayList<>();
        for (DraftWorksheet bd : all) {
            dtos.add(toDto(bd));
        }
        return dtos;
	}

	@Override
	public void deleteById(Long id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Page<DraftWorksheetDTO> search(WorksheetDataFilter filter, Pageable pageable) {
		
		return null;
	}

	@Override
	public byte[] exportData(WorksheetDataFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean validate(DraftWorksheetDTO dto) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long count(WorksheetDataFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	public DraftWorksheetDTO toDto(DraftWorksheet entity) {
	    if (entity == null) return null;

	    DraftWorksheetDTO dto = modelMapper.map(entity, DraftWorksheetDTO.class);

	    // ✅ Set Import ID (since modelMapper won’t map nested entity IDs automatically)
	    

	    // ✅ Map export models (if any)
	    if (entity.getExportModels() != null && !entity.getExportModels().isEmpty()) {
	        dto.setExportModels(entity.getExportModels().stream()
	                .map(em -> {
	                    DraftWorksheetExportModelsDTO emDto = new DraftWorksheetExportModelsDTO();
	                    emDto.setBomExportModelId(em.getBomExportModelId());
	                    emDto.setModelNo(em.getModelNo());
	                    emDto.setColNo(em.getColNo());
	                    emDto.setEmUsedQty(em.getEmUsedQty());
	                    emDto.setDutyClaimed(em.getDutyClaimed());
	                    emDto.setCifClaimed(em.getCifClaimed());
	                    emDto.setDraftWsModelId(em.getDraftWsModelId());
	                    
	                    return emDto;
	                })
	                .collect(Collectors.toList()));
	    }

	    return dto;
	}


	public DraftWorksheet toEntity(DraftWorksheetDTO dto) {
	    if (dto == null) return null;

	    DraftWorksheet entity = modelMapper.map(dto, DraftWorksheet.class);

	    // ✅ Handle ImportData manually (only if present in DTO)
	    

	    // ✅ Handle Export Models manually (if your entity has a list of export models)
	    if (dto.getExportModels() != null && !dto.getExportModels().isEmpty()) {
	        List<DraftWorksheetExportModels> exportModels = dto.getExportModels().stream()
	                .map(emDto -> {
	                    DraftWorksheetExportModels em = new DraftWorksheetExportModels();
	                    em.setDraftWsModelId(emDto.getDraftWsModelId());
	                    em.setBomExportModelId(emDto.getBomExportModelId());
	                    em.setModelNo(emDto.getModelNo());
	                    em.setColNo(emDto.getColNo());
	                    em.setEmUsedQty(emDto.getEmUsedQty());
	                    em.setDutyClaimed(emDto.getDutyClaimed());
	                    em.setCifClaimed(emDto.getCifClaimed());
	                    em.setDraftWorksheet(entity); // set back-reference
	                    return em;
	                })
	                .collect(Collectors.toList());

	        entity.setExportModels(exportModels);
	    }

	    return entity;
	}
	
	
	 @Override
	    public List<Map<String, Object>> getAllDraftGroups() {
	        List<Object[]> rows = draftWorksheetRepository.findAllDraftGroups();
	        List<Map<String, Object>> result = new ArrayList<>(rows.size());

	        for (Object[] r : rows) {
	            Map<String, Object> m = new HashMap<>(3);
	            // r[0] = claimRefNo, r[1] = claimYear, r[2] = count (Number)
	            m.put("claimRefNo", r[0] != null ? r[0].toString() : null);
	            m.put("claimYear", r[1] != null ? r[1].toString() : null);

	            Long countValue = 0L;
	            if (r[2] instanceof Number) {
	                countValue = ((Number) r[2]).longValue();
	            }
	            m.put("count", countValue);

	            result.add(m);
	        }

	        return result;
	    }


	
	

}
