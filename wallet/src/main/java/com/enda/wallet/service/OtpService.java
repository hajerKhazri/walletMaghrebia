package com.enda.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final EmailService emailService;
    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();
    private static final SecureRandom random = new SecureRandom();
    private static final int OTP_EXPIRATION_MINUTES = 10;

    public String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // =========================================================
    // TRANSACTION OTP
    // =========================================================
    public void saveTransactionOtp(String email, String otp, Long transactionId) {
        String key = "TRANSACTION:" + transactionId + ":" + email;
        otpStore.put(key, new OtpData(otp, System.currentTimeMillis()));
        log.info("🔑 OTP TRANSACTION sauvegardé - clé: {}", key);
    }

    public boolean validateTransactionOtp(String email, String otp, Long transactionId) {
        String key = "TRANSACTION:" + transactionId + ":" + email;
        return validateOtpByKey(key, otp);
    }

    // =========================================================
    // BATCH OTP
    // =========================================================
    public void saveBatchOtp(String email, String otp, Long batchId) {
        String key = "BATCH:" + batchId + ":" + email;
        otpStore.put(key, new OtpData(otp, System.currentTimeMillis()));
        log.info("🔑 OTP BATCH sauvegardé - clé: {}", key);
    }

    public boolean validateBatchOtp(String email, String otp, Long batchId) {
        String key = "BATCH:" + batchId + ":" + email;
        return validateOtpByKey(key, otp);
    }

    // =========================================================
    // MÉTHODE GÉNÉRIQUE DE VALIDATION
    // =========================================================
    private boolean validateOtpByKey(String key, String otp) {
        OtpData data = otpStore.get(key);
        if (data == null) {
            log.warn("❌ Aucun OTP trouvé pour la clé: {}", key);
            return false;
        }

        long elapsed = System.currentTimeMillis() - data.timestamp;
        if (elapsed > OTP_EXPIRATION_MINUTES * 60 * 1000) {
            log.warn("⏰ OTP expiré pour la clé: {}", key);
            otpStore.remove(key);
            return false;
        }

        boolean valid = data.otp.equals(otp);
        if (valid) {
            otpStore.remove(key);
            log.info("✅ OTP validé avec succès pour la clé: {}", key);
        } else {
            log.warn("❌ OTP invalide pour la clé: {}", key);
        }
        return valid;
    }

    // =========================================================
    // ENVOI D'OTP PAR EMAIL
    // =========================================================
    public void sendTransactionOtp(String email, String otp, Long transactionId) {
        emailService.sendTransactionOtp(email, otp, transactionId);
        saveTransactionOtp(email, otp, transactionId);
    }

    public void sendBatchOtp(String email, String otp, Long batchId) {
        emailService.sendBatchOtp(email, otp, batchId);
        saveBatchOtp(email, otp, batchId);
    }

    // =========================================================
    // MÉTHODES OBSOLÈTES (pour compatibilité)
    // =========================================================
    @Deprecated
    public void saveOtp(String email, String otp, Long entityId) {
        saveTransactionOtp(email, otp, entityId);
    }

    @Deprecated
    public boolean validateOtp(String email, String otp, Long entityId) {
        return validateTransactionOtp(email, otp, entityId);
    }

    @Deprecated
    public void sendOtp(String email, String otp) {
        emailService.sendEmail(email, "🔐 Code OTP", "Votre code OTP : " + otp);
        // Ne sauvegarde pas car on ne sait pas à quoi associer
    }

    private record OtpData(String otp, long timestamp) {}
}