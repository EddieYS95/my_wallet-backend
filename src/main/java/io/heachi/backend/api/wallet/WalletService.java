package io.heachi.backend.api.wallet;

import io.heachi.backend.Response;
import io.heachi.backend.api.wallet.WalletDto.CreateDto;
import io.heachi.backend.api.wallet.WalletDto.TransactionDto;
import io.heachi.backend.domain.transaction.repository.TransactionRepo;
import io.heachi.backend.domain.wallet.Wallet;
import io.heachi.backend.domain.wallet.WalletRepo;
import io.heachi.backend.domain.wallet.WalletType;
import io.heachi.backend.exception.LogicErrorList;
import io.heachi.backend.exception.LogicException;
import io.heachi.backend.infra.blockchain.Ethereum;
import io.heachi.backend.infra.blockchain.base.WalletInfo;
import io.heachi.backend.infra.crypto.AesUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    log.info("[WALLET] create wallet address: {}", wallet.getAddress());

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
    BigDecimal usableBalance = ethereum.getBalance(address);
    WalletDto payload = new WalletDto();
    BeanUtils.copyProperties(wallet, payload);
    payload.setUsableBalance(usableBalance);

    log.info("[WALLET] wallet detail balance: {}, usableBalance: {}", wallet.getBalance(), usableBalance);
    return Response.<WalletDto>ok().body(payload);
  }

  public Response<Page<TransactionDto>> getTransactionList(String walletAddress,
      LocalDateTime startingAfter,
      LocalDateTime endingBefore,
      Pageable pageable) {
    Wallet wallet = walletRepo.findByAddress(walletAddress)
        .orElseThrow(() -> new LogicException(LogicErrorList.DoesNotExit_Wallet));

    Page<TransactionDto> payload = transactionRepo.findAllDtoBy(wallet, startingAfter,
        endingBefore,
        pageable);

    log.info("[WALLET] get transaction list wallet address: {}", wallet.getAddress());
    return Response.<Page<TransactionDto>>ok().body(payload);
  }
}
