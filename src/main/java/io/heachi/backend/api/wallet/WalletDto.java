package io.heachi.backend.api.wallet;

import io.heachi.backend.domain.transaction.Transaction.TransactionStatus;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class WalletDto {

  private String address;
  private BigDecimal balance;

  @Data
  public static class CreateDto {

    private String password;
  }

  @Data
  public static class TransactionDto {

    private Long idfTransaction;
    private String hash;
    private TransactionStatus status;
    private Integer blockConfirmation;
    private BigDecimal value;
    private BigDecimal fee;
    private String from;
    private String to;
  }
}
