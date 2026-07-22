package com.enda.wallet.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CsvParser {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CsvRow {
        private String msisdn;
        private String matricule;
        private String montant;
        private String prenom;
        private String nom;
        private String remark;
    }

    public List<CsvRow> parseCsv(InputStream inputStream) throws IOException {
        List<CsvRow> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // Ignorer la ligne d'en-tête
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // Ignorer les lignes vides
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    // Split par virgule ou point-virgule
                    String[] columns = line.split("[,;]");

                    if (columns.length < 5) {
                        log.warn("Ligne ignorée (format invalide): {}", line);
                        continue;
                    }

                    CsvRow row = new CsvRow();
                    row.setMsisdn(columns[0].trim());
                    row.setMatricule(columns[1].trim());
                    row.setMontant(columns[2].trim());
                    row.setPrenom(columns[3].trim());
                    row.setNom(columns[4].trim());
                    row.setRemark(columns.length > 5 ? columns[5].trim() : "");

                    rows.add(row);

                } catch (Exception e) {
                    log.warn("Erreur lors du parsing de la ligne: {}", line, e);
                }
            }
        }

        log.info("{} lignes parsées avec succès", rows.size());
        return rows;
    }
}