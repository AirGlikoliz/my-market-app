package ru.yandex.practicum.mymarket.controller.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.PaymentStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ControllerUtil {

    public static boolean isAuthenticated(Authentication authentication) {
        return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public static List<ItemDto> enrichWithCartCounts(List<ItemDto> items, Map<Long, Integer> cart) {
        return items.stream()
            .map(item -> ItemDto.builder()
                 .id(item.id())
                 .title(item.title())
                 .description(item.description())
                 .imgPath(item.imgPath())
                 .price(item.price())
                 .count(cart.getOrDefault(item.id(), 0))
                 .build())
            .toList();
    }

    public static String buildPaymentMessage(PaymentStatus status) {
        if (!status.available()) {
            return status.message();
        }
        if (!status.sufficientFunds()) {
            return "Недостаточно средств на счёте (баланс: " + status.balance() + " руб.) для оформления заказа";
        }
        return "";
    }
}
