package io.heachi.backend.domain.transaction;

import io.heachi.backend.domain.transaction.Transaction.TransactionStatus;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, Long> {

  Optional<Transaction> findByHash(String hash);

  List<Transaction> findAllByBlockNumber(BigInteger blockNumber);

  Page<Transaction> searchAllByCreateAtBetween(LocalDateTime startDateTime,
      LocalDateTime endDateTime, Pageable pageable);

  Page<Transaction> searchAllByToAddressOrFromAddress(String toAddress, String fromAddress,
      Pageable pageable);

  List<Transaction> findAllByStatus(TransactionStatus pending);

  Optional<Transaction> findTopByOrderByIdfTransactionDesc();
}
