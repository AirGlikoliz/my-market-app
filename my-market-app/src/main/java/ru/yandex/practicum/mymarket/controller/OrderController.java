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
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.checkout.CheckoutService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final CheckoutService checkoutService;

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
        log.info("POST /buy - checkout");

        return checkoutService.checkout(authentication.getName())
                .map(result -> switch (result.outcome()) {
                    case EMPTY_CART, PAYMENT_DECLINED -> "redirect:/cart/items";
                    case SUCCESS -> "redirect:/orders/" + result.orderId() + "?newOrder=true";
                });
    }
}
