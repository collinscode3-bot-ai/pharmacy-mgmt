package org.pharmacy.mgmt.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.pharmacy.mgmt.model.Inventory;
import org.pharmacy.mgmt.model.Medicine;
import org.pharmacy.mgmt.model.PaymentBreakdown;
import org.pharmacy.mgmt.model.Sale;
import org.pharmacy.mgmt.model.SaleItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("MM/yy");

    private static final float OUTER_LEFT = 20f;
    private static final float OUTER_RIGHT = 575f;
    private static final float OUTER_TOP = 800f;
    private static final float OUTER_BOTTOM = 60f; 

    private static final float RIGHT_HEADER_SPLIT_X = 350f;
    private static final float HEADER_BOTTOM = 715f;
    private static final float TITLE_BOTTOM = 675f;
    private static final float TABLE_HEADER_BOTTOM = 655f;

    private static final float TOTAL_PANEL_LEFT = 410f;
    private static final float MAX_ROW_HEIGHT = 22f;
    private static final float MIN_ROW_HEIGHT = 15f;

    private static final float GST_BAND_HEIGHT = 20f;
    private static final float DETAILS_BAND_HEIGHT = 110f;
    private static final float GRAND_BAND_HEIGHT = 35f;

    private static final float[] TABLE_COLS = {20f, 40f, 215f, 245f, 275f, 325f, 360f, 400f, 455f, 485f, 515f, 575f};

    private static final BigDecimal TWO = new BigDecimal("2");

    private static final String[] BELOW_TWENTY = {
            "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
    };
    private static final String[] TENS = {
            "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
    };

    @Value("${sales.invoice.output-root:logs/invoices}")
    private String invoiceOutputRoot;

    @Value("${sales.invoice.pharmacy.name:M/S MANISH PHARMACY}")
    private String pharmacyName;

    @Value("${sales.invoice.pharmacy.address-line1:H.NO.9-1-127/4/1/1F-5A, FIRST FLOOR,REZIMENTA}")
    private String pharmacyAddressLine1;

    @Value("${sales.invoice.pharmacy.address-line2:BAZAR,SANGEET ROAD,SECUNDERABAD}")
    private String pharmacyAddressLine2;

    @Value("${sales.invoice.pharmacy.phone:9666027727}")
    private String pharmacyPhone;

    @Value("${sales.invoice.pharmacy.email:reddy.pothula@gmail.com}")
    private String pharmacyEmail;

    @Value("${sales.invoice.pharmacy.gstin:36FLJPB8033H1Z3}")
    private String pharmacyGstin;

    @Value("${sales.invoice.pharmacy.dl-no:TG/HYD/2024-122590}")
    private String pharmacyDlNo;

    @Value("${sales.invoice.terms.lines:Goods once sold will not be taken back or exchanged.|Bills not paid due date will attract 24% interest.|All disputes subject to jurisdiction only.|Prescribed Sales Tax declaration will be given.}")
    private String termsLinesRaw;

    public InvoicePdfResult generateAndArchiveInvoice(Sale sale, List<SaleItem> saleItems, List<PaymentBreakdown> paymentRows) {
        try {
            byte[] bytes = buildPdfBytes(sale, saleItems, paymentRows);
            String fileName = buildFileName(sale);
            Path archivedFile = archivePdf(fileName, bytes);
            return new InvoicePdfResult(fileName, "application/pdf", bytes, archivedFile.toString());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate invoice PDF", ex);
        }
    }

    private byte[] buildPdfBytes(Sale sale, List<SaleItem> saleItems, List<PaymentBreakdown> paymentRows) throws IOException {
        Layout layout = Layout.build(saleItems == null ? 0 : saleItems.size());

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                drawFrame(stream, layout);
                drawStaticData(stream, layout);
                drawDynamicData(stream, layout, sale, saleItems, paymentRows);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private void drawFrame(PDPageContentStream stream, Layout layout) throws IOException {
        stream.setLineWidth(0.8f);
        
        drawRect(stream, OUTER_LEFT, layout.grandTotalBottom, OUTER_RIGHT - OUTER_LEFT, OUTER_TOP - layout.grandTotalBottom);

        drawLine(stream, OUTER_LEFT, HEADER_BOTTOM, OUTER_RIGHT, HEADER_BOTTOM);
        drawLine(stream, RIGHT_HEADER_SPLIT_X, OUTER_TOP, RIGHT_HEADER_SPLIT_X, HEADER_BOTTOM);
        
        drawLine(stream, OUTER_LEFT, TITLE_BOTTOM, OUTER_RIGHT, TITLE_BOTTOM); 
        
        // This draws the specific line highlighted in the user prompt (top border of product table)
        drawLine(stream, OUTER_LEFT, TITLE_BOTTOM, OUTER_RIGHT, TITLE_BOTTOM); 

        drawLine(stream, RIGHT_HEADER_SPLIT_X, TITLE_BOTTOM, RIGHT_HEADER_SPLIT_X, TABLE_HEADER_BOTTOM);

        stream.setNonStrokingColor(212, 241, 239);
        stream.addRect(OUTER_LEFT, TABLE_HEADER_BOTTOM, OUTER_RIGHT - OUTER_LEFT, TITLE_BOTTOM - TABLE_HEADER_BOTTOM);
        stream.fill();
        stream.setNonStrokingColor(0, 0, 0);

        drawLine(stream, OUTER_LEFT, TABLE_HEADER_BOTTOM, OUTER_RIGHT, TABLE_HEADER_BOTTOM);

        for (float col : TABLE_COLS) {
            drawLine(stream, col, TITLE_BOTTOM, col, layout.tableBottom);
        }

        drawLine(stream, OUTER_LEFT, layout.tableBottom, OUTER_RIGHT, layout.tableBottom);
        drawLine(stream, OUTER_LEFT, layout.gstSummaryBottom, OUTER_RIGHT, layout.gstSummaryBottom);
        drawLine(stream, OUTER_LEFT, layout.detailsBottom, OUTER_RIGHT, layout.detailsBottom);

        drawLine(stream, TOTAL_PANEL_LEFT, layout.tableBottom, TOTAL_PANEL_LEFT, layout.grandTotalBottom);
    }

    private void drawStaticData(PDPageContentStream stream, Layout layout) throws IOException {
        writeLine(stream, 25, 785, PDType1Font.HELVETICA_BOLD, 11, pharmacyName);
        writeLine(stream, 25, 772, PDType1Font.HELVETICA, 8, pharmacyAddressLine1);
        writeLine(stream, 25, 760, PDType1Font.HELVETICA, 8, pharmacyAddressLine2);
        writeLine(stream, 25, 748, PDType1Font.HELVETICA, 8, "Phone : " + pharmacyPhone);
        writeLine(stream, 140, 748, PDType1Font.HELVETICA, 8, "DL No. " + pharmacyDlNo);
        writeLine(stream, 25, 736, PDType1Font.HELVETICA, 8, "E-Mail : " + pharmacyEmail);
        writeLine(stream, 25, 723, PDType1Font.HELVETICA_BOLD, 9, "GSTIN : " + pharmacyGstin);

        writeLine(stream, 355, 785, PDType1Font.HELVETICA_BOLD, 9, "Patient Name :");
        writeLine(stream, 355, 772, PDType1Font.HELVETICA, 8, "Patient Address :");
        writeLine(stream, 355, 759, PDType1Font.HELVETICA, 8, "Dr Name :");
        writeLine(stream, 355, 746, PDType1Font.HELVETICA, 8, "Dr Reg No. :");

        writeCentered(stream, OUTER_LEFT, RIGHT_HEADER_SPLIT_X, 685, PDType1Font.HELVETICA_BOLD, 18, "INVOICE");
        
        float thY = 662;
        writeCellCenter(stream, TABLE_COLS[0], TABLE_COLS[1], thY, PDType1Font.HELVETICA_BOLD, 7, "SN.");
        writeCellLeft(stream, TABLE_COLS[1], TABLE_COLS[2], thY, PDType1Font.HELVETICA_BOLD, 7, " PRODUCT NAME");
        writeCellCenter(stream, TABLE_COLS[2], TABLE_COLS[3], thY, PDType1Font.HELVETICA_BOLD, 7, "PACK");
        writeCellCenter(stream, TABLE_COLS[3], TABLE_COLS[4], thY, PDType1Font.HELVETICA_BOLD, 7, "HSN");
        writeCellCenter(stream, TABLE_COLS[4], TABLE_COLS[5], thY, PDType1Font.HELVETICA_BOLD, 7, "BATCH");
        writeCellCenter(stream, TABLE_COLS[5], TABLE_COLS[6], thY, PDType1Font.HELVETICA_BOLD, 7, "EXP.");
        writeCellCenter(stream, TABLE_COLS[6], TABLE_COLS[7], thY, PDType1Font.HELVETICA_BOLD, 7, "QTY");
        writeCellCenter(stream, TABLE_COLS[7], TABLE_COLS[8], thY, PDType1Font.HELVETICA_BOLD, 7, "MRP");
        writeCellCenter(stream, TABLE_COLS[8], TABLE_COLS[9], thY, PDType1Font.HELVETICA_BOLD, 7, "SGST");
        writeCellCenter(stream, TABLE_COLS[9], TABLE_COLS[10], thY, PDType1Font.HELVETICA_BOLD, 7, "CGST");
        writeCellCenter(stream, TABLE_COLS[10], TABLE_COLS[11], thY, PDType1Font.HELVETICA_BOLD, 7, "AMOUNT");

        writeLine(stream, 25, layout.gstSummaryBottom - 12, PDType1Font.HELVETICA_BOLD, 8, "Terms & Conditions");
        writeLine(stream, TOTAL_PANEL_LEFT + 10, layout.subtotalTextY, PDType1Font.HELVETICA_BOLD, 9, "SUB TOTAL");
        writeLine(stream, TOTAL_PANEL_LEFT + 10, layout.sgstTextY, PDType1Font.HELVETICA, 8, "SGST 2.5 %");
        writeLine(stream, TOTAL_PANEL_LEFT + 10, layout.cgstTextY, PDType1Font.HELVETICA, 8, "CGST 2.5 %");
        writeLine(stream, TOTAL_PANEL_LEFT + 10, layout.roundoffTextY, PDType1Font.HELVETICA, 8, "Roundoff");
        writeLine(stream, TOTAL_PANEL_LEFT + 10, layout.grandTotalTextY, PDType1Font.HELVETICA_BOLD, 11, "GRAND TOTAL");
    }

    private void drawDynamicData(PDPageContentStream stream, Layout layout, Sale sale, List<SaleItem> saleItems, List<PaymentBreakdown> paymentRows) throws IOException {
        writeLine(stream, 430, 785, PDType1Font.HELVETICA_BOLD, 9, safe(sale.getCustomerName()));
        writeLine(stream, 395, 759, PDType1Font.HELVETICA, 8, "DR.SRINIVAS REDDY"); 
        writeLine(stream, 395, 746, PDType1Font.HELVETICA, 8, "56968          9666027727");

        writeLine(stream, 400, 695, PDType1Font.HELVETICA, 9, "Invoice No.  :  A" + String.format("%05d", sale.getBillNo()));
        writeLine(stream, 400, 682, PDType1Font.HELVETICA, 9, "DATE : " + formatDateTime(sale.getCreatedAt()));

        if (saleItems != null) {
            for (int i = 0; i < saleItems.size(); i++) {
                SaleItem item = saleItems.get(i);
                Medicine med = item.getMedicine();
                Inventory inv = item.getInventory();
                float y = layout.rowTextY(i);

                writeCellCenter(stream, TABLE_COLS[0], TABLE_COLS[1], y, PDType1Font.HELVETICA, 8, String.valueOf(i + 1) + ".");
                writeCellLeft(stream, TABLE_COLS[1], TABLE_COLS[2], y, PDType1Font.HELVETICA, 8, " " + (med != null ? med.getName() : ""));
                writeCellCenter(stream, TABLE_COLS[2], TABLE_COLS[3], y, PDType1Font.HELVETICA, 8, med != null ? med.getStrength() : "1");
                writeCellCenter(stream, TABLE_COLS[3], TABLE_COLS[4], y, PDType1Font.HELVETICA, 8, String.valueOf(med != null ? med.getMedicineId() : ""));
                writeCellCenter(stream, TABLE_COLS[4], TABLE_COLS[5], y, PDType1Font.HELVETICA, 8, inv != null ? inv.getBatchNumber() : "");
                writeCellCenter(stream, TABLE_COLS[5], TABLE_COLS[6], y, PDType1Font.HELVETICA, 8, inv != null && inv.getExpirationDate() != null ? inv.getExpirationDate().format(EXPIRY_FORMAT) : "");
                writeCellCenter(stream, TABLE_COLS[6], TABLE_COLS[7], y, PDType1Font.HELVETICA, 8, String.format("%.2f", (float)item.getQuantitySold()));
                writeCellRight(stream, TABLE_COLS[7], TABLE_COLS[8], y, PDType1Font.HELVETICA, 8, money(item.getUnitPrice()));
                writeCellCenter(stream, TABLE_COLS[8], TABLE_COLS[9], y, PDType1Font.HELVETICA, 8, "2.50");
                writeCellCenter(stream, TABLE_COLS[9], TABLE_COLS[10], y, PDType1Font.HELVETICA, 8, "2.50");
                writeCellRight(stream, TABLE_COLS[10], TABLE_COLS[11], y, PDType1Font.HELVETICA, 8, money(item.getLineAmount()));
            }
        }

        BigDecimal gst = moneyValue(sale.getGstAmount());
        BigDecimal sub = moneyValue(sale.getSubtotalAmount());
        BigDecimal grand = moneyValue(sale.getGrandTotalAmount());
        BigDecimal halfGst = gst.divide(TWO, 2, RoundingMode.HALF_UP);

        writeLine(stream, 25, layout.gstSummaryTextY, PDType1Font.HELVETICA, 8, 
            "GST " + money(sub) + "*2.5+2.5%=" + money(gst) + "  ** GET WELL SOON **");

        writeRight(stream, OUTER_RIGHT - 10, layout.subtotalTextY, PDType1Font.HELVETICA_BOLD, 9, money(sub));
        writeRight(stream, OUTER_RIGHT - 10, layout.sgstTextY, PDType1Font.HELVETICA, 8, money(halfGst));
        writeRight(stream, OUTER_RIGHT - 10, layout.cgstTextY, PDType1Font.HELVETICA, 8, money(halfGst));
        writeRight(stream, OUTER_RIGHT - 10, layout.roundoffTextY, PDType1Font.HELVETICA, 8, "0.00");
        writeRight(stream, OUTER_RIGHT - 10, layout.grandTotalTextY, PDType1Font.HELVETICA_BOLD, 11, money(grand));

        float termsY = layout.gstSummaryBottom - 24;
        for (String line : termsLinesRaw.split("\\|")) {
            writeLine(stream, 25, termsY, PDType1Font.HELVETICA, 7, line.trim());
            termsY -= 10;
        }

        writeRight(stream, OUTER_RIGHT - 10, layout.detailsBottom + 35, PDType1Font.HELVETICA, 9, "For " + pharmacyName);
        writeRight(stream, OUTER_RIGHT - 10, layout.detailsBottom + 10, PDType1Font.HELVETICA, 8, "Authorised Signatory");

        writeLine(stream, 25, layout.grandTotalBottom + 10, PDType1Font.HELVETICA_BOLD, 9, amountInWords(grand));
    }

    private void writeCellLeft(PDPageContentStream stream, float s, float e, float y, PDFont f, float sz, String t) throws IOException {
        writeLine(stream, s + 2, y, f, sz, t);
    }

    private void writeCellCenter(PDPageContentStream stream, float s, float e, float y, PDFont f, float sz, String t) throws IOException {
        float w = getTextWidth(f, sz, t);
        writeLine(stream, s + (e - s - w) / 2, y, f, sz, t);
    }

    private void writeCellRight(PDPageContentStream stream, float s, float e, float y, PDFont f, float sz, String t) throws IOException {
        float w = getTextWidth(f, sz, t);
        writeLine(stream, e - w - 4, y, f, sz, t);
    }

    private void writeLine(PDPageContentStream stream, float x, float y, PDFont font, float fontSize, String text) throws IOException {
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text == null ? "" : text);
        stream.endText();
    }

    private void writeRight(PDPageContentStream stream, float rightX, float y, PDFont font, float fontSize, String text) throws IOException {
        float width = getTextWidth(font, fontSize, text);
        writeLine(stream, rightX - width, y, font, fontSize, text);
    }

    private void writeCentered(PDPageContentStream stream, float left, float right, float y, PDFont font, float fontSize, String text) throws IOException {
        float width = getTextWidth(font, fontSize, text);
        float x = left + ((right - left - width) / 2f);
        writeLine(stream, x, y, font, fontSize, text);
    }

    private float getTextWidth(PDFont font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text == null ? "" : text) / 1000f * fontSize;
    }

    private void drawRect(PDPageContentStream stream, float x, float y, float w, float h) throws IOException {
        stream.addRect(x, y, w, h);
        stream.stroke();
    }

    private void drawLine(PDPageContentStream stream, float x1, float y1, float x2, float y2) throws IOException {
        stream.moveTo(x1, y1);
        stream.lineTo(x2, y2);
        stream.stroke();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMAT) : "-";
    }

    private String money(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
    }

    private BigDecimal moneyValue(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private String amountInWords(BigDecimal amount) {
        long rupees = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        String words = numberToWords(rupees).trim();
        return "Rs. " + Character.toUpperCase(words.charAt(0)) + words.substring(1) + " only";
    }

    private String numberToWords(long number) {
        if (number == 0) return "zero";
        StringBuilder result = new StringBuilder();
        long thousand = (number / 1000) % 1000;
        if (thousand > 0) result.append(threeDigitsToWords((int) thousand)).append(" thousand ");
        result.append(threeDigitsToWords((int) (number % 1000)));
        return result.toString();
    }

    private String threeDigitsToWords(int number) {
        StringBuilder sb = new StringBuilder();
        if (number >= 100) {
            sb.append(BELOW_TWENTY[number / 100]).append(" hundred ");
            number %= 100;
        }
        if (number >= 20) {
            sb.append(TENS[number / 10]);
            if (number % 10 != 0) sb.append(" ").append(BELOW_TWENTY[number % 10]);
        } else if (number > 0) {
            sb.append(BELOW_TWENTY[number]);
        }
        return sb.toString();
    }

    private Path archivePdf(String fileName, byte[] bytes) throws IOException {
        String dayFolder = LocalDate.now().format(DATE_FOLDER_FORMAT);
        Path dayPath = Paths.get(invoiceOutputRoot, dayFolder);
        Files.createDirectories(dayPath);
        Path filePath = dayPath.resolve(fileName);
        Files.write(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return filePath;
    }

    private String buildFileName(Sale sale) {
        return (sale.getCustomerName() != null ? sale.getCustomerName().replaceAll("\\s+", "_") : "SALE") + "_" + sale.getBillNo() + ".pdf";
    }

    private record Layout(
            float tableBottom,
            float gstSummaryBottom,
            float detailsBottom,
            float grandTotalBottom,
            float gstSummaryTextY,
            float subtotalTextY,
            float sgstTextY,
            float cgstTextY,
            float roundoffTextY,
            float grandTotalTextY,
            float rowHeight
    ) {
        static Layout build(int productCount) {
            float rowH = Math.max(MIN_ROW_HEIGHT, MAX_ROW_HEIGHT);
            float tableB = TABLE_HEADER_BOTTOM - (Math.max(1, productCount) * rowH);
            float gstB = tableB - GST_BAND_HEIGHT;
            float detB = gstB - DETAILS_BAND_HEIGHT;
            float grandB = detB - GRAND_BAND_HEIGHT;

            return new Layout(
                    tableB, gstB, detB, grandB,
                    tableB - 14f, gstB - 14f, gstB - 28f, gstB - 42f, gstB - 56f, grandB + 10f, rowH
            );
        }

        float rowTextY(int rowIndex) {
            return TABLE_HEADER_BOTTOM - (rowIndex * rowHeight) - (rowHeight * 0.7f);
        }
    }

    public record InvoicePdfResult(String fileName, String contentType, byte[] bytes, String archivePath) { }
}