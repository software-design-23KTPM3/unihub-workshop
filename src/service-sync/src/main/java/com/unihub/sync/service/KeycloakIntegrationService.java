package com.unihub.sync.service;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakIntegrationService {

    private final Keycloak keycloak;

    @Value("${app.keycloak.realm}")
    private String realm;

    public void createOrUpdateUser(String username, String email, String name, String password) {
        UsersResource usersResource = keycloak.realm(realm).users();
        
        List<UserRepresentation> existingUsers = usersResource.search(username, true);
        
        if (existingUsers.isEmpty()) {
            createUser(usersResource, username, email, name, password);
        } else {
            updateUser(usersResource, existingUsers.get(0).getId(), email, name);
        }
    }

    private void createUser(UsersResource usersResource, String username, String email, String name, String password) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(name);
        user.setEnabled(true);
        user.setAttributes(Map.of("studentId", List.of(username)));

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));

        Response response = usersResource.create(user);
        if (response.getStatus() == 201) {
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
            // Gán role STUDENT cho user mới tạo
            assignRoleToUser(userId, "STUDENT");
            log.info("Successfully created Keycloak user: {} with role STUDENT", username);
        } else {
            log.error("Failed to create Keycloak user: {}. Status: {}", username, response.getStatus());
        }
    }

    private void assignRoleToUser(String userId, String roleName) {
        try {
            RoleRepresentation role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
            keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
        } catch (Exception e) {
            log.error("Failed to assign role {} to user {}: {}", roleName, userId, e.getMessage());
        }
    }

    private void updateUser(UsersResource usersResource, String userId, String email, String name) {
        UserRepresentation user = usersResource.get(userId).toRepresentation();
        user.setEmail(email);
        user.setFirstName(name);
        user.setAttributes(Map.of("studentId", List.of(user.getUsername())));
        usersResource.get(userId).update(user);
        log.info("Successfully updated Keycloak user: {}", user.getUsername());
    }
}
