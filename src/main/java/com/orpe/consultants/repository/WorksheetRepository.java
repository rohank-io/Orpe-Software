package com.orpe.consultants.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orpe.consultants.model.Worksheet;

public interface WorksheetRepository extends JpaRepository<Worksheet, Long> {

}
