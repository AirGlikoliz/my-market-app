package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @GetMapping("/cart/items")
    public Mono<String> getCartItems(Model model, ServerWebExchange exchange) {
        log.info("GET /cart/items");

        return exchange.getSession()
            .map(WebSession::getId)
            .flatMap(sessionId -> cartService.getCartItems(sessionId)
                .collectList()
                .zipWith(cartService.getTotalPrice(sessionId)))
            .doOnNext(tuple -> {
                model.addAttribute("items", tuple.getT1());
                model.addAttribute("total", tuple.getT2());
            })
            .thenReturn("cart");
    }

    @PostMapping("/cart/items")
    public Mono<String> updateCartItems(@RequestParam Long id, @RequestParam String action, ServerWebExchange exchange) {
        log.info("POST /cart/items - id: {}, action: {}", id, action);

        String actionValid = action != null ? action.toUpperCase() : "";

        return exchange.getSession()
            .map(WebSession::getId)
            .flatMap(sessionId -> switch (actionValid) {
                case "PLUS" -> cartService.increaseQuantity(sessionId, id);
                case "MINUS" -> cartService.decreaseQuantity(sessionId, id);
                case "DELETE" -> cartService.removeFromCart(sessionId, id);
                default -> {
                    log.warn("Unknown action: {}", action);
                    yield Mono.<Void>empty();
                }
            })
            .thenReturn("redirect:/cart/items");
    }
}