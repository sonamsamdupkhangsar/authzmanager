package me.sonam.authzmanager.controller.admin.user;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.signup.UserSignup;

import me.sonam.authzmanager.controller.util.MessageConstants;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.apache.tomcat.websocket.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.*;

@Controller
@RequestMapping("/admin/organizations/users")
public class AddUserController {
    private static final Logger LOG = LoggerFactory.getLogger(AddUserController.class);

    private final TokenService tokenService;
    private final OrganizationWebClient organizationWebClient;
    private final RoleWebClient roleWebClient;
    private final UserWebClient userWebClient;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;
    private final String USER_ADD = "/admin/users/add";

    @Value("${maxUsersPerOrganization}")
    private int maxUsersPerOrganization;

    public AddUserController(OrganizationWebClient organizationWebClient, RoleWebClient roleWebClient,
                             TokenService tokenService, UserWebClient userWebClient,
                             TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.tokenService = tokenService;
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.userWebClient = userWebClient;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    @GetMapping
    public Mono<String> getSignupForm(Model model, HttpServletRequest request) {
        LOG.info("returning {}", USER_ADD);

        final String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();

        return organizationWebClient.getDefaultOrganizationIdForUser(accessToken, userId, organizationHost)
                .switchIfEmpty(Mono.error(new AuthzManagerException("no default organization found")))
                        .flatMap(orgId -> roleWebClient.isSuperAdminInOrgId(accessToken, userId, orgId).zipWith(Mono.just(orgId)))
                        .flatMap(objects -> {
                                if (!objects.getT1()) {
                                    model.addAttribute("error", MessageConstants.NOT_SUPERADMIN + " " + objects.getT2());
                                    return Mono.error(new AuthenticationException(MessageConstants.NOT_SUPERADMIN));
                                }
                            return organizationWebClient.getOrganizationById(accessToken, objects.getT2());
                        })

                        .doOnNext(organization -> model.addAttribute("organization", organization))
                        .doOnNext(organization -> model.addAttribute("signupUser", new UserSignup()))
                .thenReturn(USER_ADD)
                .onErrorResume(throwable -> {
                    LOG.debug("error occurred in getting defaultOrganization", throwable);
                    LOG.error("failed to get default organization {}", throwable.getMessage());

                    model.addAttribute("organization", new Organization());
                    model.addAttribute("signupUser", new UserSignup());
                    return Mono.just(USER_ADD);
                });
    }

    @PostMapping// (consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> signupUserFromForm(@Valid @ModelAttribute("signupUser") UserSignup userSignup,
        BindingResult bindingResult, Model model, HttpServletRequest request) {

        LOG.info("delegate user add to userSignupController for userSignup {}", userSignup);

        final String PATH = "admin/clients/form";

        final String accessToken = tokenService.getAccessToken();
        userSignup.setAuthenticationId(userSignup.getEmail());
        normalizeBlankPassword(userSignup);
        UUID userId = Util.getLoggedInUserId();
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();
        userSignup.setActivationHost(organizationHost);

        return  organizationWebClient.getDefaultOrganizationIdForUser(accessToken, userId, organizationHost)
                .switchIfEmpty(Mono.error(new AuthzManagerException("no default organization found")))
                .flatMap(orgId -> roleWebClient.isSuperAdminInOrgId(accessToken, userId, orgId).zipWith(Mono.just(orgId)))
                .flatMap(objects -> {
                    if (!objects.getT1()) {
                        model.addAttribute("error", MessageConstants.NOT_SUPERADMIN + " " + objects.getT2());
                        return Mono.error(new AuthenticationException(MessageConstants.NOT_SUPERADMIN));
                    }
                    return organizationWebClient.getOrganizationById(accessToken, userSignup.getOrganizationId());
                })
                .doOnNext(organization -> model.addAttribute("organization", organization))
                        .flatMap(organization -> userSignupByAdmin(accessToken, userSignup,
                                bindingResult, model, USER_ADD, organizationHost));
    }

