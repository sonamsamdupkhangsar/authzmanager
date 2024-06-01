package me.sonam.authzmanager.controller.admin.organization;

import jakarta.validation.Valid;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.clients.user.User;

import me.sonam.authzmanager.webclients.UserWebClient;
import me.sonam.authzmanager.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

@Controller
@RequestMapping("/admin/organizations")
public class OrganizationController {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationController.class);

    private OrganizationWebClient organizationWebClient;
    private RoleWebClient roleWebClient;
    private UserWebClient userWebClient;

    public OrganizationController(OrganizationWebClient organizationWebClient, RoleWebClient roleWebClient, UserWebClient userWebClient) {
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.userWebClient = userWebClient;
    }

    /**
     * get all organizations created/owned by this user
     *
     * @param model
     * @return
     */
    @GetMapping
    public Mono<String> getOrganizations(Model model, Pageable pageable) {
        LOG.info("get organizations");
        final String PATH = "/admin/organizations/list";
        int pageSize = 5;

        if (pageable.getPageSize() < 100) {
            pageSize = pageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }

        pageable = PageRequest.of(pageable.getPageNumber(), pageSize, Sort.by("name"));
        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return organizationWebClient.getOrganizationPageByOwner(userId.getUserId(), pageable).doOnNext(restPage -> {
            LOG.info("organizationList: {}", restPage);
            model.addAttribute("page", restPage);
        }).then(Mono.just(PATH));
    }

    @GetMapping("/form")
    public Mono<String> getCreateForm(Model model) {
        LOG.info("return createForm");
        final String PATH = "admin/organizations/form";
        model.addAttribute("organization", new Organization());

        return Mono.just(PATH);
    }

    @PostMapping
    public Mono<String> updateOrganization(@Valid @ModelAttribute("organization") Organization organization, BindingResult bindingResult, Model model) {
        final String PATH = "admin/organizations/form";
        HttpMethod httpMethod = HttpMethod.POST;

        if (organization.getId() == null) {
            LOG.info("no id, this is for create");
            httpMethod = HttpMethod.POST;
        } else {
            LOG.info("has id, this is for update");
            httpMethod = HttpMethod.PUT;
        }
        if (bindingResult.hasErrors()) {
            LOG.info("user didn't enter required fields");
            model.addAttribute("error", "Data validation failed");
            return Mono.just(PATH);
        }
        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Organization org = new Organization(organization.getId(), organization.getName(), userId.getUserId());
        LOG.info("create organization from organization: {}", organization);

        return organizationWebClient.updateOrganization(org, httpMethod).flatMap(organization1 -> {
            LOG.info("got back response: {}", organization1);
            model.addAttribute("organization", organization1);
            return Mono.just(PATH);
        });
    }

    @GetMapping("/{id}")
    public Mono<String> getOrganizationById(@PathVariable("id") UUID id, Model model) {
        final String PATH = "admin/organizations/form";
        LOG.info("get organization by id: {}", id);

        return organizationWebClient.getOrganizationById(id)
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .thenReturn(PATH);
    }


    @GetMapping("/{id}/roles")
    public Mono<String> getRolesForOrganizationId(@PathVariable("id") UUID id, Model model, Pageable userPageable) {
        final String PATH = "admin/organizations/roles";
        LOG.info("get roles for organization by id: {}", id);

        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }
        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize, Sort.by("name"));

        return organizationWebClient.getOrganizationById(id)
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> roleWebClient.getRolesByOrganizationId(id, pageable))
                .doOnNext(roleRestPage ->
                        model.addAttribute("page", roleRestPage))
                .thenReturn(PATH);
    }

    /**
     * delete operation in the Thymeleaf is called thru a Ajax Java script call.
     * Just return the dashboard template after execution.
     * @param organizationId
     * @param model
     * @return
     */
    @DeleteMapping("/{id}")
    public Mono<String> delete(@PathVariable("id") UUID organizationId, Model model) {
        final String PATH = "admin/dashboard";
        LOG.info("delete organization by id {}", organizationId);

        return organizationWebClient.deleteOrganization(organizationId).doOnNext(s -> {
                    model.addAttribute("message", "deleted organization");
                })
                .then(Mono.just(PATH))
                .onErrorResume(throwable -> {
                    LOG.error("failed to delete organization", throwable);
                    model.addAttribute("error", "failed to delete organization");
                    return Mono.just(PATH);
                });
    }

    /*
    get users in the organization id
     */
    @GetMapping("/{id}/users")
    public Mono<String> getUserForOrganizationId(@PathVariable("id") UUID id, Model model, Pageable userPageable) {
        final String PATH = "admin/organizations/user";
        LOG.info("get users for organization by id: {}", id);
        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }
        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize);

        return organizationWebClient.getOrganizationById(id)
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> organizationWebClient.getUsersInOrganizationId(id, pageable))
                .flatMap(uuidPage -> {
                    LOG.info("uuidPage: {}", uuidPage.getContent());
                    model.addAttribute("page", uuidPage);
                    return userWebClient.getUserByBatchOfIds(uuidPage.getContent());
                })
                .doOnNext(users -> {
                    LOG.info("got users: {}", users);
                    model.addAttribute("users", users);
                })
                .thenReturn(PATH);
    }

    @PostMapping("/{id}/users")
    public Mono<String> findUserByAuthenticationId(@PathVariable("id") UUID organizationId,
                                                   @ModelAttribute("username") String authenticationId, final Model model, Pageable userPageable) {
        final String PATH = "admin/organizations/user";
        LOG.info("find user by authenticationId: {}", authenticationId);

        return organizationWebClient.getOrganizationById(organizationId)
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> {
                    int pageSize = 5;

                    if (userPageable.getPageSize() < 100) {
                        pageSize = userPageable.getPageSize();
                        LOG.info("taking page size from pageable: {}", pageSize);
                    }
                    Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize);
                    return organizationWebClient.getUsersInOrganizationId(organization.getId(), pageable)
                            .flatMap(uuidPage -> {
                                LOG.info("uuidPage: {}", uuidPage.getContent());
                                model.addAttribute("page", uuidPage);
                                return userWebClient.getUserByBatchOfIds(uuidPage.getContent());
                            })
                            .doOnNext(users -> {
                                LOG.info("got users: {}", users);
                                model.addAttribute("users", users);
                            }).thenReturn(organization);
                })
                .flatMap(organization -> userWebClient.findByAuthentication(authenticationId))
                .doOnNext(user -> {
                    LOG.info("found user: {}", user);
                    model.addAttribute("message", "Found user with username '" + authenticationId + "'");
                    model.addAttribute("user", user);

                }).flatMap(user -> {
                    LOG.info("checking user.id {} exists in organization, user {}", user.getId(), user);
                    return organizationWebClient.userExistsInOrganization(user.getId(), organizationId)
                            .doOnNext(aBoolean -> {
                                LOG.info("user exists? : {}", aBoolean);
                                user.getOrganizationChoice().setOrganizationId(organizationId);
                                user.getOrganizationChoice().setSelected(aBoolean);
                                //update the user in model
                                model.addAttribute("user", user);
                            });
                }
                )
                .doOnNext(aBoolean -> LOG.info("looks like it executed"))
                .onErrorResume(throwable -> {
                    LOG.error("failed to find user: {}", throwable.getMessage());

                    model.addAttribute("message", "failed to find user, "+ throwable.getMessage());
                    return Mono.just(false);
                })
                .thenReturn(PATH);

    }


    /**
     * this method will handle the form's POST method to associate user to organization:
     * Checked box: add the user to organization
     * Unchecked box: remove the user from organization
     * @param user
     * @param model
     * @param pageable
     * @return
     */

    @PostMapping("/{id}/users/add")
    public Mono<String> updateUserOrganization(@ModelAttribute("user") User user, Model model, Pageable pageable) {
        final String PATH = "admin/organizations/user";
        LOG.info("update user by authenticationId: {} to this organization", user);

        if (user.getOrganizationChoice().getSelected()) {
            LOG.info("add user to organization");

            return addUserToOrganization(PATH, user, model, pageable);
        }
        else {
            LOG.info("remove user from organization");
            return removeUserFromOrganization(PATH, user, model, pageable);
        }
    }

    private Mono<String> addUserToOrganization(final String PATH, User user, Model model, Pageable userPageable) {
        LOG.info("add user to organization: {}", user);
        return organizationWebClient.addUserToOrganization(user.getId(), user.getOrganizationChoice().getOrganizationId())
                .doOnNext(stringStringMap -> model.addAttribute("message", "user successfully added to organization with username: "+ user.getAuthenticationId()))
                .flatMap(stringStringMap -> organizationWebClient.getOrganizationById(user.getOrganizationChoice().getOrganizationId()))
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> {
                    int pageSize = 5;

                    if (userPageable.getPageSize() < 100) {
                        pageSize = userPageable.getPageSize();
                        LOG.info("taking page size from pageable: {}", pageSize);
                    }
                    Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize);
                    return organizationWebClient.getUsersInOrganizationId(organization.getId(), pageable)
                            .flatMap(uuidPage -> {
                                LOG.info("uuidPage: {}", uuidPage.getContent());
                                model.addAttribute("page", uuidPage);
                                return userWebClient.getUserByBatchOfIds(uuidPage.getContent());
                            })
                            .doOnNext(users -> {
                                LOG.info("got users: {}", users);
                                model.addAttribute("users", users);
                            }).thenReturn(organization);
                })
                .flatMap(organization -> userWebClient.getUserById(user.getId()).flatMap(user1 -> {
                    user1.getOrganizationChoice().setSelected(true);
                    user1.getOrganizationChoice().setOrganizationId(organization.getId());
                    return Mono.just(user1);
                }))
                .doOnNext(user1 -> {
                    model.addAttribute("user", null);
                    LOG.info("added to user to organization, nullify the user so the form does not show this user again");
                }
                ).onErrorResume(throwable -> {
                    LOG.error("error occured during adding user to organization", throwable);
                    model.addAttribute("message", "error occured during adding user to organization: " + throwable.getMessage());
                    return Mono.just(new User());
                }).thenReturn(PATH);
    }

    private Mono<String> removeUserFromOrganization(final String PATH, User user, Model model, Pageable userPageable) {
        LOG.info("remove user from organization: {}", user);

        return organizationWebClient.removeUserFromOrganization(user.getId(), user.getOrganizationChoice().getOrganizationId())
                .doOnNext(stringStringMap -> model.addAttribute("message", "user removed from organization successfully with username: "+user.getAuthenticationId()))
                .flatMap(stringStringMap -> organizationWebClient.getOrganizationById(user.getOrganizationChoice().getOrganizationId()))
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> {
                    int pageSize = 5;

                    if (userPageable.getPageSize() < 100) {
                        pageSize = userPageable.getPageSize();
                        LOG.info("taking page size from pageable: {}", pageSize);
                    }
                    Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize);
                    return organizationWebClient.getUsersInOrganizationId(organization.getId(), pageable)
                            .flatMap(uuidPage -> {
                                LOG.info("uuidPage: {}", uuidPage.getContent());
                                model.addAttribute("page", uuidPage);
                                return userWebClient.getUserByBatchOfIds(uuidPage.getContent());
                            })
                            .doOnNext(users -> {
                                LOG.info("got users: {}", users);
                                model.addAttribute("users", users);
                            }).thenReturn(organization);
                })
                .flatMap(organization -> userWebClient.getUserById(user.getId()).flatMap(user1 -> {
                    user1.getOrganizationChoice().setSelected(false);
                    user1.getOrganizationChoice().setOrganizationId(organization.getId());
                    return Mono.just(user1);
                }))
                .doOnNext(user1 -> {
                    model.addAttribute("user", null);
                    LOG.info("removed user from organization, null the user so the form does not show this user again");
                }
                ).onErrorResume(throwable -> {
                    LOG.error("error occurred when removing user from organization", throwable);
                    model.addAttribute("message", "error occurred when removing user from organization: " + throwable.getMessage());
                    return Mono.just(new User());
                }).thenReturn(PATH);
    }

}
