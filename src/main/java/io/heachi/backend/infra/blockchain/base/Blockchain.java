package io.heachi.backend.infra.blockchain.base;

import java.io.IOException;
import java.math.BigDecimal;

public interface Blockchain {

  WalletInfo createWallet();

  BigDecimal getBalance(String address) throws IOException;

  String transfer(String address, String privateKey, String toHash, BigDecimal eth);
}
