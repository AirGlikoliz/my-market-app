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
                            .flatMap(order -> applyPayment(username, order));
                });
    }

    private Mono<CheckoutResult> applyPayment(String username, OrderDto order) {
        return paymentService.pay(username, order.totalSum())
            .flatMap(paymentResult -> {
                if (!paymentResult.success()) {
                    log.warn("Payment declined for order {}: {}", order.id(), paymentResult.message());
                    return orderService.markFailed(order.id())
                        .thenReturn(CheckoutResult.paymentDeclined());
                }
                return orderService.markPaid(order.id())
                    .then(cartService.clearCart(username))
                    .thenReturn(CheckoutResult.success(order.id()));
            });
    }
}
