package ru.yandex.practicum.mymarket.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("cart_items")
public class CartItem {

    @Id
    private Long id;
    private String sessionId;
    private Long itemId;
    private Integer quantity;
    @LastModifiedDate
    private LocalDateTime updatedAt;

}