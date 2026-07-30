package ru.yandex.practicum.mymarket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemActionRequest(
        @NotNull @Positive Long id,
        @NotNull CartAction action,
        String search,
        SortOption sort,
        @Min(1) Integer pageNumber,
        @Min(1) @Max(100) Integer pageSize
) {}
