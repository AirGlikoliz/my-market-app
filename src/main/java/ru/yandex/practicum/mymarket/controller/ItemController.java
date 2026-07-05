package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
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
                                 Model model,
                                 ServerWebExchange exchange) {

        log.info("GET /items - search: {}, sort: {}, page: {}, size: {}", search, sort, pageNumber, pageSize);

        return exchange.getSession()
            .map(WebSession::getId)
            .flatMap(sessionId -> itemService.getItems(search, sort, pageNumber, pageSize)
                    .zipWith(cartService.getCart(sessionId)))
            .map(tuple -> {
                Page<ItemDto> page = tuple.getT1();
                Map<Long, Integer> cart = tuple.getT2();

                List<ItemDto> itemsWithCount = ControllerUtil.enrichWithCartCounts(page.getContent(), cart);

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
    public Mono<String> getItem(@PathVariable Long id, Model model, ServerWebExchange exchange) {
        log.info("GET /items/{}", id);

        return exchange.getSession()
            .map(session -> session.getId())
            .flatMap(sessionId -> itemService.getItemById(id)
                    .zipWith(cartService.getCart(sessionId)))
            .doOnNext(tuple -> {
                ItemDto item = tuple.getT1();
                Map<Long, Integer> cart = tuple.getT2();
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
    public Mono<String> updateCartFromItems(@ModelAttribute ItemActionRequest request, ServerWebExchange exchange) {

        log.info("POST /items - id: {}, action: {}, search: {}, sort: {}, page: {}, size: {}",
                request.id(), request.action(), request.search(),
                request.sort(), request.pageNumber(), request.pageSize());

        return exchange.getSession()
            .map(WebSession::getId)
            .flatMap(sessionId -> cartService.applyAction(sessionId, request.id(), request.action()))
            .thenReturn(String.format("redirect:/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d",
                request.search(), request.sort(), request.pageNumber(), request.pageSize()));
    }

    @PostMapping("/items/{id}")
    public Mono<String> updateCartFromItem(@PathVariable Long id, @ModelAttribute ItemActionRequest request, ServerWebExchange exchange) {

        log.info("POST /items/{} - action: {}", id, request.action());

        return exchange.getSession()
                .map(WebSession::getId)
                .flatMap(sessionId -> cartService.applyAction(sessionId, id, request.action()))
                .thenReturn("redirect:/items/" + id);
    }
}