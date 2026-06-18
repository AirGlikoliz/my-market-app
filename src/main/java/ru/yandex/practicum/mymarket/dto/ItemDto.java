package ru.yandex.practicum.mymarket.dto;

import lombok.Builder;
import ru.yandex.practicum.mymarket.entity.Item;

@Builder
public record ItemDto(
        Long id,
        String title,
        String description,
        String imgPath,
        Long price,
        Integer count
) {
    public static ItemDto convertToDto(Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .imgPath(item.getImgPath())
                .price(item.getPrice())
                .count(0)
                .build();
    }
}
