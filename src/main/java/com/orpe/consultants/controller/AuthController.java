package com.orpe.consultants.controller;



import com.orpe.consultants.dto.UserDTO;
import com.orpe.consultants.dto.ChartPointDto;
import com.orpe.consultants.dto.DashboardMetricsDto;
import com.orpe.consultants.dto.LoginRequest;
import com.orpe.consultants.model.User;
import com.orpe.consultants.service.DashboardService;
import com.orpe.consultants.service.DraftWorksheetService;
import com.orpe.consultants.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private DraftWorksheetService draftService; 

    // Home/Index Page - Requires Authentication
//    @GetMapping({"/", "/index"})
//    public String index(HttpSession session, Model model) {
//        // Check if user is logged in
//        User loggedInUser = (User) session.getAttribute("loggedInUser");
//        if (loggedInUser == null) {
//            // User not logged in, redirect to login page
//            log.info("User not authenticated, redirecting to login page");
//            return "redirect:/login";
//        }
//        
//        // User is authenticated, add to model and show index page
//        model.addAttribute("user", loggedInUser);
//        log.debug("User {} accessing index page", loggedInUser.getUsername());
//        return "index";
//    }
    
    
    @GetMapping({"/", "/index"})
    public String index(HttpSession session,
                        Model model,
                        @RequestParam(value = "fromDate", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @RequestParam(value = "toDate", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        // auth check (unchanged)
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            log.info("User not authenticated, redirecting to login page");
            return "redirect:/login";
        }
        model.addAttribute("user", loggedInUser);

        // Dashboard summary metrics (existing)
        var metrics = dashboardService.getDashboardMetrics(fromDate, toDate);
        model.addAttribute("dashboard", metrics);
        
        List<Map<String, Object>> draftGroups = draftService.getAllDraftGroups();
        model.addAttribute("draftGroups", draftGroups);


       
        // pass filters back to template
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        log.debug("User {} accessed dashboard, fromDate={}, toDate={}", loggedInUser.getUsername(), fromDate, toDate);
        return "index";
    }



    // Login Page (GET)
    @GetMapping("/login")
    public String showLoginPage(Model model, 
                               @RequestParam(value = "registered", required = false) String registered,
                               @RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "logout", required = false) String logout) {
        
        model.addAttribute("loginRequest", new LoginRequest());
        
        if (registered != null) {
            model.addAttribute("successMessage", "Registration successful! Please login.");
        }
        
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid credentials. Please try again.");
        }
        
        return "login";
    }

    // Login Processing (POST)
    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
                              BindingResult bindingResult,
                              Model model,
                              HttpSession session) {
        
        if (bindingResult.hasErrors()) {
            return "login";
        }

        try {
            User authenticatedUser = userService.authenticateUser(loginRequest);
            
            // Store user in session
            session.setAttribute("loggedInUser", authenticatedUser);
            session.setAttribute("username", authenticatedUser.getUsername());
            
            log.info("User logged in successfully: {}", authenticatedUser.getUsername());
            return "redirect:/index";
            
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("loginRequest", loginRequest);
            return "login";
        }
    }

    // Registration Page 
    @GetMapping("/register")
    public String showRegistrationPage(HttpSession session, Model model) {

        // 1) Check session user
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // 2) Role check: ADMIN only
        if (loggedInUser.getRole() != User.Role.ADMIN) {
            model.addAttribute("user", loggedInUser);
            return "error/unauthorisedaccess";  // your error page
        }

        // 3) Normal flow
        model.addAttribute("userDTO", new UserDTO());
        model.addAttribute("user", loggedInUser);
        return "register";
    }


    // Registration Processing (POST) - Public access
    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("userDTO") UserDTO userDTO,
                                     BindingResult bindingResult,
                                     Model model) {
        
        // 
        log.info("=== REGISTRATION DEBUG START ===");
        log.info("UserDTO received:");
        log.info("  - Username: '{}'", userDTO.getUsername());
        log.info("  - Full Name: '{}'", userDTO.getFullName());
        log.info("  - Email: '{}'", userDTO.getEmail());
        log.info("  - Password: '{}'", userDTO.getPassword() != null ? "***PROVIDED***" : "NULL");
        log.info("  - Phone: '{}'", userDTO.getPhoneNumber());
        log.info("  - Role: '{}'", userDTO.getRole());
        
        log.info("Validation errors count: {}", bindingResult.getErrorCount());
        
        if (bindingResult.hasErrors()) {
            log.error(" VALIDATION ERRORS FOUND:");
            bindingResult.getAllErrors().forEach(error -> 
                log.error("  - {}", error.getDefaultMessage()));
            log.info("=== RETURNING TO REGISTER PAGE ===");
            return "register";
        }

        try {
            log.info(" Calling userService.createUser()...");
            User createdUser = userService.createUser(userDTO);
            log.info(" User created successfully: ID={}, Username={}", 
                     createdUser.getId(), createdUser.getUsername());
            return "redirect:/login?registered=true";
            
        } catch (Exception e) {
            log.error(" Registration failed: {}", e.getMessage(), e);
            model.addAttribute("registrationError", e.getMessage());
            model.addAttribute("userDTO", userDTO);
            return "register";
        }
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        String username = (String) session.getAttribute("username");
        session.invalidate();
        log.info("User {} logged out successfully", username);
        return "redirect:/login?logout=true";
    }
    
    // Additional protected endpoints can be added here
    
    // User Profile Page - Requires Authentication
    @GetMapping("/user-profile")
    public String userProfile(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", loggedInUser);
        return "user-profile";
    }
    
    // Change Password Page - Requires Authentication
    @GetMapping("/change-password")
    public String changePasswordPage(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", loggedInUser);
        return "change-password";
    }
}
