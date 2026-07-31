package ru.yandex.practicum.mymarket.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import ru.yandex.practicum.mymarket.payment.client.api.PaymentsApi;
import ru.yandex.practicum.mymarket.payment.client.invoker.ApiClient;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentClientOAuth2Test {

    private static final String CLIENT_REGISTRATION_ID = "payment-service";
    private static final String STUB_ACCESS_TOKEN = "stub-access-token";

    private static DisposableServer tokenServer;
    private static DisposableServer paymentServer;
    private static final AtomicReference<String> capturedAuthorizationHeader = new AtomicReference<>();

    @BeforeAll
    static void startStubServers() {
        tokenServer = HttpServer.create()
                .port(0)
                .route(routes -> routes
                        .post("/oauth2/token", (request, response) ->
                                response.header("Content-Type", "application/json")
                                        .sendString(Mono.just("{\"access_token\":\"" + STUB_ACCESS_TOKEN + "\","
                                                + "\"token_type\":\"Bearer\",\"expires_in\":3600,\"scope\":\"payment.access\"}"))))
                .bindNow();

        paymentServer = HttpServer.create()
                .port(0)
                .route(routes -> routes
                        .get("/api/v1/payments/balance", (request, response) -> {
                            capturedAuthorizationHeader.set(request.requestHeaders().get("Authorization"));
                            return response.header("Content-Type", "application/json")
                                    .sendString(Mono.just("{\"balance\": 10000}"));
                        }))
                .bindNow();
    }

    @AfterAll
    static void stopStubServers() {
        tokenServer.disposeNow();
        paymentServer.disposeNow();
    }

    @BeforeEach
    void setUp() {
        capturedAuthorizationHeader.set(null);
    }

    @Test
    void outboundCallToPaymentService_carriesClientCredentialsBearerToken() {
        ClientRegistration registration = ClientRegistration.withRegistrationId(CLIENT_REGISTRATION_ID)
                .tokenUri("http://localhost:" + tokenServer.port() + "/oauth2/token")
                .clientId("market-app-client")
                .clientSecret("market-app-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("payment.access")
                .build();

        ReactiveClientRegistrationRepository registrations =
                new InMemoryReactiveClientRegistrationRepository(registration);

        PaymentClientConfig config = new PaymentClientConfig();
        ReactiveOAuth2AuthorizedClientManager authorizedClientManager = config.authorizedClientManager(registrations);
        ApiClient apiClient = config.paymentApiClient(
                "http://localhost:" + paymentServer.port(), Duration.ofSeconds(2), authorizedClientManager);
        PaymentsApi paymentsApi = config.paymentsApi(apiClient);

        paymentsApi.getBalance("buyer1").block();

        assertEquals("Bearer " + STUB_ACCESS_TOKEN, capturedAuthorizationHeader.get());
    }
}
