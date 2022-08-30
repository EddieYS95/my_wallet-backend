package io.heachi.backend.domain.transaction.repository;

import io.heachi.backend.domain.transaction.Transaction;
import io.heachi.backend.domain.transaction.Transaction.TransactionStatus;
import io.heachi.backend.domain.transaction.TransactionEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, Long>, TransactionCustomRepo {

  Optional<Transaction> findByHash(String hash);

  List<Transaction> findAllByBlockConfirmationBefore(int blockNumber);

  List<Transaction> findAllByStatus(TransactionStatus pending);

  Optional<Transaction> findTopByOrderByIdfTransactionDesc();

  @Query(value = "select event from TransactionEvent event "
      + "WHERE event.createdAt <= :endingBefore "
      + "and event.createdAt >= :staringAfter "
      + "ORDER BY event.createdAt desc ",
      countQuery = "select count(event) from TransactionEvent event")
  Page<TransactionEvent> findAllEvent(LocalDateTime staringAfter, LocalDateTime endingBefore,
      Pageable pageable);
}
