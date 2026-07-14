package ru.yandex.practicum.mymarket.dto;

import lombok.Builder;

@Builder
public record PaymentStatus(boolean available, boolean sufficientFunds, Long balance, String message) {

    public boolean checkoutAllowed() {
        return available && sufficientFunds;
    }
}
