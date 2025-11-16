package com.orpe.consultants.service.impl;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.criteria.Predicate;

import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
//    @Override
//    public Page<BomClaimDTO> search(BomClaimFilter filter, Pageable pageable) {
//        Specification<BomClaim> spec = buildSpecification(filter);
//        Page<BomClaim> page = bomClaimRepo.findAll(spec, pageable);
//        return page.map(this::entityToDto);
//    }
    
    
    
    @Override
    public Page<BomClaimDTO> search(BomClaimFilter filter, Pageable pageable) {
        Specification<BomClaim> spec = buildSpecification(filter);
        Page<BomClaim> page = bomClaimRepo.findAll(spec, pageable);

        List<BomClaimDTO> dtos = page.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }
    
    
    @Override
	public List<BomClaimDTO> search(BomClaimFilter filter) {
	    Specification<BomClaim> spec = buildSpecification(filter);

	    // Sort by createdAt descending (property name of your entity)
	    List<BomClaim> list = bomClaimRepo.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

	    return list.stream()
	            .map(this::entityToDto)
	            .collect(Collectors.toList());
	}

    
    
    public Specification<BomClaim> buildSpecification(BomClaimFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(filter.getBomPartNo())) {
                predicates.add(cb.like(cb.lower(root.get("bomPartNo")), "%" + filter.getBomPartNo().toLowerCase() + "%"));
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
           
            // Add more predicates for other filter fields

            return cb.and(predicates.toArray(new Predicate[0]));
        };
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


