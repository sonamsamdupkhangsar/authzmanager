package me.sonam.authzmanager.oauth2;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

/**
 * This mixin class is used to serialize/deserialize {@link SignatureAlgorithm}.
 *
 * @author Joe Grandja
 * @since 0.1.2
 * @see SignatureAlgorithm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
abstract class JwsAlgorithmMixin {
}
