package com.enda.wallet.model.entity;



import com.enda.wallet.model.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String refId;

    @Column(length = 50)
    private String txnId;

    @Column(nullable = false, length = 15)
    private String mobileNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validator_id")
    private User validator;

    @Column(nullable = false, updatable = false)
    private LocalDateTime initiatedAt;

    private LocalDateTime validatedAt;

    private LocalDateTime completedAt;

    @Column(length = 100)
    private String externalTraceId;

    @Column(length = 10)
    private String errorCode;


    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;
    @Column(length = 500)
    private String validationRemark;

    public void transitionTo(TransactionStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Transition impossible de %s vers %s", this.status, newStatus)
            );
        }
        this.status = newStatus;
        updateTimestamps(newStatus);
    }

    private void updateTimestamps(TransactionStatus newStatus) {
        if (newStatus == TransactionStatus.VALIDATED) {
            this.validatedAt = LocalDateTime.now();
        }
        if (newStatus.isTerminal()) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public boolean isTerminal() {
        return status.isTerminal();
    }

    @PrePersist
    protected void onCreate() {
        this.initiatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TransactionStatus.CREATED;
        }
    }
}