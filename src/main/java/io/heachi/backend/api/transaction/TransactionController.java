package io.heachi.backend.api.transaction;

import io.heachi.backend.Response;
import io.heachi.backend.api.transaction.TransactionDto.CreateDto;
import io.heachi.backend.api.transaction.TransactionDto.EventDto;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping("/events")
  public Response<Page<EventDto>> getEventList(
      @RequestParam(name = "starting_after", required = false)
      @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startingAfter,
      @RequestParam(name = "ending_before", required = false)
      @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endingBefore,
      @PageableDefault() Pageable pageable) {
    return transactionService.getEventList(startingAfter, endingBefore, pageable);
  }
}
