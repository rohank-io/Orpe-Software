package com.orpe.consultants.service.impl;

import com.orpe.consultants.dto.ImportDataDTO;
import com.orpe.consultants.dto.ImportDataFilter;
import com.orpe.consultants.model.ImportData;
import com.orpe.consultants.model.Material;
import com.orpe.consultants.repository.ImportDataRepository;
import com.orpe.consultants.repository.MaterialRepository;
import com.orpe.consultants.service.ImportDataService;

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
public class ImportDataServiceImpl implements ImportDataService {

  private final MaterialRepository materialRepo;
  private final ImportDataRepository importRepo;
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
          mat = materialRepo.findById(key).orElseGet(() ->
              materialRepo.save(Material.builder().bomPartNo(key).build())
          );
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
      mat = materialRepo.findById(key).orElseGet(() ->
          materialRepo.save(Material.builder().bomPartNo(key).build())
      );
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
    return importRepo.findAll().stream()
      .map(this::entityToDto)
      .collect(Collectors.toList());
  }

  @Override
  public Page<ImportDataDTO> search(ImportDataFilter filter, Pageable pageable) {
    Specification<ImportData> spec = buildSpecification(filter);
    Page<ImportData> page = importRepo.findAll(spec, pageable);
    return page.map(this::entityToDto);
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

      if (StringUtils.hasText(filter.getBeNo())) {
        predicates.add(cb.like(cb.lower(root.get("beNo")), "%" + filter.getBeNo().toLowerCase() + "%"));
      }
      if (StringUtils.hasText(filter.getClaimYear())) {
        predicates.add(cb.equal(root.get("claimYear"), filter.getClaimYear()));
      }
      if (StringUtils.hasText(filter.getSupplierNameAddress())) {
        predicates.add(cb.like(cb.lower(root.get("supplierNameAddress")), "%" + filter.getSupplierNameAddress().toLowerCase() + "%"));
      }
      if (StringUtils.hasText(filter.getCountryOfOrigin())) {
        predicates.add(cb.like(cb.lower(root.get("countryOfOrigin")), "%" + filter.getCountryOfOrigin().toLowerCase() + "%"));
      }
      if (filter.getBeDateFrom() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("beDate"), filter.getBeDateFrom()));
      }
      if (filter.getBeDate() != null) {
    	  predicates.add(cb.equal(root.get("beDate"), filter.getBeDate()));
      }
      if (StringUtils.hasText(filter.getBomPartNo())) {
        predicates.add(cb.like(cb.lower(root.get("bomPartNo")), "%" + filter.getBomPartNo().toLowerCase() + "%"));
      }
      if (StringUtils.hasText(filter.getDbkPartNo())) {
        predicates.add(cb.like(cb.lower(root.get("dbkPartNo")), "%" + filter.getDbkPartNo().toLowerCase() + "%"));
      }
      if (StringUtils.hasText(filter.getItchsCode())) {
        predicates.add(cb.like(cb.lower(root.get("itchsCode")), "%" + filter.getItchsCode().toLowerCase() + "%"));
      }
      if (StringUtils.hasText(filter.getPortCode())) {
        predicates.add(cb.like(cb.lower(root.get("portCode")), "%" + filter.getPortCode().toLowerCase() + "%"));
      }
      if (StringUtils.hasText(filter.getClaimRefNo())) {
        predicates.add(cb.like(cb.lower(root.get("claimRefNo")), "%" + filter.getClaimRefNo().toLowerCase() + "%"));
      }
      if (StringUtils.hasText(filter.getStockWiseEligibility())) {
        predicates.add(cb.equal(root.get("stockWiseEligibility"), filter.getStockWiseEligibility()));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static String trim(String s) { return s == null ? null : s.trim(); }
  private static String req(String s) {
    if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException("Required field missing");
    return s.trim();
  }
  private static BigDecimal reqBig(BigDecimal b) {
    if (b == null) throw new IllegalArgumentException("Required numeric field missing");
    return b;
  }
  private static BigDecimal nz(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }
  private static LocalDate reqDate(LocalDate d) {
    if (d == null) throw new IllegalArgumentException("beDate required");
    return d;
  }
}
