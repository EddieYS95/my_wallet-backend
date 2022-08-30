package io.heachi.backend.domain.transaction.repository;

import io.heachi.backend.api.transaction.TransactionDto.EventDto;
import io.heachi.backend.api.wallet.WalletDto;
import io.heachi.backend.domain.wallet.Wallet;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionCustomRepo {

  Page<EventDto> findAllEventDtoBy(LocalDateTime startingAfter, LocalDateTime endingBefore,
      Pageable pageable);

  Page<WalletDto.TransactionDto> findAllDtoBy(Wallet wallet, LocalDateTime startingAfter,
      LocalDateTime endingBefore,
      Pageable pageable);
}
