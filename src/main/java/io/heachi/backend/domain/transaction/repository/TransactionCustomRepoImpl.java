package io.heachi.backend.domain.transaction.repository;

import static io.heachi.backend.domain.transaction.QTransaction.transaction;
import static io.heachi.backend.domain.transaction.QTransactionEvent.transactionEvent;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.heachi.backend.api.transaction.TransactionDto.EventDto;
import io.heachi.backend.api.wallet.WalletDto;
import io.heachi.backend.domain.wallet.Wallet;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Slf4j
@RequiredArgsConstructor
public class TransactionCustomRepoImpl implements TransactionCustomRepo {

  private final JPAQueryFactory query;

  @Override
  public Page<EventDto> findAllEventDtoBy(LocalDateTime startingAfter,
      LocalDateTime endingBefore, Pageable pageable) {

    BooleanBuilder condition = new BooleanBuilder();
    if (startingAfter != null) {
      condition.and(transactionEvent.createdAt.after(startingAfter));
    }
    if (startingAfter != null) {
      condition.and(transactionEvent.createdAt.before(endingBefore));
    }

    List<EventDto> contents = query.select(Projections.constructor(EventDto.class,
            transactionEvent.idfEvent,
            transactionEvent.transaction.hash.as("hash"),
            transactionEvent.status,
            transactionEvent.blockConfirmation,
            transactionEvent.createdAt
        ))
        .from(transactionEvent)
        .where(condition)
        .join(transactionEvent.transaction, transaction)
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .orderBy(transactionEvent.createdAt.desc())
        .fetch();

    Long count = query.select(transactionEvent.count())
        .from(transactionEvent)
        .where(condition)
        .fetchOne();

    return new PageImpl<>(contents, pageable, count);
  }

  @Override
  public Page<WalletDto.TransactionDto> findAllDtoBy(Wallet wallet, LocalDateTime startingAfter,
      LocalDateTime endingBefore,
      Pageable pageable) {

    BooleanBuilder condition = new BooleanBuilder(transaction.toAddress.eq(wallet.getAddress())
        .or(transaction.fromAddress.eq(wallet.getAddress())));

    if (startingAfter != null) {
      condition.and(transaction.createAt.after(startingAfter));
    }
    if (startingAfter != null) {
      condition.and(transaction.createAt.before(endingBefore));
    }

    List<WalletDto.TransactionDto> contents = query.select(
            Projections.constructor(WalletDto.TransactionDto.class,
                transaction.idfTransaction,
                transaction.hash,
                transaction.status,
                transaction.blockConfirmation,
                transaction.value,
                transaction.gasPrice.as("fee"),
                transaction.fromAddress.as("from"),
                transaction.toAddress.as("to")
            ))
        .from(transaction)
        .where(condition)
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .orderBy(transaction.idfTransaction.desc())
        .fetch();

    Long count = query.select(transaction.count())
        .from(transaction)
        .where(condition)
        .fetchOne();

    return new PageImpl<>(contents, pageable, count);
  }
}
