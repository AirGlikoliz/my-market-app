package ru.yandex.practicum.mymarket.dto;

import lombok.Builder;
import ru.yandex.practicum.mymarket.entity.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public record OrderDto(
        Long id,
        LocalDateTime orderDate,
        List<OrderItemDto> items,
        Long totalSum
) {

    public static OrderDto convertToDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> OrderItemDto.builder()
                        .id(item.getId())
                        .title(item.getTitle())
                        .price(item.getPrice())
                        .count(item.getCount())
                        .build())
                .collect(Collectors.toList());

        return OrderDto.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .items(itemDtos)
                .totalSum(order.getTotalSum())
                .build();
    }
}