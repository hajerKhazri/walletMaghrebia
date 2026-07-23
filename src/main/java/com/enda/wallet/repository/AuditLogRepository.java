package com.enda.wallet.repository;

import com.enda.wallet.model.entity.AuditLog;
import com.enda.wallet.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUser(User user, Pageable pageable);
    Page<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
}