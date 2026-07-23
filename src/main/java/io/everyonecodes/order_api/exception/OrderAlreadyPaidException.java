package io.everyonecodes.order_api.exception;

public class OrderAlreadyPaidException extends RuntimeException{
    public OrderAlreadyPaidException(String message) {
        super(message);
    }
}
