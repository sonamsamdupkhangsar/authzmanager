package me.sonam.authzmanager.controller.admin.roles;

import jakarta.validation.Valid;
import me.sonam.authzmanager.clients.RoleWebClient;
import me.sonam.authzmanager.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Controller
@RequestMapping("/admin/roles")
public class RoleController {
    private static final Logger LOG = LoggerFactory.getLogger(RoleController.class);

    private RoleWebClient roleWebClient;

    public RoleController(RoleWebClient roleWebClient) {
        this.roleWebClient = roleWebClient;
    }

    @GetMapping
    public Mono<String> getRolesByUserId(Model model) {
        final String PATH = "admin/roles/list";
        LOG.info("get roles by owner id");
        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return roleWebClient.getRolesByUserId(userId.getUserId()).doOnNext(restPage -> {
            LOG.info("roleList: {}", restPage);
            model.addAttribute("page", restPage);
        }).then(Mono.just(PATH));

    }
    /**
     * get all roles in this role id
     * @param model
     * @return
     */
    @GetMapping("/role/{roleId}")
    public Mono<String> getRoleRoles(@PathVariable("roleId")UUID roleId, Model model) {
        LOG.info("return createForm");
        final String PATH = "/admin/roles/list";

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return roleWebClient.getRoles(roleId).doOnNext(restPage -> {
            LOG.info("roleList: {}", restPage);
            model.addAttribute("page", restPage);
        }).then(Mono.just(PATH));
    }

    @GetMapping("/new")
    public Mono<String> getCreateForm(Model model) {
        LOG.info("return createForm");
        final String PATH = "admin/roles/form";

        model.addAttribute("role", new Role());

        return Mono.just(PATH);
    }

    @PostMapping
    public Mono<String> updateRole(@Valid  @ModelAttribute("role") Role role, BindingResult bindingResult, Model model) {
        final String PATH = "admin/roles/form";
        HttpMethod httpMethod = HttpMethod.POST;

        if (role.getId() == null) {
            LOG.info("no id, this is for create");
        }
        else {
            LOG.info("has id, this is for update");
            httpMethod = HttpMethod.PUT;
        }
        if (bindingResult.hasErrors()) {
            LOG.info("user didn't enter required fields");
            model.addAttribute("error", "Data validation failed");
            return Mono.just(PATH);
        }
        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Role role2 = new Role(role.getId(), role.getName(), userId.getUserId());
        LOG.info("create role : {}", role);

       return roleWebClient.updateRole(role2, httpMethod).flatMap(updateRole -> {
                    LOG.info("got back response: {}", updateRole);
                    model.addAttribute("role", updateRole);
                    model.addAttribute("message", "role updated");
                    return Mono.just(PATH);
        }).onErrorResume(throwable -> {
           model.addAttribute("role", role2);
           model.addAttribute("error", "failed to update/create role");
           return Mono.just(PATH);
       });
    }

    @GetMapping("/{id}")
    public Mono<String> getRoleById(@PathVariable("id") UUID id, Model model) {
        final String PATH = "admin/roles/form";
        LOG.info("get role by id: {}", id);

        return roleWebClient.getRoleById(id)
                .doOnNext(role -> model.addAttribute("role", role))
                .thenReturn(PATH);
    }

    @DeleteMapping("/{id}")
    public Mono<String> delete(@PathVariable("id") UUID roleId, Model model) {
        final String PATH = "admin/roles/role";
        LOG.info("delete role by id {}", roleId);

        return roleWebClient.deleteRole(roleId).doOnNext(s -> {
                    model.addAttribute("message", "deleted role");
                })
                .then(Mono.just(PATH))
                .onErrorResume(throwable -> {
                    LOG.error("failed to delete role", throwable);
                    model.addAttribute("error", "failed to delete role");
                    return Mono.just(PATH);
        });
    }
}
