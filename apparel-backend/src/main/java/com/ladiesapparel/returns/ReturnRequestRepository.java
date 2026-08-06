package com.ladiesapparel.returns;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    @Query(value = "select distinct rr from ReturnRequest rr " +
            "join fetch rr.order o " +
            "join fetch rr.user u " +
            "where rr.user.id = :userId order by rr.requestedAt desc", countQuery = "select count(rr) from ReturnRequest rr where rr.user.id = :userId")
    Page<ReturnRequest> findByUserIdWithDetails(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "select distinct rr from ReturnRequest rr " +
            "join fetch rr.order o " +
            "join fetch rr.user u " +
            "where rr.status in :statuses order by rr.requestedAt asc", countQuery = "select count(rr) from ReturnRequest rr where rr.status in :statuses")
    Page<ReturnRequest> findByStatusInWithDetails(@Param("statuses") List<ReturnStatus> statuses, Pageable pageable);

    @Query("select rr from ReturnRequest rr " +
            "join fetch rr.order o " +
            "join fetch rr.user u " +
            "where rr.id = :id")
    Optional<ReturnRequest> findByIdWithDetails(@Param("id") Long id);

    List<ReturnRequest> findByOrderIdAndStatusIn(Long orderId, List<ReturnStatus> statuses);
}