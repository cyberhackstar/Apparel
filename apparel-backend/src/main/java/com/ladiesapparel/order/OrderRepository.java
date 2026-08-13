package com.ladiesapparel.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Eagerly fetches line items in the same query — used for invoice generation, where the
     * Order entity is handed off to a different service (InvoiceService) after this method
     * returns, so the collection MUST already be initialized (a plain lazy proxy would throw
     * LazyInitializationException once this method's transaction closes).
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("select o from Order o where o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);

    /** Same idea, scoped to a specific user — used by getMyOrder() for ownership + eager items in one query. */
    @Query("select distinct o from Order o left join fetch o.items where o.orderNumber = :orderNumber and o.user.id = :userId")
    Optional<Order> findByOrderNumberAndUserIdWithItems(@Param("orderNumber") String orderNumber, @Param("userId") Long userId);

    /**
     * Paginated + eager items for a single user's order history. Per-user order counts are
     * naturally bounded (nobody has millions of orders), so the classic "collection fetch join
     * forces in-memory pagination" concern is an acceptable trade-off here — the explicit
     * countQuery keeps the total-count correct despite the join.
     */
    @Query(value = "select distinct o from Order o left join fetch o.items where o.user.id = :userId order by o.createdAt desc",
           countQuery = "select count(o) from Order o where o.user.id = :userId")
    Page<Order> findByUserIdWithItems(@Param("userId") Long userId, Pageable pageable);

    /**
     * Same for the admin "all orders" view. NOTE: unlike the per-user version, this scans the
     * whole orders table — fine at typical launch scale, but if the catalog of orders grows into
     * the millions, replace with the same two-step id-then-batch-fetch pattern used in
     * ProductService.search() instead of a single fetch-joined, in-memory-paginated query.
     */
    @Query(value = "select distinct o from Order o left join fetch o.items order by o.createdAt desc",
           countQuery = "select count(o) from Order o")
    Page<Order> findAllWithItems(Pageable pageable);

    long countByStatus(OrderStatus status);
    long countByCreatedAtAfter(Instant instant);

    @Query("select coalesce(sum(o.grandTotal), 0) from Order o where o.paymentStatus = com.ladiesapparel.order.PaymentStatus.PAID")
    BigDecimal getTotalRevenue();

    @Query("select coalesce(sum(o.grandTotal), 0) from Order o " +
           "where o.paymentStatus = com.ladiesapparel.order.PaymentStatus.PAID and o.createdAt >= :since")
    BigDecimal getRevenueSince(@Param("since") Instant since);

    // Native query: PostgreSQL date_trunc groups revenue/order-count by calendar day for a chart
    @Query(value = "select date_trunc('day', created_at) as day, " +
                   "coalesce(sum(grand_total), 0) as revenue, count(*) as orders_count " +
                   "from orders " +
                   "where payment_status = 'PAID' and created_at between :from and :to " +
                   "group by day order by day",
           nativeQuery = true)
    List<Object[]> getDailySalesReport(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select o from Order o where o.createdAt between :from and :to order by o.createdAt asc")
    List<Order> findAllBetween(@Param("from") Instant from, @Param("to") Instant to);
}
