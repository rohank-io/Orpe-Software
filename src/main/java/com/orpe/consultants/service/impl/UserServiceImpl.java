package com.orpe.consultants.service.impl;
import com.orpe.consultants.dto.LoginRequest;
import com.orpe.consultants.dto.UserDTO;
import com.orpe.consultants.exception.ResourceNotFoundException;
import com.orpe.consultants.exception.UserAlreadyExistsException;
import com.orpe.consultants.exception.InvalidCredentialsException;
import com.orpe.consultants.exception.AccountInactiveException;
import com.orpe.consultants.exception.PasswordValidationException;
import com.orpe.consultants.exception.DatabaseOperationException;
import com.orpe.consultants.model.User;
import com.orpe.consultants.repository.UserRepository;
import com.orpe.consultants.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Core CRUD operations
    @Override
    public User createUser(UserDTO userDTO) {
        log.info("Creating new user with username: {}", userDTO != null ? userDTO.getUsername() : "null");
        
        // Single comprehensive validation
        if (userDTO == null || 
            !StringUtils.hasText(userDTO.getUsername()) || 
            !StringUtils.hasText(userDTO.getEmail())) {
            throw new IllegalArgumentException("User data, username and email are required");
        }
        
        // Check if username already exists
        if (existsByUsername(userDTO.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + userDTO.getUsername() + "' is already taken. Please choose a different username.");
        }
        
        // Check if email already exists
        if (existsByEmail(userDTO.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + userDTO.getEmail() + "' is already registered. Please use a different email address.");
        }
        
        try {
            User user = User.builder()
                    .username(userDTO.getUsername())
                    .fullName(userDTO.getFullName())
                    .email(userDTO.getEmail())
                    .password(passwordEncoder.encode(userDTO.getPassword()))
                    .phoneNumber(userDTO.getPhoneNumber())
                    .role(userDTO.getRole() != null ? userDTO.getRole() : User.Role.USER)
                    .status(userDTO.getStatus() != null ? userDTO.getStatus() : User.UserStatus.ACTIVE)
                    .notes(userDTO.getNotes())
                    .build();
            
            User savedUser = userRepository.save(user);
            log.info("User created successfully with ID: {}", savedUser.getId());
            return savedUser;
            
        } catch (Exception e) {
            log.error("Database error while creating user: {}", e.getMessage());
            throw new DatabaseOperationException("Failed to create user. Please try again later.");
        }
    }
    
    @Override
    public User updateUser(Long id, UserDTO userDTO) {
        log.info("Updating user with ID: {}", id);
        
        // Enhanced validation
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        
        if (userDTO == null) {
            throw new IllegalArgumentException("User data cannot be null");
        }
        
        User existingUser = findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", id));
        
        // Check username availability (excluding current user)
        if (!isUsernameAvailable(userDTO.getUsername(), id)) {
            throw new UserAlreadyExistsException("Username '" + userDTO.getUsername() + "' is already taken by another user.");
        }
        
        // Check email availability (excluding current user)
        if (!isEmailAvailable(userDTO.getEmail(), id)) {
            throw new UserAlreadyExistsException("Email '" + userDTO.getEmail() + "' is already used by another user.");
        }
        
        try {
            // Update user fields
            existingUser.setUsername(userDTO.getUsername());
            existingUser.setFullName(userDTO.getFullName());
            existingUser.setEmail(userDTO.getEmail());
            existingUser.setPhoneNumber(userDTO.getPhoneNumber());
            existingUser.setRole(userDTO.getRole());
            existingUser.setStatus(userDTO.getStatus());
            existingUser.setNotes(userDTO.getNotes());
            
            // Update password only if provided
            if (StringUtils.hasText(userDTO.getPassword())) {
                existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            }
            
            User updatedUser = userRepository.save(existingUser);
            log.info("User updated successfully with ID: {}", updatedUser.getId());
            return updatedUser;
            
        } catch (Exception e) {
            log.error("Database error while updating user: {}", e.getMessage());
            throw new DatabaseOperationException("Failed to update user. Please try again later.");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return userRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName"));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    @Override
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        
        User user = findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", id));
        
        try {
            userRepository.delete(user);
            log.info("User deleted successfully with ID: {}", id);
        } catch (Exception e) {
            log.error("Database error while deleting user: {}", e.getMessage());
            throw new DatabaseOperationException("Failed to delete user. Please try again later.");
        }
    }
    
    // Simplified Authentication
    @Override
    public User authenticateUser(LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getUsernameOrEmail());
        
        // Enhanced validation
        if (loginRequest == null || 
                !StringUtils.hasText(loginRequest.getUsernameOrEmail()) || 
                !StringUtils.hasText(loginRequest.getPassword())) {
                throw new IllegalArgumentException("Invalid login request: username/email and password are required");
            }
        
        Optional<User> userOptional = userRepository.findByUsernameOrEmail(
                loginRequest.getUsernameOrEmail(), 
                loginRequest.getUsernameOrEmail()
        );
        
        if (userOptional.isEmpty()) {
            log.warn("User not found: {}", loginRequest.getUsernameOrEmail());
            throw new InvalidCredentialsException("Invalid username or password");
        }
        
        User user = userOptional.get();
        
        // Check if account is active
        if (!user.isActive()) {
            log.warn("Account is inactive for user: {}", user.getUsername());
            
            if (user.getStatus() == User.UserStatus.INACTIVE) {
                throw new AccountInactiveException("Your account has been deactivated. Please contact administrator for assistance.");
            } else if (user.getStatus() == User.UserStatus.PENDING) {
                throw new AccountInactiveException("Your account is pending approval. Please contact administrator.");
            } else {
                throw new AccountInactiveException("Your account is not active. Please contact administrator.");
            }
        }
        
        // Validate password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", user.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }
        
        // Successful authentication
        updateLastLogin(user.getId());
        log.info("User authenticated successfully: {}", user.getUsername());
        
        return user;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean validateCredentials(String usernameOrEmail, String password) {
        try {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsernameOrEmail(usernameOrEmail);
            loginRequest.setPassword(password);
            
            authenticateUser(loginRequest);
            return true;
        } catch (Exception e) {
            log.debug("Credential validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public void updateLastLogin(Long userId) {
        User user = findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
        
        user.updateLastLogin();
        userRepository.save(user);
        log.debug("Last login updated for user ID: {}", userId);
    }
    
    // User management operations
    @Override
    public User changePassword(Long userId, String currentPassword, String newPassword) {
        log.info("Changing password for user ID: {}", userId);
        
        if (!StringUtils.hasText(currentPassword)) {
            throw new PasswordValidationException("Current password is required");
        }
        
        if (!StringUtils.hasText(newPassword)) {
            throw new PasswordValidationException("New password is required");
        }
        
        if (newPassword.length() < 6) {
            throw new PasswordValidationException("New password must be at least 6 characters long");
        }
        
        User user = findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
        
        // Validate current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect. Please enter your current password correctly.");
        }
        
        // Set new password
        user.setPassword(passwordEncoder.encode(newPassword));
        User updatedUser = userRepository.save(user);
        
        log.info("Password changed successfully for user ID: {}", userId);
        return updatedUser;
    }
    
    @Override
    public User updateUserStatus(Long userId, User.UserStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("User status cannot be null");
        }
        
        User user = findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
        
        user.setStatus(status);
        User updatedUser = userRepository.save(user);
        log.info("User status updated to {} for user ID: {}", status, userId);
        return updatedUser;
    }
    
    @Override
    public User updateUserRole(Long userId, User.Role role) {
        if (role == null) {
            throw new IllegalArgumentException("User role cannot be null");
        }
        
        User user = findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
        
        user.setRole(role);
        User updatedUser = userRepository.save(user);
        log.info("User role updated to {} for user ID: {}", role, userId);
        return updatedUser;
    }
    
    // Search and filter operations
    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.searchUsers(searchTerm, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<User> findUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<User> findUsersByStatus(User.UserStatus status) {
        return userRepository.findByStatus(status);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<User> findActiveUsers() {
        return userRepository.findActiveUsers(LocalDateTime.now());
    }
    
    // Validation operations
    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username, Long excludeUserId) {
        if (excludeUserId != null) {
            return !userRepository.existsByUsernameAndIdNot(username, excludeUserId);
        }
        return !existsByUsername(username);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email, Long excludeUserId) {
        if (excludeUserId != null) {
            return !userRepository.existsByEmailAndIdNot(email, excludeUserId);
        }
        return !existsByEmail(email);
    }
    
    // Statistics and reporting
    @Override
    @Transactional(readOnly = true)
    public long getTotalUsersCount() {
        return userRepository.count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getActiveUsersCount() {
        return userRepository.countActiveUsers();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getUsersCountByRole(User.Role role) {
        return userRepository.countUsersByRole(role);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<User> getRecentlyActiveUsers(int limit) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(30);
        Pageable pageable = PageRequest.of(0, limit);
        return userRepository.findRecentlyActiveUsers(fromDate, pageable);
    }
}
