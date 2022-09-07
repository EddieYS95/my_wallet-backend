package io.heachi.backend.api.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.heachi.backend.Response;
import io.heachi.backend.api.transaction.TransactionDto.CreateDto;
import io.heachi.backend.domain.transaction.Transaction;
import io.heachi.backend.domain.transaction.Transaction.TransactionStatus;
import io.heachi.backend.domain.transaction.repository.TransactionRepo;
import io.heachi.backend.domain.wallet.Wallet;
import io.heachi.backend.domain.wallet.WalletRepo;
import io.heachi.backend.domain.wallet.WalletType;
import io.heachi.backend.infra.blockchain.FakeEthereum;
import io.heachi.backend.infra.crypto.AesUtil;
import io.heachi.backend.observer.EtherObserver;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

@SpringBootTest
@Transactional
class TransactionServiceTest {

  @Autowired
  WalletRepo walletRepo;

  @Autowired
  TransactionRepo transactionRepo;

  FakeEthereum fakeEthereum;

  @Autowired
  AesUtil aesUtil;

  @MockBean
  EtherObserver etherObserver;

  TransactionService transactionService;


  @PostConstruct
  void postConstructor() {
    fakeEthereum = new FakeEthereum();
    transactionService = new TransactionService(walletRepo, transactionRepo, fakeEthereum, aesUtil);
  }


  @Test
  void create() {
    Wallet wallet = Wallet.builder()
        .address("fake_wallet")
        .balance(BigDecimal.valueOf(12))
        .privateKey(aesUtil.encrypt("1234567890qwerty", "fake_wallet"))
        .type(WalletType.ETHEREUM)
        .build();
    walletRepo.save(wallet);

    CreateDto createDto = new CreateDto();
    createDto.setFromAddress("fake_wallet");
    createDto.setToAddress("target_wallet");
    createDto.setEth(BigDecimal.valueOf(15));
    createDto.setPassword("1234567890qwerty");

    assertThatThrownBy(() -> transactionService.create(createDto))
        .hasMessage("NotEnough_Balance");

    createDto.setEth(BigDecimal.valueOf(11));
    assertThatThrownBy(() -> transactionService.create(createDto))
        .hasMessage("NotEnough_ChainBalance");

    createDto.setEth(BigDecimal.valueOf(0.4));
    Response<String> response = transactionService.create(createDto);
    assertThat(response).isNotNull();

    Transaction transaction = transactionRepo.findByHash("transaction_hash").orElse(null);
    assertThat(transaction).isNotNull();
    assertThat(transaction.getFromAddress()).isEqualTo("fake_wallet");
    assertThat(transaction.getToAddress()).isEqualTo("target_wallet");
    assertThat(transaction.getValue()).isEqualByComparingTo(BigDecimal.valueOf(0.4));
  }

  @Test
  void mined() {
    Transaction transaction = Transaction.builder()
        .hash("fake_transaction_hash")
        .fromAddress("from_wallet")
        .toAddress("to_wallet")
        .value(BigDecimal.valueOf(1.6))
        .status(TransactionStatus.PENDING)
        .build();
    transactionRepo.save(transaction);

    org.web3j.protocol.core.methods.response.Transaction web3Transaction = new org.web3j.protocol.core.methods.response.Transaction();
    web3Transaction.setHash("fake_transaction_hash");
    web3Transaction.setBlockNumber("0x0");
    web3Transaction.setGasPrice("0x" + Integer.toHexString(50000000));
    web3Transaction.setGas("0x" + Integer.toHexString(2));
    transactionService.mined(web3Transaction);

    assertThat(transaction.getStatus()).isEqualByComparingTo(TransactionStatus.MINED);
    assertThat(transaction.getGasPrice()).isEqualByComparingTo(
        Convert.fromWei(String.valueOf(50000000 * 2), Unit.ETHER));
  }

  @Test
  void confirmed() {
    Wallet wallet1 = Wallet.builder()
        .address("from_wallet")
        .privateKey("private_key")
        .type(WalletType.ETHEREUM)
        .balance(BigDecimal.valueOf(10))
        .build();
    walletRepo.save(wallet1);

    Transaction transaction = Transaction.builder()
        .hash("fake_transaction_hash")
        .fromAddress("from_wallet")
        .toAddress("to_wallet")
        .value(BigDecimal.valueOf(1.6))
        .gasPrice(BigDecimal.valueOf(0.0005))
        .status(TransactionStatus.MINED)
        .blockNumber(BigInteger.valueOf(10))
        .blockConfirmation(11)
        .build();
    transactionRepo.save(transaction);

    transactionService.confirmed(BigInteger.valueOf(5));
    assertThat(transaction.getStatus()).isEqualByComparingTo(TransactionStatus.MINED);

    transactionService.confirmed(BigInteger.valueOf(22));
    assertThat(transaction.getStatus()).isEqualByComparingTo(TransactionStatus.CONFIRMED);

    assertThat(wallet1.getBalance()).isEqualByComparingTo(
        BigDecimal.valueOf(10)
            .subtract(transaction.getValue())
            .subtract(transaction.getGasPrice()));
  }
}