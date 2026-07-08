package me.sonam.authzmanager.controller.admin.subdomain;

import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.clients.role.AuthzManagerRoleAssignment;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import reactor.core.publisher.Flux;
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
    public Mono<String> getSubdomainUsers(Model model, Pageable userPageable) {
        String accessToken = tokenService.getAccessToken();
        String host = tenantAuthorizationUrlResolver.currentAuthorizationHost();
        Pageable pageable = pageRequest(userPageable);

        return requireSubdomainAdmin(accessToken, host, model)
                .flatMap(subdomain -> Mono.zip(
                                organizationWebClient.getUsersBySubdomain(accessToken, host, pageable),
                                roleWebClient.getSubdomainAdminAssignments(accessToken, subdomain.getId(),
                                        PageRequest.of(0, 1000)))
                        .flatMap(result -> getSubdomainUserRows(accessToken, result.getT1())
                                .flatMap(userRows -> {
                                    Map<UUID, UUID> assignmentsByUser = result.getT2().content().stream()
                                            .collect(Collectors.toMap(AuthzManagerRoleAssignment::userId,
                                                    AuthzManagerRoleAssignment::id, (first, second) -> first));
                                    userRows.stream().filter(row -> row.user() != null).forEach(row ->
                                            row.user().setAuthzManagerRoleAssignmentId(
                                                    assignmentsByUser.get(row.user().getId())));
                                    return addSubdomainAdminEligibility(accessToken, host, userRows);
                                })
                                .doOnNext(userRows -> {
                                    model.addAttribute("subdomain", subdomain);
                                    model.addAttribute("page", result.getT1());
                                    model.addAttribute("userRows", userRows);
                                })))
                .thenReturn("admin/subdomain/users")
                .onErrorResume(throwable -> renderAccessError(model, throwable, "admin/subdomain/users"));
    }

    @PostMapping("/users/{userId}/subdomain-admin")
    public Mono<String> addSubdomainAdmin(@PathVariable UUID userId, Model model, Pageable userPageable,
                                          RedirectAttributes redirectAttributes) {
        String accessToken = tokenService.getAccessToken();
        String host = tenantAuthorizationUrlResolver.currentAuthorizationHost();

        return requireSubdomainAdmin(accessToken, host, model)
                .flatMap(subdomain -> organizationWebClient
                        .getDefaultOrganizationIdForUser(accessToken, userId, host)
                        .switchIfEmpty(Mono.error(new AuthzManagerException(
                                "User must have a default organization in this subdomain")))
                        .flatMap(organizationId -> roleWebClient
                                .isOrgAdminInOrgId(accessToken, userId, organizationId)
                                .filter(Boolean::booleanValue)
                                .switchIfEmpty(Mono.error(new AuthzManagerException(
                                        "User must be OrgAdmin for their default organization"))))
                        .then(Mono.defer(() -> roleWebClient.addSubdomainAdmin(
                                accessToken, subdomain.getId(), userId))))
                .doOnNext(assignment -> redirectAttributes.addFlashAttribute(
                        "message", "SubdomainAdmin assigned"))
                .onErrorResume(throwable -> {
                    redirectAttributes.addFlashAttribute("error", throwable.getMessage());
                    return Mono.empty();
                })
                .thenReturn(usersRedirect(userPageable));
    }

    @PostMapping("/administrators/{assignmentId}/remove")
    public Mono<String> removeSubdomainAdmin(@PathVariable UUID assignmentId, Model model, Pageable userPageable,
                                             RedirectAttributes redirectAttributes) {
        String accessToken = tokenService.getAccessToken();
        String host = tenantAuthorizationUrlResolver.currentAuthorizationHost();

        return requireSubdomainAdmin(accessToken, host, model)
                .flatMap(subdomain -> roleWebClient.removeSubdomainAdmin(
                        accessToken, subdomain.getId(), assignmentId))
                .doOnNext(ignored -> redirectAttributes.addFlashAttribute(
                        "message", "SubdomainAdmin removed"))
                .onErrorResume(throwable -> {
                    redirectAttributes.addFlashAttribute("error", throwable.getMessage());
                    return Mono.empty();
                })
                .thenReturn(usersRedirect(userPageable));
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
                            .map(membership -> new SubdomainUserRow(
                                    membership, usersById.get(membership.userId()), false))
                            .toList();
                });
    }

    private Mono<List<SubdomainUserRow>> addSubdomainAdminEligibility(String accessToken, String host,
                                                                      List<SubdomainUserRow> userRows) {
        return Flux.fromIterable(userRows)
                .flatMapSequential(row -> addSubdomainAdminEligibility(accessToken, host, row))
                .collectList();
    }

    private Mono<SubdomainUserRow> addSubdomainAdminEligibility(String accessToken, String host,
                                                                 SubdomainUserRow row) {
        if (row.user() == null || row.user().getAuthzManagerRoleAssignmentId() != null) {
            return Mono.just(row);
        }

        return organizationWebClient.getDefaultOrganizationIdForUser(accessToken, row.user().getId(), host)
                .flatMap(defaultOrganizationId -> roleWebClient.isOrgAdminInOrgId(
                        accessToken, row.user().getId(), defaultOrganizationId))
                .defaultIfEmpty(false)
                .onErrorResume(throwable -> {
                    LOG.debug("Unable to determine SubdomainAdmin eligibility for user {}",
                            row.user().getId(), throwable);
                    return Mono.just(false);
                })
                .map(eligible -> new SubdomainUserRow(row.membership(), row.user(), eligible));
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

    private String usersRedirect(Pageable userPageable) {
        return "redirect:/admin/subdomain/users?page=" + userPageable.getPageNumber()
                + "&size=" + boundedPageSize(userPageable.getPageSize());
    }

    private Mono<String> renderAccessError(Model model, Throwable throwable, String view) {
        LOG.error("SubdomainAdmin page failed: {}", throwable.getMessage(), throwable);
        model.addAttribute("error", throwable.getMessage());
        return Mono.just(view);
    }
}
