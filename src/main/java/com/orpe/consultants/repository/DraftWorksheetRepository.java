package com.orpe.consultants.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.orpe.consultants.model.DraftWorksheet;

@Repository
public interface DraftWorksheetRepository extends JpaRepository<DraftWorksheet, Long>,JpaSpecificationExecutor<DraftWorksheet> {
	@EntityGraph(attributePaths = {"exportModels"})
	Page<DraftWorksheet> findAll(Specification<DraftWorksheet> spec, Pageable pageable);
	
	@EntityGraph(attributePaths = {"exportModels"})
	List<DraftWorksheet> findAll();



}
