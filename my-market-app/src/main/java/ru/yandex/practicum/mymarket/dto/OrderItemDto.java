package ru.yandex.practicum.mymarket.dto;

import lombok.Builder;

@Builder
public record OrderItemDto(
        Long id,
        String title,
        Long price,
        Integer count
) {}
