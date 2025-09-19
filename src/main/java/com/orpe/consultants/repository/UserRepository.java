package com.orpe.consultants.repository;

import com.orpe.consultants.model.User;
import com.orpe.consultants.model.User.Role;
import com.orpe.consultants.model.User.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Basic finder methods
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    // Status and role based queries
    List<User> findByStatus(UserStatus status);
    
    List<User> findByRole(Role role);
    
    Page<User> findByStatusAndRole(UserStatus status, Role role, Pageable pageable);
    
    // Active users query
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE'")
    List<User> findActiveUsers(@Param("now") LocalDateTime now);
    
    // Search functionality
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);
    
    // Login attempt related queries
    //@Query("SELECT u FROM User u WHERE u.loginAttempts >= :maxAttempts")
    // List<User> findUsersWithExcessiveLoginAttempts(@Param("maxAttempts") Integer maxAttempts);
    
    // Update methods
    // @Modifying
    // @Query("UPDATE User u SET u.loginAttempts = :attempts, u.accountLockedUntil = :lockUntil WHERE u.id = :userId")
    // void updateLoginAttempts(@Param("userId") Long userId, 
    //                        @Param("attempts") Integer attempts, 
    //                       @Param("lockUntil") LocalDateTime lockUntil);
    
    @Modifying
    @Query("UPDATE User u SET u.lastLoginDate = :loginDate WHERE u.id = :userId")
    void updateLastLoginDate(@Param("userId") Long userId, @Param("loginDate") LocalDateTime loginDate);
    
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :userId")
    void updateUserStatus(@Param("userId") Long userId, @Param("status") UserStatus status);
    
    // Statistics queries
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE'")
    long countActiveUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countUsersByRole(@Param("role") Role role);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :fromDate")
    long countNewUsersFromDate(@Param("fromDate") LocalDateTime fromDate);
    
    // Recent activity
    @Query("SELECT u FROM User u WHERE u.lastLoginDate >= :fromDate ORDER BY u.lastLoginDate DESC")
    List<User> findRecentlyActiveUsers(@Param("fromDate") LocalDateTime fromDate, Pageable pageable);
    
    // Account maintenance
    // @Query("SELECT u FROM User u WHERE u.accountLockedUntil IS NOT NULL AND u.accountLockedUntil < :now")
    //  List<User> findExpiredLockedAccounts(@Param("now") LocalDateTime now);
    
    // Custom validation queries
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username AND u.id != :userId")
    boolean existsByUsernameAndIdNot(@Param("username") String username, @Param("userId") Long userId);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.id != :userId")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("userId") Long userId);
}
