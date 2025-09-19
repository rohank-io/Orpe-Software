package com.orpe.consultants.controller;

//import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.orpe.consultants.model.ShippingBill;
import com.orpe.consultants.service.UserService;
import com.orpe.consultants.utils.ExcelShippingBillExtractor;
import com.orpe.consultants.utils.PdfDataExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ShippingBillController {

	@PostMapping("/shippingbill/importExcel")
    public String importExcel(@RequestParam("file") MultipartFile file, Model model) {
        ShippingBill bill = new ShippingBill();
        
        try {
            bill = ExcelShippingBillExtractor.extractFromExcel(file.getInputStream());
            
            // Debug output
            System.out.println("Extracted Shipping Bill Data:");
            System.out.println("SB No: " + bill.getSbNo());
            System.out.println("SB Date: " + bill.getSbDate());
            System.out.println("Port Code: " + bill.getPortCode());
            System.out.println("LEO Date: " + bill.getLeoDate());
            System.out.println("BRC Realisation Date: " + bill.getBrcRealisationDate());
            System.out.println("Invoice No Date: " + bill.getInvoiceNoDate());
            System.out.println("Buyer Details: " + bill.getBuyerDetails());
            System.out.println("Exchange Rate: " + bill.getExchangeRate());
            System.out.println("Invoice Value: " + bill.getInvoiceValue());
            System.out.println("Currency: " + bill.getCurrency());
            System.out.println("HS CD: " + bill.getHsCd());
            System.out.println("Description: " + bill.getDescription());
            System.out.println("Model No: " + bill.getModelNo());
            System.out.println("Quantity: " + bill.getQuantity());
            System.out.println("Unit: " + bill.getUnit());
            System.out.println("FOB: " + bill.getFob());
            System.out.println("PMV Per Unit: " + bill.getPmvPerUnit());
            System.out.println("Scheme Description: " + bill.getSchemeDescription());
            System.out.println("DBK SNO: " + bill.getDbkSno());
            System.out.println("Rate: " + bill.getRate());
            System.out.println("DBK Amt SB: " + bill.getDbkAmtSb());
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to extract data from Excel: " + e.getMessage());
        }

        model.addAttribute("shippingBill", bill);
        return "shippingBillUpload";
    }
}

