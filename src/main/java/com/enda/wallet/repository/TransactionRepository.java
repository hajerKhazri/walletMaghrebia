package com.enda.wallet.repository;



import com.enda.wallet.model.entity.Transaction;
import com.enda.wallet.model.entity.User;
import com.enda.wallet.model.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByRefId(String refId);

    Page<Transaction> findByInitiator(User initiator, Pageable pageable);

    Page<Transaction> findByValidator(User validator, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:mobileNumber IS NULL OR t.mobileNumber = :mobileNumber) AND " +
            "(:fromDate IS NULL OR t.initiatedAt >= :fromDate) AND " +
            "(:toDate IS NULL OR t.initiatedAt <= :toDate)")
    Page<Transaction> findAllWithFilters(
            @Param("status") TransactionStatus status,
            @Param("mobileNumber") String mobileNumber,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.initiator = :initiator AND t.status = 'SUCCEEDED'")
    Double sumAmountByInitiator(@Param("initiator") User initiator);
}