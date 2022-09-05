package io.heachi.backend.api.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.heachi.backend.Response;
import io.heachi.backend.api.wallet.WalletDto.CreateDto;
import io.heachi.backend.api.wallet.WalletDto.TransactionDto;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.assertj.core.util.BigDecimalComparator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class WalletServiceTest {

  @Autowired
  TransactionRepo transactionRepo;

  @Autowired
  WalletRepo walletRepo;

  @Autowired
  AesUtil aesUtil;

  @MockBean
  EtherObserver etherObserver;

  FakeEthereum fakeEthereum;

  WalletService walletService;

  @PostConstruct
  void postConstructor() {
    fakeEthereum = new FakeEthereum();
    walletService = new WalletService(walletRepo, transactionRepo, fakeEthereum, aesUtil);
  }

  @Test
  void create() {
    CreateDto failCreateDto = new CreateDto();
    failCreateDto.setPassword("123123");

    assertThatThrownBy(() -> walletService.create(failCreateDto))
        .hasMessage("Short_Password");

    CreateDto createDto = new CreateDto();
    createDto.setPassword("1234567890qwerty");
    Response<WalletDto> response = walletService.create(createDto);

    assertThat(response.getPayload()).isNotNull();
    assertThat(response.getPayload().getAddress()).isEqualTo("fake_create_wallet");

    Wallet createdWallet = walletRepo.findByAddress("fake_create_wallet").orElse(null);
    assertThat(createdWallet).isNotNull();
    assertThat(createdWallet.getType()).isEqualByComparingTo(WalletType.ETHEREUM);
    assertThat(createdWallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

    String decryptPassword = aesUtil.decrypt(createDto.getPassword(),
        createdWallet.getPrivateKey());
    assertThat(decryptPassword).isEqualTo("fake_create_wallet_private_key");
  }

  @Test
  void getList() {
    Wallet wallet1 = Wallet.builder()
        .address("wallet1")
        .privateKey("privateKey1")
        .type(WalletType.ETHEREUM)
        .balance(BigDecimal.ZERO)
        .build();

    Wallet wallet2 = Wallet.builder()
        .address("wallet2")
        .privateKey("privateKey1")
        .type(WalletType.ETHEREUM)
        .balance(BigDecimal.valueOf(0.1))
        .build();

    walletRepo.save(wallet1);
    walletRepo.save(wallet2);

    Response<List<WalletDto>> response = walletService.getList();

    assertThat(response.getPayload().stream().map(WalletDto::getAddress)).contains("wallet1",
        "wallet2");
    assertThat(response.getPayload().stream().map(WalletDto::getBalance))
        .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal.class)
        .contains(BigDecimal.ZERO, BigDecimal.valueOf(0.1));
  }

  @Test
  void getDetailByAddress() {
    Wallet wallet1 = Wallet.builder()
        .address("wallet1")
        .privateKey("privateKey1")
        .type(WalletType.ETHEREUM)
        .balance(BigDecimal.ZERO)
        .build();
    walletRepo.save(wallet1);

    assertThatThrownBy(() -> walletService.getDetailByAddress("error_wallet"))
        .hasMessage("DoesNotExit_Wallet");

    Response<WalletDto> response = walletService.getDetailByAddress("wallet1");
    assertThat(response).isNotNull();

    assertThat(response.getPayload().getAddress()).isEqualTo("wallet1");
    assertThat(response.getPayload().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.getPayload().getUsableBalance()).isEqualByComparingTo(
        BigDecimal.valueOf(10));
  }

  @Test
  void getTransactionList() {
    Wallet wallet1 = Wallet.builder()
        .address("wallet1")
        .privateKey("privateKey1")
        .type(WalletType.ETHEREUM)
        .balance(BigDecimal.ZERO)
        .build();
    walletRepo.save(wallet1);

    Transaction transaction1 = Transaction.builder()
        .hash("HASH")
        .fromAddress("wallet1")
        .toAddress("fake_1")
        .value(BigDecimal.ONE)
        .gasPrice(BigDecimal.ZERO)
        .status(TransactionStatus.PENDING)
        .blockConfirmation(0)
        .blockNumber(BigInteger.valueOf(1232))
        .build();
    Transaction transaction2 = Transaction.builder()
        .hash(UUID.randomUUID().toString())
        .fromAddress("fake_2")
        .toAddress("fake_1")
        .value(BigDecimal.ONE)
        .gasPrice(BigDecimal.ZERO)
        .status(TransactionStatus.MINED)
        .blockConfirmation(6)
        .blockNumber(BigInteger.valueOf(1236))
        .build();
    Transaction transaction3 = Transaction.builder()
        .hash(UUID.randomUUID().toString())
        .fromAddress("fake_2")
        .toAddress("wallet1")
        .value(BigDecimal.ONE)
        .gasPrice(BigDecimal.ZERO)
        .status(TransactionStatus.CONFIRMED)
        .blockConfirmation(12)
        .blockNumber(BigInteger.valueOf(1244))
        .build();
    transactionRepo.save(transaction1);
    transactionRepo.save(transaction2);
    transactionRepo.save(transaction3);

    Response<Page<TransactionDto>> response = walletService.getTransactionList("wallet1",
        null,
        null,
        PageRequest.of(0, 10));
    assertThat(response).isNotNull();
    assertThat(response.getPayload().getSize()).isEqualTo(10);
    assertThat(response.getPayload().getContent().stream().map(TransactionDto::getFrom)).contains(
        "wallet1");
    assertThat(response.getPayload().getContent().stream().map(TransactionDto::getTo)).contains(
        "wallet1");

    Response<Page<TransactionDto>> oldResponse = walletService.getTransactionList("wallet1",
        LocalDateTime.now().minusDays(3),
        LocalDateTime.now().minusDays(1),
        PageRequest.of(0, 10));
    assertThat(oldResponse).isNotNull();
    assertThat(oldResponse.getPayload().getSize()).isEqualTo(10);
    assertThat(oldResponse.getPayload().getContent()).isEmpty();
  }
}