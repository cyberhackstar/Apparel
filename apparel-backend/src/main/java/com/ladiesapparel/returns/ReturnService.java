package com.ladiesapparel.returns;

import com.ladiesapparel.auth.AuthenticatedUserProvider;
import com.ladiesapparel.auth.User;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.common.PagedResponse;
import com.ladiesapparel.notification.NotificationService;
import com.ladiesapparel.notification.NotificationType;
import com.ladiesapparel.order.Order;
import com.ladiesapparel.order.OrderRepository;
import com.ladiesapparel.order.OrderStatus;
import com.ladiesapparel.order.PaymentStatus;
import com.ladiesapparel.payment.PaymentService;
import com.ladiesapparel.returns.dto.CreateReturnRequest;
import com.ladiesapparel.returns.dto.ReturnRequestResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @Value("${app.returns.window-days:7}")
    private int returnWindowDays;

    private static final List<ReturnStatus> ACTIVE_STATUSES = List.of(ReturnStatus.REQUESTED, ReturnStatus.APPROVED);

    @Transactional
    public ReturnRequestResponse requestReturn(CreateReturnRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();

        Order order = orderRepository.findByOrderNumberAndUserId(request.getOrderNumber(), user.getId())
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw ApiException.badRequest("Only delivered orders are eligible for return/exchange");
        }

        if (order.getDeliveredAt() == null
                || ChronoUnit.DAYS.between(order.getDeliveredAt(), Instant.now()) > returnWindowDays) {
            throw ApiException.badRequest("The return window (" + returnWindowDays + " days from delivery) has expired");
        }

        if (!returnRequestRepository.findByOrderIdAndStatusIn(order.getId(), ACTIVE_STATUSES).isEmpty()) {
            throw ApiException.conflict("A return/exchange request is already active for this order");
        }

        ReturnRequest returnRequest = ReturnRequest.builder()
                .order(order)
                .user(user)
                .reason(request.getReason())
                .status(ReturnStatus.REQUESTED)
                .build();

        returnRequestRepository.save(returnRequest);
        return toResponse(returnRequest);
    }

    public PagedResponse<ReturnRequestResponse> getMyReturns(Pageable pageable) {
        User user = authenticatedUserProvider.getCurrentUser();
        Page<ReturnRequest> page = returnRequestRepository.findByUserIdOrderByRequestedAtDesc(user.getId(), pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    // ---------- Admin ----------

    /** Returns requests still needing admin action: newly REQUESTED, or APPROVED and awaiting completion. */
    public PagedResponse<ReturnRequestResponse> getPendingReturns(Pageable pageable) {
        Page<ReturnRequest> page = returnRequestRepository
                .findByStatusInOrderByRequestedAtAsc(List.of(ReturnStatus.REQUESTED, ReturnStatus.APPROVED), pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public ReturnRequestResponse approve(Long id, String adminNotes) {
        ReturnRequest returnRequest = getRequestedOrThrow(id);
        returnRequest.setStatus(ReturnStatus.APPROVED);
        returnRequest.setAdminNotes(adminNotes);
        returnRequest.setResolvedAt(Instant.now());
        returnRequestRepository.save(returnRequest);

        notificationService.create(returnRequest.getUser(), "Return Approved",
                "Your return request for order " + returnRequest.getOrder().getOrderNumber() + " has been approved.",
                NotificationType.ORDER_UPDATE, "/orders/" + returnRequest.getOrder().getOrderNumber());

        return toResponse(returnRequest);
    }

    @Transactional
    public ReturnRequestResponse reject(Long id, String adminNotes) {
        ReturnRequest returnRequest = getRequestedOrThrow(id);
        returnRequest.setStatus(ReturnStatus.REJECTED);
        returnRequest.setAdminNotes(adminNotes);
        returnRequest.setResolvedAt(Instant.now());
        returnRequestRepository.save(returnRequest);

        notificationService.create(returnRequest.getUser(), "Return Request Update",
                "Your return request for order " + returnRequest.getOrder().getOrderNumber() + " was not approved."
                        + (adminNotes != null ? " Reason: " + adminNotes : ""),
                NotificationType.ORDER_UPDATE, "/orders/" + returnRequest.getOrder().getOrderNumber());

        return toResponse(returnRequest);
    }

    @Transactional
    public ReturnRequestResponse complete(Long id, String adminNotes, BigDecimal refundAmount) {
        ReturnRequest returnRequest = returnRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Return request not found"));

        if (returnRequest.getStatus() != ReturnStatus.APPROVED) {
            throw ApiException.badRequest("Only approved returns can be marked complete");
        }

        Order order = returnRequest.getOrder();

        // trigger the actual gateway refund only for prepaid orders — COD refunds are settled
        // offline (bank transfer) and just recorded here for bookkeeping
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            try {
                paymentService.refundOrder(order.getOrderNumber());
            } catch (Exception e) {
                log.error("Automatic refund failed for order {}: {}. Admin can retry via the refund endpoint.",
                        order.getOrderNumber(), e.getMessage());
            }
        }

        order.setStatus(OrderStatus.RETURNED);
        orderRepository.save(order);

        returnRequest.setStatus(ReturnStatus.COMPLETED);
        returnRequest.setAdminNotes(adminNotes);
        returnRequest.setRefundAmount(refundAmount);
        returnRequest.setResolvedAt(Instant.now());
        returnRequestRepository.save(returnRequest);

        notificationService.create(returnRequest.getUser(), "Return Completed",
                "Your return for order " + order.getOrderNumber() + " has been processed"
                        + (refundAmount != null ? ". Refund amount: Rs. " + refundAmount : "") + ".",
                NotificationType.ORDER_UPDATE, "/orders/" + order.getOrderNumber());

        return toResponse(returnRequest);
    }

    private ReturnRequest getRequestedOrThrow(Long id) {
        ReturnRequest returnRequest = returnRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Return request not found"));
        if (returnRequest.getStatus() != ReturnStatus.REQUESTED) {
            throw ApiException.badRequest("This return request has already been resolved");
        }
        return returnRequest;
    }

    private ReturnRequestResponse toResponse(ReturnRequest r) {
        return ReturnRequestResponse.builder()
                .id(r.getId())
                .orderNumber(r.getOrder().getOrderNumber())
                .customerEmail(r.getUser().getEmail())
                .reason(r.getReason())
                .status(r.getStatus())
                .adminNotes(r.getAdminNotes())
                .refundAmount(r.getRefundAmount())
                .requestedAt(r.getRequestedAt())
                .resolvedAt(r.getResolvedAt())
                .build();
    }
}
