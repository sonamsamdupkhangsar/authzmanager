package me.sonam.authzmanager.controller.admin.subdomain;

import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.controller.admin.organization.Organization;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

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
    public Mono<String> getSubdomainHome(Model model) {
        return getSubdomainOrganizations(model, PageRequest.of(0, DEFAULT_PAGE_SIZE));
    }

    @GetMapping("/organizations")
    public Mono<String> getSubdomainOrganizations(Model model, Pageable userPageable) {
        String accessToken = tokenService.getAccessToken();
        String host = tenantAuthorizationUrlResolver.currentAuthorizationHost();
        Pageable pageable = pageRequest(userPageable);

        return requireSubdomainAdmin(accessToken, host, model)
                .flatMap(subdomain -> organizationWebClient.getOrganizationsBySubdomain(accessToken, host, pageable)
                        .doOnNext(organizationPage -> {
                            model.addAttribute("subdomain", subdomain);
                            model.addAttribute("page", organizationPage);
                        }))
                .thenReturn("admin/subdomain/organizations")
                .onErrorResume(throwable -> renderAccessError(model, throwable, "admin/subdomain/organizations"));
    }

    @GetMapping("/users")
    public Mono<String> getSubdomainUsers(Model model, Pageable userPageable,
                                          @RequestParam(defaultValue = "0") int userPage,
                                          @RequestParam(defaultValue = "5") int userSize) {
        String accessToken = tokenService.getAccessToken();
        String host = tenantAuthorizationUrlResolver.currentAuthorizationHost();
        Pageable organizationPageable = pageRequest(userPageable);
        Pageable usersPageable = PageRequest.of(Math.max(userPage, 0), boundedPageSize(userSize));

        return requireSubdomainAdmin(accessToken, host, model)
                .flatMap(subdomain -> organizationWebClient.getOrganizationsBySubdomain(accessToken, host, organizationPageable)
                        .flatMap(organizationPage -> getUsersByOrganization(accessToken, organizationPage.content(), usersPageable)
                                .doOnNext(organizationUsers -> {
                                    model.addAttribute("subdomain", subdomain);
                                    model.addAttribute("page", organizationPage);
                                    model.addAttribute("userPage", usersPageable.getPageNumber());
                                    model.addAttribute("userSize", usersPageable.getPageSize());
                                    model.addAttribute("organizationUsers", organizationUsers);
                                })))
                .thenReturn("admin/subdomain/users")
                .onErrorResume(throwable -> renderAccessError(model, throwable, "admin/subdomain/users"));
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

    private Mono<List<SubdomainOrganizationUsers>> getUsersByOrganization(String accessToken,
                                                                          List<Organization> organizations,
                                                                          Pageable pageable) {
        return Flux.fromIterable(organizations)
                .flatMap(organization -> organizationWebClient
                        .getUserIdsInOrganizationId(accessToken, organization.getId(), pageable)
                        .flatMap(userIdPage -> getUsers(accessToken, userIdPage)
                                .map(users -> new SubdomainOrganizationUsers(organization, userIdPage, users))))
                .collectList();
    }

    private Mono<List<User>> getUsers(String accessToken, RestPage<UUID> userIdPage) {
        if (userIdPage == null || userIdPage.content() == null || userIdPage.content().isEmpty()) {
            return Mono.just(List.of());
        }
        return userWebClient.getUserByBatchOfIds(accessToken, userIdPage.content())
                .defaultIfEmpty(List.of());
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
