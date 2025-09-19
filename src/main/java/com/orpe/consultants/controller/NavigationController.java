package com.orpe.consultants.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.orpe.consultants.model.ShippingBill;
import com.orpe.consultants.model.User;
import com.orpe.consultants.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NavigationController {
	
	// Pdf data import page
    @GetMapping({"/exceldataimport"})
    public String pdfImportPage(HttpSession session, Model model) {
        // Check if user is logged in
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            // User not logged in, redirect to login page
            log.info("User not authenticated, redirecting to login page");
            return "redirect:/login";
        }
     // Role check: only ADMIN can access
        if (loggedInUser.getRole() != User.Role.ADMIN) {
            model.addAttribute("user", loggedInUser);
            return "error/unauthorisedaccess";
        }
        
        // User is authenticated, add to model and show index page
        model.addAttribute("user", loggedInUser);
        log.debug("User {} accessing index page", loggedInUser.getUsername());
        return "excelDataImport";
    }
    
    
    @GetMapping({"/uploadimport"})
    public String beExcelImport(HttpSession session, Model model) {
        // Check if user is logged in
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            // User not logged in, redirect to login page
            log.info("User not authenticated, redirecting to login page");
            return "redirect:/login";
        }
        
        // User is authenticated, add to model and show index page
        model.addAttribute("user", loggedInUser);
        //model.addAttribute("shippingBill", new ShippingBill());
        log.debug("User {} accessing index page", loggedInUser.getUsername());
        return "uploadImportData";
    }
    
    
    @GetMapping({"/uploadexport"})
    public String sbExcelImport(HttpSession session, Model model) {
        // Check if user is logged in
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            // User not logged in, redirect to login page
            log.info("User not authenticated, redirecting to login page");
            return "redirect:/login";
        }
        
        // User is authenticated, add to model and show index page
        model.addAttribute("user", loggedInUser);
        //model.addAttribute("shippingBill", new ShippingBill());
        log.debug("User {} accessing index page", loggedInUser.getUsername());
        return "uploadExportData";
    }
    

}
