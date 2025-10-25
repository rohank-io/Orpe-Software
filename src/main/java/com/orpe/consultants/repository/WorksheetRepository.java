package com.orpe.consultants.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


import com.orpe.consultants.model.Worksheet;

public interface WorksheetRepository extends JpaRepository<Worksheet, Long>,JpaSpecificationExecutor<Worksheet>  {
	
	@EntityGraph(attributePaths = {"exportModels"})
	Page<Worksheet> findAll(Specification<Worksheet> spec, Pageable pageable);


}
