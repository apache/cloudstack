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
import com.cloud.user.User;
import com.cloud.utils.HttpUtils;
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
import org.apache.cloudstack.api.response.LoginCmdResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.saml.SAML2AuthManager;
import org.apache.cloudstack.utils.auth.SAMLUtils;
import org.apache.log4j.Logger;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.signature.Signature;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@APICommand(name = "samlsso", description = "SP initiated SAML Single Sign On", requestHasSensitiveInfo = true, responseObject = LoginCmdResponse.class, entityType = {})
public class SAML2LoginAPIAuthenticatorCmd extends BaseCmd implements APIAuthenticator {
    public static final Logger s_logger = Logger.getLogger(SAML2LoginAPIAuthenticatorCmd.class.getName());
    private static final String s_name = "loginresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.IDP_URL, type = CommandType.STRING, description = "Identity Provider SSO HTTP-Redirect binding URL", required = true)
    private String idpUrl;

    @Inject
    ApiServerService _apiServer;
    @Inject
    EntityManager _entityMgr;
    @Inject
    SAML2AuthManager _samlAuthManager;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIdpUrl() {
        return idpUrl;
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

    public String buildAuthnRequestUrl(String idpUrl) {
        String randomSecureId = new BigInteger(130, new SecureRandom()).toString(32);
        String spId = _samlAuthManager.getServiceProviderId();
        String consumerUrl = _samlAuthManager.getSpSingleSignOnUrl();
        String identityProviderUrl = _samlAuthManager.getIdpSingleSignOnUrl();

        if (idpUrl != null) {
            identityProviderUrl = idpUrl;
        }

        String redirectUrl = "";
        try {
            DefaultBootstrap.bootstrap();
            AuthnRequest authnRequest = SAMLUtils.buildAuthnRequestObject(randomSecureId, spId, identityProviderUrl, consumerUrl);
            redirectUrl = identityProviderUrl + "?SAMLRequest=" + SAMLUtils.encodeSAMLRequest(authnRequest);
        } catch (ConfigurationException | FactoryConfigurationError | MarshallingException | IOException e) {
            s_logger.error("SAML AuthnRequest message building error: " + e.getMessage());
        }
        return redirectUrl;
    }

    public Response processSAMLResponse(String responseMessage) {
        Response responseObject = null;
        try {
            responseObject = SAMLUtils.decodeSAMLResponse(responseMessage);

        } catch (ConfigurationException | ParserConfigurationException | SAXException | IOException | UnmarshallingException e) {
            s_logger.error("SAMLResponse processing error: " + e.getMessage());
        }
        return responseObject;
    }

    @Override
    public String authenticate(final String command, final Map<String, Object[]> params, final HttpSession session, final String remoteAddress, final String responseType, final StringBuilder auditTrailSb, final HttpServletResponse resp) throws ServerApiException {
        try {
            if (!params.containsKey("SAMLResponse")) {
                String idpUrl = null;
                final String[] idps = (String[])params.get(ApiConstants.IDP_URL);
                if (idps != null && idps.length > 0) {
                    idpUrl = idps[0];
                }
                String redirectUrl = buildAuthnRequestUrl(idpUrl);
                resp.sendRedirect(redirectUrl);
                return "";
            } else {
                final String samlResponse = ((String[])params.get("SAMLResponse"))[0];
                Response processedSAMLResponse = processSAMLResponse(samlResponse);
                String statusCode = processedSAMLResponse.getStatus().getStatusCode().getValue();
                if (!statusCode.equals(StatusCode.SUCCESS_URI)) {
                    throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, _apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                            "Identity Provider send a non-successful authentication status code",
                            params, responseType));
                }

                Signature sig = processedSAMLResponse.getSignature();
                //SignatureValidator validator = new SignatureValidator(credential);
                //validator.validate(sig);

                String uniqueUserId = null;
                String accountName = "admin"; //GET from config, try, fail
                Long domainId = 1L; // GET from config, try, fail
                String username = null;
                String password = "";
                String firstName = "";
                String lastName = "";
                String timeZone = "";
                String email = "";

                Assertion assertion = processedSAMLResponse.getAssertions().get(0);
                NameID nameId = assertion.getSubject().getNameID();

                if (nameId.getFormat().equals(NameIDType.PERSISTENT) || nameId.getFormat().equals(NameIDType.EMAIL)) {
                    username = nameId.getValue();
                    uniqueUserId = SAMLUtils.createSAMLId(username);
                    if (nameId.getFormat().equals(NameIDType.EMAIL)) {
                        email = username;
                    }
                }

                String issuer = assertion.getIssuer().getValue();
                String audience = assertion.getConditions().getAudienceRestrictions().get(0).getAudiences().get(0).getAudienceURI();
                AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);
                List<Attribute> attributes = attributeStatement.getAttributes();

                // Try capturing standard LDAP attributes
                for (Attribute attribute: attributes) {
                    String attributeName = attribute.getName();
                    String attributeValue = attribute.getAttributeValues().get(0).getDOM().getTextContent();
                    if (attributeName.equalsIgnoreCase("uid") && uniqueUserId == null) {
                        username = attributeValue;
                        uniqueUserId = SAMLUtils.createSAMLId(username);
                    } else if (attributeName.equalsIgnoreCase("givenName")) {
                        firstName = attributeValue;
                    } else if (attributeName.equalsIgnoreCase(("sn"))) {
                        lastName = attributeValue;
                    } else if (attributeName.equalsIgnoreCase("mail")) {
                        email = attributeValue;
                    }
                }

                User user = _entityMgr.findByUuid(User.class, uniqueUserId);
                if (user == null && uniqueUserId != null && username != null
                        && accountName != null && domainId != null) {
                    CallContext.current().setEventDetails("UserName: " + username + ", FirstName :" + password + ", LastName: " + lastName);
                    user = _accountService.createUser(username, password, firstName, lastName, email, timeZone, accountName, domainId, uniqueUserId);
                }

                if (user != null) {
                    try {
                        if (_apiServer.verifyUser(user.getId())) {
                            LoginCmdResponse loginResponse = (LoginCmdResponse) _apiServer.loginUser(session, username, user.getPassword(), domainId, null, remoteAddress, params);
                            resp.addCookie(new Cookie("userid", loginResponse.getUserId()));
                            resp.addCookie(new Cookie("domainid", loginResponse.getDomainId()));
                            resp.addCookie(new Cookie("role", loginResponse.getType()));
                            resp.addCookie(new Cookie("username", URLEncoder.encode(loginResponse.getUsername(), HttpUtils.UTF_8)));
                            resp.addCookie(new Cookie("sessionKey", URLEncoder.encode(loginResponse.getSessionKey(), HttpUtils.UTF_8)));
                            resp.addCookie(new Cookie("account", URLEncoder.encode(loginResponse.getAccount(), HttpUtils.UTF_8)));
                            resp.addCookie(new Cookie("timezone", URLEncoder.encode(loginResponse.getTimeZone(), HttpUtils.UTF_8)));
                            resp.addCookie(new Cookie("userfullname", loginResponse.getFirstName() + "%20" + loginResponse.getLastName()));
                            resp.sendRedirect("http://localhost:8080/client");
                            return ApiResponseSerializer.toSerializedString(loginResponse, responseType);

                        }
                    } catch (final CloudAuthenticationException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            auditTrailSb.append("SP initiated SAML authentication using HTTP redirection failed:");
            auditTrailSb.append(e.getMessage());
        }
        throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, _apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                "Unable to authenticate or retrieve user while performing SAML based SSO",
                params, responseType));
    }

    @Override
    public APIAuthenticationType getAPIType() {
        return APIAuthenticationType.LOGIN_API;
    }
}
