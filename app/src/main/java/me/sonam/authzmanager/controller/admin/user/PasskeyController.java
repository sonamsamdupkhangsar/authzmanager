package me.sonam.authzmanager.controller.admin.user;

import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/admin/user/passkeys")
public class PasskeyController {
    private static final Logger LOG = LoggerFactory.getLogger(PasskeyController.class);

    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    public PasskeyController(TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    @GetMapping
    public RedirectView passkeyEnrollment() {
        String returnUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/admin/user/profile")
                .build()
                .toUriString();
        String enrollmentUrl = UriComponentsBuilder
                .fromUriString(tenantAuthorizationUrlResolver.currentIssuerUri())
                .path("/mfa/passkeys")
                .queryParam("return_url", returnUrl)
                .build()
                .toUriString();
        LOG.info("redirect user to passkey enrollment url {}", enrollmentUrl);
        return new RedirectView(enrollmentUrl);
    }
}
