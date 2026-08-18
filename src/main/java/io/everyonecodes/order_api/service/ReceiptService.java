package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.Order;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReceiptService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    private static final DateTimeFormatter RECEIPT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy 'at' HH:mm");

    public ReceiptService(JavaMailSender mailSender, @Value("${receipt.from.address}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendReceipt(Order order) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromAddress);
            helper.setTo(order.getCustomerEmail());
            helper.setSubject("Thank you for ordering from our Restaurant!");
            helper.setText(buildHtmlBody(order), true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            log.error("Failed to build receipt for order {} — invalid message content, customer email was '{}'",
                    order.getId(), order.getCustomerEmail(), ex);
        } catch (MailException ex) {
            log.error("Failed to send receipt for order {}", order.getId(), ex);
        }
    }

    private String buildHtmlBody(Order order) {
        String itemRows = order.getOrderedItems().stream()
                .map(item -> {
                    String extras = item.getSelectedExtras().stream()
                            .map(e -> "<div style=\"color:#666;font-size:13px;\">+ "
                                    + e.getExtra().getName() + " (" + e.getPriceAtPurchase() + " €)</div>")
                            .collect(Collectors.joining());

                    return "<tr>"
                            + "<td style=\"padding:8px;border-bottom:1px solid #eee;\">"
                            + item.getMenuItem().getName() + extras + "</td>"
                            + "<td style=\"padding:8px;border-bottom:1px solid #eee;text-align:center;\">"
                            + item.getQuantity() + "</td>"
                            + "<td style=\"padding:8px;border-bottom:1px solid #eee;text-align:right;\">"
                            + item.getPriceAtPurchase() + " €</td>"
                            + "</tr>";
                })
                .collect(Collectors.joining());

        return "<html><body style=\"font-family:Arial,sans-serif;color:#333;\">"
                + "<h2>Order #" + order.getId() + "</h2>"
                + "<p>Thank you for your order!</p>"
                + "<p style=\"color:#666;font-size:13px;\">Paid: " + order.getPaidAt().format(RECEIPT_DATE_FORMAT) + "</p>"
                + "<table style=\"width:100%;border-collapse:collapse;\">"
                + "<tr>"
                + "<th style=\"text-align:left;padding:8px;border-bottom:2px solid #333;\">Item</th>"
                + "<th style=\"text-align:center;padding:8px;border-bottom:2px solid #333;\">Qty</th>"
                + "<th style=\"text-align:right;padding:8px;border-bottom:2px solid #333;\">Price</th>"
                + "</tr>"
                + itemRows
                + "</table>"
                + "<p style=\"text-align:right;font-size:16px;\"><strong>Total: "
                + order.getTotalPrice() + " €</strong></p>"
                + "</body></html>";
    }
}