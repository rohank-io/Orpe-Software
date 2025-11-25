package com.orpe.consultants.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestParam;

import com.orpe.consultants.dto.ExportDataDTO;
import com.orpe.consultants.dto.ExportDataFilter;
import com.orpe.consultants.model.User;
import com.orpe.consultants.service.ExportDataService;


import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SbWiseDbkCalculationController {
	
	private final ExportDataService exportDataService;
	
	
	@GetMapping("/dbkcalculation/dataselect")
	public String showImportDataList(@RequestParam(required = false) String filterField,
			@RequestParam(required = false) String filterValue, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "1000") int size, HttpSession session, Model model) {

		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			log.info("User not authenticated, redirecting to login page");
			return "redirect:/login";
		}

		model.addAttribute("user", loggedInUser);

		ExportDataFilter.ExportDataFilterBuilder filterBuilder = ExportDataFilter.builder();

		if (filterField != null && filterValue != null && !filterValue.isBlank()) {
			switch (filterField) {
			case "sbNo":
				filterBuilder.sbNo(filterValue);
				break;

			case "sbDateFrom":
				try {
					LocalDate dateValue = LocalDate.parse(filterValue, DateTimeFormatter.ISO_DATE);
					filterBuilder.sbDateFrom(dateValue);
				} catch (DateTimeParseException e) {
					// Log or handle invalid date format gracefully
					log.warn("Invalid beDate format: " + filterValue);
				}
				break;
				
			case "sbDateTo":
				try {
					LocalDate dateValue = LocalDate.parse(filterValue, DateTimeFormatter.ISO_DATE);
					filterBuilder.sbDateTo(dateValue);
				} catch (DateTimeParseException e) {
					// Log or handle invalid date format gracefully
					log.warn("Invalid beDate format: " + filterValue);
				}
				break;
			// Add more supported filters here
			}
		}

		ExportDataFilter filter = filterBuilder.build();

		Pageable pageable = PageRequest.of(page, size, Sort.by("sbDate").descending());
		Page<ExportDataDTO> resultPage = exportDataService.search(filter, pageable);

		model.addAttribute("exportDataPage", resultPage);
		model.addAttribute("filterField", filterField);
		model.addAttribute("filterValue", filterValue);
		model.addAttribute("currentPage", page);
		model.addAttribute("pageSize", size);

		return "dbkCalculationDataSelect";
	}
	
	

}
