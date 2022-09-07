package io.heachi.backend.api.transaction;

import io.heachi.backend.Response;
import io.heachi.backend.api.transaction.TransactionDto.CreateDto;
import io.heachi.backend.api.transaction.TransactionDto.EventDto;
import io.heachi.backend.domain.transaction.Transaction;
import io.heachi.backend.domain.transaction.Transaction.TransactionStatus;
import io.heachi.backend.domain.transaction.repository.TransactionRepo;
import io.heachi.backend.domain.wallet.Wallet;
import io.heachi.backend.domain.wallet.WalletRepo;
import io.heachi.backend.exception.LogicErrorList;
import io.heachi.backend.exception.LogicException;
import io.heachi.backend.infra.blockchain.base.Blockchain;
import io.heachi.backend.infra.crypto.AesUtil;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {

  private final WalletRepo walletRepo;
  private final TransactionRepo transactionRepo;

  private final Blockchain ethereum;
  private final AesUtil aesUtil;

  public Response<String> create(CreateDto createDto) {
    Wallet wallet = walletRepo.findByAddress(createDto.getFromAddress())
        .orElseThrow(() -> new LogicException(LogicErrorList.DoesNotExit_Wallet));

    String privateKey = aesUtil.decrypt(createDto.getPassword(), wallet.getPrivateKey());
    if (privateKey == null) {
      throw new LogicException(LogicErrorList.Invalid_Password);
    }

    if (wallet.getBalance().compareTo(createDto.getEth()) < 0) {
      throw new LogicException(LogicErrorList.NotEnough_Balance);
    }

    BigDecimal chainBalance = ethereum.getBalance(wallet.getAddress());
    if (chainBalance.compareTo(createDto.getEth()) < 0) {
      throw new LogicException(LogicErrorList.NotEnough_ChainBalance);
    }

    String transactionHash = ethereum.transfer(wallet.getAddress(), privateKey,
        createDto.getToAddress(),
        createDto.getEth());

    if (transactionHash == null) {
      throw new LogicException(LogicErrorList.Fail_Transaction);
    }

    Transaction transaction = Transaction.builder()
        .hash(transactionHash)
        .fromAddress(wallet.getAddress())
        .toAddress(createDto.getToAddress())
        .value(createDto.getEth())
        .status(TransactionStatus.PENDING)
        .build();
    transactionRepo.save(transaction);
    transaction.pending();

    log.info("[TRANSACTION] create transaction(PENDING) hash: {}, value: {}", transactionHash,
        createDto.getEth());
    return Response.<String>ok().body("Transaction Created");
  }

  public void mined(org.web3j.protocol.core.methods.response.Transaction web3Transaction) {
    try {
      Transaction transaction = transactionRepo.findByHash(web3Transaction.getHash())
          .orElse(null);
      BigInteger usedGasPrice = web3Transaction.getGasPrice().multiply(web3Transaction.getGas());

      if (transaction == null) {
        Wallet wallet = walletRepo.findByAddress(web3Transaction.getTo()).orElse(null);
        if (wallet == null) {
          return;
        }

        transaction = Transaction.builder()
            .hash(web3Transaction.getHash())
            .fromAddress(web3Transaction.getFrom())
            .toAddress(wallet.getAddress())
            .blockConfirmation(0)
            .value(Convert.fromWei(web3Transaction.getValue().toString(), Unit.ETHER))
            .status(TransactionStatus.PENDING)
            .build();
        transactionRepo.save(transaction);
      }

      BigInteger minedBlockNumber = web3Transaction.getBlockNumber();

      transaction.mined(minedBlockNumber,
          Convert.fromWei(usedGasPrice.toString(), Unit.ETHER));

      log.info(
          "[TRANSACTION] mine transaction(MINED) hash: {}, blockNumber: {}, transaction fee: {}",
          transaction.getHash(), minedBlockNumber,
          Convert.fromWei(usedGasPrice.toString(), Unit.ETHER));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void confirmed(BigInteger blockNumber) {
    List<Transaction> transactionList = transactionRepo.findAllByBlockConfirmationBefore(12);
    for (Transaction transaction : transactionList) {

      if (transaction.getBlockNumber() != null
          && transaction.getBlockNumber().compareTo(blockNumber) < 0) {
        transaction.addBlockConfirmation();
      }

      if (transaction.getBlockConfirmation() >= 12) {
        Wallet fromWallet = walletRepo.findByAddress(transaction.getFromAddress()).orElse(null);
        Wallet toWallet = walletRepo.findByAddress(transaction.getToAddress()).orElse(null);

        transaction.confirm(fromWallet, toWallet);
        log.info("[TRANSACTION] confirm transaction(CONFIRMED) hash: {}, value: {}",
            transaction.getHash(), transaction.getValue());
      }
    }
  }

  public boolean checkRegularTransaction(
      org.web3j.protocol.core.methods.response.Transaction transaction) {
    return transaction.getTo() != null && transaction.getFrom() != null
        && !transaction.getValue().equals(BigInteger.ZERO);
  }

  public void processPendingTransaction() {
    try {
      List<Transaction> transactionList = transactionRepo.findAllByStatus(
          TransactionStatus.PENDING);
      BigInteger latestBlockNumber = ethereum.getLatestBlockNumber();

      for (Transaction transaction : transactionList) {
        org.web3j.protocol.core.methods.response.Transaction web3Transaction = ethereum.findTransaction(
            transaction.getHash()).orElse(null);
        if (web3Transaction == null) {
          continue;
        }

        if (web3Transaction.getBlockNumber() != null) {
          BigInteger usedGasPrice = web3Transaction.getGasPrice()
              .multiply(web3Transaction.getGas());

          transaction.mined(web3Transaction.getBlockNumber(),
              Convert.fromWei(usedGasPrice.toString(), Unit.ETHER));

          if (transaction.checkConfirmedBy(latestBlockNumber)) {
            Wallet fromWallet = walletRepo.findByAddress(transaction.getFromAddress()).orElse(null);
            Wallet toWallet = walletRepo.findByAddress(transaction.getToAddress()).orElse(null);
            transaction.confirm(fromWallet, toWallet);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void processMinedTransaction() {
    List<Transaction> transactionList = transactionRepo.findAllByStatus(TransactionStatus.MINED);
    BigInteger latestBlockNumber = ethereum.getLatestBlockNumber();

    for (Transaction transaction : transactionList) {
      if (transaction.checkConfirmedBy(latestBlockNumber)) {
        Wallet fromWallet = walletRepo.findByAddress(transaction.getFromAddress()).orElse(null);
        Wallet toWallet = walletRepo.findByAddress(transaction.getToAddress()).orElse(null);
        transaction.confirm(fromWallet, toWallet);
      }
    }
  }

  public BigInteger getLatestTransactionBlockNumber() {
    Transaction transaction = transactionRepo.findTopByOrderByIdfTransactionDesc()
        .orElse(null);
    if (transaction == null) {
      return null;
    }
    return transaction.getBlockNumber();
  }

  public Response<Page<EventDto>> getEventList(LocalDateTime startingAfter,
      LocalDateTime endingBefore,
      Pageable pageable) {
    Page<EventDto> events = transactionRepo.findAllEventDtoBy(startingAfter, endingBefore,
        pageable);
    log.info("[TRANSACTION] getEventList startingAfter: {}, endingBefore:{}", startingAfter,
        endingBefore);
    return Response.<Page<EventDto>>ok().body(events);
  }
}
