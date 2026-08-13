package com.ladiesapparel.order;

import com.ladiesapparel.address.Address;
import com.ladiesapparel.address.AddressRepository;
import com.ladiesapparel.auth.AuthenticatedUserProvider;
import com.ladiesapparel.auth.User;
import com.ladiesapparel.cart.Cart;
import com.ladiesapparel.cart.CartItem;
import com.ladiesapparel.cart.CartRepository;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.common.PagedResponse;
import com.ladiesapparel.coupon.CouponService;
import com.ladiesapparel.email.EmailService;
import com.ladiesapparel.notification.NotificationService;
import com.ladiesapparel.notification.NotificationType;
import com.ladiesapparel.order.dto.*;
import com.ladiesapparel.product.ProductImage;
import com.ladiesapparel.product.ProductVariant;
import com.ladiesapparel.product.ProductVariantRepository;
import com.ladiesapparel.sms.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository variantRepository;
    private final CouponService couponService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final SmsService smsService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Value("${app.shipping.free-above:999}")
    private BigDecimal freeShippingAbove;

    @Value("${app.shipping.flat-charge:79}")
    private BigDecimal flatShippingCharge;

    @Value("${app.cod.max-order-value:5000}")
    private BigDecimal codMaxOrderValue;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(OrderStatus.PLACED, OrderStatus.CONFIRMED);

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "dashboardSummary", allEntries = true),
            @CacheEvict(cacheNames = "topProducts", allEntries = true),
            @CacheEvict(cacheNames = "lowStock", allEntries = true),
    })
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.badRequest("Your cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw ApiException.badRequest("Your cart is empty");
        }

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), user.getId())
                .orElseThrow(() -> ApiException.notFound("Delivery address not found"));

        // 1) Validate stock for every item BEFORE touching anything
        for (CartItem item : cart.getItems()) {
            ProductVariant variant = item.getProductVariant();
            if (!variant.isActive() || variant.getStockQuantity() < item.getQuantity()) {
                throw ApiException.badRequest(
                        variant.getProduct().getName() + " (" + variant.getSize() + "/" + variant.getColor() +
                                ") no longer has enough stock. Please update your cart.");
            }
        }

        // 2) Compute pricing
        BigDecimal subtotal = cart.getItems().stream()
                .map(this::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gstAmount = cart.getItems().stream()
                .map(this::gstPortionOfLine)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedCouponCode = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            discountAmount = couponService.validateAndComputeDiscount(request.getCouponCode(), user, subtotal);
            appliedCouponCode = request.getCouponCode().trim().toUpperCase();
        }

        BigDecimal amountAfterDiscount = subtotal.subtract(discountAmount);
        BigDecimal shippingCharge = amountAfterDiscount.compareTo(freeShippingAbove) >= 0
                ? BigDecimal.ZERO
                : flatShippingCharge;

        BigDecimal grandTotal = amountAfterDiscount.add(shippingCharge);

        // 3) COD eligibility check
        if (request.getPaymentMethod() == PaymentMethod.COD && grandTotal.compareTo(codMaxOrderValue) > 0) {
            throw ApiException.badRequest(
                    "Cash on Delivery is not available for orders above Rs. " + codMaxOrderValue +
                            ". Please choose online payment.");
        }

        // 4) Create the order + snapshot items, deduct stock
        Order order = Order.builder()
                .orderNumber(generateUniqueOrderNumber())
                .user(user)
                .recipientName(address.getFullName())
                .recipientPhone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .landmark(address.getLandmark())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .couponCode(appliedCouponCode)
                .shippingCharge(shippingCharge)
                .gstAmount(gstAmount)
                .grandTotal(grandTotal)
                .status(OrderStatus.PLACED)
                .paymentMethod(request.getPaymentMethod())
                // Actual gateway verification happens via /api/payments/razorpay/verify + webhook
                // (Payments module). COD flips to PAID automatically when marked DELIVERED below.
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        List<OrderItem> orderItems = cart.getItems().stream()
                .map(item -> {
                    ProductVariant variant = item.getProductVariant();
                    var product = variant.getProduct();

                    String imageUrl = product.getImages().stream()
                            .filter(ProductImage::isPrimary)
                            .findFirst()
                            .or(() -> product.getImages().stream().min(Comparator.comparing(ProductImage::getDisplayOrder)))
                            .map(ProductImage::getImageUrl)
                            .orElse(null);

                    BigDecimal unitPrice = product.getBasePrice().add(variant.getAdditionalPrice());

                    // deduct stock now
                    variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
                    variantRepository.save(variant);

                    return OrderItem.builder()
                            .order(order)
                            .productVariant(variant)
                            .productName(product.getName())
                            .imageUrl(imageUrl)
                            .size(variant.getSize())
                            .color(variant.getColor())
                            .sku(variant.getSku())
                            .unitPrice(unitPrice)
                            .quantity(item.getQuantity())
                            .lineTotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                            .build();
                })
                .collect(Collectors.toList());

        order.setItems(orderItems);
        orderRepository.save(order);

        // 5) Record coupon usage, clear cart, send confirmation email
        if (appliedCouponCode != null) {
            couponService.recordUsage(appliedCouponCode, user, order.getOrderNumber());
        }

        cart.getItems().clear();
        cartRepository.save(cart);

        emailService.sendOrderConfirmationEmail(user.getEmail(), order.getOrderNumber(), grandTotal.toString());
        smsService.sendSms(order.getRecipientPhone(), "Your Ladies Apparel order " + order.getOrderNumber() + " for Rs. " + grandTotal + " has been placed.");
        notificationService.create(user, "Order Placed",
                "Your order " + order.getOrderNumber() + " has been placed successfully.",
                NotificationType.ORDER_UPDATE, "/orders/" + order.getOrderNumber());

        return toResponse(order);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "dashboardSummary", allEntries = true),
            @CacheEvict(cacheNames = "lowStock", allEntries = true),
    })
    public OrderResponse cancelOrder(String orderNumber) {
        User user = authenticatedUserProvider.getCurrentUser();
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, user.getId())
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw ApiException.badRequest("This order can no longer be cancelled");
        }

        // restore stock
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            variantRepository.save(variant);
        }

        order.setStatus(OrderStatus.CANCELLED);
        // Note: if paymentStatus was PAID (Razorpay), trigger a refund via
        // POST /api/admin/payments/refund/{orderNumber} — not done automatically here
        // so an admin can review before money moves.
        orderRepository.save(order);

        emailService.sendOrderStatusUpdateEmail(user.getEmail(), order.getOrderNumber(), "Cancelled");
        notificationService.create(user, "Order Cancelled",
                "Your order " + order.getOrderNumber() + " has been cancelled.",
                NotificationType.ORDER_UPDATE, "/orders/" + order.getOrderNumber());

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(String orderNumber) {
        User user = authenticatedUserProvider.getCurrentUser();
        Order order = orderRepository.findByOrderNumberAndUserIdWithItems(orderNumber, user.getId())
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        return toResponse(order);
    }

    /** Returns the raw entity (not the DTO) for invoice generation — owner or admin only. */
    @Transactional(readOnly = true)
    public Order getOrderEntityForInvoice(String orderNumber) {
        User user = authenticatedUserProvider.getCurrentUser();
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        boolean isOwner = order.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == com.ladiesapparel.auth.Role.ADMIN
                || user.getRole() == com.ladiesapparel.auth.Role.SUPER_ADMIN;

        if (!isOwner && !isAdmin) {
            throw ApiException.unauthorized("You do not have access to this order");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getMyOrders(Pageable pageable) {
        User user = authenticatedUserProvider.getCurrentUser();
        Page<Order> page = orderRepository.findByUserIdWithItems(user.getId(), pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    // ---------- Admin ----------

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrdersForAdmin(Pageable pageable) {
        Page<Order> page = orderRepository.findAllWithItems(pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "dashboardSummary", allEntries = true),
            @CacheEvict(cacheNames = "topProducts", allEntries = true),
    })
    public OrderResponse updateStatus(String orderNumber, OrderStatus newStatus) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw ApiException.badRequest("Cannot change status of a " + order.getStatus() + " order");
        }

        order.setStatus(newStatus);

        // Cash collected at the doorstep — COD orders are only "paid" once actually delivered.
        if (newStatus == OrderStatus.DELIVERED
                && order.getPaymentMethod() == PaymentMethod.COD
                && order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        // marks the start of the return/exchange eligibility window
        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(Instant.now());
        }

        orderRepository.save(order);

        // Keep the customer in the loop for the milestones that actually matter to them
        if (newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.OUT_FOR_DELIVERY
                || newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.CANCELLED) {
            emailService.sendOrderStatusUpdateEmail(order.getUser().getEmail(), order.getOrderNumber(), newStatus.name());
            smsService.sendOrderStatusSms(order.getRecipientPhone(), order.getOrderNumber(), newStatus.name().replace('_', ' '));
            notificationService.create(order.getUser(), "Order Update",
                    "Your order " + order.getOrderNumber() + " is now " + newStatus.name().replace('_', ' ') + ".",
                    NotificationType.ORDER_UPDATE, "/orders/" + order.getOrderNumber());
        }

        return toResponse(order);
    }

    // ---------- Helpers ----------

    private BigDecimal lineTotal(CartItem item) {
        ProductVariant variant = item.getProductVariant();
        BigDecimal unitPrice = variant.getProduct().getBasePrice().add(variant.getAdditionalPrice());
        return unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    /** Extracts the GST portion of a line total, assuming basePrice is GST-inclusive. Informational only — not added to grandTotal. */
    private BigDecimal gstPortionOfLine(CartItem item) {
        BigDecimal line = lineTotal(item);
        BigDecimal gstPercent = item.getProductVariant().getProduct().getGstPercentage();
        BigDecimal divisor = BigDecimal.ONE.add(gstPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal basePortion = line.divide(divisor, 2, RoundingMode.HALF_UP);
        return line.subtract(basePortion);
    }

    private String generateUniqueOrderNumber() {
        String datePart = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneId.of("Asia/Kolkata"))
                .format(Instant.now());
        String candidate;
        do {
            candidate = "ORD" + datePart + (1000 + RANDOM.nextInt(9000));
        } while (orderRepository.existsByOrderNumber(candidate));
        return candidate;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productName(item.getProductName())
                        .imageUrl(item.getImageUrl())
                        .size(item.getSize())
                        .color(item.getColor())
                        .sku(item.getSku())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .addressLine1(order.getAddressLine1())
                .addressLine2(order.getAddressLine2())
                .landmark(order.getLandmark())
                .city(order.getCity())
                .state(order.getState())
                .pincode(order.getPincode())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .couponCode(order.getCouponCode())
                .shippingCharge(order.getShippingCharge())
                .gstAmount(order.getGstAmount())
                .grandTotal(order.getGrandTotal())
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
