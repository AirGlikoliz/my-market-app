package ru.yandex.practicum.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class BalanceServiceImpl implements BalanceService {

    private final AtomicLong balance;

    public BalanceServiceImpl(@Value("${payment.balance.initial:100000}") long initialBalance) {
        this.balance = new AtomicLong(initialBalance);
        log.info("Payment service started with initial balance: {}", initialBalance);
    }

    @Override
    public Mono<Long> getBalance() {
        return Mono.fromSupplier(balance::get);
    }

    @Override
    public Mono<PaymentResponse> pay(long amount) {
        return Mono.fromSupplier(() -> {
            if (amount <= 0) throw new IllegalArgumentException("amount must be bigger than 0");

            long current, updated;
            do {
                current = balance.get();
                if (current < amount) {
                    log.error("Payment declined: insufficient funds, balance={}, requested={}", current, amount);
                    return new PaymentResponse(false, current, "Insufficient funds");
                }
                updated = current - amount;
            } while (!balance.compareAndSet(current, updated));

            log.info("Payment successful: charged={}, remainingBalance={}", amount, updated);
            return new PaymentResponse(true, updated, "Payment successful");
        });
    }
}
