package com.enda.wallet.service;

import com.enda.wallet.model.dto.request.LoginRequest;
import com.enda.wallet.model.dto.response.LoginResponse;
import com.enda.wallet.model.entity.User;
import com.enda.wallet.model.enums.Role;
import com.enda.wallet.repository.UserRepository;
import com.enda.wallet.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Tentative de connexion pour: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Nom d'utilisateur ou mot de passe incorrect"));

        // Vérifier si le compte est verrouillé
        if (user.isLocked()) {
            throw new RuntimeException("Compte verrouillé. Réessayez dans " + lockDurationMinutes + " minutes.");
        }

        // Vérifier si le compte est actif
        if (!user.getActive()) {
            throw new RuntimeException("Compte désactivé. Contactez l'administrateur.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Réinitialiser les tentatives échouées
            user.resetFailedAttempts();

            // Mettre à jour la dernière connexion
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Générer les tokens
            String accessToken = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);

            log.info("Connexion réussie pour: {}", request.getUsername());

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(86400000L) // 24h
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build();

        } catch (BadCredentialsException e) {
            // Incrémenter les tentatives échouées
            user.incrementFailedAttempts();

            if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
                user.lockAccount(lockDurationMinutes);
                log.warn("Compte verrouillé pour {} après {} tentatives échouées",
                        request.getUsername(), maxLoginAttempts);
                throw new RuntimeException("Compte verrouillé après " + maxLoginAttempts + " tentatives échouées");
            }

            userRepository.save(user);
            log.warn("Échec de connexion pour {} (tentative {}/{})",
                    request.getUsername(), user.getFailedLoginAttempts(), maxLoginAttempts);
            throw new BadCredentialsException("Nom d'utilisateur ou mot de passe incorrect");
        }
    }

    public LoginResponse refreshToken(String refreshToken) {
        log.info("Tentative de rafraîchissement du token");

        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Token de rafraîchissement invalide");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Créer une nouvelle authentication
        Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, user.getAuthorities());

        String newAccessToken = tokenProvider.generateToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public void logout(String token) {
        log.info("Déconnexion");
        // Ici vous pouvez ajouter un blacklist de tokens JWT si nécessaire
        SecurityContextHolder.clearContext();
    }

    @Transactional
    public User createDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .email("admin@wallet.tn")
                    .firstName("Admin")
                    .lastName("Système")
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            return userRepository.save(admin);
        }
        return userRepository.findByUsername("admin").orElse(null);
    }
}