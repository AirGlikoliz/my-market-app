package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.PaymentResult;
import ru.yandex.practicum.mymarket.dto.PaymentStatus;
import ru.yandex.practicum.mymarket.payment.client.api.PaymentsApi;
import ru.yandex.practicum.mymarket.payment.client.model.PaymentRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String UNAVAILABLE_MESSAGE = "Сервис платежей недоступен";

    private final PaymentsApi paymentsApi;

    @Override
    public Mono<PaymentStatus> checkBalance(String username, Long requiredAmount) {
        return paymentsApi.getBalance(username)
                .map(response -> PaymentStatus.builder()
                        .available(true)
                        .sufficientFunds(response.getBalance() >= requiredAmount)
                        .balance(response.getBalance())
                        .build())
                .onErrorResume(ex -> {
                    log.warn("Payment service unavailable while checking balance: {}", ex.getMessage());
                    return Mono.just(PaymentStatus.builder()
                            .available(false)
                            .sufficientFunds(false)
                            .message(UNAVAILABLE_MESSAGE)
                            .build());
                });
    }

    @Override
    public Mono<PaymentResult> pay(String username, Long amount) {
        return paymentsApi.makePayment(new PaymentRequest().amount(amount).username(username))
                .map(response -> new PaymentResult(
                        Boolean.TRUE.equals(response.getSuccess()),
                        response.getMessage()))
                .onErrorResume(ex -> {
                    log.warn("Payment service unavailable while making payment: {}", ex.getMessage());
                    return Mono.just(new PaymentResult(false, UNAVAILABLE_MESSAGE));
                });
    }
}
