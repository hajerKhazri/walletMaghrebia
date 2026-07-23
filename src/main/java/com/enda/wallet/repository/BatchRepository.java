package com.enda.wallet.repository;



import com.enda.wallet.model.dto.response.BatchExportDto;
import com.enda.wallet.model.entity.Batch;
import com.enda.wallet.model.entity.User;
import com.enda.wallet.model.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    Page<Batch> findByInitiator(User initiator, Pageable pageable);

    Page<Batch> findByStatus(BatchStatus status, Pageable pageable);
    @Query("SELECT b FROM Batch b JOIN FETCH b.initiator LEFT JOIN FETCH b.validator")
    List<Batch> findAllWithUsers();
}
