package authorization.token;

import org.springframework.security.oauth2.core.OAuth2Token;

/**
 * The names of the "claims" that may be contained in an @link OAuth2TokenClaimsSet and
 * are associated to an {@link OAuth2Token}.
 *
 * @author Joe Grandja
 * @since 7.0
 * see OAuth2TokenClaimAccessor
 * see OAuth2TokenClaimsSet
 * @see OAuth2Token
 */
public final class OAuth2TokenClaimNames {

    /**
     * {@code iss} - the Issuer claim identifies the principal that issued the OAuth 2.0
     * Token
     */
    public static final String ISS = "iss";

    /**
     * {@code sub} - the Subject claim identifies the principal that is the subject of the
     * OAuth 2.0 Token
     */
    public static final String SUB = "sub";

    /**
     * {@code aud} - the Audience claim identifies the recipient(s) that the OAuth 2.0
     * Token is intended for
     */
    public static final String AUD = "aud";

    /**
     * {@code exp} - the Expiration time claim identifies the expiration time on or after
     * which the OAuth 2.0 Token MUST NOT be accepted for processing
     */
    public static final String EXP = "exp";

    /**
     * {@code nbf} - the Not Before claim identifies the time before which the OAuth 2.0
     * Token MUST NOT be accepted for processing
     */
    public static final String NBF = "nbf";

    /**
     * {@code iat} - The Issued at claim identifies the time at which the OAuth 2.0 Token
     * was issued
     */
    public static final String IAT = "iat";

    /**
     * {@code jti} - The ID claim provides a unique identifier for the OAuth 2.0 Token
     */
    public static final String JTI = "jti";

    private OAuth2TokenClaimNames() {
    }

}
