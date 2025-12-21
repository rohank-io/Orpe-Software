package com.orpe.consultants.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
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

import jakarta.servlet.http.HttpServletResponse;
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

	/**
	 * Show export data list with a single-field filter (filterField + filterValue).
	 * Supported filterField values: sbNo, sbDateFrom, sbDateTo, claimYear,
	 * customerName, portCode, claimRefNo, modelNo, dbkSno, sbUtilization
	 */
	@GetMapping("/exportdata/list")
	public String showExportDataList(@RequestParam(required = false) String filterField,
			@RequestParam(required = false) String filterValue, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "100") int size, HttpSession session, Model model) {

		// Auth check
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			log.info("User not authenticated, redirecting to login page");
			return "redirect:/login";
		}
		model.addAttribute("user", loggedInUser);

		// Build filter using builder pattern
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
	
	
	//*****************TO DOWNLOAD THE EXCEL FILE OF EXPORT MATERIAL**************
	@GetMapping("/exportdata/excel/download")
	public String downloadExportDataList(@RequestParam(required = false) String filterField,
			@RequestParam(required = false) String filterValue, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "100") int size, HttpSession session, Model model) {

		// Auth check
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			log.info("User not authenticated, redirecting to login page");
			return "redirect:/login";
		}
		model.addAttribute("user", loggedInUser);

		// Build filter using builder pattern
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

		return "exportDataExcelDownload";
	}
	
	
	@PostMapping("/exportdata/downloadExcel")
	public void downloadExcel(
	        @RequestParam String selectedIds,
	        HttpServletResponse response) throws IOException {

	    List<Long> ids = Arrays.stream(selectedIds.split(","))
	            .map(Long::valueOf)
	            .toList();

	    response.setContentType(
	        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	    response.setHeader(
	        "Content-Disposition", "attachment; filename=Export_Data.xlsx");

	    exportDataService.writeExportExcel(ids, response.getOutputStream());
	}

}
