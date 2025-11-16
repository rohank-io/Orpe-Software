package com.orpe.consultants.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orpe.consultants.model.BomExportModelQuantity;

public interface BomExportModelReposioty extends JpaRepository<BomExportModelQuantity, Long> {
	List<BomExportModelQuantity> findByBomDataBomId(Long bomId);
	
	List<BomExportModelQuantity> findByBomDataBomIdIn(Collection<Long> bomIds);
	
}
