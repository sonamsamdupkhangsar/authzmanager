package me.sonam.authzmanager.controller.admin.subdomain;

import jakarta.servlet.http.HttpServletRequest;
import me.sonam.authzmanager.advice.SubdomainMenuAdvice;
import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/subdomain")
public class SubdomainAdminController {
    private static final Logger LOG = LoggerFactory.getLogger(SubdomainAdminController.class);
    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 50;

    private final OrganizationWebClient organizationWebClient;
    private final RoleWebClient roleWebClient;
    private final UserWebClient userWebClient;
    private final TokenService tokenService;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    public SubdomainAdminController(OrganizationWebClient organizationWebClient,
                                    RoleWebClient roleWebClient,
                                    UserWebClient userWebClient,
                                    TokenService tokenService,
                                    TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.userWebClient = userWebClient;
        this.tokenService = tokenService;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    @GetMapping
    public Mono<String> getSubdomainHome(Model model, HttpServletRequest request) {
        return getSubdomainOrganizations(model, PageRequest.of(0, DEFAULT_PAGE_SIZE), request);
    }

    @GetMapping("/organizations")
    public Mono<String> getSubdomainOrganizations(Model model, Pageable userPageable, HttpServletRequest request) {
        String accessToken = tokenService.getAccessToken();
        String host = tenantAuthorizationUrlResolver.currentAuthorizationHost();
        Pageable pageable = pageRequest(userPageable);

        return requireSubdomainAdmin(accessToken, host, model)
                .doOnNext(subdomain -> showSubdomainMenu(request, model))
                .flatMap(subdomain -> organizationWebClient.getOrganizationsBySubdomain(accessToken, host, pageable)
                        .doOnNext(organizationPage -> {
                            model.addAttribute("subdomain", subdomain);
                            model.addAttribute("page", organizationPage);
                        }))
                .thenReturn("admin/subdomain/organizations")
                .onErrorResume(throwable -> renderAccessError(model, throwable, "admin/subdomain/organizations"));
    }

    @GetMapping("/users")
    public Mono<String> getSubdomainUsers(Model model, Pageable userPageable, HttpServletRequest request) {
        String accessToken = tokenService.getAccessToken();
        String host = tenantAuthorizationUrlResolver.currentAuthorizationHost();
        Pageable pageable = pageRequest(userPageable);

        return requireSubdomainAdmin(accessToken, host, model)
                .doOnNext(subdomain -> showSubdomainMenu(request, model))
                .flatMap(subdomain -> organizationWebClient.getUsersBySubdomain(accessToken, host, pageable)
                        .flatMap(userMembershipPage -> getSubdomainUserRows(accessToken, userMembershipPage)
                                .doOnNext(userRows -> {
                                    model.addAttribute("subdomain", subdomain);
                                    model.addAttribute("page", userMembershipPage);
                                    model.addAttribute("userRows", userRows);
                                })))
                .thenReturn("admin/subdomain/users")
                .onErrorResume(throwable -> renderAccessError(model, throwable, "admin/subdomain/users"));
    }

    private void showSubdomainMenu(HttpServletRequest request, Model model) {
        request.getSession().setAttribute(SubdomainMenuAdvice.SHOW_SUBDOMAIN_MENU_SESSION_ATTRIBUTE, true);
        model.addAttribute(SubdomainMenuAdvice.SHOW_SUBDOMAIN_MENU_SESSION_ATTRIBUTE, true);
    }

    private Mono<Subdomain> requireSubdomainAdmin(String accessToken, String host, Model model) {
        UUID userId = Util.getLoggedInUserId();
        LOG.info("check SubdomainAdmin access for user {} and host {}", userId, host);

        return organizationWebClient.getSubdomainByHost(accessToken, host)
                .doOnNext(subdomain -> model.addAttribute("subdomain", subdomain))
                .flatMap(subdomain -> roleWebClient.isSubdomainAdminInSubdomainId(accessToken, userId, subdomain.getId())
                        .flatMap(isSubdomainAdmin -> {
                            if (isSubdomainAdmin) {
                                return Mono.just(subdomain);
                            }
                            return Mono.error(new AuthzManagerException("You need to be a SubdomainAdmin for " + host));
                        }));
    }

    private Mono<List<SubdomainUserRow>> getSubdomainUserRows(String accessToken,
                                                              RestPage<SubdomainOrganizationUser> userMembershipPage) {
        if (userMembershipPage == null || userMembershipPage.content() == null || userMembershipPage.content().isEmpty()) {
            return Mono.just(List.of());
        }

        List<UUID> userIds = userMembershipPage.content().stream()
                .map(SubdomainOrganizationUser::userId)
                .distinct()
                .toList();

        return userWebClient.getUserByBatchOfIds(accessToken, userIds)
                .defaultIfEmpty(List.of())
                .map(users -> {
                    Map<UUID, User> usersById = users.stream()
                            .collect(Collectors.toMap(User::getId, Function.identity(), (first, second) -> first));
                    return userMembershipPage.content().stream()
                            .map(membership -> new SubdomainUserRow(membership, usersById.get(membership.userId())))
                            .toList();
                });
    }

    private Pageable pageRequest(Pageable userPageable) {
        return PageRequest.of(userPageable.getPageNumber(), boundedPageSize(userPageable.getPageSize()),
                Sort.by("name"));
    }

    private int boundedPageSize(int requestedPageSize) {
        if (requestedPageSize > 0 && requestedPageSize <= MAX_PAGE_SIZE) {
            return requestedPageSize;
        }
        return DEFAULT_PAGE_SIZE;
    }

    private Mono<String> renderAccessError(Model model, Throwable throwable, String view) {
        LOG.error("SubdomainAdmin page failed: {}", throwable.getMessage(), throwable);
        model.addAttribute("error", throwable.getMessage());
        return Mono.just(view);
    }
}
