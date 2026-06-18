package ru.yandex.practicum.mymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long id;
    private String title;
    private String description;
    private String imgPath;
    private Long price;
    private Integer count;
}
