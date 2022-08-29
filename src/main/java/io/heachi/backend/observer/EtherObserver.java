package io.heachi.backend.observer;

import io.heachi.backend.api.transaction.TransactionService;
import io.heachi.backend.infra.blockchain.Ethereum;
import java.math.BigInteger;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.EthBlock;

@Component
@RequiredArgsConstructor
@Slf4j
public class EtherObserver {

  private final Ethereum ethereum;
  private final TransactionService transactionService;

  @PostConstruct
  public void postConstruct() {
    startObserver();
  }

  private void subscribeFutureBlock() {
    log.info("start observer");
    ethereum.subscribeBlock(this::processBlock);
  }

  private void startObserver() {
    transactionService.processPendingTransaction();
    transactionService.processMinedTransaction();

    BigInteger latestTransactionBlockNumber = transactionService.getLatestTransactionBlockNumber();
    if (latestTransactionBlockNumber == null) {
      subscribeFutureBlock();
      return;
    }

    ethereum.subscribePastAndFutureBlock(latestTransactionBlockNumber,
        this::processBlock);
  }

  private void processBlock(EthBlock block) {
    block.getBlock().getTransactions()
        .stream().map(
            transactionResult -> (org.web3j.protocol.core.methods.response.Transaction) transactionResult.get())
        .filter(transactionService::checkRegularTransaction)
        .forEach(transactionService::mined);

    transactionService.confirmed(block.getBlock().getNumber());
  }
}
