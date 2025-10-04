package com.orpe.consultants.service.impl;

import com.orpe.consultants.dto.ImportDataDTO;
import com.orpe.consultants.dto.ImportDataFilter;
import com.orpe.consultants.model.BomData;
import com.orpe.consultants.model.BomExportModelQuantity;
import com.orpe.consultants.model.ImportData;
import com.orpe.consultants.model.Material;
import com.orpe.consultants.repository.BomDataRepository;
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
  private final BomDataRepository bomDataRepository;
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
  
  
  
  @Override
  @Transactional
  public List<ImportDataDTO> fetchImportDataWithExportModels(List<Long> importIds) {
      List<ImportData> imports = importRepo.findAllById(importIds);
      Set<String> partNos = imports.stream()
          .map(i -> (i.getMaterial() != null) ? i.getMaterial().getBomPartNo() : null)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());

      List<BomData> bomList = bomDataRepository.findAllByMaterial_BomPartNoIn(partNos);

      // Group BomData by bomPartNo, then flatten aggregated export model lists per bomPartNo
      Map<String, List<BomExportModelQuantity>> exportModelsByPartNo = bomList.stream()
          .collect(Collectors.groupingBy(
              bom -> bom.getMaterial().getBomPartNo(),
              Collectors.mapping(BomData::getExportModels, Collectors.toList())
          ))
          .entrySet().stream()
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> e.getValue().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList())
          ));

      List<ImportDataDTO> details = new ArrayList<>();
      for (ImportData imp : imports) {
          String partNo = (imp.getMaterial() != null) ? imp.getMaterial().getBomPartNo() : null;
          List<BomExportModelQuantity> exModels = partNo != null ? exportModelsByPartNo.getOrDefault(partNo, Collections.emptyList()) : Collections.emptyList();

          ImportDataDTO dto = new ImportDataDTO();
          dto.setImportData(imp);
          dto.setExportModels(exModels);
          details.add(dto);
      }
      return details;
  }

}
