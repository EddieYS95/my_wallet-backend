package io.heachi.backend.api.transaction;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TransactionDto {


  @Data

  public static class CreateDto {

    private String fromAddress;
    private String password;
    private String toAddress;
    private BigDecimal eth;
  }
}
