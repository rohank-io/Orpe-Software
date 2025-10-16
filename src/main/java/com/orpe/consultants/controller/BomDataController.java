package com.orpe.consultants.controller;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.orpe.consultants.dto.BomDataDTO;
import com.orpe.consultants.dto.BomDataFilter;
import com.orpe.consultants.model.User;
import com.orpe.consultants.service.BomDataService;
import com.orpe.consultants.utils.BomDataExtractor;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BomDataController {

    private final BomDataExtractor excelBomService;
    
    private final BomDataService bomDataService;
    

    @PostMapping("/bomdata/importExcel")
    public String importExcel(@RequestParam("file") MultipartFile file, Model model) {
        try {
            // Parse BOM data from Excel file using BomDataExtractor
            List<BomDataDTO> rows = excelBomService.parseBomSheet(file);

            model.addAttribute("bomRows", rows);
            model.addAttribute("rowCount", rows.size());
            model.addAttribute("bomFileName", file.getOriginalFilename());

            if (rows.isEmpty()) {
                model.addAttribute("error", "No rows could be parsed from Excel. Please check column headers.");
            }

        } catch (Exception ex) {
            log.error("Error parsing BOM Excel file", ex);
            model.addAttribute("error", "Failed to parse Excel: " + ex.getMessage());
        }
        return "uploadBomData";  // Your Thymeleaf or JSP template name
    }
    
    
    @PostMapping("/bomdata/saveBulk")
    public ResponseEntity<?> saveBulk(@RequestBody List<BomDataDTO> rows) {
        int savedCount = bomDataService.saveBulk(rows);
        return ResponseEntity.ok("Saved " + savedCount + " BOM rows!");
    }
    
    
    
    @GetMapping("/bomdata/list")
	public String showBomDataList(@RequestParam(required = false) String filterField,
			@RequestParam(required = false) String filterValue, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, HttpSession session, Model model) {

		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			log.info("User not authenticated, redirecting to login page");
			return "redirect:/login";
		}

		model.addAttribute("user", loggedInUser);

		BomDataFilter.BomDataFilterBuilder filterBuilder = BomDataFilter.builder();

		if (filterField != null && filterValue != null && !filterValue.isBlank()) {
			switch (filterField) {
			case "bomPartNo":
				filterBuilder.bomPartNo(filterValue);
				break;
			case "clientName":
				filterBuilder.clientName(filterValue);
				break;
			case "status":
				filterBuilder.status(filterValue);
				break;	
			
			// Add more supported filters here
			}
		}

		BomDataFilter filter = filterBuilder.build();

		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<BomDataDTO> resultPage = bomDataService.search(filter, pageable);

		model.addAttribute("bomDataPage", resultPage);
		model.addAttribute("filterField", filterField);
		model.addAttribute("filterValue", filterValue);
		model.addAttribute("currentPage", page);
		model.addAttribute("pageSize", size);

		return "bomDataList";
	}

	@GetMapping("/bomdata/delete/{bomId}")
	public String deleteBom(@PathVariable Long bomId, @RequestParam(required = false) String filterField,
			@RequestParam(required = false) String filterValue, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "100") int size, HttpSession session) {

		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			return "redirect:/login";
		}

		try {
			bomDataService.deleteById(bomId);
			return "redirect:/bomdata/list?success=BOM Data deleted successfully" + "&filterField="
					+ (filterField != null ? filterField : "") + "&filterValue="
					+ (filterValue != null ? filterValue : "") + "&page=" + page + "&size=" + size;
		} catch (Exception e) {
			return "redirect:/bomdata/list?error=Error deleting bom data" + "&filterField="
					+ (filterField != null ? filterField : "") + "&filterValue="
					+ (filterValue != null ? filterValue : "") + "&page=" + page + "&size=" + size;
		}
	}


}

