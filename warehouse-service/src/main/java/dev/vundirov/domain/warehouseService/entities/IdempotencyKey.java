package dev.vundirov.domain.warehouseService.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "idempotency_keys")
public class IdempotencyKey {
  @Id
  @Column(name = "message_id", nullable = false, length = Integer.MAX_VALUE)
  private String messageId;


}