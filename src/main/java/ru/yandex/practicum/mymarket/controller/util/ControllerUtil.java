package ru.yandex.practicum.mymarket.controller.util;

import ru.yandex.practicum.mymarket.dto.ItemDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ControllerUtil {

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
}
