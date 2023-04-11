// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.api.auth.APIAuthenticator;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.api.response.LoginCmdResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.saml.SAML2AuthManager;
import org.apache.cloudstack.saml.SAMLPluginConstants;
import org.apache.cloudstack.saml.SAMLProviderMetadata;
import org.apache.cloudstack.saml.SAMLTokenVO;
import org.apache.cloudstack.saml.SAMLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.EncryptedAssertion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.encryption.ChainingEncryptedKeyResolver;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.validation.ValidationException;
import org.xml.sax.SAXException;

import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.Account;
import com.cloud.user.DomainManager;
import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.db.EntityManager;

@APICommand(name = "samlSso", description = "SP initiated SAML Single Sign On", requestHasSensitiveInfo = true, responseObject = LoginCmdResponse.class, entityType = {})
public class SAML2LoginAPIAuthenticatorCmd extends BaseCmd implements APIAuthenticator, Configurable {
    public static final Logger s_logger = Logger.getLogger(SAML2LoginAPIAuthenticatorCmd.class.getName());
    private static final String s_name = "loginresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.IDP_ID, type = CommandType.STRING, description = "Identity Provider Entity ID", required = true)
    private String idpId;

    @Inject
    ApiServerService apiServer;
    @Inject
    EntityManager entityMgr;
    @Inject
    DomainManager domainMgr;
    @Inject
    private UserAccountDao userAccountDao;

    protected static ConfigKey<String> saml2FailedLoginRedirectUrl = new ConfigKey<String>("Advanced", String.class, "saml2.failed.login.redirect.url", "",
            "The URL to redirect the SAML2 login failed message (the default vaulue is empty).", true);

    SAML2AuthManager samlAuthManager;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIdpId() {
        return idpId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.Type.NORMAL.ordinal();
    }

    @Override
    public void execute() throws ServerApiException {
        // We should never reach here
        throw new ServerApiException(ApiErrorCode.METHOD_NOT_ALLOWED, "This is an authentication api, cannot be used directly");
    }

    public Response processSAMLResponse(String responseMessage) {
        Response responseObject = null;
        try {
            DefaultBootstrap.bootstrap();
            responseObject = SAMLUtils.decodeSAMLResponse(responseMessage);

        } catch (ConfigurationException | FactoryConfigurationError | ParserConfigurationException | SAXException | IOException | UnmarshallingException e) {
            s_logger.error("SAMLResponse processing error: " + e.getMessage());
        }
        return responseObject;
    }

