package com.enda.wallet.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    // ✅ Compte réel
    private static final String FROM_EMAIL = "khazrihajer41@gmail.com";
    // ✅ Nom affiché à la place de l'email
    private static final String FROM_NAME = "Wallet Maghrebia";

    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);

            // ✅ Le plus important : définir l'expéditeur avec le nom
            helper.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));

            mailSender.send(mimeMessage);
            log.info("✅ Email envoyé à {} (expéditeur: {} <{}>)", to, FROM_NAME, FROM_EMAIL);
        } catch (Exception e) {
            log.error("❌ Erreur envoi email à {} : {}", to, e.getMessage());
            throw new RuntimeException("Erreur envoi email : " + e.getMessage());
        }
    }

    // =========================================================
    // RÉFÉRENCE TRANSACTION
    // =========================================================
    public void sendTransactionReference(String to, String refId, Long transactionId, Double amount) {
        String subject = "📄 Code de votre transaction #" + transactionId;
        String body = String.format("""
            Bonjour,

            Votre transaction a été créée avec succès.

            📋 Détails :
            • Référence : %s
            • ID transaction : %d
            • Montant : %.2f TND

            Vous pouvez suivre l'état de votre transaction sur la plateforme.

            Cordialement,
            L'équipe Wallet Maghrebia
            """, refId, transactionId, amount);
        sendEmail(to, subject, body);
    }

    // =========================================================
    // RÉFÉRENCE BATCH
    // =========================================================
    public void sendBatchReference(String to, Long batchId, Integer totalLines) {
        String subject = "📄 Code de votre batch #" + batchId;
        String body = String.format("""
            Bonjour,

            Votre batch a été créé avec succès.

            📋 Détails :
            • ID batch : %d
            • Nombre de lignes : %d

            Vous pouvez suivre l'état de votre batch sur la plateforme.

            Cordialement,
            L'équipe Wallet Maghrebia
            """, batchId, totalLines);
        sendEmail(to, subject, body);
    }

    // =========================================================
    // OTP POUR BATCH
    // =========================================================
    public void sendBatchOtp(String to, String otp, Long batchId) {
        String subject = "🔐 Code OTP pour validation du batch #" + batchId;
        String body = String.format("""
            Bonjour,

            Vous avez demandé la validation du batch #%d.

            🔑 Votre code OTP : %s

            ⏰ Ce code est valable 10 minutes.

            Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.

            Cordialement,
            L'équipe Wallet Maghrebia
            """, batchId, otp);
        sendEmail(to, subject, body);
    }

    // =========================================================
    // OTP POUR TRANSACTION
    // =========================================================
    public void sendTransactionOtp(String to, String otp, Long transactionId) {
        String subject = "🔐 Code OTP pour validation de la transaction #" + transactionId;
        String body = String.format("""
            Bonjour,

            Vous avez demandé la validation de la transaction #%d.

            🔑 Votre code OTP : %s

            ⏰ Ce code est valable 10 minutes.

            Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.

            Cordialement,
            L'équipe Wallet Maghrebia
            """, transactionId, otp);
        sendEmail(to, subject, body);
    }

    // =========================================================
    // CONFIRMATION VALIDATION BATCH
    // =========================================================
    public void sendBatchValidationConfirmation(String to, Long batchId, String validatorName) {
        String subject = "✅ Batch #" + batchId + " validé avec succès";
        String body = String.format("""
            Bonjour,

            Le batch #%d a été validé avec succès par %s.

            Toutes les transactions du batch sont maintenant en cours de traitement.

            Cordialement,
            L'équipe Wallet Maghrebia
            """, batchId, validatorName);
        sendEmail(to, subject, body);
    }

    // =========================================================
    // CONFIRMATION VALIDATION TRANSACTION
    // =========================================================
    public void sendTransactionValidationConfirmation(String to, Long transactionId, String validatorName) {
        String subject = "✅ Transaction #" + transactionId + " validée avec succès";
        String body = String.format("""
            Bonjour,

            La transaction #%d a été validée avec succès par %s.

            Le montant va être crédité sur le wallet du bénéficiaire.

            Cordialement,
            L'équipe Wallet Maghrebia
            """, transactionId, validatorName);
        sendEmail(to, subject, body);
    }
}