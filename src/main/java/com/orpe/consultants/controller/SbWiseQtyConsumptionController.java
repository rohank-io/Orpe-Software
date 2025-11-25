package com.orpe.consultants.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.orpe.consultants.dto.BomClaimDTO;
import com.orpe.consultants.dto.BomClaimFilter;
import com.orpe.consultants.dto.ImportDataDTO;
import com.orpe.consultants.dto.ImportDataFilter;
import com.orpe.consultants.dto.SbWiseQuantityConsumptionDTO;
import com.orpe.consultants.model.User;
import com.orpe.consultants.service.BomClaimService;
import com.orpe.consultants.service.SbWiseQtyConsumptionService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SbWiseQtyConsumptionController {

	private final BomClaimService bomClaimService;
	private final SbWiseQtyConsumptionService sbWiseQtyConsumptionService;

	@GetMapping("/sbwiseqtyconsumption/bomclaimdata/select")
	public String showBomDataList(@RequestParam(required = false) String filterField,
			@RequestParam(required = false) String filterValue, HttpSession session, Model model) {

		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			log.info("User not authenticated, redirecting to login page");
			return "redirect:/login";
		}

		model.addAttribute("user", loggedInUser);

		BomClaimFilter.BomClaimFilterBuilder filterBuilder = BomClaimFilter.builder();

		if (filterField != null && filterValue != null && !filterValue.isBlank()) {
			switch (filterField) {
			case "bomPartNo":
				filterBuilder.bomPartNo(filterValue);
				break;
			case "clientName":
				filterBuilder.clientName(filterValue);
				break;
			case "claimYear":
				filterBuilder.claimYear(filterValue);
				break;
			case "claimRefNo":
				filterBuilder.claimRefNo(filterValue);
				break;

			// Add more supported filters here
			}
		}

		BomClaimFilter filter = filterBuilder.build();

		List<BomClaimDTO> resultPage = bomClaimService.search(filter);

		model.addAttribute("bomDataPage", resultPage);
		model.addAttribute("filterField", filterField);
		model.addAttribute("filterValue", filterValue);

		return "uploadSbQtyConsumption";
	}

	@PostMapping("/sb-qty-consumption/bulk-save")
	public ResponseEntity<Map<String, Object>> saveBulk(@RequestBody List<SbWiseQuantityConsumptionDTO> rows) {
		try {
			int savedCount = sbWiseQtyConsumptionService.saveBulk(rows);

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("savedCount", savedCount);
			response.put("message", "Saved " + savedCount + " records successfully.");

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> error = new HashMap<>();
			error.put("status", "error");
			error.put("message", "Failed to save data: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
		}
	}

	@GetMapping("/sb-consumption/list")
	public String showQtyConsumptionList(@RequestParam(required = false) String filterField,
			@RequestParam(required = false) String filterValue,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

			HttpSession session, Model model) {

		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			log.info("User not authenticated, redirecting to login page");
			return "redirect:/login";
		}

		model.addAttribute("user", loggedInUser);

		BomClaimFilter filter = new BomClaimFilter();
		filter.setFilterField(filterField);
		filter.setFilterValue(filterValue);
		filter.setFromDate(fromDate);
		filter.setToDate(toDate);

		List<SbWiseQuantityConsumptionDTO> resultPage = sbWiseQtyConsumptionService.search(filter);

		model.addAttribute("qtyConsumptionPage", resultPage);
		model.addAttribute("filterField", filterField);
		model.addAttribute("filterValue", filterValue);
		model.addAttribute("fromDate", fromDate);
		model.addAttribute("toDate", toDate);

		return "sbQtyConsumptionList";
	}

	@GetMapping("/sb-consumption/groups")
	public String showGrouped(HttpSession session, Model model) {

		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			log.info("User not authenticated, redirecting to login page");
			return "redirect:/login";
		}

		model.addAttribute("user", loggedInUser);

		List<SbWiseQuantityConsumptionDTO> groups = sbWiseQtyConsumptionService.getGroupedByClaimRefNoAndClaimYear();
		model.addAttribute("groups", groups);
		return "sbQtyConsumptionListGrouped"; // templates/sb-qty/groups.html
	}
	
	
	
	@GetMapping("/sb-consumption/detail")
    public ResponseEntity<List<SbWiseQuantityConsumptionDTO>> getDetail(
            @RequestParam("claimRefNo") String claimRefNo,
            @RequestParam("claimYear") String claimYear) {

        if (claimRefNo == null || claimRefNo.isBlank() ||
            claimYear == null || claimYear.isBlank()) {
            log.warn("Detail endpoint called with invalid params: claimRefNo='{}', claimYear='{}'",
                     claimRefNo, claimYear);
            return ResponseEntity.badRequest().body(List.of());
        }

        List<SbWiseQuantityConsumptionDTO> data =
                sbWiseQtyConsumptionService.getDetailsByClaimRefNoAndClaimYear(claimRefNo, claimYear);

        return ResponseEntity.ok(data);
    }

}
