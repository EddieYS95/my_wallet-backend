package io.heachi.backend.exception;

import lombok.Data;

@Data
public class InternalServerExceptionVo {
  private int errorCode;
  private String errorMsg;
  private String payload;
}
