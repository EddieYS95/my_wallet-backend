package io.heachi.backend.api.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.heachi.backend.domain.transaction.Transaction.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class TransactionDto {

  @Data
  @AllArgsConstructor
  public static class EventDto {

    private Long idfEvent;
    private String hash;
    private TransactionStatus status;
    private Integer blockConfirmation;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
  }

  @Data
  public static class CreateDto {

    private String fromAddress;
    private String password;
    private String toAddress;
    private BigDecimal eth;
  }
}
