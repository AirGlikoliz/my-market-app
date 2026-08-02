package ru.yandex.practicum.mymarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import ru.yandex.practicum.mymarket.payment.client.api.PaymentsApi;
import ru.yandex.practicum.mymarket.payment.client.invoker.ApiClient;

import java.time.Duration;

@Configuration
public class PaymentClientConfig {

    private static final String CLIENT_REGISTRATION_ID = "payment-service";

    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {

        var authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository));
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    @Bean
    public ApiClient paymentApiClient(@Value("${payment.service.base-url}") String baseUrl,
                                       @Value("${payment.service.timeout}") Duration timeout,
                                       ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2Filter.setDefaultClientRegistrationId(CLIENT_REGISTRATION_ID);

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(timeout);

        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(oauth2Filter)
                .build();

        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(baseUrl);
        return apiClient;
    }

    @Bean
    public PaymentsApi paymentsApi(ApiClient paymentApiClient) {
        return new PaymentsApi(paymentApiClient);
    }
}
