package com.orpe.consultants.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.orpe.consultants.dto.BomDataDTO;
import com.orpe.consultants.dto.BomDataFilter;
import com.orpe.consultants.dto.BomExportModelQuantityDTO;
import com.orpe.consultants.model.BomData;
import com.orpe.consultants.model.BomExportModelQuantity;
import com.orpe.consultants.model.Material;
import com.orpe.consultants.model.Models;
import com.orpe.consultants.repository.BomDataRepository;
import com.orpe.consultants.repository.BomExportModelReposioty;
import com.orpe.consultants.repository.MaterialRepository;
import com.orpe.consultants.repository.ModelsRepository;
import com.orpe.consultants.service.BomDataService;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BomDataServiceImpl implements BomDataService {

    private final BomDataRepository bomDataRepository;
    private final ModelMapper modelMapper;
    
   

    private final MaterialRepository materialRepository;  
    private final ModelsRepository modelsRepository;  
    private final BomExportModelReposioty bomExportModelQuantityRepository;

    @Override
    public int saveBulk(List<BomDataDTO> dtos) {
        int savedCount = 0;
        Map<String, Material> materialCache = new HashMap<>();

        for (BomDataDTO dto : dtos) {
            Material material = null;
            if (dto.getBomPartNo() != null && !dto.getBomPartNo().isBlank()) {
                String bomPartNo = dto.getBomPartNo().trim();
                material = materialCache.get(bomPartNo);
                if (material == null) {
                    material = materialRepository.findById(bomPartNo)
                        .orElseGet(() -> materialRepository.save(Material.builder().bomPartNo(bomPartNo).build()));
                    materialCache.put(bomPartNo, material);
                }
            }

            BomData entity = dtoToEntity(dto);
            entity.setMaterial(material);
            // dbkPartNo is handled as plain string in dtoToEntity
            bomDataRepository.save(entity);
            savedCount++;
        }
        return savedCount;
    }


    @Override
    public BomDataDTO save(BomDataDTO dto) {
        BomData entity = dtoToEntity(dto);
        BomData saved = bomDataRepository.save(entity);
        return entityToDto(saved);
    }

    @Override
    public Optional<BomDataDTO> findById(Long bomId) {
        return bomDataRepository.findById(bomId)
                .map(this::entityToDto);
    }

    @Override
    public void deleteById(Long bomId) {
        bomDataRepository.deleteById(bomId);
    }

    @Override
    public List<BomDataDTO> findAll() {
        List<BomData> all = bomDataRepository.findAll();
        List<BomDataDTO> dtos = new ArrayList<>();
        for (BomData bd : all) {
            dtos.add(entityToDto(bd));
        }
        return dtos;
    }
    
 // BomServiceImpl.java (or where findAll() is implemented)
    @Override
    @Transactional
    public List<BomDataDTO> findAllForExport() {
    	// 1) fetch all parents
        List<BomData> parents = bomDataRepository.findAll();

        if (parents.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) collect parent ids
        List<Long> parentIds = parents.stream()
                .map(BomData::getBomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 3) fetch all children in one query
        List<BomExportModelQuantity> allChildren = Collections.emptyList();
        if (!parentIds.isEmpty()) {
            allChildren = bomExportModelQuantityRepository.findByBomDataBomIdIn(parentIds);
        }

        // 4) group children by parent id
        Map<Long, List<BomExportModelQuantity>> childrenByParent = allChildren.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c.getBomData().getBomId()));

        // 5) map parents -> DTO and attach child DTOs
        List<BomDataDTO> dtos = new ArrayList<>(parents.size());
        for (BomData parent : parents) {
            BomDataDTO dto = entityToDto(parent); // your existing mapper (ensure it does not assume children are loaded)

            // build child DTOs from grouped list (override whatever entityToDto gave if needed)
            List<BomExportModelQuantity> childEntities = childrenByParent.getOrDefault(parent.getBomId(), Collections.emptyList());
            List<BomExportModelQuantityDTO> childDtos = childEntities.stream()
                    .map(em -> BomExportModelQuantityDTO.builder()
                            .id(em.getId())
                            .bomId(parent.getBomId())
                            .modelNo(em.getModelNo())
                            .quantity(em.getQuantity())
                            .status(em.getStatus())
                            .build())
                    .collect(Collectors.toList());

            dto.setExportModels(childDtos);
            dtos.add(dto);
        }

        return dtos;
    }


    @Override
    public Page<BomDataDTO> search(BomDataFilter filter, Pageable pageable) {
        Specification<BomData> spec = buildSpecification(filter);
        Page<BomData> page = bomDataRepository.findAll(spec, pageable);

        List<BomDataDTO> dtos = page.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    
    
    public Specification<BomData> buildSpecification(BomDataFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(filter.getBomPartNo())) {
                predicates.add(cb.like(cb.lower(root.get("material").get("bomPartNo")), "%" + filter.getBomPartNo().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(filter.getClaimRefNo())) {
                predicates.add(cb.like(cb.lower(root.get("claimRefNo")), "%" + filter.getClaimRefNo().toLowerCase() + "%"));
            }
            
            if (StringUtils.hasText(filter.getClaimYear())) {
        	    predicates.add(cb.like(cb.lower(root.get("claimYear")), "%" + filter.getClaimYear().toLowerCase() + "%"));
            }
            
            if (StringUtils.hasText(filter.getClientName())) {
                predicates.add(cb.like(cb.lower(root.get("clientName")), "%" + filter.getClientName().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(filter.getStatus())) {
                predicates.add(cb.like(cb.lower(root.get("status")), "%" + filter.getStatus().toLowerCase() + "%"));
            }
            // Add more predicates for other filter fields

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public byte[] exportData(BomDataFilter filter) {
        // TODO: Implement export logic as CSV or Excel bytes
        return new byte[0];
    }

    @Override
    public boolean validate(BomDataDTO dto) {
        if (dto == null) return false;
   //     if (dto.getClaimRefNo() == null || dto.getClaimRefNo().isBlank()) return false;
        if (dto.getBomPartNo() == null || dto.getBomPartNo().isBlank()) return false;
        return true;
    }

    @Override
    public long count(BomDataFilter filter) {
        // TODO: Implement count based on filtering criteria
        return bomDataRepository.count();
    }

    public BomData dtoToEntity(BomDataDTO dto) {
        Material material = null;
        if (dto.getBomPartNo() != null && !dto.getBomPartNo().isBlank()) {
            String bomPartNoTrimmed = dto.getBomPartNo().trim();
            material = materialRepository.findById(bomPartNoTrimmed).orElse(null);
            
        }

        // dbkPartNo is simple string, model (Models entity) is not a direct reference in BomData
        // so only fetch Model entity if you want, else omit. Assuming not linked:
        // Uncomment if you switch dbkPartNo to model entity:
        /*
        Models model = null;
        if (dto.getDbkPartNo() != null && !dto.getDbkPartNo().isBlank()) {
            String modelNoTrimmed = dto.getDbkPartNo().trim();
            model = modelsRepository.findById(modelNoTrimmed)
                .orElseThrow(() -> new IllegalArgumentException("Model not found for modelNo: " + modelNoTrimmed));
        }
        */

        BomData entity = BomData.builder()
            .bomId(dto.getBomId())
            .claimRefNo(dto.getClaimRefNo())
            .claimYear(dto.getClaimYear())
            .materialDesc(dto.getMaterialDesc())
            .material(material)
            .alternateBoePartNo(dto.getAlternateBoePartNo())
            .dbkPartNo(dto.getDbkPartNo())  // String as is
            .importedIndigenous(dto.getImportedIndigenous())
            .unit(dto.getUnit())
            .clientName(dto.getClientName())
            .grandTotal(dto.getGrandTotal())
            .netWeightKg(dto.getNetWeightKg())
            .createdAt(dto.getCreatedAt())
            .build();

        List<BomExportModelQuantity> children = new ArrayList<>();
        if (dto.getExportModels() != null) {
            for (BomExportModelQuantityDTO em : dto.getExportModels()) {
                BomExportModelQuantity child = BomExportModelQuantity.builder()
                    .id(em.getId())
                    .modelNo(em.getModelNo())
                    .quantity(em.getQuantity())
                    .status(em.getStatus())
                    .bomData(entity)
                    .build();
                children.add(child);
            }
        }
        entity.setExportModels(children);

        return entity;
    }


    public BomDataDTO entityToDto(BomData entity) {
        List<BomExportModelQuantityDTO> childDtos = new ArrayList<>();
        if (entity.getExportModels() != null) {
            for (BomExportModelQuantity em : entity.getExportModels()) {
                BomExportModelQuantityDTO emDto = BomExportModelQuantityDTO.builder()
                    .id(em.getId())
                    .modelNo(em.getModelNo())
                    .quantity(em.getQuantity())
                    .status(em.getStatus())
                    .build();
                childDtos.add(emDto);
            }
        }
        return BomDataDTO.builder()
            .bomId(entity.getBomId())
            .claimRefNo(entity.getClaimRefNo())
            .claimYear(entity.getClaimYear())
            .materialDesc(entity.getMaterialDesc())
            .bomPartNo(entity.getMaterial() != null ? entity.getMaterial().getBomPartNo() : null)
            .alternateBoePartNo(entity.getAlternateBoePartNo())
            .dbkPartNo(entity.getDbkPartNo())    // simple string field
            .importedIndigenous(entity.getImportedIndigenous())
            .unit(entity.getUnit())
            .clientName(entity.getClientName())
            .grandTotal(entity.getGrandTotal())
            .netWeightKg(entity.getNetWeightKg())
            .createdAt(entity.getCreatedAt())
            .exportModels(childDtos)
            .build();
    }
}
