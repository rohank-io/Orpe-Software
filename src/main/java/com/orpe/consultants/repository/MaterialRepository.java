package com.orpe.consultants.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orpe.consultants.model.Material;

@Repository
public interface MaterialRepository extends JpaRepository<Material, String> {
	List<Material> findAllByBomPartNoIn(Collection<String> bomPartNos);
}
