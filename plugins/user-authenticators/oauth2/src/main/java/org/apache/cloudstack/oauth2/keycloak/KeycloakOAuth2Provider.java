package org.apache.cloudstack.oauth2.keycloak;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.AccessTokenResponse;

import javax.inject.Inject;
import java.util.List;

public class KeycloakOAuth2Provider extends AdapterBase implements UserOAuth2Authenticator {

    protected String accessToken = null;
    protected String refreshToken = null;

    @Inject
    OauthProviderDao _oauthProviderDao;

    @Override
    public String getName() {
        return "keycloak";
    }

    @Override
    public String getDescription() {
        return "Keycloak OAuth2 Provider Plugin";
    }

    @Override
    public boolean verifyUser(String email, String secretCode) {
        if (StringUtils.isAnyEmpty(email, secretCode)) {
            throw new CloudAuthenticationException("Either email or secret code should not be null/empty");
        }

        OauthProviderVO providerVO = _oauthProviderDao.findByProvider(getName());
        if (providerVO == null) {
            throw new CloudAuthenticationException("Keycloak provider is not registered, so user cannot be verified");
        }

        String verifiedEmail = verifyCodeAndFetchEmail(secretCode);
        if (verifiedEmail == null || !email.equals(verifiedEmail)) {
            throw new CloudRuntimeException("Unable to verify the email address with the provided secret");
        }
        clearAccessAndRefreshTokens();

        return true;
    }

    @Override
    public String verifyCodeAndFetchEmail(String secretCode) {
        OauthProviderVO keycloakProvider = _oauthProviderDao.findByProvider(getName());
        String clientId = keycloakProvider.getClientId();
        String clientSecret = keycloakProvider.getSecretKey();
        String redirectUri = keycloakProvider.getRedirectUri();
        String authServerUrl = keycloakProvider.getAuthenticationUri();

        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(keycloakProvider.getProviderName())
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .code(secretCode)
                .build();

        AccessTokenResponse tokenResponse = keycloak.tokenManager().getAccessToken();

        accessToken = tokenResponse.getToken();
        refreshToken = tokenResponse.getRefreshToken();

        List<UserRepresentation> users = keycloak.realm(keycloakProvider.getProviderName()).users().search("", 0, 1);
        if (users.isEmpty()) {
            throw new CloudRuntimeException("No user found with the provided secret");
        }

        return users.get(0).getEmail();
    }

    protected void clearAccessAndRefreshTokens() {
        accessToken = null;
        refreshToken = null;
    }

    @Override
    public String getUserEmailAddress() throws CloudRuntimeException {
        return null;
    }
}

