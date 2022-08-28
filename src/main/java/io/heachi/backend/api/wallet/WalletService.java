package io.heachi.backend.api.wallet;

import io.heachi.backend.Response;
import io.heachi.backend.api.wallet.WalletDto.CreateDto;
import io.heachi.backend.api.wallet.WalletDto.TransactionDto;
import io.heachi.backend.domain.transaction.Transaction;
import io.heachi.backend.domain.transaction.Transaction.TransactionStatus;
import io.heachi.backend.domain.transaction.TransactionRepo;
import io.heachi.backend.domain.wallet.Wallet;
import io.heachi.backend.domain.wallet.WalletRepo;
import io.heachi.backend.domain.wallet.WalletType;
import io.heachi.backend.exception.LogicErrorList;
import io.heachi.backend.exception.LogicException;
import io.heachi.backend.infra.blockchain.Ethereum;
import io.heachi.backend.infra.blockchain.base.WalletInfo;
import io.heachi.backend.infra.crypto.AesUtil;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WalletService {

  private final WalletRepo walletRepo;
  private final TransactionRepo transactionRepo;

  private final Ethereum ethereum;
  private final AesUtil aesUtil;

  public Response<WalletDto> create(CreateDto createDto) {
    Pattern pattern = Pattern.compile("[A-Za-z0-9]{16}");
    if (!pattern.matcher(createDto.getPassword()).matches()) {
      throw new LogicException(LogicErrorList.Invalid_Password);
    }

    WalletInfo walletInfo = ethereum.createWallet();

    Wallet wallet = Wallet.builder()
        .address(walletInfo.getAddress())
        .privateKey(aesUtil.encrypt(createDto.getPassword(), walletInfo.getPrivateKey()))
        .balance(BigDecimal.ZERO)
        .type(WalletType.ETHEREUM)
        .build();
    walletRepo.save(wallet);

    WalletDto payload = new WalletDto();
    BeanUtils.copyProperties(wallet, payload);

    return Response.<WalletDto>ok().body(payload);
  }

  public Response<List<WalletDto>> getList() {
    List<WalletDto> payload = walletRepo.findAll().stream()
        .map((wallet -> {
          WalletDto walletDto = new WalletDto();
          BeanUtils.copyProperties(wallet, walletDto);
          return walletDto;
        })).collect(Collectors.toList());
    return Response.<List<WalletDto>>ok().body(payload);
  }

  public Response<WalletDto> getDetailByAddress(String address) {
    Wallet wallet = walletRepo.findByAddress(address)
        .orElseThrow(() -> new LogicException(LogicErrorList.DoesNotExit_Wallet));

    WalletDto payload = new WalletDto();
    BeanUtils.copyProperties(wallet, payload);

    return Response.<WalletDto>ok().body(payload);
  }

  public Response<Page<TransactionDto>> getTransactionList(String walletAddress,
      String startingAfter,
      String endingBefore,
      Pageable pageable) {
    Wallet wallet = walletRepo.findByAddress(walletAddress)
        .orElseThrow(() -> new LogicException(LogicErrorList.DoesNotExit_Wallet));
    Page<Transaction> transactionPage;

    // QueryDSL을 이용한 동적 쿼리 생성 및 최적화
    if (startingAfter == null && endingBefore == null) {
      transactionPage = transactionRepo.searchAllByToAddressOrFromAddress(wallet.getAddress(),
          wallet.getAddress(), pageable);
    } else if (endingBefore == null) {
      Transaction startTransaction = transactionRepo.findByHash(startingAfter)
          .orElseThrow(() -> new LogicException(LogicErrorList.DoesNotExit_Transaction));

      transactionPage = transactionRepo.searchAllByCreateAtGreaterThanEqual(
          startTransaction.getCreateAt(),
          pageable);
    } else if (startingAfter == null) {
      Transaction endTransaction = transactionRepo.findByHash(endingBefore)
          .orElseThrow(() -> new LogicException(LogicErrorList.DoesNotExit_Transaction));

      transactionPage = transactionRepo.searchAllByCreateAtLessThanEqual(
          endTransaction.getCreateAt(),
          pageable);
    } else {
      Transaction startTransaction = transactionRepo.findByHash(startingAfter)
          .orElseThrow(() -> new LogicException(LogicErrorList.DoesNotExit_Transaction));
      Transaction endTransaction = transactionRepo.findByHash(endingBefore)
          .orElseThrow(() -> new LogicException(LogicErrorList.DoesNotExit_Transaction));

      transactionPage = transactionRepo.searchAllByCreateAtBetween(startTransaction.getCreateAt(),
          endTransaction.getCreateAt(),
          pageable);
    }

    BigInteger latestBlockNumber = ethereum.getLatestBlockNumber();

    Page<TransactionDto> payload = transactionPage.map((transaction -> {
      TransactionDto transactionDto = new TransactionDto();
      BeanUtils.copyProperties(transaction, transactionDto);
      transactionDto.setFee(transaction.getGasPrice());
      transactionDto.setTo(transaction.getToAddress());
      transactionDto.setFrom(transaction.getFromAddress());

      if (transaction.getStatus() != TransactionStatus.PENDING) {
        int blockConfirmation = transaction.calculateBlockConfirmation(latestBlockNumber);
        transactionDto.setBlockConfirmation(blockConfirmation);
      }
      return transactionDto;
    }));

    return Response.<Page<TransactionDto>>ok().body(payload);
  }
}
