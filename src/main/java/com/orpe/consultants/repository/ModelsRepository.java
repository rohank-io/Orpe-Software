package com.orpe.consultants.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.orpe.consultants.model.Models;



@Repository
public interface ModelsRepository extends JpaRepository<Models, String> {
	List<Models> findAllByModelNoIn(Collection<String> modelNos);
}
