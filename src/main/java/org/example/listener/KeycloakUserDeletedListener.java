package org.example.listener;

import lombok.extern.slf4j.Slf4j;
import org.example.configuration.RabbitMQConfig;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KeycloakUserDeletedListener {

    @Value("${keycloak.url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.client-id}")
    private String keycloakClientId;

    @Value("${keycloak.client-secret}")
    private String keycloakClientSecret;

    @RabbitListener(queues = RabbitMQConfig.KEYCLOAK_QUEUE)
    public void handleUserDeletedFromKeycloak(String userId) {
        log.info("Received request to delete user from Keycloak: {}", userId);

        try {
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(keycloakUrl)
                    .realm(keycloakRealm)
                    .clientId(keycloakClientId)
                    .clientSecret(keycloakClientSecret)
                    .grantType("client_credentials")
                    .build();

            keycloak.realm(keycloakRealm)
                    .users()
                    .delete(userId);

            log.info("Successfully deleted user from Keycloak: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete user from Keycloak: {}", e.getMessage());
            throw e;
        }
    }
}