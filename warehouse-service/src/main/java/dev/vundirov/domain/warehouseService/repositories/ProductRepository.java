package dev.vundirov.domain.warehouseService.repositories;

import dev.vundirov.domain.warehouseService.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Integer> {

  @Modifying
  @Query("UPDATE Product s SET s.quantity = s.quantity - :amount " +
          "WHERE s.id = :productId AND s.quantity >= :amount")
  int reserveStock(@Param("productId") Integer productId, @Param("amount") Integer amount);
}