package com.ladiesapparel.invoice;

import com.ladiesapparel.order.Order;
import com.ladiesapparel.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InvoiceController {

    private final OrderService orderService;
    private final InvoiceService invoiceService;

    @GetMapping("/api/orders/{orderNumber}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable String orderNumber) {
        Order order = orderService.getOrderEntityForInvoice(orderNumber);
        byte[] pdfBytes = invoiceService.generateInvoice(order);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("invoice-" + orderNumber + ".pdf")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
