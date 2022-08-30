package io.heachi.backend.api.wallet;

import io.heachi.backend.domain.transaction.Transaction.TransactionStatus;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class WalletDto {

  private String address;
  private BigDecimal balance;
  private BigDecimal usableBalance;

  @Data
  public static class CreateDto {

    private String password;
  }

  @Data
  @AllArgsConstructor
  public static class TransactionDto {

    private Long idfTransaction;
    private String hash;
    private TransactionStatus status;
    private int blockConfirmation;
    private BigDecimal value;
    private BigDecimal fee;
    private String from;
    private String to;
  }
}
