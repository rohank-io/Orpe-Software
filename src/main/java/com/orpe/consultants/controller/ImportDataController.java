package com.orpe.consultants.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.orpe.consultants.dto.ImportDataDTO;
import com.orpe.consultants.dto.ImportDataFilter;
import com.orpe.consultants.model.User;
import com.orpe.consultants.service.ImportDataService;
import com.orpe.consultants.utils.ImportDataExtractor;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ImportDataController {
	private final ImportDataExtractor excelImportService;
	
	private final ImportDataService importDataService;

	  
	@PostMapping("/importdata/importExcel")
	public String importExcel(@RequestParam("file") MultipartFile file, Model model) {
	    try {
	        List<ImportDataDTO> rows = excelImportService.parseImportSheet(file);

	        model.addAttribute("importRows", rows);
	        model.addAttribute("rowCount", rows.size());
	        model.addAttribute("importFileName", file.getOriginalFilename());

	        if (rows.isEmpty()) {
	            model.addAttribute("error", "No rows could be parsed from Excel. Please check column headers.");
	        }

	    } catch (Exception ex) {
	        model.addAttribute("error", "Failed to parse Excel: " + ex.getMessage());
	    }
	    return "uploadImportData"; // ✅ make sure this matches your template filename
	}



	  @GetMapping("/pdfimport")
	  public String importPage() {
	    return "uploadImportData";
	  }
	  
	  
	  @GetMapping("/importdata/list")
	  public String showImportDataList(
	      @RequestParam(required = false) String filterField,
	      @RequestParam(required = false) String filterValue,
	      @RequestParam(defaultValue = "0") int page,
	      @RequestParam(defaultValue = "100") int size,
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

	        case "beDate":
	          try {
	            LocalDate dateValue = LocalDate.parse(filterValue, DateTimeFormatter.ISO_DATE);
	            filterBuilder.beDate(dateValue);
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

	    return "importDataList";
	  }
	  
	  
	  @GetMapping("/importdata/delete/{importId}")
	  public String deleteUser(
	      @PathVariable Long importId, 
	      @RequestParam(required = false) String filterField,
	      @RequestParam(required = false) String filterValue,
	      @RequestParam(defaultValue = "0") int page,
	      @RequestParam(defaultValue = "100") int size,
	      HttpSession session) {

	      User loggedInUser = (User) session.getAttribute("loggedInUser");
	      if (loggedInUser == null) {
	          return "redirect:/login";
	      }

	      try {
	          importDataService.deleteById(importId);
	          return "redirect:/importdata/list?success=Import Data deleted successfully"
	                 + "&filterField=" + (filterField != null ? filterField : "")
	                 + "&filterValue=" + (filterValue != null ? filterValue : "")
	                 + "&page=" + page
	                 + "&size=" + size;
	      } catch (Exception e) {
	          return "redirect:/importdata/list?error=Error deleting import data"
	                 + "&filterField=" + (filterField != null ? filterField : "")
	                 + "&filterValue=" + (filterValue != null ? filterValue : "")
	                 + "&page=" + page
	                 + "&size=" + size;
	      }
	  }





	  
	  
	  
	  @PostMapping(path = "/importdata/bulk-save", consumes = "application/json", produces = "application/json")
	  @ResponseBody
	  public Map<String, Object> bulkSave(@RequestBody List<ImportDataDTO> rows) {
	    int saved = importDataService.saveBulk(rows);
	    return Map.of("savedCount", saved);
	  }
	}



