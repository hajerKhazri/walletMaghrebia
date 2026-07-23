package com.enda.wallet.util;

import com.enda.wallet.model.entity.AuditLog;
import com.enda.wallet.model.entity.User;
import com.enda.wallet.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogger {

    private final AuditLogRepository auditLogRepository;

    public void logAction(User user, String action, String details, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(auditLog);
            log.debug("Action journalisée: {} par {}", action, user.getUsername());
        } catch (Exception e) {
            log.error("Erreur lors de la journalisation: {}", e.getMessage());
        }
    }
}