package ru.yandex.practicum.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.paymentservice.api.PaymentsApi;
import ru.yandex.practicum.paymentservice.model.BalanceResponse;
import ru.yandex.practicum.paymentservice.model.PaymentRequest;
import ru.yandex.practicum.paymentservice.model.PaymentResponse;
import ru.yandex.practicum.paymentservice.service.BalanceService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentController implements PaymentsApi {

    private final BalanceService balanceService;

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(ServerWebExchange exchange) {
        log.info("GET /api/v1/payments/balance");

        return balanceService.getBalance()
                .map(balance -> ResponseEntity.ok(new BalanceResponse().balance(balance)));
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> makePayment(Mono<PaymentRequest> paymentRequest, ServerWebExchange exchange) {
        return paymentRequest
                .doOnNext(request -> log.info("POST /api/v1/payments - amount: {}", request.getAmount()))
                .flatMap(request -> balanceService.pay(request.getAmount()))
                .map(response -> ResponseEntity.ok(new PaymentResponse()
                        .success(response.success())
                        .balance(response.balance())
                        .message(response.message())));
    }
}
