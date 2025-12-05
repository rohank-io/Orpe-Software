package com.orpe.consultants.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.orpe.consultants.dto.ExportDataDTO;
import com.orpe.consultants.dto.ExportDataFilter;
import com.orpe.consultants.dto.SbWiseDbkCalculationDTO;
import com.orpe.consultants.model.User;
import com.orpe.consultants.service.ExportDataService;
import com.orpe.consultants.service.SbWiseDbkCalculationService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SbWiseDbkCalculationController {
	
	private final ExportDataService exportDataService;
	private final SbWiseDbkCalculationService dbkCalculationService;
	
	
	@GetMapping("/dbkcalculation/dataselect")
	public String showDbkCalculationDataSelect(
	        @RequestParam(required = false) String filterField,
	        @RequestParam(required = false) String filterValue,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "1000") int size,
	        HttpSession session,
	        Model model) {

	    User loggedInUser = (User) session.getAttribute("loggedInUser");
	    if (loggedInUser == null) {
	        log.info("User not authenticated, redirecting to login page");
	        return "redirect:/login";
	    }
	    model.addAttribute("user", loggedInUser);

	    ExportDataFilter.ExportDataFilterBuilder builder = ExportDataFilter.builder();

	    if (filterField != null && filterValue != null) {
	        String v = filterValue.trim();
	        if (!v.isBlank()) {
	            switch (filterField) {
	                case "sbNo":
	                    builder.sbNo(v);
	                    break;
	                case "sbDateFrom":
	                    try {
	                        LocalDate from = LocalDate.parse(v, DateTimeFormatter.ISO_DATE);
	                        builder.sbDateFrom(from);
	                    } catch (DateTimeParseException ex) {
	                        log.warn("Invalid sbDateFrom value: {}", v);
	                    }
	                    break;
	                case "sbDateTo":
	                    try {
	                        LocalDate to = LocalDate.parse(v, DateTimeFormatter.ISO_DATE);
	                        builder.sbDateTo(to);
	                    } catch (DateTimeParseException ex) {
	                        log.warn("Invalid sbDateTo value: {}", v);
	                    }
	                    break;
	                // optional: commonly useful filters (add/remove as required)
	                case "claimYear":
	                    builder.claimYear(v);
	                    break;
	                case "customerName":
	                    builder.customerName(v);
	                    break;
	                case "portCode":
	                    builder.portCode(v);
	                    break;
	                case "claimRefNo":
	                    builder.claimRefNo(v);
	                    break;
	                case "modelNo":
	                    builder.modelNo(v);
	                    break;
	                case "dbkSno":
	                    builder.dbkSno(v);
	                    break;
	                case "sbUtilization":
	                    builder.sbUtilization(v);
	                    break;
	                default:
	                    log.debug("Unknown filterField requested: {}", filterField);
	            }
	        }
	    }

	    ExportDataFilter filter = builder.build();

	    Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("sbDate").descending());
	    Page<ExportDataDTO> resultPage = exportDataService.search(filter, pageable);

	    model.addAttribute("exportDataPage", resultPage);
	    model.addAttribute("filterField", filterField);
	    model.addAttribute("filterValue", filterValue);
	    model.addAttribute("currentPage", page);
	    model.addAttribute("pageSize", size);

	    return "dbkCalculationDataSelect";
	}

	
	
	@PostMapping("/dbkcalculation/datacalculate")
    public String calculateForSelectedExports(
            @RequestParam(value = "selectedExportIds", required = false) List<Long> selectedExportIds,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        // 1) login check
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            log.warn("Unauthenticated access to /dbkcalculation/datacalculate — redirecting to login.");
            return "redirect:/login";
        }

        // 2) validate selection
        if (selectedExportIds == null || selectedExportIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("warning", "Please select at least one export row before calculating.");
            return "redirect:/dbkcalculation/dataselect";
        }

        try {
            // 3) perform calculation
            List<SbWiseDbkCalculationDTO> rows = dbkCalculationService.calculateForExportIds(selectedExportIds, loggedInUser);

            if (rows == null) rows = Collections.emptyList();

            // 4) add results to model and forward to result view
            model.addAttribute("calculationRows", rows);
            model.addAttribute("count", rows.size());
            model.addAttribute("selectedExportIds", selectedExportIds);

            // you may re-use the same selection page or show a dedicated result template
            return "dbkCalculationDataEdit"; // <-- change to your result template
        } catch (Exception ex) {
            log.error("Error while calculating DBK for exports: {}", selectedExportIds, ex);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred while calculating. See logs for details.");
            return "redirect:/dbkcalculation/dataselect";
        }
    }
	
	

}
