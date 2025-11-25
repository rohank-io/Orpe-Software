package com.orpe.consultants.repository;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.orpe.consultants.dto.SbWiseQuantityConsumptionDTO;
import com.orpe.consultants.model.SbWiseQuantityConsumption;

@Repository
public interface SbWiseQuantityConsumptionRepository extends JpaRepository<SbWiseQuantityConsumption, Long>, JpaSpecificationExecutor<SbWiseQuantityConsumption> {
	
	
	@Query("""
	           select 
	               s.claimRefNo,
	               s.claimYear,
	               min(s.createdAt),
	               sum(s.usedQty)
	           from SbWiseQuantityConsumption s
	           group by s.claimRefNo, s.claimYear
	           order by s.claimRefNo, s.claimYear
	           """)
	    List<Object[]> findGroupedByClaimRefNoAndClaimYear();
	    
	    
	    List<SbWiseQuantityConsumption> findByClaimRefNoAndClaimYearOrderByBoeNoAscDbkPartNoAscSbNoAscExportModelNoAsc(
	            String claimRefNo,
	            String claimYear
	    );

	  
}
