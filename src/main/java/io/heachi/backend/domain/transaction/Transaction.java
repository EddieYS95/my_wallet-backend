package io.heachi.backend.domain.transaction;

import io.heachi.backend.domain.wallet.Wallet;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Table(name = "_Transaction",
    indexes = @Index(name = "transaction_hash", unique = true, columnList = "hash"))
@Entity
@Getter
@Builder
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
  @Column(precision = 36, scale = 18)
  private BigDecimal value;
  @Column(precision = 36, scale = 18)
  private BigDecimal gasPrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionStatus status = TransactionStatus.PENDING;

  private String fromAddress;
  private String toAddress;
  private BigInteger blockNumber;

  @CreationTimestamp
  private LocalDateTime createAt;
  @UpdateTimestamp
  private LocalDateTime updateAt;

  public void mined(BigInteger minedBlockNumber, BigDecimal usedGasPrice) {
    if (this.status != TransactionStatus.PENDING) {
      return;
    }

    this.blockNumber = minedBlockNumber;
    this.gasPrice = usedGasPrice;
    this.status = TransactionStatus.MINED;
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
  }

  //blockNumber를 이용하여 confirmed상태인지 확인합니다.
  public boolean checkConfirmedBy(BigInteger blockNumber) {
    return this.getBlockNumber().longValue() + 11
        < blockNumber.longValue();
  }
}
