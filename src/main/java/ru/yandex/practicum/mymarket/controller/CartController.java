package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @GetMapping("/cart/items")
    public String getCartItems(Model model) {
        log.info("GET /cart/items");

        List<ItemDto> items = cartService.getCartItems();
        Long total = cartService.getTotalPrice();

        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "cart";
    }

    @PostMapping("/cart/items")
    public String updateCartItems(@RequestParam Long id, @RequestParam String action) {

        log.info("POST /cart/items - id: {}, action: {}", id, action);

        switch (action.toUpperCase()) {
            case "PLUS" -> cartService.increaseQuantity(id);
            case "MINUS" -> cartService.decreaseQuantity(id);
            case "DELETE" -> cartService.removeFromCart(id);
            default -> log.warn("Unknown action: {}", action);
        }

        return "redirect:/cart/items";
    }
}