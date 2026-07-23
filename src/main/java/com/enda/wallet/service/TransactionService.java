package com.enda.wallet.service;

import com.enda.wallet.model.dto.request.TransactionRequest;
import com.enda.wallet.model.dto.request.ValidateOtpRequest;
import com.enda.wallet.model.dto.response.TransactionResponse;
import com.enda.wallet.model.entity.Transaction;
import com.enda.wallet.model.entity.User;
import com.enda.wallet.model.enums.Role;
import com.enda.wallet.model.enums.TransactionStatus;
import com.enda.wallet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OtpService otpService;
    private final UserService userService;
    private final EmailService emailService; // ✅ Injection du service email

    // =========================================================
    // CRÉER UNE TRANSACTION
    // =========================================================
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, User currentUser) {
        Transaction transaction = new Transaction();
        transaction.setRefId(UUID.randomUUID().toString().substring(0, 10));
        transaction.setMobileNumber(request.getMobileNumber());
        transaction.setAmount(request.getAmount());
        transaction.setRemarks(request.getRemarks());
        transaction.setInitiator(currentUser);
        transaction.setStatus(TransactionStatus.CREATED);
        transaction.setInitiatedAt(LocalDateTime.now());

        transaction = transactionRepository.save(transaction);
        log.info("Transaction créée avec id: {}", transaction.getId());

        // ✅ Envoi du code de référence à l'initiateur (avec conversion)
        try {
            emailService.sendTransactionReference(
                    currentUser.getEmail(),
                    transaction.getRefId(),
                    transaction.getId(),
                    transaction.getAmount().doubleValue()  // ← conversion explicite
            );
            log.info("📧 Email de référence transaction envoyé à {}", currentUser.getEmail());
        } catch (Exception e) {
            log.warn("⚠️ Erreur envoi email : {}", e.getMessage());
        }

        initiateValidation(transaction.getId());
        return toResponse(transaction);
    }

    // =========================================================
    // INITIER LA VALIDATION (ENVOI OTP)
    // =========================================================
    @Transactional
    public void initiateValidation(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée"));

        if (transaction.getStatus() != TransactionStatus.CREATED) {
            throw new RuntimeException("La transaction n'est pas en état d'être validée");
        }

        User validator = userService.findAvailableValidator();
        String validatorEmail = validator.getEmail();

        String otp = otpService.generateOtp();
        // ✅ Utiliser sendTransactionOtp qui sauvegarde et envoie
        otpService.sendTransactionOtp(validatorEmail, otp, transactionId);

        transaction.setStatus(TransactionStatus.AWAITING_VALIDATION);
        transaction.setValidator(validator);
        transactionRepository.save(transaction);

        log.info("✅ OTP envoyé à {} pour la transaction {}", validatorEmail, transactionId);
    }

    // =========================================================
    // VALIDER UNE TRANSACTION (AVEC OTP)
    // =========================================================
    @Transactional
    public TransactionResponse validateTransaction(Long id, ValidateOtpRequest request, User currentUser) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée"));

        if (transaction.getStatus() != TransactionStatus.AWAITING_VALIDATION) {
            throw new RuntimeException("La transaction n'est pas en attente de validation");
        }

        String validatorEmail = transaction.getValidator() != null
                ? transaction.getValidator().getEmail()
                : currentUser.getEmail();

        boolean valid = otpService.validateTransactionOtp(validatorEmail, request.getOtp(), id);
        if (!valid) {
            throw new RuntimeException("OTP invalide ou expiré");
        }

        // Enregistrer la remarque si présente
        if (request.getRemark() != null && !request.getRemark().isEmpty()) {
            transaction.setValidationRemark(request.getRemark());
        }

        transaction.setStatus(TransactionStatus.VALIDATED);
        transaction.setValidatedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        // Simuler l'appel externe (ex: bankToWallet)
        transaction.setStatus(TransactionStatus.SUCCEEDED);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        // ✅ Envoyer la confirmation de validation à l'initiateur
        try {
            emailService.sendTransactionValidationConfirmation(
                    transaction.getInitiator().getEmail(),
                    transaction.getId(),
                    currentUser.getUsername()
            );
            log.info("📧 Confirmation de validation envoyée à {}", transaction.getInitiator().getEmail());
        } catch (Exception e) {
            log.warn("⚠️ Erreur envoi email : {}", e.getMessage());
        }

        log.info("✅ Transaction {} validée avec succès", transaction.getId());

        return toResponse(transaction);
    }

    // =========================================================
    // AUTRES MÉTHODES (GET, CANCEL, REJECT, ETC.)
    // =========================================================
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée"));
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(String status, String mobileNumber,
                                                        String fromDate, String toDate,
                                                        Pageable pageable) {
        return transactionRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getMyTransactions(User currentUser, Pageable pageable) {
        return transactionRepository.findByInitiator(currentUser, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getPendingTransactions(Pageable pageable) {
        return transactionRepository.findByStatus(TransactionStatus.AWAITING_VALIDATION, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public TransactionResponse cancelTransaction(Long id, User currentUser) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée"));

        if (transaction.getStatus() != TransactionStatus.CREATED &&
                transaction.getStatus() != TransactionStatus.AWAITING_VALIDATION) {
            throw new RuntimeException("La transaction ne peut pas être annulée");
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse rejectTransaction(Long id, String reason, User currentUser) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée"));

        if (transaction.getStatus() != TransactionStatus.AWAITING_VALIDATION) {
            throw new RuntimeException("La transaction n'est pas en attente de validation");
        }

        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setValidator(currentUser);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse checkExternalStatus(Long id) {
        return getTransaction(id);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request, User currentUser) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée"));

        if (transaction.getStatus() != TransactionStatus.CREATED) {
            throw new RuntimeException("Impossible de modifier une transaction déjà soumise");
        }

        if (!transaction.getInitiator().getId().equals(currentUser.getId()) && !currentUser.hasRole(Role.ADMIN)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cette transaction");
        }

        if (request.getRemarks() != null) {
            transaction.setRemarks(request.getRemarks());
        }

        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    // =========================================================
    // CONVERSION EN DTO
    // =========================================================
    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .refId(transaction.getRefId())
                .mobileNumber(transaction.getMobileNumber())
                .amount(transaction.getAmount())
                .remarks(transaction.getRemarks())
                .status(transaction.getStatus()) // Enum → sera sérialisé en String par Jackson
                .initiatorUsername(transaction.getInitiator() != null ? transaction.getInitiator().getUsername() : null)
                .validatorUsername(transaction.getValidator() != null ? transaction.getValidator().getUsername() : null)
                .initiatedAt(transaction.getInitiatedAt())
                .validatedAt(transaction.getValidatedAt())
                .completedAt(transaction.getCompletedAt())
                .validationRemark(transaction.getValidationRemark())
                .build();
    }
}