package ru.yandex.practicum.paymentservice.service;

public record PaymentResponse(boolean success, long balance, String message) {
}
