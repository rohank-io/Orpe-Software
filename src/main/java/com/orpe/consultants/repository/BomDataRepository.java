package com.orpe.consultants.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.orpe.consultants.model.BomData;


@Repository
public interface BomDataRepository extends JpaRepository<BomData, Long>, JpaSpecificationExecutor<BomData> {

	@EntityGraph(attributePaths = {"exportModels"})
    Page<BomData> findAll(Pageable pageable);
	
	List<BomData> findAllByMaterial_BomPartNoIn(Collection<String> partNos);
}
