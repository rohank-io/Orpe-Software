package com.orpe.consultants.service.impl;

import com.orpe.consultants.dto.ExportDataDTO;
import com.orpe.consultants.dto.ExportDataFilter;
import com.orpe.consultants.model.ExportData;
import com.orpe.consultants.model.Models;
import com.orpe.consultants.repository.ExportDataRepository;
import com.orpe.consultants.repository.ModelsRepository; // if you have one
import com.orpe.consultants.service.ExportDataService;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExportDataServiceImpl implements ExportDataService {

    private final ExportDataRepository exportRepo;
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
}
