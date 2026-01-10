package dev.vundirov.domain.warehouseService.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {
  @Id
  @Column(name = "id", nullable = false)
  private Integer id;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

}