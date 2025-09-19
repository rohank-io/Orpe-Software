package com.orpe.consultants.controller;



import com.orpe.consultants.dto.UserDTO;
import com.orpe.consultants.exception.DatabaseOperationException;
import com.orpe.consultants.exception.ResourceNotFoundException;
import com.orpe.consultants.exception.UserAlreadyExistsException;
import com.orpe.consultants.model.User;
import com.orpe.consultants.model.User.Role;
import com.orpe.consultants.model.User.UserStatus;
import com.orpe.consultants.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    // Display User List
    @GetMapping("/users")
    public String getUserList(HttpSession session, Model model) {
        // Check if user is logged in
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            log.info("User not authenticated, redirecting to login page");
            return "redirect:/login";
        }
        
        try {
            // Fetch all users
            List<User> users = userService.findAllUsers();
            model.addAttribute("users", users);
            model.addAttribute("loggedInUser", loggedInUser);
            
            log.info("User list displayed for logged in user: {}", loggedInUser.getUsername());
            return "userList";
            
        } catch (Exception e) {
            log.error("Error fetching user list: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error loading users. Please try again.");
            return "userList";
        }
    }
    
    // View Single User Details
    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, HttpSession session, Model model) {
        // Check if user is logged in
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        
        try {
            Optional<User> userOptional = userService.findById(id);
            if (userOptional.isPresent()) {
                model.addAttribute("user", userOptional.get());
                model.addAttribute("loggedInUser", loggedInUser);
                return "userDetails";
            } else {
                model.addAttribute("errorMessage", "User not found");
                return "redirect:/users";
            }
            
        } catch (Exception e) {
            log.error("Error fetching user details: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error loading user details");
            return "redirect:/users";
        }
    }
    
    // Delete User
    @GetMapping("/users/delete/{id}")  // Use GET instead of POST
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        
        try {
            userService.deleteUser(id);
            return "redirect:/users?success=User deleted successfully";
        } catch (Exception e) {
            return "redirect:/users?error=Error deleting user";
        }
    }
    
    
    @PostMapping("/users/{id}/update")
    public String updateUser(@PathVariable @Positive Long id,
                           @RequestParam String fullName,
                           @RequestParam String username,
                           @RequestParam String email,
                           @RequestParam(required = false) String phoneNumber,
                           @RequestParam String role,
                           @RequestParam String status,
                           @RequestParam(required = false) String notes,
                           @RequestParam(required = false) String password,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        
        // Authentication check
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            log.warn("Unauthorized update attempt for user ID: {}", id);
            return "redirect:/login";
        }
        
        try {
            // Create UserDTO from form parameters
            UserDTO userDTO = new UserDTO();
            userDTO.setUsername(username.trim());
            userDTO.setFullName(fullName.trim());
            userDTO.setEmail(email.trim());
            userDTO.setPhoneNumber(phoneNumber != null ? phoneNumber.trim() : null);
            userDTO.setRole(Role.valueOf(role));
            userDTO.setStatus(UserStatus.valueOf(status));
            userDTO.setNotes(notes != null ? notes.trim() : null);
            
            // Only set password if provided
            if (StringUtils.hasText(password)) {
                userDTO.setPassword(password);
            }
            
            // Update user via service
            User updatedUser = userService.updateUser(id, userDTO);
            
            log.info("User successfully updated - ID: {}, Updated by: {}", 
                    updatedUser.getId(), loggedInUser.getUsername());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "User '" + updatedUser.getUsername() + "' updated successfully!");
            
            return "redirect:/users/" + id;
            
        } catch (ResourceNotFoundException e) {
            log.warn("User not found for update - ID: {}, Requested by: {}", 
                    id, loggedInUser.getUsername());
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/users";
            
        } catch (UserAlreadyExistsException e) {
            log.warn("Duplicate data conflict during user update - ID: {}, Error: {}", 
                    id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/users/" + id;
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for user update - ID: {}, Error: {}", 
                    id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid input: " + e.getMessage());
            return "redirect:/users/" + id;
            
        } catch (DatabaseOperationException e) {
            log.error("Database error during user update - ID: {}, Error: {}", 
                    id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "System error occurred. Please try again later.");
            return "redirect:/users/" + id;
            
        } catch (Exception e) {
            log.error("Unexpected error during user update - ID: {}, Error: {}", 
                    id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "An unexpected error occurred. Please contact support if the problem persists.");
            return "redirect:/users/" + id;
        }
    }

}
