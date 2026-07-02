package ru.yandex.practicum.mymarket.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.exception.OrderNotFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({ItemNotFoundException.class, OrderNotFoundException.class})
    public Mono<Rendering> handleNotFound(RuntimeException ex) {
        log.error("Not found error: {}", ex.getMessage());
        return Mono.just(Rendering.view("error")
                .status(HttpStatus.NOT_FOUND)
                .modelAttribute("error", "404")
                .modelAttribute("message", ex.getMessage())
                .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<Rendering> handleBadRequest(IllegalArgumentException ex) {
        log.error("Bad request: {}", ex.getMessage());
        return Mono.just(Rendering.view("error")
                .status(HttpStatus.BAD_REQUEST)
                .modelAttribute("error", "400")
                .modelAttribute("message", ex.getMessage())
                .build());
    }
}