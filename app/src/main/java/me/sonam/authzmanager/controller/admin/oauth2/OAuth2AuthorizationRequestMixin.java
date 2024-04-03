package me.sonam.authzmanager.controller.admin.oauth2;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


/**
 * This mixin class is used to serialize/deserialize {@link OAuth2AuthorizationRequest}.
 * It also registers a custom deserializer {@link OAuth2AuthorizationRequestDeserializer}.
 *
 * @author Joe Grandja
 * @since 0.1.2
 * @see OAuth2AuthorizationRequest
 * @see OAuth2AuthorizationRequestDeserializer
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonDeserialize(using = OAuth2AuthorizationRequestDeserializer.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class OAuth2AuthorizationRequestMixin {

}
