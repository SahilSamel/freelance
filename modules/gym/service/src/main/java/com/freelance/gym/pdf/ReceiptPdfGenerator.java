package com.freelance.gym.pdf;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates an official, beautifully styled PDF Receipt matching the layout of the Member Modal Card.
 */
public class ReceiptPdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public record ReceiptPlanItem(
            String planName,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal price
    ) {}

    public record ReceiptInstallmentItem(
            int installmentNumber,
            BigDecimal amount,
            BigDecimal paidAmount,
            LocalDate dueDate,
            LocalDate paidDate,
            String paymentMethod,
            String transactionId,
            String status
    ) {
        public ReceiptInstallmentItem(
                int installmentNumber,
                BigDecimal amount,
                BigDecimal paidAmount,
                LocalDate dueDate,
                LocalDate paidDate,
                String paymentMethod,
                String status
        ) {
            this(installmentNumber, amount, paidAmount, dueDate, paidDate, paymentMethod, null, status);
        }
    }

    public record ReceiptModel(
            String gymName,
            String gymAddress,
            String gymPhone,
            String receiptNumber,
            LocalDate issueDate,
            String memberName,
            String memberPhone,
            String memberUsername,
            List<ReceiptPlanItem> plans,
            List<ReceiptInstallmentItem> installments,
            BigDecimal totalPlanCost,
            BigDecimal totalAmountPaid,
            BigDecimal balanceDue,
            LocalDate nextDueDate,
            String paymentMode,
            String billingAddress,
            String gstin,
            String logoUrl
    ) {
        public ReceiptModel(
                String gymName,
                String gymAddress,
                String gymPhone,
                String receiptNumber,
                LocalDate issueDate,
                String memberName,
                String memberPhone,
                String memberUsername,
                List<ReceiptPlanItem> plans,
                List<ReceiptInstallmentItem> installments,
                BigDecimal totalPlanCost,
                BigDecimal totalAmountPaid,
                BigDecimal balanceDue,
                LocalDate nextDueDate,
                String paymentMode
        ) {
            this(
                    gymName,
                    gymAddress,
                    gymPhone,
                    receiptNumber,
                    issueDate,
                    memberName,
                    memberPhone,
                    memberUsername,
                    plans,
                    installments,
                    totalPlanCost,
                    totalAmountPaid,
                    balanceDue,
                    nextDueDate,
                    paymentMode,
                    null,
                    null,
                    null
            );
        }

        public ReceiptModel(
                String gymName,
                String gymAddress,
                String gymPhone,
                String receiptNumber,
                LocalDate issueDate,
                String memberName,
                String memberPhone,
                String memberUsername,
                List<ReceiptPlanItem> plans,
                List<ReceiptInstallmentItem> installments,
                BigDecimal totalPlanCost,
                BigDecimal totalAmountPaid,
                BigDecimal balanceDue,
                LocalDate nextDueDate,
                String paymentMode,
                String billingAddress,
                String gstin
        ) {
            this(
                    gymName,
                    gymAddress,
                    gymPhone,
                    receiptNumber,
                    issueDate,
                    memberName,
                    memberPhone,
                    memberUsername,
                    plans,
                    installments,
                    totalPlanCost,
                    totalAmountPaid,
                    balanceDue,
                    nextDueDate,
                    paymentMode,
                    billingAddress,
                    gstin,
                    null
            );
        }
    }

    public static byte[] generateReceiptPdf(ReceiptModel model) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Font setup: Attempt to use Arial / system font supporting unicode ₹ (U+20B9)
            Font titleFont;
            Font subtitleFont;
            Font sectionHeadingFont;
            Font boldFont;
            Font regularFont;
            Font smallMutedFont;

            BaseFont unicodeBase = null;
            String[] candidateFontPaths = {
                    "C:/Windows/Fonts/arial.ttf",
                    "C:/Windows/Fonts/arialbd.ttf",
                    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
            };
            for (String path : candidateFontPaths) {
                try {
                    java.io.File f = new java.io.File(path);
                    if (f.exists() && f.canRead()) {
                        unicodeBase = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                        if (unicodeBase.charExists(8377)) {
                            break;
                        } else {
                            unicodeBase = null;
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (unicodeBase != null) {
                titleFont = new Font(unicodeBase, 18, Font.BOLD, new Color(15, 23, 42));
                subtitleFont = new Font(unicodeBase, 10, Font.NORMAL, new Color(100, 116, 139));
                sectionHeadingFont = new Font(unicodeBase, 12, Font.BOLD, new Color(30, 41, 59));
                boldFont = new Font(unicodeBase, 10, Font.BOLD, new Color(15, 23, 42));
                regularFont = new Font(unicodeBase, 10, Font.NORMAL, new Color(51, 65, 85));
                smallMutedFont = new Font(unicodeBase, 8, Font.NORMAL, new Color(100, 116, 139));
            } else {
                titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 23, 42));
                subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
                sectionHeadingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(30, 41, 59));
                boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(15, 23, 42));
                regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(51, 65, 85));
                smallMutedFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 116, 139));
            }

            // 1. Header Table (Gym Branding & Receipt Info)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{65, 35});

            PdfPCell gymCell = new PdfPCell();
            gymCell.setBorder(Rectangle.NO_BORDER);

            // Logo space / image from system configuration (gym.brand.logoUrl)
            if (model.logoUrl() != null && !model.logoUrl().isBlank()) {
                try {
                    com.lowagie.text.Image logoImg = null;
                    if (model.logoUrl().startsWith("data:image/") && model.logoUrl().contains("base64,")) {
                        String b64 = model.logoUrl().substring(model.logoUrl().indexOf("base64,") + 7);
                        byte[] imgBytes = java.util.Base64.getDecoder().decode(b64);
                        logoImg = com.lowagie.text.Image.getInstance(imgBytes);
                    } else if (model.logoUrl().startsWith("http://") || model.logoUrl().startsWith("https://")) {
                        logoImg = com.lowagie.text.Image.getInstance(new java.net.URL(model.logoUrl()));
                    } else {
                        // Local filesystem path fallback
                        java.io.File f = new java.io.File(model.logoUrl());
                        if (f.exists()) {
                            logoImg = com.lowagie.text.Image.getInstance(model.logoUrl());
                        }
                    }

                    if (logoImg != null) {
                        logoImg.scaleToFit(140, 48);
                        logoImg.setSpacingAfter(6);
                        gymCell.addElement(logoImg);
                    }
                } catch (Exception ignored) {
                    // Fall back to clean title text
                }
            }

            gymCell.addElement(new Paragraph(model.gymName() != null ? model.gymName() : "FITNESS CENTER", titleFont));
            String effectiveAddress = (model.billingAddress() != null && !model.billingAddress().isBlank())
                    ? model.billingAddress()
                    : model.gymAddress();
            if (effectiveAddress != null && !effectiveAddress.isBlank()) {
                gymCell.addElement(new Paragraph(effectiveAddress, subtitleFont));
            }
            if (model.gymPhone() != null && !model.gymPhone().isBlank()) {
                gymCell.addElement(new Paragraph("Phone: " + model.gymPhone(), subtitleFont));
            }
            if (model.gstin() != null && !model.gstin().isBlank()) {
                gymCell.addElement(new Paragraph("GSTIN: " + model.gstin(), subtitleFont));
            }
            headerTable.addCell(gymCell);

            PdfPCell metaCell = new PdfPCell();
            metaCell.setBorder(Rectangle.NO_BORDER);
            metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            String dateStr = model.issueDate() != null ? model.issueDate().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);
            Paragraph rDate = new Paragraph("Date: " + dateStr, boldFont);
            rDate.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(rDate);
            headerTable.addCell(metaCell);

            document.add(headerTable);
            document.add(new Paragraph(" "));

            // 2. Member Information Box
            PdfPTable memberTable = new PdfPTable(1);
            memberTable.setWidthPercentage(100);

            PdfPCell mBox = new PdfPCell();
            mBox.setBackgroundColor(new Color(248, 250, 252));
            mBox.setBorderColor(new Color(226, 232, 240));
            mBox.setPadding(10);
            mBox.addElement(new Paragraph("BILLED TO:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(100, 116, 139))));
            mBox.addElement(new Paragraph(model.memberName() != null ? model.memberName() : "Member", boldFont));
            if (model.memberPhone() != null && !model.memberPhone().isBlank()) {
                mBox.addElement(new Paragraph("Contact: " + model.memberPhone(), regularFont));
            }
            memberTable.addCell(mBox);

            document.add(memberTable);
            document.add(new Paragraph(" "));

            // 3. Covered Plans Table
            document.add(new Paragraph("MEMBERSHIP PLANS & SERVICES", sectionHeadingFont));
            PdfPTable plansTable = new PdfPTable(2);
            plansTable.setWidthPercentage(100);
            plansTable.setWidths(new float[]{60, 40});
            plansTable.setSpacingBefore(6);

            addHeaderCell(plansTable, "Plan / Service", boldFont);
            addHeaderCell(plansTable, "Validity Period", boldFont);

            if (model.plans() != null && !model.plans().isEmpty()) {
                for (ReceiptPlanItem item : model.plans()) {
                    String period = (item.startDate() != null && item.endDate() != null)
                            ? item.startDate().format(DATE_FMT) + " – " + item.endDate().format(DATE_FMT)
                            : "Active Access";
                    addBodyCell(plansTable, item.planName(), regularFont);
                    addBodyCell(plansTable, period, regularFont);
                }
            } else {
                addBodyCell(plansTable, "General Membership", regularFont);
                addBodyCell(plansTable, "Ongoing", regularFont);
            }
            document.add(plansTable);
            document.add(new Paragraph(" "));

            // 4. Installments & Payment Breakdown
            document.add(new Paragraph("PAYMENT SCHEDULE & BREAKDOWN", sectionHeadingFont));
            PdfPTable instTable = new PdfPTable(6);
            instTable.setWidthPercentage(100);
            instTable.setWidths(new float[]{14, 18, 16, 20, 18, 14});
            instTable.setSpacingBefore(6);

            addHeaderCell(instTable, "Installment", boldFont);
            addHeaderCell(instTable, "Due Date", boldFont);
            addHeaderCell(instTable, "Amount Due", boldFont);
            addHeaderCell(instTable, "Paid Date & Method", boldFont);
            addHeaderCell(instTable, "TXN ID", boldFont);
            addHeaderCell(instTable, "Status", boldFont);

            Font paidStatusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(22, 101, 52));
            Font pendingStatusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(180, 83, 9));

            if (model.installments() != null && !model.installments().isEmpty()) {
                for (ReceiptInstallmentItem inst : model.installments()) {
                    String instLabel = "Inst #" + (inst.installmentNumber() > 0 ? inst.installmentNumber() : 1);
                    String dDate = inst.dueDate() != null ? inst.dueDate().format(DATE_FMT) : "-";
                    String due = formatRupeeAmount(inst.amount());
                    boolean isPaid = "PAID".equalsIgnoreCase(inst.status());

                    String paidDetails;
                    if (isPaid) {
                        String pDate = inst.paidDate() != null ? inst.paidDate().format(DATE_FMT) : dDate;
                        String pMode = (inst.paymentMethod() != null && !inst.paymentMethod().isBlank()) ? inst.paymentMethod() : "Direct";
                        paidDetails = pDate + " (" + pMode + ")";
                    } else {
                        paidDetails = "-";
                    }

                    String txnStr = (inst.transactionId() != null && !inst.transactionId().isBlank())
                            ? inst.transactionId()
                            : (isPaid && model.receiptNumber() != null ? model.receiptNumber() : "-");

                    addBodyCell(instTable, instLabel, regularFont);
                    addBodyCell(instTable, dDate, regularFont);
                    addBodyCell(instTable, due, regularFont);
                    addBodyCell(instTable, paidDetails, regularFont);
                    addBodyCell(instTable, txnStr, regularFont);
                    addBodyCell(instTable, isPaid ? "PAID" : "PENDING", isPaid ? paidStatusFont : pendingStatusFont);
                }
            } else {
                String paidFallback = formatRupeeAmount(model.totalAmountPaid());
                addBodyCell(instTable, "Inst #1", regularFont);
                addBodyCell(instTable, dateStr, regularFont);
                addBodyCell(instTable, paidFallback, regularFont);
                addBodyCell(instTable, dateStr + " (" + (model.paymentMode() != null ? model.paymentMode() : "Direct") + ")", regularFont);
                addBodyCell(instTable, model.receiptNumber() != null ? model.receiptNumber() : "-", regularFont);
                addBodyCell(instTable, "PAID", paidStatusFont);
            }
            document.add(instTable);
            document.add(new Paragraph(" "));

            // 5. Total Balance & GST Breakdown Summary Box
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{45, 55});

            PdfPCell noteCell = new PdfPCell();
            noteCell.setBorder(Rectangle.NO_BORDER);
            noteCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            noteCell.addElement(new Paragraph("Thank you for your payment!", boldFont));
            noteCell.addElement(new Paragraph("This is an official system-generated digital receipt.\nPlease preserve it for your membership records.", smallMutedFont));
            if (model.gstin() != null && !model.gstin().isBlank()) {
                Paragraph gstPara = new Paragraph("GSTIN: " + model.gstin(), smallMutedFont);
                gstPara.setSpacingBefore(4);
                noteCell.addElement(gstPara);
            }
            summaryTable.addCell(noteCell);

            PdfPCell totalBox = new PdfPCell();
            totalBox.setBackgroundColor(new Color(241, 245, 249));
            totalBox.setBorderColor(new Color(203, 213, 225));
            totalBox.setPadding(8);
            totalBox.setVerticalAlignment(Element.ALIGN_MIDDLE);

            BigDecimal paid = model.totalAmountPaid() != null ? model.totalAmountPaid() : BigDecimal.ZERO;
            BigDecimal bal = model.balanceDue() != null ? model.balanceDue() : BigDecimal.ZERO;
            BigDecimal totalQuoted = (model.totalPlanCost() != null && model.totalPlanCost().compareTo(BigDecimal.ZERO) > 0)
                    ? model.totalPlanCost()
                    : paid.add(bal);

            // GST Breakdown Calculation (18% GST inclusive on amount paid: Base = Paid / 1.18, CGST = 9%, SGST = 9%)
            BigDecimal baseAmount;
            BigDecimal cgstAmount;
            BigDecimal sgstAmount;
            if (paid.compareTo(BigDecimal.ZERO) > 0) {
                baseAmount = paid.divide(new BigDecimal("1.18"), 2, java.math.RoundingMode.HALF_UP);
                BigDecimal totalTax = paid.subtract(baseAmount);
                cgstAmount = totalTax.divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
                sgstAmount = totalTax.subtract(cgstAmount);
            } else {
                baseAmount = BigDecimal.ZERO.setScale(2);
                cgstAmount = BigDecimal.ZERO.setScale(2);
                sgstAmount = BigDecimal.ZERO.setScale(2);
            }

            PdfPTable gstTable = new PdfPTable(2);
            gstTable.setWidthPercentage(100);
            gstTable.setWidths(new float[]{60, 40});

            addTaxRow(gstTable, "Total Quoted (Price):", formatRupeeAmount(totalQuoted), boldFont);
            addTaxRow(gstTable, "Taxable Amount:", formatRupeeAmount(baseAmount), regularFont);
            addTaxRow(gstTable, "CGST (9%):", formatRupeeAmount(cgstAmount), regularFont);
            addTaxRow(gstTable, "SGST (9%):", formatRupeeAmount(sgstAmount), regularFont);
            addTaxRow(gstTable, "Total Paid (Incl. GST):", formatRupeeAmount(paid), boldFont);

            totalBox.addElement(gstTable);

            if (bal.compareTo(BigDecimal.ZERO) > 0) {
                Color pendingColor = new Color(180, 83, 9);
                Font pendingFont = unicodeBase != null
                        ? new Font(unicodeBase, 9, Font.BOLD, pendingColor)
                        : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, pendingColor);
                Paragraph pBal = new Paragraph("Total Pending: " + formatRupeeAmount(bal), pendingFont);
                pBal.setSpacingBefore(4);
                totalBox.addElement(pBal);
            }

            summaryTable.addCell(totalBox);
            document.add(summaryTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF receipt", e);
        }
    }

    private static String formatRupeeAmount(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        BigDecimal rounded = amount.setScale(2, java.math.RoundingMode.HALF_UP);
        long integerPart = rounded.longValue();
        String intStr = String.valueOf(Math.abs(integerPart));

        StringBuilder sb = new StringBuilder();
        if (intStr.length() <= 3) {
            sb.append(intStr);
        } else {
            String lastThree = intStr.substring(intStr.length() - 3);
            String rest = intStr.substring(0, intStr.length() - 3);
            StringBuilder restGrouped = new StringBuilder();
            for (int i = 0; i < rest.length(); i++) {
                if (i > 0 && (rest.length() - i) % 2 == 0) {
                    restGrouped.append(",");
                }
                restGrouped.append(rest.charAt(i));
            }
            sb.append(restGrouped).append(",").append(lastThree);
        }

        if (rounded.compareTo(BigDecimal.ZERO) < 0) {
            sb.insert(0, "-");
        }

        // Add 2 decimal places
        String plain = rounded.toPlainString();
        int dotIdx = plain.indexOf('.');
        if (dotIdx != -1) {
            sb.append(".").append(plain.substring(dotIdx + 1));
        } else {
            sb.append(".00");
        }

        return "₹" + sb.toString();
    }

    private static void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setBorderColor(new Color(203, 213, 225));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private static void addBodyCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private static void addTaxRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(2);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(2);
        table.addCell(valueCell);
    }
}
