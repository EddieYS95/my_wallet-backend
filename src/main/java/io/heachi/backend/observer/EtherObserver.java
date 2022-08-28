package io.heachi.backend.observer;

import io.heachi.backend.api.transaction.TransactionService;
import io.heachi.backend.infra.blockchain.Ethereum;
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
    runMissingAction();
  }

  private void startObserver() {
    ethereum.subscribeBlock(this::processTransaction);
  }

  private void runMissingAction() {
    transactionService.processPendingTransaction();
    transactionService.processMinedTransaction();

    ethereum.subscribePastBlock(transactionService.getLatestTransactionBlockNumber(),
        this::processTransaction);
  }

  private void processTransaction(EthBlock block) {
    block.getBlock().getTransactions()
        .stream().map(
            transactionResult -> (org.web3j.protocol.core.methods.response.Transaction) transactionResult.get())
        .filter(transactionService::checkRegularTransaction)
        .forEach(transactionService::mined);

    transactionService.confirmed(block.getBlock().getNumber());
  }
}
