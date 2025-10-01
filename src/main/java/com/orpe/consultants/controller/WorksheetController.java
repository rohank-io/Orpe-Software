package com.orpe.consultants.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

import com.orpe.consultants.dto.ImportDataDTO;
import com.orpe.consultants.dto.ImportDataFilter;
import com.orpe.consultants.model.ImportData;
import com.orpe.consultants.model.User;
import com.orpe.consultants.service.ImportDataService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WorksheetController {
	
	private final ImportDataService importDataService;
	
	
	@GetMapping({"/worksheet/importdata/select"})
    public String worksheetdataSelect( @RequestParam(required = false) String filterField,
  	      @RequestParam(required = false) String filterValue,
  	      @RequestParam(defaultValue = "0") int page,
  	      @RequestParam(defaultValue = "200") int size,
  	      HttpSession session,
  	      Model model) {

  	    User loggedInUser = (User) session.getAttribute("loggedInUser");
  	    if (loggedInUser == null) {
  	      log.info("User not authenticated, redirecting to login page");
  	      return "redirect:/login";
  	    }

  	    model.addAttribute("user", loggedInUser);

  	    ImportDataFilter.ImportDataFilterBuilder filterBuilder = ImportDataFilter.builder();

  	    if (filterField != null && filterValue != null && !filterValue.isBlank()) {
  	      switch (filterField) {
  	        case "beNo":
  	          filterBuilder.beNo(filterValue);
  	          break;
  	          
  	      case "claimYear":
	          filterBuilder.claimYear(filterValue);
	          break;  

  	        case "beDateFrom":
  	          try {
  	            LocalDate dateValue = LocalDate.parse(filterValue, DateTimeFormatter.ISO_DATE);
  	            filterBuilder.beDateFrom(dateValue);
  	          } catch (DateTimeParseException e) {
  	            // Log or handle invalid date format gracefully
  	            log.warn("Invalid beDate format: " + filterValue);
  	          }
  	          break;
  	          
  	        case "beDateTo":
  		          try {
  		            LocalDate dateValue = LocalDate.parse(filterValue, DateTimeFormatter.ISO_DATE);
  		            filterBuilder.beDateTo(dateValue);
  		          } catch (DateTimeParseException e) {
  		            // Log or handle invalid date format gracefully
  		            log.warn("Invalid beDate format: " + filterValue);
  		          }
  		          break;
  	        // Add more supported filters here
  	      }
  	    }

  	    ImportDataFilter filter = filterBuilder.build();

  	    Pageable pageable = PageRequest.of(page, size, Sort.by("beDate").descending());
  	    Page<ImportDataDTO> resultPage = importDataService.search(filter, pageable);

  	    model.addAttribute("importDataPage", resultPage);
  	    model.addAttribute("filterField", filterField);
  	    model.addAttribute("filterValue", filterValue);
  	    model.addAttribute("currentPage", page);
  	    model.addAttribute("pageSize", size);
        return "worksheetDataSelect";
    }
	
	
	@PostMapping("/worksheet/importdata/edit")
    public String editSelectedImportData(
            @RequestParam(name = "selectedRows", required = false) List<Long> selectedImportIds,
            Model model,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", loggedInUser);

        if (selectedImportIds == null || selectedImportIds.isEmpty()) {
            model.addAttribute("errorMessage", "No rows selected. Please select rows before clicking Calculate.");
            // Ideally redirect back or reload selection page
            return "redirect:/worksheet/importdata/select";
        }

        // Fetch selected import data rows by IDs
        List<ImportDataDTO> worksheetDetails = importDataService.fetchImportDataWithExportModels(selectedImportIds);
        model.addAttribute("importDataDetails", worksheetDetails);

        // Pass the selected list to the editing view
        

        // Return the Thymeleaf edit page for worksheet data
        return "worksheetDataEdit";
    }

}
