package ru.yandex.practicum.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class BalanceServiceImpl implements BalanceService {

    private final long initialBalance;
    private final Map<String, AtomicLong> balances = new ConcurrentHashMap<>();

    public BalanceServiceImpl(@Value("${payment.balance.initial:100000}") long initialBalance) {
        this.initialBalance = initialBalance;
        log.info("Payment service started with initial balance per user: {}", initialBalance);
    }

    @Override
    public Mono<Long> getBalance(String username) {
        return Mono.fromSupplier(() -> balanceFor(username).get());
    }

    @Override
    public Mono<PaymentResponse> pay(String username, long amount) {
        return Mono.fromSupplier(() -> {
            if (amount <= 0) throw new IllegalArgumentException("amount must be bigger than 0");

            AtomicLong balance = balanceFor(username);
            long current, updated;
            do {
                current = balance.get();
                if (current < amount) {
                    log.error("Payment declined for {}: insufficient funds, balance={}, requested={}", username, current, amount);
                    return new PaymentResponse(false, current, "Insufficient funds");
                }
                updated = current - amount;
            } while (!balance.compareAndSet(current, updated));

            log.info("Payment successful for {}: charged={}, remainingBalance={}", username, amount, updated);
            return new PaymentResponse(true, updated, "Payment successful");
        });
    }

    private AtomicLong balanceFor(String username) {
        return balances.computeIfAbsent(username, key -> new AtomicLong(initialBalance));
    }
}
