package ru.yandex.practicum.mymarket.dto;

import lombok.Builder;

@Builder
public record PagingInfo(
        int pageSize,
        int pageNumber,
        boolean hasPrevious,
        boolean hasNext
) {}
