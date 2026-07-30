package ru.yandex.practicum.mymarket.dto;

public record CheckoutResult(Outcome outcome, Long orderId) {

    public enum Outcome {
        EMPTY_CART,
        PAYMENT_DECLINED,
        SUCCESS
    }

    public static CheckoutResult emptyCart() {
        return new CheckoutResult(Outcome.EMPTY_CART, null);
    }

    public static CheckoutResult paymentDeclined() {
        return new CheckoutResult(Outcome.PAYMENT_DECLINED, null);
    }

    public static CheckoutResult success(Long orderId) {
        return new CheckoutResult(Outcome.SUCCESS, orderId);
    }
}
