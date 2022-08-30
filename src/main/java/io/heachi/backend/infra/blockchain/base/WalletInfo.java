package io.heachi.backend.infra.blockchain.base;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WalletInfo {

  private String address;
  private String privateKey;
}
