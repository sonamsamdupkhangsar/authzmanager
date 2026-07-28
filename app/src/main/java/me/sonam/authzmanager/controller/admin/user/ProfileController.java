package me.sonam.authzmanager.controller.admin.user;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequestMapping("/admin/user/profile")
public class ProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(ProfileController.class);

    private UserWebClient userWebClient;
    private static final String PATH = "/admin/user/profile";
    private TokenService tokenService;

    public ProfileController(UserWebClient userWebClient, TokenService tokenService) {
        this.userWebClient = userWebClient;
        this.tokenService = tokenService;
    }

    @GetMapping
    public Mono<String> getProfile(Model model) {
        LOG.info("get profile for the logged in user");

        final String accessToken = tokenService.getAccessToken();

        return userWebClient.getUserById(accessToken, getUserId())
                .doOnNext(user -> {
                    LOG.info("got user: {}", user);
                    String profilePhotoJson = user.getProfilePhoto();
                    if (user.getProfilePhoto() != null && !user.getProfilePhoto().isEmpty()) {
                        final String thumbnailUrl = getProfileUrl(profilePhotoJson);
                        LOG.debug("set profile photo thumbnail URL");
                        user.setProfilePhoto(thumbnailUrl);
                    }

            model.addAttribute("user", user);
        }).thenReturn(PATH);
    }

    /**
     * This method will extract the thumbnail url from the profilePhoto json
     * @param profilePhotoJson
     * @return thumbnail string
     */
    private String getProfileUrl(String profilePhotoJson) {
        LOG.debug("profile photo metadata received");

        if (profilePhotoJson == null || profilePhotoJson.isEmpty()) {
            LOG.info("profilePhoto json is empty or null, return empty string");
            return "";
        }
        try {
            JsonElement jsonElement = JsonParser.parseString(profilePhotoJson);
            LOG.debug("profile photo metadata type: {}", jsonElement.getClass());

            JsonObject jsonObject2 = null;
            if (jsonElement.isJsonPrimitive()) {
                JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
                // Get the primitive value (string, number, boolean)
                JsonElement jsonElement2 = JsonParser.parseString(jsonPrimitive.getAsString());
                LOG.info("jsonPrimitive to jsonElement.isJsonObject ?: {}", jsonElement2.isJsonObject());

                jsonObject2 = jsonElement2.getAsJsonObject();
                final String thumbnailUrl = jsonObject2.get("thumbnailUrl").getAsString();
                LOG.debug("thumbnail URL extracted from profile photo metadata");
                return thumbnailUrl;
            } else if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                final String thumbnailUrl = jsonObject.get("thumbnailUrl").getAsString();
                LOG.debug("thumbnail URL extracted from profile photo metadata");
                return thumbnailUrl;
            } else {
                return "empty";
            }
        }
        catch (Exception e) {
            LOG.error("profilePhoto json is not in valid format: {}", e.getMessage());
            LOG.info("exception stack trace is", e);
            return "";
        }
    }

    @PostMapping(path = "/photo", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public Mono<String> updateProfilePhoto(@RequestPart("file") MultipartFile multipartFile, Model model) {
        LOG.info("update profile photo");

        final String accessToken = tokenService.getAccessToken();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (multipartFile.isEmpty() || multipartFile.getContentType() == null) {
            return Mono.error(new IllegalArgumentException("Select an image to upload"));
        }
        try {
            return userWebClient.uploadProfilePhoto(accessToken, authentication.getName(),
                            multipartFile.getOriginalFilename(),
                            MediaType.parseMediaType(multipartFile.getContentType()), multipartFile.getBytes())
                    .thenReturn("redirect:" + PATH);
        } catch (IOException exception) {
            return Mono.error(new IllegalArgumentException("Could not read the selected image", exception));
        }
    }

    /**
     * this will upload the file with html post containing text fields also.
     * The content type from html post is set to application/octet-stream.
     * @param user contains the form user properties
     * @param model Model for adding user
     * @return Path
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public Mono<String> updateProfile(@ModelAttribute User user,  Model model){

        LOG.info("update profile for the logged in user: {}", user);

        final String accessToken = tokenService.getAccessToken();
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        LOG.debug("override authentication name with the logged-in user.");
        user.setAuthenticationId(authentication.getName());

        LOG.info("authentication {},\n authentication.principal {}", authentication,authentication.getName());

        return userWebClient.updateProfile(accessToken, user)
                .switchIfEmpty(Mono.just("is empty"))
                .doOnNext(s -> LOG.info("updated profile, server response: {}", s))
                .flatMap(s -> userWebClient.getUserById(accessToken, user.getId()))
                .flatMap(user1 -> {
                    LOG.info("got user: {}", user1);
                    String profilePhotoJson = user1.getProfilePhoto();
                    if (user1.getProfilePhoto() != null && !user1.getProfilePhoto().isEmpty()) {
                        final String thumbnailUrl = getProfileUrl(profilePhotoJson);
                        LOG.debug("set profile photo thumbnail URL");
                        user1.setProfilePhoto(thumbnailUrl);
                    }

                    model.addAttribute("user", user1);
                    return Mono.just(PATH);
                });
    }

    public UUID getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LOG.info("authentication: {}", authentication);

        DefaultOidcUser defaultOidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userIdString = defaultOidcUser.getAttribute("userId");
        UUID userId = UUID.fromString(userIdString);
        return userId;
    }

}
