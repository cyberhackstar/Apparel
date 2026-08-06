package com.ladiesapparel.order;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("select count(oi) > 0 from OrderItem oi " +
           "where oi.productVariant.product.id = :productId " +
           "and oi.order.user.id = :userId " +
           "and oi.order.status = com.ladiesapparel.order.OrderStatus.DELIVERED")
    boolean existsDeliveredPurchase(@Param("productId") Long productId, @Param("userId") Long userId);

    @Query("select oi.productVariant.product.id as productId, " +
           "oi.productVariant.product.name as productName, " +
           "sum(oi.quantity) as totalSold " +
           "from OrderItem oi " +
           "group by oi.productVariant.product.id, oi.productVariant.product.name " +
           "order by sum(oi.quantity) desc")
    List<TopProductProjection> findTopSellingProducts(Pageable pageable);

    interface TopProductProjection {
        Long getProductId();
        String getProductName();
        Long getTotalSold();
    }
}
