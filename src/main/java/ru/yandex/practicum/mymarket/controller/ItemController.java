package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.controller.util.ControllerUtil;
import ru.yandex.practicum.mymarket.dto.ItemActionRequest;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.PagingInfo;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;
    private static final List<Integer> PAGE_SIZES = List.of(2, 5, 10, 20, 50, 100);

    @GetMapping({"/", "/items"})
    public Mono<String> getItems(@RequestParam(required = false) String search,
                     @RequestParam(required = false, defaultValue = "NO") String sort,
                     @RequestParam(required = false, defaultValue = "1") int pageNumber,
                     @RequestParam(required = false, defaultValue = "5") int pageSize,
                     Model model) {

        log.info("GET /items - search: {}, sort: {}, page: {}, size: {}", search, sort, pageNumber, pageSize);

        return itemService.getItems(search, sort, pageNumber, pageSize)
            .map(page -> {
                List<ItemDto> itemsWithCount = ControllerUtil.enrichWithCartCounts(page.getContent(), cartService.getCart());

                model.addAttribute("items", itemsWithCount);
                model.addAttribute("search", search != null ? search : "");
                model.addAttribute("sort", sort);
                model.addAttribute("paging", PagingInfo.builder()
                    .pageSize(pageSize)
                    .pageNumber(pageNumber)
                    .hasPrevious(pageNumber > 1)
                    .hasNext(page.hasNext())
                    .build());
                model.addAttribute("pageSizes", PAGE_SIZES);

                return "items";
            });
    }

    @GetMapping("/items/{id}")
    public Mono<String> getItem(@PathVariable Long id, Model model) {
        log.info("GET /items/{}", id);

        return itemService.getItemById(id)
            .doOnNext(item -> {
                Map<Long, Integer> cart = cartService.getCart();
                int count = cart.getOrDefault(id, 0);
                model.addAttribute("item", ItemDto.builder()
                    .id(item.id())
                    .title(item.title())
                    .description(item.description())
                    .imgPath(item.imgPath())
                    .price(item.price())
                    .count(count)
                    .build());
            })
            .thenReturn("item");
    }

    @PostMapping("/items")
    public Mono<String> updateCartFromItems(@ModelAttribute ItemActionRequest request) {

        log.info("POST /items - id: {}, action: {}, search: {}, sort: {}, page: {}, size: {}",
                request.id(), request.action(), request.search(),
                request.sort(), request.pageNumber(), request.pageSize());

        switch (request.action().toUpperCase()) {
            case "PLUS" -> cartService.increaseQuantity(request.id());
            case "MINUS" -> cartService.decreaseQuantity(request.id());
            default -> log.warn("Unknown action: {}", request.action());
        }

        return Mono.just(String.format("redirect:/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d",
                request.search(), request.sort(), request.pageNumber(), request.pageSize()));
    }

    @PostMapping("/items/{id}")
    public Mono<String> updateCartFromItem(@PathVariable Long id, @ModelAttribute ItemActionRequest request) {

        log.info("POST /items/{} - action: {}", id, request.action());

        switch (request.action().toUpperCase()) {
            case "PLUS" -> cartService.increaseQuantity(id);
            case "MINUS" -> cartService.decreaseQuantity(id);
            default -> log.warn("Unknown action: {}", request.action());
        }

        return Mono.just("redirect:/items/" + id);
    }
}