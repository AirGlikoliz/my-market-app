package ru.yandex.practicum.mymarket.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import ru.yandex.practicum.mymarket.exception.ItemNotFoundException;
import ru.yandex.practicum.mymarket.exception.OrderNotFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({ItemNotFoundException.class, OrderNotFoundException.class})
    public ModelAndView handleNotFound(RuntimeException ex) {
        log.error("Not found error: {}", ex.getMessage());

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("error", "404");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.NOT_FOUND);

        return mav;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleBadRequest(IllegalArgumentException ex) {
        log.error("Bad request: {}", ex.getMessage());

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("error", "400");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.BAD_REQUEST);

        return mav;
    }
}
