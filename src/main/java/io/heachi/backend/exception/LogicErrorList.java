package io.heachi.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LogicErrorList {

  Invalid_Password(100, "Short_Password", "패스워드는 16자의 숫자 + 영문."),
  Fail_Transaction(200, "Fail_Transaction", "트랜잭션 요청에 실패했습니다."),
  NotEnough_Balance(201, "NotEnough_Balance", "지갑의 잔액이 부족합니다."),
  NotEnough_ChainBalance(202, "NotEnough_ChainBalance",
      "출금가능한 지갑의 잔액이 부족합니다. PENDING중이거나 MINED상태의 트랜잭션을 확인해 주세요."),
  DoesNotExit_Wallet(-1, "DoesNotExit_Wallet", "지갑을 찾을 수 없습니다."),
  DoesNotExit_Transaction(-2, "DoesNotExit_Transaction", "트랜잭션을 찾을 수 없습니다.");

  private final int errorCode;
  private final String errorMsg;
  private final String payload;
}
