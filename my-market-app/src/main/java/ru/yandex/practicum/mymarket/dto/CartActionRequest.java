package ru.yandex.practicum.mymarket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartActionRequest(
        @NotNull @Positive Long id,
        @NotNull CartAction action
) {}
