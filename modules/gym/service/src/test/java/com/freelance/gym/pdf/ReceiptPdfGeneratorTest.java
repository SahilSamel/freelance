package com.freelance.gym.pdf;

import com.freelance.gym.pdf.ReceiptPdfGenerator.ReceiptInstallmentItem;
import com.freelance.gym.pdf.ReceiptPdfGenerator.ReceiptModel;
import com.freelance.gym.pdf.ReceiptPdfGenerator.ReceiptPlanItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReceiptPdfGeneratorTest {

    @Test
    void testGenerateReceiptPdf_WithMultipleInstallments() {
        List<ReceiptPlanItem> plans = List.of(
                new ReceiptPlanItem("Gold 1 Month", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), BigDecimal.valueOf(3000)),
                new ReceiptPlanItem("Personal Training 10 Sessions", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28), BigDecimal.valueOf(5000))
        );

        List<ReceiptInstallmentItem> installments = List.of(
                new ReceiptInstallmentItem(1, BigDecimal.valueOf(4000), BigDecimal.valueOf(4000), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), "UPI", "PAID"),
                new ReceiptInstallmentItem(2, BigDecimal.valueOf(4000), BigDecimal.ZERO, LocalDate.of(2026, 2, 1), null, "-", "PENDING")
        );

        ReceiptModel model = new ReceiptModel(
                "Power Fitness Hub",
                "123 Gym Street, Bandra West, Mumbai",
                "+91 98765 43210",
                "TXN_MOCK_12345",
                LocalDate.of(2026, 1, 1),
                "Aaryan Thakur",
                "+919876543210",
                "mem_9876543210",
                plans,
                installments,
                BigDecimal.valueOf(8000),
                BigDecimal.valueOf(4000),
                BigDecimal.valueOf(4000),
                LocalDate.of(2026, 2, 1),
                "UPI"
        );

        byte[] pdfBytes = ReceiptPdfGenerator.generateReceiptPdf(model);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF bytes should be non-empty and well-formed");
        // Verify PDF Magic Bytes (%PDF-)
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
        assertEquals('-', (char) pdfBytes[4]);
    }

    @Test
    void testGenerateReceiptPdf_EmptyPlansAndInstallmentsFallback() {
        ReceiptModel model = new ReceiptModel(
                "Power Fitness Hub",
                null,
                null,
                "REC-FALLBACK",
                LocalDate.now(),
                "Walk-in Guest",
                null,
                null,
                List.of(),
                List.of(),
                BigDecimal.valueOf(1500),
                BigDecimal.valueOf(1500),
                BigDecimal.ZERO,
                null,
                "Cash"
        );

        byte[] pdfBytes = ReceiptPdfGenerator.generateReceiptPdf(model);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
    }

    @Test
    void testRupeeSymbolSupport() throws Exception {
        com.lowagie.text.pdf.BaseFont arial = com.lowagie.text.pdf.BaseFont.createFont(
                "C:/Windows/Fonts/arial.ttf",
                com.lowagie.text.pdf.BaseFont.IDENTITY_H,
                com.lowagie.text.pdf.BaseFont.EMBEDDED
        );
        assertTrue(arial.charExists(8377), "Arial should contain ₹ (8377)");

        com.lowagie.text.Document doc = new com.lowagie.text.Document();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        com.lowagie.text.pdf.PdfWriter.getInstance(doc, baos);
        doc.open();
        com.lowagie.text.Font f = new com.lowagie.text.Font(arial, 12);
        doc.add(new com.lowagie.text.Paragraph("Amount: \u20B9 10,000", f));
        doc.close();

        byte[] pdfBytes = baos.toByteArray();
        assertTrue(pdfBytes.length > 500);
    }

    @Test
    void testGenerateReceiptPdf_WithGstBreakdownAndBillingAddress() {
        ReceiptModel model = new ReceiptModel(
                "IronPulse Fitness",
                "Platinum Hub, Link Road",
                "+91 98765 43210",
                "REC-GST-10000",
                LocalDate.of(2026, 10, 17),
                "Sahil Samel",
                "+919876543210",
                "sahilsamel",
                List.of(new ReceiptPlanItem("Annual Gym Membership", LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30), BigDecimal.valueOf(10000))),
                List.of(new ReceiptInstallmentItem(1, BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 17), "UPI", "PAID")),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(10000),
                BigDecimal.ZERO,
                null,
                "UPI",
                "Platinum Hub, Link Road, Mumbai, Maharashtra 400053",
                "27AAAAA0000A1Z5"
        );

        byte[] pdfBytes = ReceiptPdfGenerator.generateReceiptPdf(model);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
        assertEquals('-', (char) pdfBytes[4]);
    }
}
