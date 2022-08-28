package io.heachi.backend.api.transaction;

import io.heachi.backend.Response;
import io.heachi.backend.api.transaction.TransactionDto.CreateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("transactions")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionService transactionService;

  @PostMapping()
  public Response<String> create(@RequestBody CreateDto createDto) {
    return transactionService.create(createDto);
  }

}
