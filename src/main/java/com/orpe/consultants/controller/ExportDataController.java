package com.orpe.consultants.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.orpe.consultants.dto.ExportDataDTO;
import com.orpe.consultants.dto.ExportDataFilter;
//import com.orpe.consultants.dto.ImportDataDTO;
//import com.orpe.consultants.dto.ImportDataFilter;
import com.orpe.consultants.model.User;
import com.orpe.consultants.service.ExportDataService;

import com.orpe.consultants.utils.ExportDataExtractor;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ExportDataController {

	private final ExportDataExtractor excelExportService;

	private final ExportDataService exportDataService;

	@PostMapping("/exportdata/exportExcel")
	public String importExcel(@RequestParam("file") MultipartFile file, Model model) {
		try {
			List<ExportDataDTO> rows = excelExportService.parseExportSheet(file);

			model.addAttribute("exportRows", rows);
			model.addAttribute("rowCount", rows.size());
			model.addAttribute("exportFileName", file.getOriginalFilename());

			if (rows.isEmpty()) {
				model.addAttribute("error", "No rows could be parsed from Excel. Please check column headers.");
			}

		} catch (Exception ex) {
			model.addAttribute("error", "Failed to parse Excel: " + ex.getMessage());
		}
		return "uploadExportData"; // ✅ make sure this matches your template filename
	}

	@GetMapping("/exportdata/list")
	public String showImportDataList(@RequestParam(required = false) String filterField,
			@RequestParam(required = false) String filterValue, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "100") int size, HttpSession session, Model model) {

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

		return "exportDataList";
	}

	@GetMapping("/exportdata/delete/{exportId}")
	public String deleteUser(@PathVariable Long exportId, @RequestParam(required = false) String filterField,
			@RequestParam(required = false) String filterValue, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "100") int size, HttpSession session) {

		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			return "redirect:/login";
		}

		try {
			exportDataService.deleteById(exportId);
			return "redirect:/exportdata/list?success=Export Data deleted successfully" + "&filterField="
					+ (filterField != null ? filterField : "") + "&filterValue="
					+ (filterValue != null ? filterValue : "") + "&page=" + page + "&size=" + size;
		} catch (Exception e) {
			return "redirect:/exportdata/list?error=Error deleting export data" + "&filterField="
					+ (filterField != null ? filterField : "") + "&filterValue="
					+ (filterValue != null ? filterValue : "") + "&page=" + page + "&size=" + size;
		}
	}

	@PostMapping(path = "/exportdata/bulk-save", consumes = "application/json", produces = "application/json")
	@ResponseBody
	public Map<String, Object> bulkSave(@RequestBody List<ExportDataDTO> rows) {
		int saved = exportDataService.saveBulk(rows);
		return Map.of("savedCount", saved);
	}
}
