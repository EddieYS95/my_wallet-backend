package io.heachi.backend.observer;

import io.heachi.backend.api.transaction.TransactionService;
import io.heachi.backend.infra.blockchain.Ethereum;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

  private void runMissingAction() {
    transactionService.processPendingTransaction();
    transactionService.processMinedTransaction();

    ethereum.subscribePastBlock(transactionService.getLatestTransactionCount(), (block) -> {
      log.info("past block number {}", block.getBlock().getNumber());
      block.getBlock().getTransactions()
          .stream().map(
              transactionResult -> (org.web3j.protocol.core.methods.response.Transaction) transactionResult.get())
          .forEach(transactionService::mining);
      transactionService.confirm(block.getBlock().getNumber());
    });
  }

  private void startObserver() {
    transactionService.subscribeBlock();

    ethereum.subscribeBlock(block -> {
      log.info("block number {}", block.getBlock().getNumber());
      block.getBlock().getTransactions().stream()
          .map(
              transactionResult -> (org.web3j.protocol.core.methods.response.Transaction) transactionResult.get())
          .filter(transactionService::checkRegularTransaction)
          .forEach(transactionService::mining);

      transactionService.confirm(block.getBlock().getNumber());
    });
  }


}
