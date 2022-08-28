package io.heachi.backend.infra.blockchain.base;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public interface Blockchain {

  WalletInfo createWallet();

  BigInteger getBalance(String address) throws IOException;

  String transfer(String address, String privateKey, String toHash, BigDecimal eth);
}
