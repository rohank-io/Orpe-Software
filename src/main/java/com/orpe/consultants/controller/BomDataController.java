package com.orpe.consultants.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.orpe.consultants.dto.BomDataDTO;
import com.orpe.consultants.utils.BomDataExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BomDataController {

    private final BomDataExtractor excelBomService;

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
}

