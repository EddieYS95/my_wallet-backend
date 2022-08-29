package io.heachi.backend.domain.transaction;

import io.heachi.backend.domain.transaction.Transaction.TransactionStatus;
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

  Page<Transaction> searchAllByCreateAtBetween(LocalDateTime startDateTime,
      LocalDateTime endDateTime, Pageable pageable);

  Page<Transaction> searchAllByCreateAtGreaterThanEqual(LocalDateTime startDateTime,
      Pageable pageable);

  Page<Transaction> searchAllByCreateAtLessThanEqual(LocalDateTime endDateTime, Pageable pageable);

  Page<Transaction> searchAllByToAddressOrFromAddress(String toAddress, String fromAddress,
      Pageable pageable);

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
