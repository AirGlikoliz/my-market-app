package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    @GetMapping("/orders")
    public String getOrders(Model model) {
        log.info("GET /orders");

        List<OrderDto> orders = orderService.getAllOrders();

        model.addAttribute("orders", orders);

        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String getOrder(@PathVariable Long id, @RequestParam(required = false, defaultValue = "false") boolean newOrder,
            Model model) {

        log.info("GET /orders/{} - newOrder: {}", id, newOrder);

        OrderDto order = orderService.getOrderById(id);

        model.addAttribute("order", order);
        model.addAttribute("newOrder", newOrder);

        return "order";
    }

    @PostMapping("/buy")
    public String createOrder() {
        log.info("POST /buy - creating order from cart");

        if (cartService.getCartItems().isEmpty()) {
            log.warn("Cannot create order: cart is empty");
            return "redirect:/cart";
        }

        OrderDto order = orderService.createOrderFromCart();

        return "redirect:/orders/" + order.id() + "?newOrder=true";
    }
}
