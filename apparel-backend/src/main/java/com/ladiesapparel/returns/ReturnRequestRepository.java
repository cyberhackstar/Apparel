package com.ladiesapparel.returns;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    Page<ReturnRequest> findByUserIdOrderByRequestedAtDesc(Long userId, Pageable pageable);
    Page<ReturnRequest> findByStatusOrderByRequestedAtAsc(ReturnStatus status, Pageable pageable);
    Page<ReturnRequest> findByStatusInOrderByRequestedAtAsc(List<ReturnStatus> statuses, Pageable pageable);
    List<ReturnRequest> findByOrderIdAndStatusIn(Long orderId, List<ReturnStatus> statuses);
}
