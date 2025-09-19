package com.orpe.consultants.service;

import com.orpe.consultants.dto.LoginRequest;
import com.orpe.consultants.dto.UserDTO;
import com.orpe.consultants.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {
    
    // Core CRUD operations
    User createUser(UserDTO userDTO);
    User updateUser(Long id, UserDTO userDTO);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findAllUsers();
    Page<User> findAllUsers(Pageable pageable);
    void deleteUser(Long id);
    
    // Authentication operations
    User authenticateUser(LoginRequest loginRequest);
    boolean validateCredentials(String usernameOrEmail, String password);
    void updateLastLogin(Long userId);
 //  void incrementLoginAttempts(Long userId);
 //    void resetLoginAttempts(Long userId);
 //     void lockUserAccount(Long userId, int lockDurationMinutes);
    
    // User management operations
    User changePassword(Long userId, String currentPassword, String newPassword);
    User updateUserStatus(Long userId, User.UserStatus status);
    User updateUserRole(Long userId, User.Role role);
    
    // Search and filter operations
    Page<User> searchUsers(String searchTerm, Pageable pageable);
    List<User> findUsersByRole(User.Role role);
    List<User> findUsersByStatus(User.UserStatus status);
    List<User> findActiveUsers();
    
    // Validation operations
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean isUsernameAvailable(String username, Long excludeUserId);
    boolean isEmailAvailable(String email, Long excludeUserId);
    
    // Statistics and reporting
    long getTotalUsersCount();
    long getActiveUsersCount();
    long getUsersCountByRole(User.Role role);
    List<User> getRecentlyActiveUsers(int limit);
    
    // Account maintenance
 //  void unlockExpiredAccounts();
 //   List<User> findUsersWithExcessiveLoginAttempts(int maxAttempts);
}
