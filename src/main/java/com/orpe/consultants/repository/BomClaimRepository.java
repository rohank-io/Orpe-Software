package com.orpe.consultants.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.orpe.consultants.model.BomClaim;

public interface BomClaimRepository extends JpaRepository<BomClaim, Long>, JpaSpecificationExecutor<BomClaim> {

}
