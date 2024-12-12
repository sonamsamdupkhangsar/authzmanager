package me.sonam.authzmanager.controller.admin.user;

import cloud.sonam.s3.config.S3ClientConfigurationProperties;
import cloud.sonam.s3.file.S3Service;
import cloud.sonam.s3.file.util.ImageUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class ProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(ProfileController.class);

    private UserWebClient userWebClient;
    private static final String PATH = "/admin/user/profile";
    private TokenService tokenService;

    @Value("${profilePhotoFolder}")
    private String profilePhotoFolder;

    @Autowired
    private S3Service s3Service;
    @Autowired
    private S3ClientConfigurationProperties s3ClientConfigurationProperties;



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
                        LOG.info("set thumbnailUrl in profilePhoto: {}", thumbnailUrl);
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
        LOG.info("got profilePhoto json: {}", profilePhotoJson);

        if (profilePhotoJson == null || profilePhotoJson.isEmpty()) {
            LOG.info("profilePhoto json is empty or null, return empty string");
            return "";
        }
        try {
            JsonElement jsonElement = JsonParser.parseString(profilePhotoJson);
            LOG.info("jsonElement: {}", jsonElement.toString());
            LOG.info("json.instance of {}", jsonElement.getClass());

            JsonObject jsonObject2 = null;
            if (jsonElement.isJsonPrimitive()) {
                JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
                // Get the primitive value (string, number, boolean)
                LOG.debug("json primitive: {}", jsonPrimitive);
                LOG.debug("jsonPrimitive.string: {}", jsonPrimitive.getAsString());
                JsonElement jsonElement2 = JsonParser.parseString(jsonPrimitive.getAsString());
                LOG.info("jsonPrimitive to jsonElement.isJsonObject ?: {}", jsonElement2.isJsonObject());

                jsonObject2 = jsonElement2.getAsJsonObject();
                final String thumbnailUrl = jsonObject2.get("thumbnailUrl").getAsString();
                LOG.info("jsonPrimitive thumbnailUrl: {}", thumbnailUrl);
                return thumbnailUrl;
            } else if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                final String thumbnailUrl = jsonObject.get("thumbnailUrl").getAsString();
                LOG.info("thumbnailUrl: {}", thumbnailUrl);
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
        // get the userid before processing because the subsequent calls in the webflux will
        // start in a new thread and will lose user/token context because this api will be called by
        // javascript call where the logged-in user context gets lost in a separate thread.
        UUID userId = getUserId();

        return handleFileUpload(multipartFile)
                .switchIfEmpty(Mono.error(new AuthzManagerException("Failed to upload file")))
                .flatMap(jsonObject -> {
                    return userWebClient.getUserById(accessToken, userId)
                                    .flatMap(user -> {

                                        LOG.info("jsonObject: {}", jsonObject.toString());
                                        user.setProfilePhoto(jsonObject.toString());
                                        return Mono.just(user);
                                    });
                })
                .flatMap(user -> userWebClient.updateProfilePhoto(accessToken, user).zipWith(Mono.just(user)))
                .doOnNext(objects -> LOG.info("updated profilePhoto, server response: {}", objects.getT1()))
                .flatMap(objects -> userWebClient.getUserById(accessToken, objects.getT2().getId()))
                .doOnNext(user -> {
                            LOG.info("got user: {}", user);
                            String profilePhotoJson = user.getProfilePhoto();
                            if (user.getProfilePhoto() != null && !user.getProfilePhoto().isEmpty()) {
                                final String thumbnailUrl = getProfileUrl(profilePhotoJson);
                                LOG.info("set thumbnailUrl in profilePhoto: {}", thumbnailUrl);
                                user.setProfilePhoto(thumbnailUrl);
                            }
                            model.addAttribute("user", user);
                }).thenReturn(PATH);
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


        return userWebClient.updateProfile(accessToken, user)
                .switchIfEmpty(Mono.just("is empty"))
                .doOnNext(s -> LOG.info("updated profile, server response: {}", s))
                .flatMap(s -> userWebClient.getUserById(accessToken, user.getId()))
                .flatMap(user1 -> {
                    LOG.info("got user: {}", user1);
                    String profilePhotoJson = user1.getProfilePhoto();
                    if (user1.getProfilePhoto() != null && !user1.getProfilePhoto().isEmpty()) {
                        final String thumbnailUrl = getProfileUrl(profilePhotoJson);
                        LOG.info("set thumbnailUrl in profilePhoto: {}", thumbnailUrl);
                        user1.setProfilePhoto(thumbnailUrl);
                    }

                    model.addAttribute("user", user1);
                    return Mono.just(PATH);
                });
    }

    private User getObject(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        LOG.info("get object from jsonString: {}", jsonString);

        try {
            return objectMapper.readValue(jsonString, User.class);
        } catch (JsonProcessingException e) {
            LOG.error("failed to convert json to User class", e);
        }
        return null;
    }

    /**
     * This method will jsonObject contain json for uploaded file, thumbnail and full image key info.
     * To access the public-read, you need to attach the ${aws.s3.bucket} value. For example
     * this authzmanager has 'authzmanager' for the bucket property.  So the actual photo
     * will be accessible as $authzmanager/${photoFileKey} or in full form
     * ${aws.s3.subdomain}/${aws.s3.bucket}/photos/profile/thumbnail/2024-11-21T11:20:29.117461.jpeg
     * or this "https://spaces.yourdomain.com/authzmanager/photos/profile/thumbnail/2024-11-21T11:20:29.117461.jpeg"
     * @param file
     * @return JsonObject
     */
    public Mono<JsonObject> handleFileUpload(MultipartFile file) {
        LOG.info("handle file upload");

        if (file.isEmpty()) {
            LOG.info("there is no file to upload");
            return Mono.empty();
        }
        else {
            LOG.info("file original name to upload is '{}'", file.getOriginalFilename());
        }

        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(file.getBytes());
            Flux<ByteBuffer> byteBufferFlux = Flux.just(byteBuffer);
            LOG.info("file.getContentType: {}", file.getContentType());

            final MediaType mediaType = MediaType.valueOf(file.getContentType());
            LOG.info("mediaType: {}", mediaType);

            String fileFormat = ImageUtil.getFileFormat(mediaType, file.getOriginalFilename());
            LOG.info("fileFormat: {}", fileFormat);

            LocalDateTime localDateTime = LocalDateTime.now();

            final String prefixPath = s3ClientConfigurationProperties.getPhotoPath() + profilePhotoFolder;

            Dimension thumbnailDimension = new Dimension(s3ClientConfigurationProperties.getThumbnailSize().getWidth(),
                    s3ClientConfigurationProperties.getThumbnailSize().getHeight());

            return s3Service.uploadFile(byteBufferFlux, prefixPath, file.getOriginalFilename(), mediaType, file.getSize(),
                            ObjectCannedACL.PRIVATE, localDateTime)
                    .flatMap(fileKey -> {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("profilePhotoKey", fileKey);
                        jsonObject.addProperty("profilePhotoUrl",
                                s3ClientConfigurationProperties.getSubdomain()+s3ClientConfigurationProperties.getBucket()+fileKey);
                        jsonObject.addProperty("profilePhotoAcl", ObjectCannedACL.PRIVATE.toString());
                        return Mono.just(jsonObject);
                    })

                    //.doOnNext(fileKey -> LOG.info("fileKey: {}, photo upload done, creating photo thumbnail next.", fileKey))
                    .flatMap(jsonObject1 -> s3Service.createPresignedUrl(Mono.just(jsonObject1.get("profilePhotoKey").getAsString()))
                            .zipWith(Mono.just(jsonObject1)))
                    .doOnNext(objects -> LOG.info("presigned url: {}", objects.getT1()))
                    .flatMap(objects -> s3Service.createPhotoThumbnail(localDateTime, objects.getT1(), prefixPath,
                                    ObjectCannedACL.PUBLIC_READ, file.getName(), mediaType, thumbnailDimension)
                            .zipWith(Mono.just(objects.getT2())))
                    .doOnNext(objects -> LOG.info("Photo thumbnail done."))
                    .flatMap(objects -> {
                        JsonObject jsonObject1 = objects.getT2();
                        jsonObject1.addProperty("thumbnailUrl", s3ClientConfigurationProperties.getSubdomain()
                                + "/" + s3ClientConfigurationProperties.getBucket() + "/" + objects.getT1());
                        jsonObject1.addProperty("fileKey", objects.getT1());
                        jsonObject1.addProperty("thumbnailAcl", ObjectCannedACL.PUBLIC_READ.toString());
                        return Mono.just(jsonObject1);
                    });

        } catch (IOException e) {
          LOG.error("exception occurred", e);

          return Mono.empty();
        }

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
