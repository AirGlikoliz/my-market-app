package ru.yandex.practicum.mymarket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import ru.yandex.practicum.mymarket.dto.CartAction;
import ru.yandex.practicum.mymarket.dto.SortOption;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, CartAction.class, new StringToEnumIgnoringCaseConverter<>(CartAction.class));
        registry.addConverter(String.class, SortOption.class, new StringToEnumIgnoringCaseConverter<>(SortOption.class));
    }

    private record StringToEnumIgnoringCaseConverter<T extends Enum<T>>(
            Class<T> enumType) implements Converter<String, T> {

        @Override
        public T convert(String source) {
            return Enum.valueOf(enumType, source.trim().toUpperCase());
        }
    }
}
