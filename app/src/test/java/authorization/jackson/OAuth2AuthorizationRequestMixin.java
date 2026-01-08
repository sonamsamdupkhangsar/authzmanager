package authorization.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.annotation.JsonDeserialize;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * This mixin class is used to serialize/deserialize {@link org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest}.
 * It also registers a custom deserializer {@link OAuth2AuthorizationRequestDeserializer}.
 *
 * @author Joe Grandja
 * @since 7.0
 * @see OAuth2AuthorizationRequest
 * @see OAuth2AuthorizationRequestDeserializer
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonDeserialize(using = OAuth2AuthorizationRequestDeserializer.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
abstract class OAuth2AuthorizationRequestMixin {

}
