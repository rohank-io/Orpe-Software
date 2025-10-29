package com.orpe.consultants.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.orpe.consultants.dto.BomClaimDTO;
import com.orpe.consultants.dto.BomDataDTO;
import com.orpe.consultants.exception.ExcelParseException;
import com.orpe.consultants.service.BomClaimService;
import com.orpe.consultants.utils.BomClaimExtractor;
import com.orpe.consultants.utils.ParseError;
import com.orpe.consultants.utils.ParseResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BomClaimController {
	
	private final BomClaimExtractor bomClaimExtractorService;
	
	private final BomClaimService bomClaimService;
	
	@PostMapping("/bomclaim/importExcel")
	public String claimExcel(@RequestParam("file") MultipartFile file, Model model) {
	    if (file == null || file.isEmpty()) {
	        model.addAttribute("error", "No file selected. Please upload a valid Excel file.");
	        return "uploadBomClaim";
	    }

	    try {
	        // ✅ Parse Excel using extractor — now returns ParseResult
	        ParseResult<BomClaimDTO> result = 
	                bomClaimExtractorService.parseImportSheet(file, LocaleContextHolder.getLocale());

	        List<BomClaimDTO> rows = result.getRows();
	        List<ParseError> errors = result.getErrors();

	        // ✅ Add parsed data
	        model.addAttribute("bomRows", rows);
	        model.addAttribute("rowCount", rows.size());
	        model.addAttribute("bomFileName", file.getOriginalFilename());

	        // ✅ Handle validation / parse issues
	        if (!errors.isEmpty()) {
	            model.addAttribute("parseErrors", errors);
	        }

	        if (rows.isEmpty() && errors.isEmpty()) {
	            model.addAttribute("error", "No data found in Excel. Please check column headers and try again.");
	        }

	    } catch (ExcelParseException ex) {
	        log.error("Excel parse failure: {}", ex.getMessage(), ex);
	        model.addAttribute("error", "Error reading Excel file: " + ex.getMessage());
	    } catch (Exception ex) {
	        log.error("Unexpected error parsing Excel: {}", ex.getMessage(), ex);
	        model.addAttribute("error", "Unexpected failure: " + ex.getMessage());
	    }

	    return "uploadBomClaim"; // Thymeleaf/JSP page
	}
	
	@PostMapping("/bomclaim/bulk-save")
	public ResponseEntity<Map<String, Object>> saveBulk(@RequestBody List<BomClaimDTO> rows) {
	    try {
	        int savedCount = bomClaimService.saveBulk(rows);

	        Map<String, Object> response = new HashMap<>();
	        response.put("status", "success");
	        response.put("savedCount", savedCount);
	        response.put("message", "Saved " + savedCount + " BOM rows successfully.");

	        return ResponseEntity.ok(response);
	    } catch (Exception e) {
	        Map<String, Object> error = new HashMap<>();
	        error.put("status", "error");
	        error.put("message", "Failed to save BOM data: " + e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	    }
	}

	
	
	


}
