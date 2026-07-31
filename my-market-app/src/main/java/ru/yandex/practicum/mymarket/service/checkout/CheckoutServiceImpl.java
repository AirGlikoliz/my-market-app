package ru.yandex.practicum.mymarket.service.checkout;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartSnapshot;
import ru.yandex.practicum.mymarket.dto.CheckoutResult;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.PaymentService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutServiceImpl implements CheckoutService {

    private final CartService cartService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    @Override
    public Mono<CheckoutResult> checkout(String username) {
        return cartService.getCartItems(username)
                .collectList()
                .map(items -> new CartSnapshot(items, items.stream().mapToLong(item -> item.price() * item.count()).sum()))
                .flatMap(snapshot -> {
                    if (snapshot.isEmpty()) {
                        log.warn("Cannot checkout: cart is empty, user {}", username);
                        return Mono.just(CheckoutResult.emptyCart());
                    }
                    return orderService.createPendingOrder(username, snapshot)
                            .flatMap(order -> settlePayment(username, order));
                });
    }

    private Mono<CheckoutResult> settlePayment(String username, OrderDto order) {
        return paymentService.pay(username, order.totalSum())
            .flatMap(paymentResult -> {
                if (!paymentResult.success()) {
                    log.warn("Payment declined for order {}: {}", order.id(), paymentResult.message());
                    return orderService.markFailed(order.id())
                        .onErrorResume(ex -> {
                            log.error("Order {} was declined but could not be marked FAILED, user {}", order.id(), username, ex);
                            return Mono.empty();
                        })
                        .thenReturn(CheckoutResult.paymentDeclined());
                }
                return orderService.markPaid(order.id())
                    .onErrorResume(ex -> {
                        log.error("Payment succeeded for order {} (user {}) but marking it PAID failed - needs manual reconciliation", order.id(), username, ex);
                        return Mono.empty();
                    })
                    .then(cartService.clearCart(username)
                        .onErrorResume(ex -> {
                            log.error("Payment succeeded for order {} (user {}) but clearing the cart failed", order.id(), username, ex);
                            return Mono.empty();
                        }))
                    .thenReturn(CheckoutResult.success(order.id()));
            });
    }
}
