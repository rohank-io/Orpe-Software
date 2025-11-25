package com.orpe.consultants.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.orpe.consultants.model.SbWiseDbkCalculation;

@Repository
public interface SbWiseDbkCalculationRepository extends JpaRepository<SbWiseDbkCalculation, Long>, JpaSpecificationExecutor<SbWiseDbkCalculation> {

}
