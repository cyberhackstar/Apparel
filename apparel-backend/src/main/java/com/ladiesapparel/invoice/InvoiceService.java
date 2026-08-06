package com.ladiesapparel.invoice;

import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.order.Order;
import com.ladiesapparel.order.OrderItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceService {

    private static final Color WINE = new Color(122, 46, 56);
    private static final Color LIGHT_GREY = new Color(245, 245, 243);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy")
            .withZone(ZoneId.of("Asia/Kolkata"));

    public byte[] generateInvoice(Order order) {
        try {
            Document document = new Document(PageSize.A4, 40, 40, 50, 50);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, WINE);
            Font headingFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);

            // Header
            Paragraph title = new Paragraph("Ladies Apparel", titleFont);
            document.add(title);
            document.add(new Paragraph("TAX INVOICE", headingFont));
            document.add(Chunk.NEWLINE);

            // Order meta + address, side by side
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{1, 1});

            PdfPCell orderMetaCell = new PdfPCell();
            orderMetaCell.setBorder(Rectangle.NO_BORDER);
            orderMetaCell.addElement(new Paragraph("Invoice / Order No: " + order.getOrderNumber(), normalFont));
            orderMetaCell.addElement(new Paragraph("Order Date: " + DATE_FORMAT.format(order.getCreatedAt()), normalFont));
            orderMetaCell.addElement(new Paragraph("Payment Method: " + order.getPaymentMethod(), normalFont));
            metaTable.addCell(orderMetaCell);

            PdfPCell addressCell = new PdfPCell();
            addressCell.setBorder(Rectangle.NO_BORDER);
            addressCell.addElement(new Paragraph("Ship To:", headingFont));
            addressCell.addElement(new Paragraph(order.getRecipientName(), normalFont));
            addressCell.addElement(new Paragraph(order.getAddressLine1() +
                    (order.getAddressLine2() != null ? ", " + order.getAddressLine2() : ""), normalFont));
            addressCell.addElement(new Paragraph(order.getCity() + ", " + order.getState() + " - " + order.getPincode(), normalFont));
            addressCell.addElement(new Paragraph("Phone: " + order.getRecipientPhone(), normalFont));
            metaTable.addCell(addressCell);

            document.add(metaTable);
            document.add(Chunk.NEWLINE);

            // Items table
            PdfPTable itemsTable = new PdfPTable(5);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{3, 1.2f, 0.8f, 1.2f, 1.3f});

            addHeaderCell(itemsTable, "Item", headingFont);
            addHeaderCell(itemsTable, "Size/Color", headingFont);
            addHeaderCell(itemsTable, "Qty", headingFont);
            addHeaderCell(itemsTable, "Unit Price", headingFont);
            addHeaderCell(itemsTable, "Total", headingFont);

            for (OrderItem item : order.getItems()) {
                itemsTable.addCell(cell(item.getProductName(), normalFont));
                itemsTable.addCell(cell(item.getSize() + " / " + item.getColor(), normalFont));
                itemsTable.addCell(cell(String.valueOf(item.getQuantity()), normalFont));
                itemsTable.addCell(cell("Rs. " + item.getUnitPrice(), normalFont));
                itemsTable.addCell(cell("Rs. " + item.getLineTotal(), normalFont));
            }

            document.add(itemsTable);
            document.add(Chunk.NEWLINE);

            // Totals
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(50);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.setWidths(new float[]{1, 1});

            addTotalRow(totalsTable, "Subtotal", "Rs. " + order.getSubtotal(), normalFont);
            if (order.getDiscountAmount().signum() > 0) {
                addTotalRow(totalsTable, "Discount" + (order.getCouponCode() != null ? " (" + order.getCouponCode() + ")" : ""),
                        "- Rs. " + order.getDiscountAmount(), normalFont);
            }
            addTotalRow(totalsTable, "Shipping", order.getShippingCharge().signum() == 0 ? "FREE" : "Rs. " + order.getShippingCharge(), normalFont);
            addTotalRow(totalsTable, "GST (included)", "Rs. " + order.getGstAmount(), smallFont);
            addTotalRow(totalsTable, "Grand Total", "Rs. " + order.getGrandTotal(), headingFont);

            document.add(totalsTable);
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            Paragraph footer = new Paragraph(
                    "This is a computer-generated invoice and does not require a signature.\n" +
                            "For queries, contact support@ladiesapparel.com",
                    smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw ApiException.badRequest("Failed to generate invoice: " + e.getMessage());
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(LIGHT_GREY);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        return cell;
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(4);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }
}
