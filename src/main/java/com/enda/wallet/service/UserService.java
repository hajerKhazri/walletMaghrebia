package com.enda.wallet.service;

import com.enda.wallet.exception.ResourceNotFoundException;
import com.enda.wallet.model.dto.request.RegisterRequest;
import com.enda.wallet.model.entity.User;
import com.enda.wallet.model.enums.Role;
import com.enda.wallet.repository.TransactionRepository;
import com.enda.wallet.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionRepository transactionRepository;
    @PersistenceContext
    private EntityManager entityManager;

    // ─── Création d’un utilisateur (avec détachement) ───
    @Transactional
    public User createUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Nom d'utilisateur déjà pris.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setId(null);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(Role.INITIATEUR);   // rôle par défaut
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setPasswordHash(encodedPassword);

        User savedUser = userRepository.save(user);
        entityManager.detach(savedUser);
        savedUser.setPasswordHash(null);
        return savedUser;
    }

    public double getBalance(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // ✅ Calcul du solde : somme des transactions réussies initiées par l'utilisateur
        Double balance = transactionRepository.sumAmountByInitiator(user);
        return balance != null ? balance : 0.0;
    }

    // ─── Toutes les autres méthodes inchangées ───
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public List<User> getAllUsersList() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public User updateUser(Long id, User userDetails) {
        User existing = getUserById(id);
        if (userDetails.getUsername() != null && !userDetails.getUsername().isEmpty()) {
            if (!existing.getUsername().equals(userDetails.getUsername())
                    && userRepository.existsByUsername(userDetails.getUsername())) {
                throw new RuntimeException("Username déjà pris.");
            }
            existing.setUsername(userDetails.getUsername());
        }
        if (userDetails.getEmail() != null && !userDetails.getEmail().isEmpty()) {
            if (!existing.getEmail().equals(userDetails.getEmail())
                    && userRepository.existsByEmail(userDetails.getEmail())) {
                throw new RuntimeException("Email déjà utilisé.");
            }
            existing.setEmail(userDetails.getEmail());
        }
        if (userDetails.getFirstName() != null) existing.setFirstName(userDetails.getFirstName());
        if (userDetails.getLastName() != null) existing.setLastName(userDetails.getLastName());
        if (userDetails.getRole() != null) existing.setRole(userDetails.getRole());
        if (userDetails.getActive() != null) existing.setActive(userDetails.getActive());
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            existing.setPasswordHash(passwordEncoder.encode(userDetails.getPassword()));
        }
        return userRepository.save(existing);
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = getUserById(id);
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(Long id) {
        User user = getUserById(id);
        user.setActive(true);
        userRepository.save(user);
    }

    @Transactional
    public User changeUserRole(Long id, Role newRole) {
        User user = getUserById(id);
        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = getUserById(id);
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 6 caractères");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void sendPasswordResetEmail(String email) {
        // À implémenter
    }

    public void resetPassword(String token, String newPassword) {
        // À implémenter
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == role)
                .toList();
    }

    public List<User> getActiveUsers() {
        return userRepository.findAll().stream()
                .filter(User::getActive)
                .toList();
    }

    public User findAvailableValidator() {
        return userRepository.findAll().stream()
                .filter(u -> u.hasRole(Role.VALIDATEUR) && u.getActive())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Aucun validateur disponible"));
    }
}