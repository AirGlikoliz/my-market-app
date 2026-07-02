package ru.yandex.practicum.mymarket.dto;

public record ItemActionRequest(
        Long id,
        String action,
        String search,
        String sort,
        Integer pageNumber,
        Integer pageSize
) {}