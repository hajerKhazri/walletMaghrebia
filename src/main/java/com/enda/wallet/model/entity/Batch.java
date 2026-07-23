package com.enda.wallet.model.entity;



import com.enda.wallet.model.enums.BatchStatus;
import com.enda.wallet.model.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "batches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(length = 500)
    private String filePath;
    @Column(length = 500)
    private String validationRemark;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalLines = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer successfulLines = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer failedLines = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BatchStatus status = BatchStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validator_id")
    private User validator;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime validatedAt;

    private LocalDateTime completedAt;


    @Column(length = 500)
    private String reportPath;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setBatch(this);
        this.totalLines = transactions.size();
    }

    public void incrementSuccessfulLines() {
        this.successfulLines++;
    }

    public void incrementFailedLines() {
        this.failedLines++;
    }

    public void calculateStatistics() {
        this.successfulLines = (int) transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCEEDED)
                .count();
        this.failedLines = (int) transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.FAILED)
                .count();
        this.totalLines = transactions.size();
    }

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }

    public void setRejectionReason(String reason) {
    }
}