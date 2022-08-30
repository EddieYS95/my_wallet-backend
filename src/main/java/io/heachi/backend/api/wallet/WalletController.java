package io.heachi.backend.api.wallet;

import io.heachi.backend.Response;
import io.heachi.backend.api.wallet.WalletDto.CreateDto;
import io.heachi.backend.api.wallet.WalletDto.TransactionDto;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

  private final WalletService walletService;

  @PostMapping()
  public Response<WalletDto> create(@RequestBody CreateDto createDto) {
    return walletService.create(createDto);
  }

  @GetMapping()
  public Response<List<WalletDto>> getList() {
    return walletService.getList();
  }

  @GetMapping("/{address}")
  public Response<WalletDto> getDetailByAddress(@PathVariable String address) {
    return walletService.getDetailByAddress(address);
  }

  @GetMapping("/{address}/transactions")
  public Response<Page<TransactionDto>> getTransactionList(@PathVariable String address,
      @RequestParam(name = "starting_after", required = false)
      @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startingAfter,
      @RequestParam(name = "ending_before", required = false)
      @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endingBefore,
      @PageableDefault(sort = "idfTransaction", direction = Sort.Direction.DESC) Pageable pageable) {
    return walletService.getTransactionList(address, startingAfter, endingBefore, pageable);
  }
}
