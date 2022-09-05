package io.heachi.backend.infra.blockchain;

import io.heachi.backend.infra.blockchain.base.Blockchain;
import io.heachi.backend.infra.blockchain.base.WalletInfo;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import org.web3j.protocol.core.methods.response.Transaction;

public class FakeEthereum implements Blockchain {

  @Override
  public WalletInfo createWallet() {
    return new WalletInfo("fake_create_wallet", "fake_create_wallet_private_key");
  }

  @Override
  public BigDecimal getBalance(String address) {
    return BigDecimal.valueOf(10);
  }

  @Override
  public String transfer(String address, String privateKey, String toHash, BigDecimal eth) {
    return "transaction_hash";
  }

  @Override
  public Optional<Transaction> findTransaction(String hash) {
    return Optional.empty();
  }

  @Override
  public BigInteger getLatestBlockNumber() {
    return null;
  }
}
