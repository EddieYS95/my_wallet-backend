package io.heachi.backend.domain.transaction;

import io.heachi.backend.domain.transaction.Transaction.TransactionStatus;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Builder
@Table(name = "_transaction_event",
    indexes = @Index(name = "created_at_indexing", columnList = "created_at DESC"))
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idfEvent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "idf_transaction")
  private Transaction transaction;

  @Enumerated(EnumType.STRING)
  private TransactionStatus status;

  private Integer blockConfirmation;

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;
}
