package com.orpe.consultants.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

import java.util.stream.Collectors;


import com.orpe.consultants.dto.BomClaimFilter;
import com.orpe.consultants.dto.SbWiseQuantityConsumptionDTO;

import com.orpe.consultants.model.SbWiseQuantityConsumption;

import com.orpe.consultants.repository.SbWiseQuantityConsumptionRepository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;


import com.orpe.consultants.service.SbWiseQtyConsumptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SbWiseQtyConsumptionServiceImpl implements SbWiseQtyConsumptionService {
	
	private final SbWiseQuantityConsumptionRepository sbWiseQuantityConsumptionRepository;
	private final ModelMapper modelMapper;
	
	@Override
    @Transactional
    public int saveBulk(List<SbWiseQuantityConsumptionDTO> rows) {
        List<SbWiseQuantityConsumption> entities = rows.stream().map(dto ->
            SbWiseQuantityConsumption.builder()
                .sbQtyConsumptionId(dto.getSbQtyConsumptionId())
                .claimRefNo(dto.getClaimRefNo())
                .claimYear(dto.getClaimYear())
                .dbkPartNo(dto.getDbkPartNo())
                .boeNo(dto.getBoeNo())
                .usedQty(dto.getUsedQty())
                .exportModelNo(dto.getExportModelNo())
                .sbNo(dto.getSbNo())
                .claimId(dto.getClaimId())
                .build()
        ).collect(Collectors.toList());

        sbWiseQuantityConsumptionRepository.saveAll(entities);
        return entities.size();
    }
	
	
	
	
	
	private SbWiseQuantityConsumption dtoToEntity(SbWiseQuantityConsumptionDTO dto) {
        return modelMapper.map(dto, SbWiseQuantityConsumption.class);
    }

    private SbWiseQuantityConsumptionDTO entityToDto(SbWiseQuantityConsumption entity) {
        return modelMapper.map(entity, SbWiseQuantityConsumptionDTO.class);
    }

    
    
    @Override
    public List<SbWiseQuantityConsumptionDTO> search(BomClaimFilter filter) {
        Specification<SbWiseQuantityConsumption> spec = buildSpecification(filter);

        // Sort by createdAt descending
        List<SbWiseQuantityConsumption> list = sbWiseQuantityConsumptionRepository.findAll(
            spec,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return list.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    public Specification<SbWiseQuantityConsumption> buildSpecification(BomClaimFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Generic filter-field / filter-value support (optional)
            String field = filter.getFilterField();
            String value = filter.getFilterValue();

            if (field != null && !field.isBlank() && value != null && !value.isBlank()) {
                // fields that should use LIKE (case-insensitive)
                List<String> stringFields = List.of(
                    "boeNo",
                    "claimYear",
                    "clientName",
                    "dbkPartNo",
                    "bomPartNo",
                    "exportModelNo",
                    "sbNo",
                    "claimRefNo"
                );

                if (stringFields.contains(field)) {
                    predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
                } else {
                    // you can add additional typed-field checks here if needed
                    predicates.add(cb.equal(root.get(field), value));
                }
            } else {
                // Specific field filters (if user provided explicit filter properties)
                if (StringUtils.hasText(filter.getDbkPartNo())) {
                    predicates.add(cb.like(cb.lower(root.get("dbkPartNo")), "%" + filter.getDbkPartNo().toLowerCase() + "%"));
                }
                if (StringUtils.hasText(filter.getClaimRefNo())) {
                    predicates.add(cb.like(cb.lower(root.get("claimRefNo")), "%" + filter.getClaimRefNo().toLowerCase() + "%"));
                }
                if (StringUtils.hasText(filter.getClaimYear())) {
                    predicates.add(cb.like(cb.lower(root.get("claimYear")), "%" + filter.getClaimYear().toLowerCase() + "%"));
                }
                if (StringUtils.hasText(filter.getSbNo())) {
                    predicates.add(cb.like(cb.lower(root.get("sbNo")), "%" + filter.getSbNo().toLowerCase() + "%"));
                }
                if (StringUtils.hasText(filter.getExportModelNo())) {
                    predicates.add(cb.like(cb.lower(root.get("exportModelNo")), "%" + filter.getExportModelNo().toLowerCase() + "%"));
                }
                if (StringUtils.hasText(filter.getBoeNo())) {
                    predicates.add(cb.like(cb.lower(root.get("boeNo")), "%" + filter.getBoeNo().toLowerCase() + "%"));
                }
                // add more specific field checks as needed...
            }

            // Date range filtering on createdAt
            // NOTE: I assume filter.getFromDate() and filter.getToDate() are of type java.time.LocalDate.
            // If they are LocalDateTime, remove the atStartOfDay()/atTime(...) conversions and use directly.
            if (filter.getFromDate() != null) {
                // compare createdAt >= fromDate at start of day
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        filter.getFromDate().atStartOfDay()));
            }
            if (filter.getToDate() != null) {
                // compare createdAt <= toDate at end of day
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"),
                        filter.getToDate().atTime(java.time.LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    
    
	
	

	


}
