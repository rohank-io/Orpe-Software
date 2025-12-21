package com.orpe.consultants.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.orpe.consultants.dto.SbWiseDbkCalculationDTO;
import com.orpe.consultants.model.SbWiseDbkCalculation;
import com.orpe.consultants.model.User;

@Repository
public interface SbWiseDbkCalculationRepository extends JpaRepository<SbWiseDbkCalculation, Long>, JpaSpecificationExecutor<SbWiseDbkCalculation> {
	
	@Query("""
		       SELECT d.claimRefNo, d.claimYear, COUNT(d), MIN(d.shippingBillDate)
		       FROM SbWiseDbkCalculation d
		       GROUP BY d.claimRefNo, d.claimYear
		       ORDER BY MIN(d.shippingBillDate) ASC
		       """)
		List<Object[]> findAllDbkGroups();
		
		
		List<SbWiseDbkCalculation> findByClaimRefNoAndClaimYearOrderByShippingBillDateAsc(String claimRefNo, String claimYear);	


}
