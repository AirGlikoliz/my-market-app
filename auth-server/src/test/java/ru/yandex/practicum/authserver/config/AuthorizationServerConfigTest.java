package ru.yandex.practicum.authserver.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthorizationServerConfigTest {

    private static final String CLIENT_ID = "market-app-client";
    private static final String CLIENT_SECRET = "market-app-secret";

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private AuthorizationServerSettings authorizationServerSettings;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void marketAppClient_ShouldBeRegisteredWithClientCredentialsAndPaymentAccessScope() {
        RegisteredClient client = registeredClientRepository.findByClientId(CLIENT_ID);

        assertNotNull(client);
        assertEquals(CLIENT_ID, client.getClientId());
        assertTrue(client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS));
        assertTrue(client.getScopes().contains(AuthorizationServerConfig.PAYMENT_SCOPE));
        assertTrue(client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC));
    }

    @Test
    void marketAppClient_SecretShouldBeHashedNotStoredInPlainText() {
        RegisteredClient client = registeredClientRepository.findByClientId(CLIENT_ID);

        assertNotNull(client.getClientSecret());
        assertNotEquals(CLIENT_SECRET, client.getClientSecret());
        assertTrue(passwordEncoder.matches(CLIENT_SECRET, client.getClientSecret()));
    }

    @Test
    void unknownClient_ShouldNotBeRegistered() {
        assertNull(registeredClientRepository.findByClientId("some-other-client"));
    }

    @Test
    void authorizationServerSettings_ShouldExposeConfiguredIssuer() {
        assertEquals("http://localhost:9000", authorizationServerSettings.getIssuer());
    }
}
