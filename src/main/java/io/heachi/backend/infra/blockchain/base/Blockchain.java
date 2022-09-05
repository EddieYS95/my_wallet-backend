package io.heachi.backend.infra.blockchain.base;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Transaction;

public interface Blockchain {

  WalletInfo createWallet();

  BigDecimal getBalance(String address);

  String transfer(String address, String privateKey, String toHash, BigDecimal eth);

  Optional<Transaction> findTransaction(String hash);

  BigInteger getLatestBlockNumber();
}
