package com.orpe.consultants.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orpe.consultants.dto.DraftWorksheetDTO;
import com.orpe.consultants.dto.ImportDataDTO;
import com.orpe.consultants.dto.ImportDataFilter;
import com.orpe.consultants.dto.WorksheetDTO;
import com.orpe.consultants.dto.WorksheetDataFilter;
import com.orpe.consultants.model.DraftWorksheet;
import com.orpe.consultants.model.ImportData;
import com.orpe.consultants.model.User;
import com.orpe.consultants.model.Worksheet;
import com.orpe.consultants.service.DraftWorksheetService;
import com.orpe.consultants.service.ImportDataService;
import com.orpe.consultants.service.WorksheetService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WorksheetController {

	private final ImportDataService importDataService;
	private final WorksheetService worksheetService;
	private final DraftWorksheetService draftWorksheetService;
	private final ObjectMapper mapper;

	@GetMapping("/worksheet/importdata/select")
	public String worksheetdataSelect(@RequestParam(required = false) String filterField,
			@RequestParam(required = false) String filterValue,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "1000") int size,
			HttpSession session, Model model) {

		// Authentication check omitted for brevity
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			return "redirect:/login";
		}
		model.addAttribute("user", loggedInUser);

		ImportDataFilter filter = new ImportDataFilter();
		filter.setFilterField(filterField);
		filter.setFilterValue(filterValue);
		filter.setFromDate(fromDate);
		filter.setToDate(toDate);

		Pageable pageable = PageRequest.of(page, size, Sort.by("beDate").descending());
		Page<ImportDataDTO> resultPage = importDataService.findWithPositiveClosingBalance(filter, pageable);

		model.addAttribute("importDataPage", resultPage);
		model.addAttribute("filterField", filterField);
		model.addAttribute("filterValue", filterValue);
		model.addAttribute("fromDate", fromDate);
		model.addAttribute("toDate", toDate);
		model.addAttribute("currentPage", page);
		model.addAttribute("pageSize", size);

		return "worksheetDataSelect";
	}

	@PostMapping("/worksheet/importdata/edit")
	public String editSelectedImportData(
			@RequestParam(name = "selectedRows", required = false) List<Long> selectedImportIds, Model model,
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

	@PostMapping("/worksheet/saveBulk")
	public ResponseEntity<?> saveBulk(@RequestBody List<WorksheetDTO> worksheetDTOList, HttpSession session,
			@Autowired ObjectMapper mapper) { // <-- Inject Spring's configured mapper

		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
		}

		if (worksheetDTOList == null || worksheetDTOList.isEmpty()) {
			return ResponseEntity.badRequest().body("No worksheet data to save");
		}

		// ✅ Use the injected mapper (already has JavaTimeModule)
		try {
			String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(worksheetDTOList);
			System.out.println("Incoming worksheetDTOList payload:\n" + json);
		} catch (Exception e) {
			System.out.println("Failed to print incoming worksheetDTOList: " + e.getMessage());
		}

		try {
			worksheetService.saveBulkWorksheets(worksheetDTOList);
			return ResponseEntity.ok("Bulk save successful");
		} catch (ConstraintViolationException ex) {
			ex.getConstraintViolations().forEach(
					cv -> System.err.println("Validation failed at " + cv.getPropertyPath() + ": " + cv.getMessage()));
			return ResponseEntity.badRequest().body("Validation errors occurred. See server logs for details.");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error during bulk save: " + e.getMessage());
		}
	}

	@GetMapping("/worksheet/list")
	public String worksheetdataList(@RequestParam(required = false) String filterField,
			@RequestParam(required = false) String filterValue,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size,
			HttpSession session, Model model) {

		// Authentication check omitted for brevity
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			return "redirect:/login";
		}
		model.addAttribute("user", loggedInUser);

		WorksheetDataFilter filter = new WorksheetDataFilter();
		filter.setFilterField(filterField);
		filter.setFilterValue(filterValue);
		filter.setFromDate(fromDate);
		filter.setToDate(toDate);

		Pageable pageable = PageRequest.of(page, size, Sort.by("beDate").descending());
		Page<WorksheetDTO> resultPage = worksheetService.search(filter, pageable);

		model.addAttribute("importDataPage", resultPage);
		model.addAttribute("filterField", filterField);
		model.addAttribute("filterValue", filterValue);
		model.addAttribute("fromDate", fromDate);
		model.addAttribute("toDate", toDate);
		model.addAttribute("currentPage", page);
		model.addAttribute("pageSize", size);

		return "worksheetDataList";
	}

	@PostMapping("/worksheet/saveDraft")
	public ResponseEntity<?> saveDraft(@RequestBody List<DraftWorksheetDTO> draftWorksheetDTOList,
			HttpSession session) {

		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
		}

		if (draftWorksheetDTOList == null || draftWorksheetDTOList.isEmpty()) {
			return ResponseEntity.badRequest().body("No draft worksheet data to save");
		}

		try {
			// ✅ Now uses globally configured ObjectMapper (has JavaTimeModule)
			String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(draftWorksheetDTOList);
			System.out.println("Incoming draftWorksheetDTOList payload:\n" + json);
		} catch (Exception e) {
			System.out.println("Failed to print incoming draftWorksheetDTOList: " + e.getMessage());
		}

		try {
			draftWorksheetService.saveBulkWorksheets(draftWorksheetDTOList);
			return ResponseEntity.ok("Draft save successful");
		} catch (ConstraintViolationException ex) {
			ex.getConstraintViolations().forEach(
					cv -> System.err.println("Validation failed at " + cv.getPropertyPath() + ": " + cv.getMessage()));
			return ResponseEntity.badRequest().body("Validation errors occurred. See server logs for details.");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error during draft save: " + e.getMessage());
		}
	}
	
	
	@GetMapping("/worksheet/selected")
    public String listWorksheetsForGroup(@RequestParam String username,
                                     @RequestParam String claimRefNo,
                                     @RequestParam String claimYear,
                                     HttpSession session,
                                     Model model) {

        // 1. LOGIN CHECK
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            log.warn("Unauthenticated access to /draftworksheet/list — redirecting to login.");
            return "redirect:/login";
        }

        // 2. ROLE CHECK (optional)
        // Allow ADMIN or the owner (username match)
       
        // 3. Fetch the draft records
        List<Worksheet> worksheetList = worksheetService.getWorksheetByUserAndClaimRefAndYear(username, claimRefNo, claimYear);

        // 4. Add data to model
        model.addAttribute("worksheetList", worksheetList);
        model.addAttribute("selectedUsername", username);
        model.addAttribute("claimRefNo", claimRefNo);
        model.addAttribute("claimYear", claimYear);
        model.addAttribute("count", worksheetList != null ? worksheetList.size() : 0);

        log.info("Loaded {}  worksheet(s) for user={}, refNo={}, year={}", 
        		worksheetList.size(), username, claimRefNo, claimYear);

        // 5. Return the list page
        return "worksheetDataUserwise";
    }

	@PostMapping("/draftworksheet/updateBulk")
	public ResponseEntity<?> updateBulkDrafts(@RequestBody List<DraftWorksheetDTO> drafts) {
		try {
			draftWorksheetService.updateBulkDrafts(drafts);
			return ResponseEntity.ok("Updated successfully");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
	}

	@GetMapping("/draftworksheet/list")
	public String draftWorksheetdataList(HttpSession session, Model model) {
		// Authentication check
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			return "redirect:/login";
		}
		model.addAttribute("user", loggedInUser);

		// Fetch all draft worksheets (service should call repository.findAll() which
		// uses @EntityGraph)
		List<DraftWorksheetDTO> draftList = draftWorksheetService.findAll();

		// Put the list into the model for the view
		model.addAttribute("draftList", draftList);

		return "worksheetDraftList"; // or whichever Thymeleaf view you use for drafts
	}
	
	
	// --- LIST PAGE FOR A SINGLE GROUP ---
    @GetMapping("/draftworksheet/selected")
    public String listDraftsForGroup(@RequestParam String username,
                                     @RequestParam String claimRefNo,
                                     @RequestParam String claimYear,
                                     HttpSession session,
                                     Model model) {

        // 1. LOGIN CHECK
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            log.warn("Unauthenticated access to /draftworksheet/list — redirecting to login.");
            return "redirect:/login";
        }

        // 2. ROLE CHECK (optional)
        // Allow ADMIN or the owner (username match)
       
        // 3. Fetch the draft records
        List<DraftWorksheet> draftList = draftWorksheetService.getDraftsByUserAndClaimRefAndYear(username, claimRefNo, claimYear);

        // 4. Add data to model
        model.addAttribute("draftList", draftList);
        model.addAttribute("selectedUsername", username);
        model.addAttribute("claimRefNo", claimRefNo);
        model.addAttribute("claimYear", claimYear);
        model.addAttribute("count", draftList != null ? draftList.size() : 0);

        log.info("Loaded {} draft worksheet(s) for user={}, refNo={}, year={}", 
        		draftList.size(), username, claimRefNo, claimYear);

        // 5. Return the list page
        return "worksheetDraftSelected";
    }
    
    
    
    @GetMapping("/drafts")
	public String draftWorksheet(HttpSession session, Model model) {
		// Authentication check
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			return "redirect:/login";
		}
		model.addAttribute("user", loggedInUser);

		 List<Map<String, Object>> draftGroups = draftWorksheetService.getAllDraftGroups();
	        model.addAttribute("draftGroups", draftGroups);


		return "worksheetDraft"; // or whichever Thymeleaf view you use for drafts
	}
}


