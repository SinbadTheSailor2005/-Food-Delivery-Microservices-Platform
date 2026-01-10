package dev.vundirov.orderservice.domain.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem {
  @Id
  @Column(name = "id", nullable = false)
  private Integer id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @NotNull
  @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2)
  private BigDecimal priceAtPurchase;

  @NotNull
  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;

    Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
    Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();

    if (thisEffectiveClass != oEffectiveClass) return false;

    OrderItem orderItem = (OrderItem) o;
    return getId() != null && Objects.equals(getId(), orderItem.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
  }

}