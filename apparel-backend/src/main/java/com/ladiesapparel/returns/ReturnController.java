package com.ladiesapparel.returns;

import com.ladiesapparel.common.ApiResponse;
import com.ladiesapparel.common.PagedResponse;
import com.ladiesapparel.returns.dto.CreateReturnRequest;
import com.ladiesapparel.returns.dto.ResolveReturnRequest;
import com.ladiesapparel.returns.dto.ReturnRequestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    // ---------- Customer endpoints ----------

    @PostMapping("/api/returns")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> requestReturn(@Valid @RequestBody CreateReturnRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Return/exchange request submitted", returnService.requestReturn(request)));
    }

    @GetMapping("/api/returns")
    public ResponseEntity<ApiResponse<PagedResponse<ReturnRequestResponse>>> getMyReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("requestedAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Return requests fetched", returnService.getMyReturns(pageable)));
    }

    // ---------- Admin endpoints ----------

    @GetMapping("/api/admin/returns")
    public ResponseEntity<ApiResponse<PagedResponse<ReturnRequestResponse>>> getPendingReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        Pageable pageable = PageRequest.of(page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("Pending returns fetched", returnService.getPendingReturns(pageable)));
    }

    @PatchMapping("/api/admin/returns/{id}/approve")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> approve(@PathVariable Long id,
                                                                       @RequestBody ResolveReturnRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Return approved", returnService.approve(id, request.getAdminNotes())));
    }

    @PatchMapping("/api/admin/returns/{id}/reject")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> reject(@PathVariable Long id,
                                                                      @RequestBody ResolveReturnRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Return rejected", returnService.reject(id, request.getAdminNotes())));
    }

    @PatchMapping("/api/admin/returns/{id}/complete")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> complete(@PathVariable Long id,
                                                                        @RequestBody ResolveReturnRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Return completed",
                returnService.complete(id, request.getAdminNotes(), request.getRefundAmount())));
    }
}
