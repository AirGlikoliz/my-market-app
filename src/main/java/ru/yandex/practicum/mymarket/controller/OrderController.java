package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    @GetMapping("/orders")
    public Mono<String> getOrders(Model model) {
        log.info("GET /orders");

        return orderService.getAllOrders()
                .collectList()
                .doOnNext(orders -> model.addAttribute("orders", orders))
                .thenReturn("orders");
    }

    @GetMapping("/orders/{id}")
    public Mono<String> getOrder(@PathVariable Long id,
                                 @RequestParam(required = false, defaultValue = "false") boolean newOrder,
                                 Model model) {

        log.info("GET /orders/{} - newOrder: {}", id, newOrder);

        return orderService.getOrderById(id)
                .doOnNext(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("newOrder", newOrder);
                })
                .thenReturn("order");
    }

    @PostMapping("/buy")
    public Mono<String> createOrder(ServerWebExchange exchange) {
        log.info("POST /buy - creating order from cart");

        return exchange.getSession()
            .map(WebSession::getId)
            .flatMap(sessionId -> cartService.getCartItems(sessionId)
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        log.warn("Cannot create order: cart is empty");
                        return Mono.just("redirect:/cart");
                    }
                    return orderService.createOrderFromCart(sessionId)
                            .map(order -> "redirect:/orders/" + order.id() + "?newOrder=true");
                }));
    }
}