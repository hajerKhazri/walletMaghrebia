package com.enda.wallet.service;

import com.enda.wallet.model.dto.response.VerifyWalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankToWalletClient {

    private final RestTemplate restTemplate;

    @Value("${app.api.bank-to-wallet.base-url:http://10.27.105.214:2081/bankToWallet}")
    private String baseUrl;

    // 🔧 ACTIVER LE MODE MOCK
    private boolean mockEnabled = true;

    // ============================================================
    // API 1 : VÉRIFICATION D'UTILISATEUR
    // ============================================================
    public VerifyWalletResponse verifyUser(String mobileNumber, String userType) {
        log.info("🔍 Vérification du wallet pour: {}", mobileNumber);

        if (mockEnabled) {
            return mockVerifyUser(mobileNumber, userType);
        }

        // Appel réel à l'API...
        try {
            String url = baseUrl + "/userEnquiry";

            Map<String, String> request = new HashMap<>();
            request.put("serviceType", "VERIFY_USER");
            request.put("mobileNumber", mobileNumber);
            request.put("userType", userType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String enquiryStatus = (String) body.get("enquiryStatus");
                String userStatus = (String) body.get("userStatus");

                return VerifyWalletResponse.builder()
                        .exists("SUCCEEDED".equals(enquiryStatus) && "Y".equals(userStatus))
                        .workspaceId((String) body.get("workspaceId"))
                        .mobileNumber((String) body.get("mobileNumber"))
                        .userStatus(userStatus)
                        .firstName((String) body.get("firstName"))
                        .lastName((String) body.get("lastName"))
                        .kycIdType((String) body.get("kycIdType"))
                        .kycIdValue((String) body.get("kycIdValue"))
                        .traceId((String) body.get("traceId"))
                        .errorCode((String) body.get("errorCode"))
                        .errorMessage((String) body.get("errorMsg"))
                        .build();
            }

            return errorResponse("500", "Réponse vide de l'API");

        } catch (Exception e) {
            log.error("❌ Erreur API userEnquiry: {}", e.getMessage());
            return errorResponse("500", e.getMessage());
        }
    }

    // ============================================================
    // API 2 : INITIATION DE TRANSACTION
    // ============================================================
    public Map<String, Object> initiateTransaction(String refId, String mobileNumber,
                                                   String amount, String remarks, String userType) {
        log.info("💰 Initiation transaction refId: {}", refId);

        if (mockEnabled) {
            Map<String, Object> response = new HashMap<>();
            response.put("enquiryStatus", "SUCCEEDED");
            response.put("txnStatus", "TS");
            response.put("traceId", UUID.randomUUID().toString());
            response.put("txnId", "BW" + System.currentTimeMillis() + ".A" + UUID.randomUUID().toString().substring(0, 5));
            response.put("errorCode", "");
            response.put("errorMsg", "");
            log.info("✅ [MOCK] Transaction initiée avec succès");
            return response;
        }

        // Appel réel...
        try {
            String url = baseUrl + "/txnInitiate";
            Map<String, String> request = new HashMap<>();
            request.put("serviceType", "BIBWREQ_TXN");
            request.put("refId", refId);
            request.put("mobileNumber", mobileNumber);
            request.put("amount", amount);
            request.put("remarks", remarks != null ? remarks : "");
            request.put("userType", userType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getBody() != null) {
                return response.getBody();
            }

            Map<String, Object> error = new HashMap<>();
            error.put("enquiryStatus", "FAILED");
            error.put("errorMsg", "Réponse vide");
            return error;

        } catch (Exception e) {
            log.error("❌ Erreur API txnInitiate: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("enquiryStatus", "FAILED");
            error.put("errorMsg", e.getMessage());
            return error;
        }
    }

    // ============================================================
    // API 3 : CONSULTATION DE TRANSACTION
    // ============================================================
    public Map<String, Object> enquireTransaction(String refId) {
        log.info("🔎 Consultation transaction refId: {}", refId);

        if (mockEnabled) {
            Map<String, Object> response = new HashMap<>();
            response.put("refIdTxnStatus", "200");
            response.put("referenceId", "BW" + System.currentTimeMillis() + ".A" + UUID.randomUUID().toString().substring(0, 5));
            response.put("refIdTxnAmount", "100.0");
            response.put("enquiryStatus", "200");
            response.put("txnId", "TI" + System.currentTimeMillis() + ".A" + UUID.randomUUID().toString().substring(0, 5));
            response.put("refIdTxnState", "TS");
            return response;
        }

        // Appel réel...
        try {
            String url = baseUrl + "/txnEnquiry";
            Map<String, String> request = new HashMap<>();
            request.put("serviceType", "TXN_ENQUIRY");
            request.put("refId", refId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getBody() != null) {
                return response.getBody();
            }

            Map<String, Object> error = new HashMap<>();
            error.put("enquiryStatus", "FAILED");
            error.put("errorMsg", "Réponse vide");
            return error;

        } catch (Exception e) {
            log.error("❌ Erreur API txnEnquiry: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("enquiryStatus", "FAILED");
            error.put("errorMsg", e.getMessage());
            return error;
        }
    }

    // ============================================================
    // 🔧 MÉTHODES UTILITAIRES
    // ============================================================

    private VerifyWalletResponse errorResponse(String errorCode, String errorMessage) {
        return VerifyWalletResponse.builder()
                .exists(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    // ============================================================
    // 🧪 MOCK 100% DYNAMIQUE (SANS LISTES STATIQUES)
    // ============================================================
    private VerifyWalletResponse mockVerifyUser(String mobileNumber, String userType) {
        log.info("🔧 [MOCK] Vérification wallet pour: {}", mobileNumber);

        // Vérifier que le numéro est valide (8 chiffres)
        if (mobileNumber == null || mobileNumber.length() != 8 || !mobileNumber.matches("[0-9]+")) {
            log.warn("❌ [MOCK] Numéro invalide: {}", mobileNumber);
            return VerifyWalletResponse.builder()
                    .exists(false)
                    .mobileNumber(mobileNumber)
                    .errorCode("400")
                    .errorMessage("INVALID_MSISDN")
                    .build();
        }

        // 📌 TOUT EST GÉNÉRÉ DYNAMIQUEMENT À PARTIR DU NUMÉRO
        // Le numéro devient la source de toutes les données

        // 🔹 Générer un prénom à partir du numéro
        String firstName = generateDynamicFirstName(mobileNumber);

        // 🔹 Générer un nom à partir du numéro
        String lastName = generateDynamicLastName(mobileNumber);

        // 🔹 Générer un KYC ID à partir du numéro
        String kycId = generateDynamicKycId(mobileNumber);

        // 🔹 Générer un type de KYC
        String kycType = generateDynamicKycType(mobileNumber);

        log.info("✅ [MOCK] Wallet trouvé: {} {}", firstName, lastName);

        return VerifyWalletResponse.builder()
                .exists(true)
                .workspaceId("SUBSCRIBER")
                .mobileNumber(mobileNumber)
                .userStatus("Y")
                .firstName(firstName)
                .lastName(lastName)
                .kycIdType(kycType)
                .kycIdValue(kycId)
                .traceId(UUID.randomUUID().toString())
                .errorCode("200")
                .errorMessage("SUCCESS")
                .build();
    }

    // ============================================================
    // 🧬 GÉNÉRATION DYNAMIQUE COMPLÈTE
    // ============================================================

    /**
     * Génère un prénom dynamiquement à partir du numéro de téléphone
     * Chaque chiffre est converti en lettre pour former un nom unique
     */
    private String generateDynamicFirstName(String mobileNumber) {
        // Chaque chiffre correspond à une lettre
        String[] letters = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

        StringBuilder name = new StringBuilder();
        for (char c : mobileNumber.toCharArray()) {
            int digit = Character.getNumericValue(c);
            // Convertir le chiffre en lettre (0->A, 1->B, etc.)
            name.append(letters[digit % letters.length]);
        }

        // Ajouter des voyelles pour rendre le nom prononçable
        String result = name.toString();
        result = result.replace("A", "a").replace("E", "e").replace("I", "i").replace("O", "o").replace("U", "u");

        // Mettre la première lettre en majuscule
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    /**
     * Génère un nom dynamiquement à partir du numéro de téléphone
     * Utilise une combinaison différente pour obtenir un nom différent
     */
    private String generateDynamicLastName(String mobileNumber) {
        // Inverser le numéro pour avoir un nom différent
        String reversed = new StringBuilder(mobileNumber).reverse().toString();

        String[] letters = {"B", "C", "D", "F", "G", "H", "J", "K", "L", "M"};

        StringBuilder name = new StringBuilder();
        for (char c : reversed.toCharArray()) {
            int digit = Character.getNumericValue(c);
            name.append(letters[digit % letters.length]);
        }

        String result = name.toString();
        result = result.replace("B", "b").replace("C", "c").replace("D", "d").replace("F", "f").replace("G", "g")
                .replace("H", "h").replace("J", "j").replace("K", "k").replace("L", "l").replace("M", "m");

        // Ajouter un suffixe "i" pour faire plus réaliste
        return Character.toUpperCase(result.charAt(0)) + result.substring(1) + "i";
    }

    /**
     * Génère un ID KYC dynamiquement
     */
    private String generateDynamicKycId(String mobileNumber) {
        // Utiliser le hash du numéro pour générer un ID unique
        long hash = Math.abs(mobileNumber.hashCode());
        return String.format("%08d", hash % 100000000);
    }

    /**
     * Génère un type de KYC dynamiquement
     */
    private String generateDynamicKycType(String mobileNumber) {
        // Alterner entre différents types selon le numéro
        String[] types = {"National ID", "Passeport", "Carte de séjour", "Permis de conduire"};
        int index = Math.abs(mobileNumber.hashCode()) % types.length;
        return types[index];
    }
}