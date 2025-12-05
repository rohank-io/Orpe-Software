package com.orpe.consultants.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.orpe.consultants.model.DraftWorksheet;
import com.orpe.consultants.model.Worksheet;

public interface WorksheetRepository extends JpaRepository<Worksheet, Long>, JpaSpecificationExecutor<Worksheet> {

	@EntityGraph(attributePaths = { "exportModels" })
	Page<Worksheet> findAll(Specification<Worksheet> spec, Pageable pageable);

	@Query("""
			   SELECT d.username, d.claimRefNo, d.claimYear, COUNT(d)
			   FROM Worksheet d
			   GROUP BY d.username, d.claimRefNo, d.claimYear
			   ORDER BY MAX(d.createdAt) DESC
			""")
	List<Object[]> findAllWorksheetsGroupsByUserClaimRefAndYear();

	@EntityGraph(attributePaths = { "exportModels" })
	List<Worksheet> findByUsernameAndClaimRefNoAndClaimYearOrderByCreatedAtDesc(String username, String claimRefNo,
			String claimYear);

	@EntityGraph(attributePaths = "exportModels")
	Optional<Worksheet> findByWorksheetId(Long worksheetId);

}
