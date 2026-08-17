package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
public class ReceiptService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public ReceiptService(JavaMailSender mailSender, @Value("${receipt.from.address}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendReceipt(Order order) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setText(buildPlainTextBody(order));
        try {
            message.setFrom(fromAddress);
            message.setTo(order.getCustomerEmail());
            message.setSubject("testing");
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Mail sending failed");
        }
    }

    private String buildPlainTextBody(Order order) {
        return "                    Order #" + order.getId()
                + "\nCustomer Email: " + order.getCustomerEmail() + "    Created At: " + order.getCreatedAt() + "\n" +
                "Items: \n" +
                order.getOrderedItems()
                        .stream()
                        .map(item -> "  " + item.getMenuItem().getName() + "   quantity: " + item.getQuantity() + "    Price: " + item.getPriceAtPurchase() + " Euro\n" +
                                "       Selected Extras: \n" +
                                "           " + item.getSelectedExtras()
                                .stream()
                                .map(extra -> extra.getExtra().getName() + "   Price: " + extra.getPriceAtPurchase())
                                .collect(Collectors.joining("\n")))
                        .collect(Collectors.joining("\n")) + "\n" +
                "Total Price: " + order.getTotalPrice() + " Euro";
    }
}