    private Mono<String> userSignupByAdmin(String accessToken, UserSignup userSignup, BindingResult bindingResult,
                                           Model model, final String PATH, String subdomain) {
        LOG.info("User signup initiated by admin for user: {}", userSignup);

        if (bindingResult.hasErrors()) {
            LOG.info("required fields missing for user {}", bindingResult.getAllErrors());
            model.addAttribute("error", "Data validation failed");
            return Mono.just(PATH);
        }
        return preflightCanAddUserToOrganization(accessToken, userSignup, subdomain)
                .then(userWebClient.signupUser(accessToken, userSignup))
                .flatMap(s -> {
                    LOG.info("user has been added successfully with message: {}", s);
                    StringBuilder stringBuilder = new StringBuilder(userSignup.getFirstName())
                            .append(" ").append(userSignup.getLastName())
                            .append(" has been added successfully! ");

                    if (!userSignup.isActive()) {
                        stringBuilder.append("Let the user know they will get a email to activate")
                                .append(" their account");
                    } else {
                        stringBuilder.append("Their account is now active and can log-in with the ")
                                .append("password that was set by you.");
                    }

                    model.addAttribute("message", stringBuilder.toString());
                    return Mono.just(PATH);
                })
                .flatMap(s -> userWebClient.findByAuthenticationId(accessToken, userSignup.getAuthenticationId()))
                .flatMap(user -> organizationWebClient.addUserToOrganization(accessToken, user.getId(),
                        userSignup.getOrganizationId(), subdomain, true).zipWith(Mono.just(user)))
                .flatMap(objects -> organizationWebClient.setDefaultOrganization(accessToken,
                        userSignup.getOrganizationId(), objects.getT2().getId()))
                .thenReturn(PATH)
                .onErrorResume(throwable -> {
                    LOG.info("exception occured in signing up user by admin {}", throwable.getMessage());
                    setErrorInModel(throwable, model, "failed to add user by admin");
                    model.addAttribute("userSignup", userSignup);
                    return Mono.just(PATH);
                });

    }

    private Mono<Void> preflightCanAddUserToOrganization(String accessToken, UserSignup userSignup, String subdomain) {
        return organizationWebClient.canAddUserToOrganization(accessToken, userSignup.getOrganizationId(), subdomain)
                .then(userWebClient.findByAuthenticationId(accessToken, userSignup.getAuthenticationId())
                        .onErrorResume(throwable -> {
                            LOG.info("user {} does not exist yet, skipping user-specific organization preflight",
                                    userSignup.getAuthenticationId());
                            return Mono.empty();
                        })
                        .flatMap(user -> organizationWebClient.canAddUserToOrganization(accessToken, user.getId(),
                                userSignup.getOrganizationId(), subdomain)));
    }

    // Prevent an unchecked or blank password field from being treated as a real password during add-user signup.
    private void normalizeBlankPassword(UserSignup userSignup) {
        char[] password = userSignup.getPassword();
        if (password == null || password.length == 0) {
            LOG.info("admin signup password is absent or empty; leaving password unset");
            userSignup.setPassword(null);
            return;
        }

        LOG.info("admin signup password was submitted with length {}", password.length);
        for (char c : password) {
            if (!Character.isWhitespace(c)) {
                LOG.info("admin signup password contains non-whitespace characters; keeping submitted password");
                return;
            }
        }
        LOG.info("admin signup password contains only whitespace; clearing password");
        userSignup.setPassword(null);
    }

    private void setErrorInModel(Throwable throwable, Model model, String defaultErrMessage) {
        LOG.error("exception occured in signup user", throwable);
        LOG.error(defaultErrMessage);

        if (throwable instanceof WebClientResponseException webClientResponseException) {
            Map<String, String> map = webClientResponseException.getResponseBodyAs(
                    new ParameterizedTypeReference<>() {});

            if (map != null) {
                LOG.error("{}: {}", defaultErrMessage, map.get("error"));

                if (map.get("error") != null) {
                    model.addAttribute("error", map.get("error"));
                }
                else {
                    LOG.error("there is no error key in the map, add map itself to error message");
                    model.addAttribute("error", map);
                }
            }
            else {
                LOG.error("map is null on response for throwable", throwable);
                model.addAttribute("error", defaultErrMessage + throwable.getMessage());
            }
            LOG.error("{}: {}", defaultErrMessage, throwable.getMessage());
        } else {
            //set model error attribute to present back to user
            model.addAttribute("error", defaultErrMessage  + throwable.getMessage());
        }
    }

}
