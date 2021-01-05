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

import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.Account;
import com.cloud.user.DomainManager;
import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.db.EntityManager;
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
import org.apache.cloudstack.saml.SAML2AuthManager;
import org.apache.cloudstack.saml.SAMLPluginConstants;
import org.apache.cloudstack.saml.SAMLProviderMetadata;
import org.apache.cloudstack.saml.SAMLTokenVO;
import org.apache.cloudstack.saml.SAMLUtils;
import org.apache.log4j.Logger;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.EncryptedAssertion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.encryption.EncryptedKeyResolver;
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

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

@APICommand(name = "samlSso", description = "SP initiated SAML Single Sign On", requestHasSensitiveInfo = true, responseObject = LoginCmdResponse.class, entityType = {})
public class SAML2LoginAPIAuthenticatorCmd extends BaseCmd implements APIAuthenticator {
    public static final Logger s_logger = Logger.getLogger(SAML2LoginAPIAuthenticatorCmd.class.getName());
    private static final String s_name = "loginresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.IDP_ID, type = CommandType.STRING, description = "Identity Provider Entity ID", required = true)
    private String idpId;

    @Parameter(name = ApiConstants.REDIRECT_ON_ERROR, type = CommandType.BOOLEAN, description = "If true, redirect to login page on login errors; defaults to false.  Provides error detail in a cookie named " + SAMLPluginConstants.SAML_LOGIN_MSG_COOKIE)
    private Boolean redirectOnError;

    @Inject
    ApiServerService apiServer;
    @Inject
    EntityManager entityMgr;
    @Inject
    DomainManager domainMgr;
    @Inject
    private UserAccountDao userAccountDao;

    SAML2AuthManager samlAuthManager;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIdpId() {
        return idpId;
    }

    public Boolean getRedirectOnError() {
        return redirectOnError == null ? false : redirectOnError;
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
        return Account.ACCOUNT_TYPE_NORMAL;
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
            if (params.containsKey(ApiConstants.REDIRECT_ON_ERROR)) {
                redirectOnError = Boolean.parseBoolean(((String[])params.get(ApiConstants.REDIRECT_ON_ERROR))[0]);
            }

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
                samlAuthManager.saveToken(authnId, domainPath, idpMetadata.getEntityId(), null, null);
                s_logger.debug("Sending SAMLRequest id=" + authnId);
                String redirectUrl = SAMLUtils.buildAuthnRequestUrl(authnId, spMetadata, idpMetadata, SAML2AuthManager.SAMLSignatureAlgorithm.value(), req, this.getRedirectOnError());
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
                String samlSessionIndex = null;
                Issuer issuer = processedSAMLResponse.getIssuer();
                SAMLProviderMetadata spMetadata = samlAuthManager.getSPMetadata();
                SAMLProviderMetadata idpMetadata = samlAuthManager.getIdPMetadata(issuer.getValue());

                String responseToId = processedSAMLResponse.getInResponseTo();
                s_logger.debug("Received SAMLResponse in response to id=" + responseToId);
                SAMLTokenVO token = samlAuthManager.getToken(responseToId);
                if (token != null) {
                    samlAuthManager.unauthorizeToken(token);
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
                    if (assertion != null && assertion.getSubject() != null && assertion.getSubject().getNameID() != null) {
                        session.setAttribute(SAMLPluginConstants.SAML_NAMEID, assertion.getSubject().getNameID().getValue());
                    }
                    if (assertion != null && samlSessionIndex == null) {
                        samlSessionIndex = SAMLUtils.getSessionIndexFromAssertion(assertion);
                    }
                }

                if (spMetadata != null && spMetadata.getKeyPair() != null && spMetadata.getKeyPair().getPrivate() != null) {
                    Credential credential = SecurityHelper.getSimpleCredential(spMetadata.getKeyPair().getPublic(), spMetadata.getKeyPair().getPrivate());
                    StaticKeyInfoCredentialResolver keyInfoResolver = new StaticKeyInfoCredentialResolver(credential);
                    EncryptedKeyResolver keyResolver = new InlineEncryptedKeyResolver();
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
                            if (samlSessionIndex == null) {
                                samlSessionIndex = SAMLUtils.getSessionIndexFromAssertion(assertion);
                            }
                        }
                    }
                }

                if (username == null) {
                    throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                            "Failed to find admin configured username attribute in the SAML Response. Please ask your administrator to check SAML user attribute name.", params, responseType));
                }

                if (samlSessionIndex == null) {
                    throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                            "Failed to find SessionIndex in SAML Response.", params, responseType));
                }

                UserAccount userAccount = null;
                List<UserAccountVO> possibleUserAccounts = userAccountDao.getAllUsersByNameAndEntity(username, issuer.getValue());
                if (possibleUserAccounts != null && possibleUserAccounts.size() > 0) {
                    // Log into the first enabled user account
                    // Users can switch to other allowed accounts later
                    for (UserAccountVO possibleUserAccount: possibleUserAccounts) {
                        if (possibleUserAccount.getAccountState().equals(Account.State.enabled.toString())) {
                            userAccount = possibleUserAccount;
                            break;
                        }
                    }
                }

                if (userAccount == null || userAccount.getExternalEntity() == null || !samlAuthManager.isUserAuthorized(userAccount.getId(), issuer.getValue())) {
                    throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                            "Your authenticated user is not authorized for SAML Single Sign-On, please contact your administrator",
                            params, responseType));
                }

                try {
                    if (apiServer.verifyUser(userAccount.getId())) {
                        LoginCmdResponse loginResponse = (LoginCmdResponse) apiServer.loginUser(session, userAccount.getUsername(), userAccount.getUsername() + userAccount.getSource().toString(),
                                userAccount.getDomainId(), null, remoteAddress, params);
                        SAMLUtils.setupSamlUserCookies(loginResponse, resp);
                        token.setSessionIndex(samlSessionIndex);
                        if(SAML2AuthManager.SAMLSupportHostnameAliases.value()) {
                            token.setSpBaseUrl(SAMLUtils.getBaseUrl(req));
                            s_logger.debug("Wrote SpBaseUrl to token table: " + token.getSpBaseUrl());
                        }
                        samlAuthManager.updateToken(token);
                        samlAuthManager.attachTokenToSession(session, token);
                        SAMLUtils.redirectToSAMLCloudStackRedirectionUrl(resp, req);
                        return ApiResponseSerializer.toSerializedString(loginResponse, responseType);
                    }
                } catch (CloudAuthenticationException | IOException exception) {
                    s_logger.debug("SAML Login failed to log in the user due to: " + exception.getMessage());
                }
            }
        } catch (IOException e) {
            auditTrailSb.append("SP initiated SAML authentication using HTTP redirection failed:");
            auditTrailSb.append(e.getMessage());
        } catch (ServerApiException e) {
            if (this.getRedirectOnError()) {
                try {
                    SAMLUtils.redirectToSAMLCloudStackRedirectionUrl(resp, req, SAMLUtils.getErrorTextFromXml(e.getDescription()));
                } catch (Exception e2) {
                    auditTrailSb.append("SAML HTTP redirection on error failed:");
                    auditTrailSb.append(e2.getMessage());
                    s_logger.error("SAML Unable to redirect on login error: " + e2.getMessage());
                }
            } else {
                throw(e);
            }
        }
        throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                "Unable to authenticate user while performing SAML based SSO. Please make sure your user/account has been added, enable and authorized by the admin before you can authenticate. Please contact your administrator.",
                params, responseType));
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
}