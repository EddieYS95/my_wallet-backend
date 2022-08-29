package io.heachi.backend.domain.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "wallet")
@Builder
@AllArgsConstructor
@Getter
public class Wallet {

  public Wallet() {
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idfWallet;

  @Column(nullable = false, unique = true)
  private String address;

  @Column(nullable = false)
  private String privateKey;

  @Enumerated(EnumType.STRING)
  private WalletType type;

  @Column(precision = 36, scale = 18)
  private BigDecimal balance;

  @CreationTimestamp
  private LocalDateTime createAt;
  @UpdateTimestamp
  private LocalDateTime updateAt;

  public void receivedEther(BigDecimal ether) {
    this.balance = this.balance.add(ether);
  }

  public void transfer(Wallet to, BigDecimal ether, BigDecimal gasPrice) {
    if (to != null) {
      to.receivedEther(ether);
    }
    this.balance = this.balance.subtract(ether.add(gasPrice));
  }
}
