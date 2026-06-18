package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mymarket.controller.util.ControllerUtil;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.PagingInfo;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;
    private static final List<Integer> PAGE_SIZES = List.of(2, 5, 10, 20, 50, 100);

    @GetMapping({"/", "/items"})
    public String getItems(@RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            Model model) {

        log.info("GET /items - search: {}, sort: {}, page: {}, size: {}", search, sort, pageNumber, pageSize);

        Page<ItemDto> itemPage = itemService.getItems(search, sort, pageNumber, pageSize);

        List<ItemDto> itemsWithCount = ControllerUtil.enrichWithCartCounts(itemPage.getContent(), cartService.getCart());

        model.addAttribute("items", itemsWithCount);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("sort", sort);
        model.addAttribute("paging", PagingInfo.builder()
                .pageSize(pageSize)
                .pageNumber(pageNumber)
                .hasPrevious(pageNumber > 1)
                .hasNext(itemPage.hasNext())
                .build());
        model.addAttribute("pageSizes", PAGE_SIZES);

        return "items";
    }

    @GetMapping("/items/{id}")
    public String getItem(@PathVariable Long id, Model model) {
        log.info("GET /items/{}", id);

        ItemDto item = itemService.getItemById(id);

        var cart = cartService.getCart();
        int count = cart.getOrDefault(id, 0);

        ItemDto itemWithCount = ItemDto.builder()
                .id(item.id())
                .title(item.title())
                .description(item.description())
                .imgPath(item.imgPath())
                .price(item.price())
                .count(count)
                .build();

        model.addAttribute("item", itemWithCount);

        return "item";
    }

    @PostMapping("/items")
    public String updateCartFromItems(@RequestParam Long id, @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            @RequestParam String action) {

        log.info("POST /items - id: {}, action: {}, search: {}, sort: {}, page: {}, size: {}",
                id, action, search, sort, pageNumber, pageSize);

        switch (action.toUpperCase()) {
            case "PLUS" -> cartService.increaseQuantity(id);
            case "MINUS" -> cartService.decreaseQuantity(id);
            default -> log.warn("Unknown action: {}", action);
        }

        return String.format("redirect:/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d",
                search != null ? search : "", sort, pageNumber, pageSize);
    }

    @PostMapping("/items/{id}")
    public String updateCartFromItem(@PathVariable Long id, @RequestParam String action) {

        log.info("POST /items/{} - action: {}", id, action);

        switch (action.toUpperCase()) {
            case "PLUS" -> cartService.increaseQuantity(id);
            case "MINUS" -> cartService.decreaseQuantity(id);
            default -> log.warn("Unknown action: {}", action);
        }

        return "redirect:/items/" + id;
    }
}