package com.orpe.consultants.service.impl;


import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.criteria.Predicate;

import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.orpe.consultants.dto.BomClaimDTO;
import com.orpe.consultants.dto.BomClaimFilter;
import com.orpe.consultants.exception.ResourceNotFoundException;
import com.orpe.consultants.model.BomClaim;
import com.orpe.consultants.repository.BomClaimRepository;
import com.orpe.consultants.service.BomClaimService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@Slf4j
@Service
@RequiredArgsConstructor
public class BomClaimServiceImpl implements BomClaimService {

    private final BomClaimRepository bomClaimRepo;
    private final ModelMapper modelMapper;

    // region ===== Bulk Save =====
    @Override
    public int saveBulk(List<BomClaimDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            log.warn("⚠️ No BOM Claim rows provided for bulk save.");
            return 0;
        }

        List<BomClaim> entities = new ArrayList<>();
        int rowNumber = 0;

        for (BomClaimDTO dto : rows) {
            rowNumber++;
            try {
                BomClaim entity = dtoToEntity(dto);

                if (entity.getCreatedAt() == null) {
                    entity.setCreatedAt(LocalDateTime.now());
                }
                entity.setUpdatedAt(LocalDateTime.now());

                entities.add(entity);
            } catch (Exception e) {
                log.error("❌ Failed to map row #{} (ClaimRefNo={}): {}", 
                        rowNumber, dto.getClaimRefNo(), e.getMessage());
            }
        }

        if (entities.isEmpty()) {
            log.warn("⚠️ No valid BOM Claim records to save after mapping.");
            return 0;
        }

        bomClaimRepo.saveAll(entities);
        log.info("✅ Successfully saved {} BOM Claim records.", entities.size());
        return entities.size();
    }
    // endregion

    // region ===== CRUD =====
    @Override
    public BomClaimDTO save(BomClaimDTO dto) {
        BomClaim entity = dtoToEntity(dto);
        entity = bomClaimRepo.save(entity);
        return entityToDto(entity);
    }

    @Override
    public Optional<BomClaimDTO> findById(Long claimId) {
        return bomClaimRepo.findById(claimId).map(this::entityToDto);
    }

    @Override
    public void deleteById(Long claimId) {
        if (!bomClaimRepo.existsById(claimId)) {
            throw new ResourceNotFoundException("BOM Claim not found for ID: " + claimId);
        }
        bomClaimRepo.deleteById(claimId);
        log.info("🗑️ Deleted BOM Claim ID: {}", claimId);
    }

    @Override
    public List<BomClaimDTO> findAll() {
        return bomClaimRepo.findAll()
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }
    // endregion

    // region ===== Search and Count =====
    @Override
    public Page<BomClaimDTO> search(BomClaimFilter filter, Pageable pageable) {
        Specification<BomClaim> spec = buildSpecification(filter);
        Page<BomClaim> page = bomClaimRepo.findAll(spec, pageable);
        return page.map(this::entityToDto);
    }

    @Override
    public long count(BomClaimFilter filter) {
        return bomClaimRepo.count(buildSpecification(filter));
    }
    // endregion

    // region ===== DTO <-> Entity Mapping =====
    private BomClaim dtoToEntity(BomClaimDTO dto) {
        return modelMapper.map(dto, BomClaim.class);
    }

    private BomClaimDTO entityToDto(BomClaim entity) {
        return modelMapper.map(entity, BomClaimDTO.class);
    }
    // endregion

    // region ===== Specification Builder =====
    private Specification<BomClaim> buildSpecification(BomClaimFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            String field = trim(filter.getFilterField());
            String value = trim(filter.getFilterValue());

            if (field != null && !field.isBlank() && value != null && !value.isBlank()) {
                List<String> stringFields = List.of(
                    "claimRefNo",
                    "claimYear",
                    "materialDescription",
                    "bomPartNo",
                    "altBoePartNo",
                    "dbkPartNo",
                    "importedOrIndigenous",
                    "unit",
                    "boeNo",
                    "exportModelNo",
                    "sbNo",
                    "clientName"
                );

                if (stringFields.contains(field)) {
                    predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
                }
            }

            // Future date range filters (if you ever add LocalDate fields)
            // e.g., claimDate, createdAt, updatedAt
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate().atStartOfDay()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getToDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    // endregion

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    // region ===== Unused Methods (Future Use) =====
    @Override
    public byte[] exportData(BomClaimFilter filter) {
        // TODO: implement Excel/CSV export if needed
        return new byte[0];
    }

    @Override
    public boolean validate(BomClaimDTO dto) {
        return dto.getClaimRefNo() != null && !dto.getClaimRefNo().isBlank();
    }
	
    // endregion
}


