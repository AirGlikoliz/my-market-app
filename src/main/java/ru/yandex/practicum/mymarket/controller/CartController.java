package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @GetMapping("/cart/items")
    public Mono<String> getCartItems(Model model) {
        log.info("GET /cart/items");

        return cartService.getCartItems()
                .collectList()
                .zipWith(cartService.getTotalPrice())
                .doOnNext(tuple -> {
                    model.addAttribute("items", tuple.getT1());
                    model.addAttribute("total", tuple.getT2());
                })
                .thenReturn("cart");
    }

    @PostMapping("/cart/items")
    public Mono<String> updateCartItems(@RequestParam Long id, @RequestParam String action) {
        log.info("POST /cart/items - id: {}, action: {}", id, action);

        String actionValid = action != null ? action.toUpperCase() : "";
        switch (actionValid) {
            case "PLUS" -> cartService.increaseQuantity(id);
            case "MINUS" -> cartService.decreaseQuantity(id);
            case "DELETE" -> cartService.removeFromCart(id);
            default -> log.warn("Unknown action: {}", action);
        }
        return Mono.just("redirect:/cart/items");
    }
}