    @Override
    public String authenticate(final String command, final Map<String, Object[]> params, final HttpSession session, final InetAddress remoteAddress, final String responseType, final StringBuilder auditTrailSb, final HttpServletRequest req, final HttpServletResponse resp) throws ServerApiException {
        try {
            if (!params.containsKey(SAMLPluginConstants.SAML_RESPONSE) && !params.containsKey("SAMLart")) {
                String idpId = null;
                String domainPath = null;

                if (params.containsKey(ApiConstants.IDP_ID)) {
                    idpId = ((String[])params.get(ApiConstants.IDP_ID))[0];
                }

                if (params.containsKey(ApiConstants.DOMAIN)) {
                    domainPath = ((String[])params.get(ApiConstants.DOMAIN))[0];
                }

                if (domainPath != null && !domainPath.isEmpty()) {
                    if (!domainPath.startsWith("/")) {
                        domainPath = "/" + domainPath;
                    }
                    if (!domainPath.endsWith("/")) {
                        domainPath = domainPath + "/";
                    }
                }

                SAMLProviderMetadata spMetadata = samlAuthManager.getSPMetadata();
                SAMLProviderMetadata idpMetadata = samlAuthManager.getIdPMetadata(idpId);
                if (idpMetadata == null) {
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR, apiServer.getSerializedApiError(ApiErrorCode.PARAM_ERROR.getHttpCode(),
                            "IdP ID (" + idpId + ") is not found in our list of supported IdPs, cannot proceed.",
                            params, responseType));
                }
                if (idpMetadata.getSsoUrl() == null || idpMetadata.getSsoUrl().isEmpty()) {
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR, apiServer.getSerializedApiError(ApiErrorCode.PARAM_ERROR.getHttpCode(),
                            "IdP ID (" + idpId + ") has no Single Sign On URL defined please contact "
                                    + idpMetadata.getContactPersonName() + " <" + idpMetadata.getContactPersonEmail() + ">, cannot proceed.",
                            params, responseType));
                }
                String authnId = SAMLUtils.generateSecureRandomId();
                samlAuthManager.saveToken(authnId, domainPath, idpMetadata.getEntityId());
                s_logger.debug("Sending SAMLRequest id=" + authnId);
                String redirectUrl = SAMLUtils.buildAuthnRequestUrl(authnId, spMetadata, idpMetadata, SAML2AuthManager.SAMLSignatureAlgorithm.value());
                resp.sendRedirect(redirectUrl);
                return "";
            } if (params.containsKey("SAMLart")) {
                throw new ServerApiException(ApiErrorCode.UNSUPPORTED_ACTION_ERROR, apiServer.getSerializedApiError(ApiErrorCode.UNSUPPORTED_ACTION_ERROR.getHttpCode(),
                        "SAML2 HTTP Artifact Binding is not supported",
                        params, responseType));
            } else {
                final String samlResponse = ((String[])params.get(SAMLPluginConstants.SAML_RESPONSE))[0];
                Response processedSAMLResponse = this.processSAMLResponse(samlResponse);
                String statusCode = processedSAMLResponse.getStatus().getStatusCode().getValue();
                if (!statusCode.equals(StatusCode.SUCCESS_URI)) {
                    throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                            "Identity Provider send a non-successful authentication status code",
                            params, responseType));
                }

                String username = null;
                Issuer issuer = processedSAMLResponse.getIssuer();
                SAMLProviderMetadata spMetadata = samlAuthManager.getSPMetadata();
                SAMLProviderMetadata idpMetadata = samlAuthManager.getIdPMetadata(issuer.getValue());

                String responseToId = processedSAMLResponse.getInResponseTo();
                s_logger.debug("Received SAMLResponse in response to id=" + responseToId);
                SAMLTokenVO token = samlAuthManager.getToken(responseToId);
                if (token != null) {
                    if (!(token.getEntity().equalsIgnoreCase(issuer.getValue()))) {
                        throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                                "The SAML response contains Issuer Entity ID that is different from the original SAML request",
                                params, responseType));
                    }
                } else {
                    throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                            "Received SAML response for a SSO request that we may not have made or has expired, please try logging in again",
                            params, responseType));
                }

                // Set IdpId for this session
                session.setAttribute(SAMLPluginConstants.SAML_IDPID, issuer.getValue());

                Signature sig = processedSAMLResponse.getSignature();
                if (idpMetadata.getSigningCertificate() != null && sig != null) {
                    BasicX509Credential credential = new BasicX509Credential();
                    credential.setEntityCertificate(idpMetadata.getSigningCertificate());
                    SignatureValidator validator = new SignatureValidator(credential);
                    try {
                        validator.validate(sig);
                    } catch (ValidationException e) {
                        s_logger.error("SAML Response's signature failed to be validated by IDP signing key:" + e.getMessage());
                        throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                                "SAML Response's signature failed to be validated by IDP signing key",
                                params, responseType));
                    }
                }
                if (username == null) {
                    username = SAMLUtils.getValueFromAssertions(processedSAMLResponse.getAssertions(), SAML2AuthManager.SAMLUserAttributeName.value());
                }

                for (Assertion assertion: processedSAMLResponse.getAssertions()) {
                    if (assertion!= null && assertion.getSubject() != null && assertion.getSubject().getNameID() != null) {
                        session.setAttribute(SAMLPluginConstants.SAML_NAMEID, assertion.getSubject().getNameID().getValue());
                        break;
                    }
                }

                if (idpMetadata.getEncryptionCertificate() != null && spMetadata != null
                        && spMetadata.getKeyPair() != null && spMetadata.getKeyPair().getPrivate() != null) {
                    Credential credential = SecurityHelper.getSimpleCredential(idpMetadata.getEncryptionCertificate().getPublicKey(),
                            spMetadata.getKeyPair().getPrivate());
                    StaticKeyInfoCredentialResolver keyInfoResolver = new StaticKeyInfoCredentialResolver(credential);
                    ChainingEncryptedKeyResolver keyResolver = new ChainingEncryptedKeyResolver();
                    keyResolver.getResolverChain().add(new InlineEncryptedKeyResolver());
                    keyResolver.getResolverChain().add(new EncryptedElementTypeEncryptedKeyResolver());
                    Decrypter decrypter = new Decrypter(null, keyInfoResolver, keyResolver);
                    decrypter.setRootInNewDocument(true);
                    List<EncryptedAssertion> encryptedAssertions = processedSAMLResponse.getEncryptedAssertions();
                    if (encryptedAssertions != null) {
                        for (EncryptedAssertion encryptedAssertion : encryptedAssertions) {
                            Assertion assertion = null;
                            try {
                                assertion = decrypter.decrypt(encryptedAssertion);
                            } catch (DecryptionException e) {
                                s_logger.warn("SAML EncryptedAssertion error: " + e.toString());
                            }
                            if (assertion == null) {
                                continue;
                            }
                            Signature encSig = assertion.getSignature();
                            if (idpMetadata.getSigningCertificate() != null && encSig != null) {
                                BasicX509Credential sigCredential = new BasicX509Credential();
                                sigCredential.setEntityCertificate(idpMetadata.getSigningCertificate());
                                SignatureValidator validator = new SignatureValidator(sigCredential);
                                try {
                                    validator.validate(encSig);
                                } catch (ValidationException e) {
                                    s_logger.error("SAML Response's signature failed to be validated by IDP signing key:" + e.getMessage());
                                    throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                                            "SAML Response's signature failed to be validated by IDP signing key",
                                            params, responseType));
                                }
                            }
                            if (assertion.getSubject() != null && assertion.getSubject().getNameID() != null) {
                                session.setAttribute(SAMLPluginConstants.SAML_NAMEID, assertion.getSubject().getNameID().getValue());
                            }
                            if (username == null) {
                                username = SAMLUtils.getValueFromAttributeStatements(assertion.getAttributeStatements(), SAML2AuthManager.SAMLUserAttributeName.value());
                            }
                        }
                    }
                }

                if (username == null) {
                    throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                            "Failed to find admin configured username attribute in the SAML Response. Please ask your administrator to check SAML user attribute name.", params, responseType));
                }

                UserAccount userAccount = null;
                List<UserAccountVO> possibleUserAccounts = userAccountDao.getAllUsersByNameAndEntity(username, issuer.getValue());
                if (possibleUserAccounts != null && possibleUserAccounts.size() > 0) {
                    // Log into the first enabled user account
                    // Users can switch to other allowed accounts later
                    for (UserAccountVO possibleUserAccount : possibleUserAccounts) {
                        if (possibleUserAccount.getAccountState().equals(Account.State.ENABLED.toString())) {
                            userAccount = possibleUserAccount;
                            break;
                        }
                    }
                }

                whenFailToAuthenticateThrowExceptionOrRedirectToUrl(params, responseType, resp, issuer, userAccount);

                try {
                    if (apiServer.verifyUser(userAccount.getId())) {
                        LoginCmdResponse loginResponse = (LoginCmdResponse) apiServer.loginUser(session, userAccount.getUsername(), userAccount.getUsername() + userAccount.getSource().toString(),
                                userAccount.getDomainId(), null, remoteAddress, params);
                        SAMLUtils.setupSamlUserCookies(loginResponse, resp);
                        resp.sendRedirect(SAML2AuthManager.SAMLCloudStackRedirectionUrl.value());
                        return ApiResponseSerializer.toSerializedString(loginResponse, responseType);
                    }
                } catch (CloudAuthenticationException | IOException exception) {
                    s_logger.debug("SAML Login failed to log in the user due to: " + exception.getMessage());
                }
            }
        } catch (IOException e) {
            auditTrailSb.append("SP initiated SAML authentication using HTTP redirection failed:");
            auditTrailSb.append(e.getMessage());
        }
        throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                "Unable to authenticate user while performing SAML based SSO. Please make sure your user/account has been added, enable and authorized by the admin before you can authenticate. Please contact your administrator.",
                params, responseType));
    }

    /**
     * If it fails to authenticate the user, the method gets the value from configuration
     * Saml2FailedLoginRedirectUrl; if the user configured an error URL then it redirects to that
     * URL, otherwise it throws the ServerApiException
     */
    protected void whenFailToAuthenticateThrowExceptionOrRedirectToUrl(final Map<String, Object[]> params, final String responseType, final HttpServletResponse resp, Issuer issuer,
            UserAccount userAccount) throws IOException {
        if (userAccount == null || userAccount.getExternalEntity() == null || !samlAuthManager.isUserAuthorized(userAccount.getId(), issuer.getValue())) {
            String saml2RedirectUrl = saml2FailedLoginRedirectUrl.value();
            if (StringUtils.isBlank(saml2RedirectUrl)) {
                throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                        "Your authenticated user is not authorized for SAML Single Sign-On, please contact your administrator", params, responseType));
            } else {
                resp.sendRedirect(saml2RedirectUrl);
            }
        }
    }

    @Override
    public APIAuthenticationType getAPIType() {
        return APIAuthenticationType.LOGIN_API;
    }

    @Override
    public void setAuthenticators(List<PluggableAPIAuthenticator> authenticators) {
        for (PluggableAPIAuthenticator authManager: authenticators) {
            if (authManager != null && authManager instanceof SAML2AuthManager) {
                samlAuthManager = (SAML2AuthManager) authManager;
            }
        }
        if (samlAuthManager == null) {
            s_logger.error("No suitable Pluggable Authentication Manager found for SAML2 Login Cmd");
        }
    }

    @Override
    public String getConfigComponentName() {
        return SAML2LoginAPIAuthenticatorCmd.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {saml2FailedLoginRedirectUrl};
    }

}
