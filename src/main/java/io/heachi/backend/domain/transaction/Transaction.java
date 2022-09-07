package io.heachi.backend.domain.transaction;

import io.heachi.backend.domain.wallet.Wallet;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Table(name = "_Transaction",
    indexes = @Index(name = "transaction_hash", unique = true, columnList = "hash"))
@Entity
@Getter
@Builder
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {


  public enum TransactionStatus {
    PENDING, MINED, CONFIRMED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idfTransaction;

  private String hash;
  @Column(precision = 36, scale = 18, name = "_value")
  private BigDecimal value;
  @Column(precision = 36, scale = 18)
  private BigDecimal gasPrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionStatus status;

  private String fromAddress;
  private String toAddress;
  private BigInteger blockNumber;
  private int blockConfirmation;

  @CreationTimestamp
  private LocalDateTime createAt;
  @UpdateTimestamp
  private LocalDateTime updateAt;

  @OneToMany(mappedBy = "transaction", cascade = {CascadeType.PERSIST})
  List<TransactionEvent> eventList;

  public void pending() {
    if (status == TransactionStatus.MINED || status == TransactionStatus.CONFIRMED) {
      return;
    }

    TransactionEvent event = TransactionEvent.builder()
        .transaction(this)
        .blockConfirmation(this.blockConfirmation)
        .status(status)
        .build();

    if (eventList == null) {
      eventList = new ArrayList<>();
    }

    eventList.add(event);
  }

  public void mined(BigInteger minedBlockNumber, BigDecimal usedGasPrice) {
    if (this.status != TransactionStatus.PENDING) {
      return;
    }

    this.blockConfirmation = 1;
    this.blockNumber = minedBlockNumber;
    this.gasPrice = usedGasPrice;
    this.status = TransactionStatus.MINED;

    TransactionEvent event = TransactionEvent.builder()
        .transaction(this)
        .blockConfirmation(this.blockConfirmation)
        .status(status)
        .build();

    if (eventList == null) {
      eventList = new ArrayList<>();
    }

    eventList.add(event);
  }

  public void addBlockConfirmation() {
    this.blockConfirmation += 1;

    TransactionEvent event = TransactionEvent.builder()
        .transaction(this)
        .blockConfirmation(this.blockConfirmation)
        .status(TransactionStatus.MINED)
        .build();
    if (eventList == null) {
      eventList = new ArrayList<>();
    }
    eventList.add(event);
  }

  public void confirm(Wallet from, Wallet to) {
    if (this.status != TransactionStatus.MINED) {
      return;
    }

    if (from != null) {
      from.transfer(to, this.getValue(), this.getGasPrice());
    } else if (to != null) {
      to.receivedEther(this.getValue());
    }

    this.status = TransactionStatus.CONFIRMED;
    TransactionEvent event = TransactionEvent.builder()
        .transaction(this)
        .blockConfirmation(12)
        .status(this.status)
        .build();
    if (eventList == null) {
      eventList = new ArrayList<>();
    }
    eventList.add(event);
  }

  //blockNumber를 이용하여 confirmed상태인지 확인합니다.
  public boolean checkConfirmedBy(BigInteger blockNumber) {
    return this.getBlockNumber().longValue() + 11
        < blockNumber.longValue();
  }

  public int calculateBlockConfirmation(BigInteger latestBlockNumber) {
    BigInteger blockConfirmation = latestBlockNumber.subtract(this.blockNumber);
    if (blockConfirmation.compareTo(BigInteger.valueOf(11)) < 0) {
      return blockConfirmation.intValue() + 1;
    } else {
      return 12;
    }
  }
}
