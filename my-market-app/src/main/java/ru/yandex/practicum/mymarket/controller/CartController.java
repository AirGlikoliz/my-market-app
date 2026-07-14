package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartActionRequest;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.PaymentService;

import java.util.List;

import static ru.yandex.practicum.mymarket.controller.util.ControllerUtil.buildPaymentMessage;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final PaymentService paymentService;

    @GetMapping("/cart/items")
    public Mono<String> getCartItems(Model model, ServerWebExchange exchange) {
        log.info("GET /cart/items");

        return exchange.getSession()
            .map(WebSession::getId)
            .flatMap(sessionId -> cartService.getCartItems(sessionId)
                .collectList()
                .zipWith(cartService.getTotalPrice(sessionId))
                .flatMap(tuple -> {
                    List<ItemDto> items = tuple.getT1();
                    Long total = tuple.getT2();
                    model.addAttribute("items", items);
                    model.addAttribute("total", total);

                    if (items.isEmpty()) {
                        model.addAttribute("checkoutAvailable", false);
                        model.addAttribute("paymentMessage", "");
                        return Mono.just("cart");
                    }

                    return paymentService.checkBalance(total)
                        .doOnNext(status -> {
                            model.addAttribute("checkoutAvailable", status.checkoutAllowed());
                            model.addAttribute("paymentMessage", buildPaymentMessage(status));
                        })
                        .thenReturn("cart");
                }));
    }

    @PostMapping("/cart/items")
    public Mono<String> updateCartItems(@ModelAttribute CartActionRequest request, ServerWebExchange exchange) {
        log.info("POST /cart/items - id: {}, action: {}", request.id(), request.action());

        String actionValid = request.action() != null ? request.action().toUpperCase() : "";

        return exchange.getSession()
            .map(WebSession::getId)
            .flatMap(sessionId -> switch (actionValid) {
                case "PLUS" -> cartService.increaseQuantity(sessionId, request.id());
                case "MINUS" -> cartService.decreaseQuantity(sessionId, request.id());
                case "DELETE" -> cartService.removeFromCart(sessionId, request.id());
                default -> {
                    log.warn("Unknown action: {}", request.action());
                    yield Mono.<Void>empty();
                }
            })
            .thenReturn("redirect:/cart/items");
    }
}