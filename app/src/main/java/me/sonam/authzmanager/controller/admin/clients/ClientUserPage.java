package me.sonam.authzmanager.controller.admin.clients;

import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ClientUserPage {
    Mono<String> setUsersAndsersInClientOrganizationUserRole(String accessToken, UUID id, Model model, Pageable userPageable);
}
