package com.enda.wallet.service;

import com.enda.wallet.model.dto.request.BatchUpdateRequest;
import com.enda.wallet.model.dto.request.ValidateOtpRequest;
import com.enda.wallet.model.dto.response.AnalysisResult;
import com.enda.wallet.model.dto.response.BatchResponse;
import com.enda.wallet.model.dto.response.TransactionResponse;
import com.enda.wallet.model.entity.Batch;
import com.enda.wallet.model.entity.Transaction;
import com.enda.wallet.model.entity.User;
import com.enda.wallet.model.enums.BatchStatus;
import com.enda.wallet.model.enums.Role;
import com.enda.wallet.model.enums.TransactionStatus;
import com.enda.wallet.repository.BatchRepository;
import com.enda.wallet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchService {

    private final BatchRepository batchRepository;
    private final TransactionRepository transactionRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final RestTemplate restTemplate;

    @Value("${app.ia.service.url}")
    private String iaServiceUrl;

    // =========================================================
    // UPLOAD BATCH (AVEC ANALYSE IA + PARSING MANUEL)
    // =========================================================
    @Transactional
    public BatchResponse uploadBatch(MultipartFile file, User currentUser) {
        try {
            log.info("📤 Upload du fichier : {}", file.getOriginalFilename());

            // 1. Créer le batch
            Batch batch = new Batch();
            batch.setFilename(file.getOriginalFilename());
            batch.setStatus(BatchStatus.PENDING);
            batch.setInitiator(currentUser);
            batch.setUploadedAt(LocalDateTime.now());
            batch = batchRepository.save(batch);
            log.info("✅ Batch créé avec l'id : {}", batch.getId());

            // 2. Appel IA
            log.info("🤖 Appel du service IA pour analyse...");
            AnalysisResult aiResult = analyzeWithAI(file);

            if (aiResult != null && aiResult.isHasErrors()) {
                String errorSummary = aiResult.getErrors().stream()
                        .map(e -> "Ligne " + e.getLine() + ": " + String.join(", ", e.getErrors()))
                        .collect(Collectors.joining("; "));

                log.warn("⚠️ Rejet du batch par IA : {}", errorSummary);
                batch.setStatus(BatchStatus.REJECTED);
                batch.setValidationRemark("Erreurs IA : " + errorSummary);
                batch = batchRepository.save(batch);
                return toResponse(batch);
            }

            log.info("✅ Aucune erreur IA détectée, parsing du CSV...");

            // 3. Parser le CSV
            List<String[]> lines = readCsv(file);

            if (lines.size() <= 1) {
                log.warn("⚠️ Le fichier CSV est vide ou ne contient que l'en-tête");
                batch.setTotalLines(0);
                batch.setSuccessfulLines(0);
                batch.setFailedLines(0);
                batch = batchRepository.save(batch);
                return toResponse(batch);
            }

            List<Transaction> transactions = new ArrayList<>();
            List<String> errorMessages = new ArrayList<>();
            int successful = 0;
            int failed = 0;

            for (int i = 1; i < lines.size(); i++) {
                String[] columns = lines.get(i);
                List<String> rowErrors = new ArrayList<>();

                try {
                    if (columns.length < 6) {
                        rowErrors.add("Ligne incomplète (6 colonnes requises)");
                    } else {
                        String msisdn = columns[0].trim();
                        String matricule = columns[1].trim();
                        String montantStr = columns[2].trim();
                        String prenom = columns[3].trim();
                        String nom = columns[4].trim();
                        String remark = columns.length > 5 ? columns[5].trim() : "";

                        // Vérifier le numéro de téléphone
                        if (msisdn.length() != 8 || !msisdn.matches("[0-9]+")) {
                            rowErrors.add("Numéro invalide (8 chiffres)");
                        }

                        // Vérifier le matricule
                        if (matricule.isEmpty()) {
                            rowErrors.add("Matricule requis");
                        }

                        // Vérifier le montant
                        try {
                            BigDecimal amount = new BigDecimal(montantStr);
                            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                                rowErrors.add("Montant doit être > 0");
                            }
                        } catch (NumberFormatException e) {
                            rowErrors.add("Montant invalide (nombre requis)");
                        }

                        // Vérifier le prénom
                        if (prenom.isEmpty()) {
                            rowErrors.add("Prénom requis");
                        }

                        // Vérifier le nom
                        if (nom.isEmpty()) {
                            rowErrors.add("Nom requis");
                        }

                        if (rowErrors.isEmpty()) {
                            Transaction t = new Transaction();
                            t.setRefId(UUID.randomUUID().toString().substring(0, 10));
                            t.setMobileNumber(msisdn);
                            t.setAmount(new BigDecimal(montantStr));
                            t.setRemarks(remark);
                            t.setInitiator(currentUser);
                            t.setStatus(TransactionStatus.CREATED);
                            t.setInitiatedAt(LocalDateTime.now());
                            t.setBatch(batch);
                            transactions.add(t);
                            successful++;
                        } else {
                            failed++;
                            for (String err : rowErrors) {
                                errorMessages.add("Ligne " + i + ": " + err);
                            }
                        }
                    }
                } catch (Exception e) {
                    failed++;
                    errorMessages.add("Ligne " + i + ": " + e.getMessage());
                }
            }

            // ✅ SI DES ERREURS SONT DÉTECTÉES → REJETER LE BATCH
            if (failed > 0) {
                String errorSummary = String.join("; ", errorMessages);
                log.warn("⚠️ Rejet du batch : {} erreurs détectées", failed);
                batch.setStatus(BatchStatus.REJECTED);
                batch.setValidationRemark("Erreurs : " + errorSummary);
                batch.setTotalLines(lines.size() - 1);
                batch.setSuccessfulLines(successful);
                batch.setFailedLines(failed);
                batch = batchRepository.save(batch);

                // ✅ Renvoyer le batch rejeté avec les erreurs
                return toResponse(batch);
            }

            // ✅ Si aucune erreur → Sauvegarder les transactions
            if (!transactions.isEmpty()) {
                transactionRepository.saveAll(transactions);
                log.info("✅ {} transactions sauvegardées", transactions.size());
            }

            // Mise à jour des compteurs
            batch.setTotalLines(transactions.size());
            batch.setSuccessfulLines(successful);
            batch.setFailedLines(0);
            batch = batchRepository.save(batch);

            // ✅ Envoi de la référence du batch par email
            try {
                emailService.sendBatchReference(
                        currentUser.getEmail(),
                        batch.getId(),
                        batch.getTotalLines()
                );
                log.info("📧 Email de référence batch envoyé à {}", currentUser.getEmail());
            } catch (Exception e) {
                log.warn("⚠️ Erreur envoi email : {}", e.getMessage());
            }

            log.info("✅ Batch {} traité : {} transactions, {} succès, {} échecs",
                    batch.getId(), transactions.size(), successful, 0);

            return toResponse(batch);

        } catch (Exception e) {
            log.error("❌ Erreur upload batch", e);
            throw new RuntimeException("Erreur lors de l'upload du batch : " + e.getMessage());
        }
    }

    // =========================================================
    // ANALYSE IA
    // =========================================================
    private AnalysisResult analyzeWithAI(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<AnalysisResult> response = restTemplate.exchange(
                    iaServiceUrl,
                    HttpMethod.POST,
                    requestEntity,
                    AnalysisResult.class
            );

            AnalysisResult result = response.getBody();
            log.info("📊 IA a analysé {} lignes, {} erreurs trouvées",
                    result != null ? result.getTotalLines() : 0,
                    result != null ? result.getErrorCount() : 0);

            if (result != null && result.isHasErrors()) {
                log.info("📝 Détail des erreurs IA :");
                for (AnalysisResult.ErrorLine error : result.getErrors()) {
                    log.info("   Ligne {} : {}", error.getLine(), String.join(", ", error.getErrors()));
                }
            }

            return result;

        } catch (Exception e) {
            log.error("❌ Erreur appel IA : {}", e.getMessage());
            return null;
        }
    }

    // =========================================================
    // LECTURE CSV
    // =========================================================
    private List<String[]> readCsv(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            return reader.lines()
                    .map(line -> line.split(","))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Erreur de lecture du fichier CSV", e);
        }
    }

    // =========================================================
    // GET / ALL / MY
    // =========================================================
    @Transactional(readOnly = true)
    public BatchResponse getBatch(Long id) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch non trouvé"));
        return toResponse(batch);
    }

    @Transactional(readOnly = true)
    public Page<BatchResponse> getAllBatches(Pageable pageable) {
        return batchRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BatchResponse> getMyBatches(User currentUser, Pageable pageable) {
        return batchRepository.findByInitiator(currentUser, pageable)
                .map(this::toResponse);
    }

    // =========================================================
    // VALIDATE BATCH (avec mise à jour des compteurs)
    // =========================================================
    @Transactional
    public BatchResponse validateBatch(Long id, ValidateOtpRequest request, User currentUser) {
        log.info("🔐 Validation du batch {} par {}", id, currentUser.getUsername());

        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch non trouvé"));

        // 1. Vérifier que le batch est en attente
        if (batch.getStatus() != BatchStatus.PENDING) {
            throw new RuntimeException("Le batch n'est pas en attente de validation");
        }

        // 2. Vérifier l'OTP
        String otp = request.getOtp();
        if (otp == null || otp.length() != 6) {
            throw new RuntimeException("Code OTP invalide (6 chiffres requis)");
        }

        boolean valid = otpService.validateBatchOtp(currentUser.getEmail(), otp, id);
        if (!valid) {
            throw new RuntimeException("OTP invalide ou expiré");
        }

        // 3. Valider le batch et mettre à jour les compteurs
        log.info("✅ OTP valide, validation du batch {}", id);

        List<Transaction> transactions = batch.getTransactions();
        int total = transactions.size();
        int success = 0;
        int failed = 0;

        for (Transaction t : transactions) {
            t.setStatus(TransactionStatus.VALIDATED);
            t.setValidator(currentUser);
            t.setValidatedAt(LocalDateTime.now());
            transactionRepository.save(t);
            success++;
        }

        batch.setTotalLines(total);
        batch.setSuccessfulLines(success);
        batch.setFailedLines(0);
        batch.setStatus(BatchStatus.VALIDATED);
        batch.setValidator(currentUser);
        batch.setValidatedAt(LocalDateTime.now());

        if (request.getRemark() != null && !request.getRemark().isEmpty()) {
            batch.setValidationRemark(request.getRemark());
        }

        batch = batchRepository.save(batch);

        // ✅ Envoyer la confirmation de validation à l'initiateur
        try {
            emailService.sendBatchValidationConfirmation(
                    batch.getInitiator().getEmail(),
                    batch.getId(),
                    currentUser.getUsername()
            );
            log.info("📧 Confirmation de validation envoyée à {}", batch.getInitiator().getEmail());
        } catch (Exception e) {
            log.warn("⚠️ Erreur envoi email : {}", e.getMessage());
        }

        log.info("✅ Batch {} validé : {} lignes, {} succès, {} échecs",
                batch.getId(), total, success, 0);

        return toResponse(batch);
    }

    // =========================================================
    // REJETER UN BATCH
    // =========================================================
    @Transactional
    public BatchResponse rejectBatch(Long id, String reason, User currentUser) {
        log.info("⛔ Rejet du batch {} par {}", id, currentUser.getUsername());

        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch non trouvé"));

        batch.setStatus(BatchStatus.REJECTED);
        batch.setValidator(currentUser);
        if (reason != null && !reason.isEmpty()) {
            batch.setValidationRemark("Rejeté : " + reason);
        }
        batch = batchRepository.save(batch);

        log.info("✅ Batch {} rejeté", id);
        return toResponse(batch);
    }

    // =========================================================
    // AUTRES MÉTHODES
    // =========================================================
    public String generateReport(Long id) {
        return "Rapport généré pour le batch " + id;
    }

    @Transactional(readOnly = true)
    public String exportCsv() {
        List<Batch> batches = batchRepository.findAllWithUsers();
        StringBuilder sb = new StringBuilder();

        sb.append("ID Batch,Statut Batch,Initiateur,Validateur,Fichier,Date upload,ID Transaction,Référence,Bénéficiaire,Montant,Motif,Statut Transaction,Date initiation\n");

        for (Batch batch : batches) {
            List<Transaction> transactions = batch.getTransactions();
            if (transactions != null && !transactions.isEmpty()) {
                for (Transaction t : transactions) {
                    sb.append(String.format("%d,%s,%s,%s,%s,%s,%d,%s,%s,%.3f,%s,%s,%s\n",
                            batch.getId(),
                            batch.getStatus() != null ? batch.getStatus().name() : "",
                            batch.getInitiator() != null ? batch.getInitiator().getUsername() : "",
                            batch.getValidator() != null ? batch.getValidator().getUsername() : "",
                            batch.getFilename() != null ? batch.getFilename() : "",
                            batch.getUploadedAt() != null ? batch.getUploadedAt().toString() : "",
                            t.getId(),
                            t.getRefId(),
                            t.getMobileNumber(),
                            t.getAmount(),
                            t.getRemarks() != null ? t.getRemarks() : "",
                            t.getStatus() != null ? t.getStatus().name() : "",
                            t.getInitiatedAt() != null ? t.getInitiatedAt().toString() : ""
                    ));
                }
            } else {
                sb.append(String.format("%d,%s,%s,%s,%s,%s,,,,,,\n",
                        batch.getId(),
                        batch.getStatus() != null ? batch.getStatus().name() : "",
                        batch.getInitiator() != null ? batch.getInitiator().getUsername() : "",
                        batch.getValidator() != null ? batch.getValidator().getUsername() : "",
                        batch.getFilename() != null ? batch.getFilename() : "",
                        batch.getUploadedAt() != null ? batch.getUploadedAt().toString() : ""
                ));
            }
        }
        return sb.toString();
    }

    @Transactional
    public BatchResponse updateBatch(Long id, BatchUpdateRequest request, User currentUser) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch non trouvé"));

        if (batch.getStatus() != BatchStatus.PENDING) {
            throw new RuntimeException("Impossible de modifier un batch déjà traité");
        }

        if (!batch.getInitiator().getId().equals(currentUser.getId()) && !currentUser.hasRole(Role.ADMIN)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier ce batch");
        }

        if (request.getFilename() != null && !request.getFilename().isEmpty()) {
            batch.setFilename(request.getFilename());
        }

        batch = batchRepository.save(batch);
        return toResponse(batch);
    }

    @Transactional
    public void deleteBatch(Long id, User currentUser) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch non trouvé"));

        if (batch.getStatus() != BatchStatus.PENDING) {
            throw new RuntimeException("Impossible de supprimer un batch déjà traité");
        }

        if (!batch.getInitiator().getId().equals(currentUser.getId()) && !currentUser.hasRole(Role.ADMIN)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer ce batch");
        }

        batchRepository.delete(batch);
    }

    @Transactional
    public void initiateValidation(Long id, User currentUser) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch non trouvé"));

        if (batch.getStatus() != BatchStatus.PENDING) {
            throw new RuntimeException("Seuls les batches en attente peuvent être validés");
        }

        String email = currentUser.getEmail();
        String otp = otpService.generateOtp();

        otpService.sendBatchOtp(email, otp, id);

        batch.setValidator(currentUser);
        batchRepository.save(batch);

        log.info("✅ OTP envoyé à {} pour le batch {}", email, id);
    }

    // =========================================================
    // CONVERSION BATCH → BATCHRESPONSE (avec erreurs)
    // =========================================================
    private BatchResponse toResponse(Batch batch) {
        List<BatchResponse.ErrorDetail> errors = null;
        if (batch.getStatus() == BatchStatus.REJECTED && batch.getValidationRemark() != null) {
            errors = parseErrors(batch.getValidationRemark());
        }

        return BatchResponse.builder()
                .id(batch.getId())
                .status(batch.getStatus() != null ? batch.getStatus().name() : null)
                .filename(batch.getFilename())
                .totalLines(batch.getTotalLines())
                .successfulLines(batch.getSuccessfulLines())
                .failedLines(batch.getFailedLines())
                .initiatorUsername(batch.getInitiator() != null ? batch.getInitiator().getUsername() : null)
                .validatorUsername(batch.getValidator() != null ? batch.getValidator().getUsername() : null)
                .uploadedAt(batch.getUploadedAt())
                .validatedAt(batch.getValidatedAt())
                .validationRemark(batch.getValidationRemark())
                .errors(errors)
                .build();
    }

    // =========================================================
    // PARSER LES ERREURS
    // =========================================================
    private List<BatchResponse.ErrorDetail> parseErrors(String validationRemark) {
        List<BatchResponse.ErrorDetail> errors = new ArrayList<>();
        if (validationRemark == null) return errors;

        String clean = validationRemark.replace("Erreurs : ", "").replace("Erreurs IA : ", "").trim();
        String[] parts = clean.split("; ");

        for (String part : parts) {
            if (part.trim().isEmpty()) continue;
            Pattern pattern = Pattern.compile("Ligne\\s+(\\d+):\\s+(.+)");
            Matcher matcher = pattern.matcher(part.trim());
            if (matcher.find()) {
                int line = Integer.parseInt(matcher.group(1));
                String msg = matcher.group(2);
                String field = detectField(msg);
                errors.add(BatchResponse.ErrorDetail.builder()
                        .line(line)
                        .field(field)
                        .message(msg)
                        .build());
            } else {
                // Erreur générale sans numéro de ligne
                errors.add(BatchResponse.ErrorDetail.builder()
                        .line(0)
                        .field("général")
                        .message(part.trim())
                        .build());
            }
        }
        return errors;
    }

    private String detectField(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("numéro") || lower.contains("téléphone") || lower.contains("msisdn") || lower.contains("phone")) {
            return "msisdn";
        }
        if (lower.contains("montant") || lower.contains("amount")) {
            return "montant";
        }
        if (lower.contains("matricule")) {
            return "matricule";
        }
        if (lower.contains("prénom") || lower.contains("prenom")) {
            return "prenom";
        }
        if (lower.contains("nom")) {
            return "nom";
        }
        if (lower.contains("remark") || lower.contains("motif")) {
            return "remark";
        }
        return "général";
    }

    // =========================================================
    // RÉCUPÉRATION DES TRANSACTIONS D'UN BATCH
    // =========================================================
    @Transactional(readOnly = true)
    public List<TransactionResponse> getBatchTransactions(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch non trouvé"));
        return batch.getTransactions().stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse toTransactionResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .refId(t.getRefId())
                .mobileNumber(t.getMobileNumber())
                .amount(t.getAmount())
                .remarks(t.getRemarks())
                .status(t.getStatus())
                .initiatorUsername(t.getInitiator() != null ? t.getInitiator().getUsername() : null)
                .validatorUsername(t.getValidator() != null ? t.getValidator().getUsername() : null)
                .initiatedAt(t.getInitiatedAt())
                .build();
    }
}