package io.heachi.backend.exception;

public class LogicException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private final LogicErrorList errorCode;

  public LogicException(LogicErrorList errorCode) {
    super(errorCode.getErrorMsg());
    this.errorCode = errorCode;
  }

  public LogicErrorList getErrorCode() {
    return errorCode;
  }

}