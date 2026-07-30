package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.PaymentService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final PaymentService paymentService;

    @GetMapping("/orders")
    public Mono<String> getOrders(Model model, Authentication authentication) {
        log.info("GET /orders");

        model.addAttribute("username", authentication.getName());

        return orderService.getAllOrders(authentication.getName())
                .collectList()
                .doOnNext(orders -> model.addAttribute("orders", orders))
                .thenReturn("orders");
    }

    @GetMapping("/orders/{id}")
    public Mono<String> getOrder(@PathVariable Long id,
                                 @RequestParam(required = false, defaultValue = "false") boolean newOrder,
                                 Model model,
                                 Authentication authentication) {

        log.info("GET /orders/{} - newOrder: {}", id, newOrder);

        model.addAttribute("username", authentication.getName());

        return orderService.getOrderById(id, authentication.getName())
                .doOnNext(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("newOrder", newOrder);
                })
                .thenReturn("order");
    }

    @PostMapping("/buy")
    public Mono<String> createOrder(Authentication authentication) {
        log.info("POST /buy - creating order from cart");

        String username = authentication.getName();

        return cartService.getCartItems(username)
            .collectList()
            .zipWith(cartService.getTotalPrice(username))
            .flatMap(tuple -> {
                if (tuple.getT1().isEmpty()) {
                    log.warn("Cannot create order: cart is empty");
                    return Mono.just("redirect:/cart/items");
                }
                return paymentService.pay(username, tuple.getT2())
                    .flatMap(paymentResult -> {
                        if (!paymentResult.success()) {
                            log.warn("Payment declined: {}", paymentResult.message());
                            return Mono.just("redirect:/cart/items");
                        }
                        return orderService.createOrderFromCart(username)
                                .map(order -> "redirect:/orders/" + order.id() + "?newOrder=true");
                    });
            });
    }
}
