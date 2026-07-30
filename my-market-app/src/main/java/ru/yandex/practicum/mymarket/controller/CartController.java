package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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
    public Mono<String> getCartItems(Model model, Authentication authentication) {
        log.info("GET /cart/items");

        String username = authentication.getName();
        model.addAttribute("username", username);

        return cartService.getCartItems(username)
            .collectList()
            .zipWith(cartService.getTotalPrice(username))
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

                return paymentService.checkBalance(username, total)
                    .doOnNext(status -> {
                        model.addAttribute("checkoutAvailable", status.checkoutAllowed());
                        model.addAttribute("paymentMessage", buildPaymentMessage(status));
                    })
                    .thenReturn("cart");
            });
    }

    @PostMapping("/cart/items")
    public Mono<String> updateCartItems(@ModelAttribute CartActionRequest request, Authentication authentication) {
        log.info("POST /cart/items - id: {}, action: {}", request.id(), request.action());

        String username = authentication.getName();
        String actionValid = request.action() != null ? request.action().toUpperCase() : "";

        Mono<Void> action = switch (actionValid) {
            case "PLUS" -> cartService.increaseQuantity(username, request.id());
            case "MINUS" -> cartService.decreaseQuantity(username, request.id());
            case "DELETE" -> cartService.removeFromCart(username, request.id());
            default -> {
                log.warn("Unknown action: {}", request.action());
                yield Mono.<Void>empty();
            }
        };

        return action.thenReturn("redirect:/cart/items");
    }
}
