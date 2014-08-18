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

package com.cloud.api.auth;

import com.cloud.api.ApiServerService;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.HttpUtils;
import com.cloud.utils.db.EntityManager;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.LoginCmdResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.NameIDPolicy;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.NameIDPolicyBuilder;
import org.opensaml.saml2.core.impl.RequestedAuthnContextBuilder;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

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

    public String buildAuthnRequestUrl(String consumerUrl, String identityProviderUrl) {
        String randomId = new BigInteger(130, new SecureRandom()).toString(32);
        String spId = "org.apache.cloudstack";
        String redirectUrl = "";
        try {
            DefaultBootstrap.bootstrap();
            AuthnRequest authnRequest = this.buildAuthnRequestObject(randomId, spId, identityProviderUrl, consumerUrl);
            redirectUrl = identityProviderUrl + "?SAMLRequest=" + encodeAuthnRequest(authnRequest);
        } catch (ConfigurationException | FactoryConfigurationError | MarshallingException | IOException e) {
            s_logger.error("SAML AuthnRequest message building error: " + e.getMessage());
        }
        return redirectUrl;
    }

    private AuthnRequest buildAuthnRequestObject(String authnId, String spId, String idpUrl, String consumerUrl) {
        // Issuer object
        IssuerBuilder issuerBuilder = new IssuerBuilder();
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(spId);

        // NameIDPolicy
        NameIDPolicyBuilder nameIdPolicyBuilder = new NameIDPolicyBuilder();
        NameIDPolicy nameIdPolicy = nameIdPolicyBuilder.buildObject();
        nameIdPolicy.setFormat(NameIDType.PERSISTENT);
        nameIdPolicy.setSPNameQualifier("Apache CloudStack");
        nameIdPolicy.setAllowCreate(true);

        // AuthnContextClass
        AuthnContextClassRefBuilder authnContextClassRefBuilder = new AuthnContextClassRefBuilder();
        AuthnContextClassRef authnContextClassRef = authnContextClassRefBuilder.buildObject(
                SAMLConstants.SAML20_NS,
                "AuthnContextClassRef", "saml");
        authnContextClassRef.setAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");

        // AuthnContex
        RequestedAuthnContextBuilder requestedAuthnContextBuilder = new RequestedAuthnContextBuilder();
        RequestedAuthnContext requestedAuthnContext = requestedAuthnContextBuilder.buildObject();
        requestedAuthnContext
                .setComparison(AuthnContextComparisonTypeEnumeration.MINIMUM);
        requestedAuthnContext.getAuthnContextClassRefs().add(
                authnContextClassRef);

        // Creation of AuthRequestObject
        AuthnRequestBuilder authRequestBuilder = new AuthnRequestBuilder();
        AuthnRequest authnRequest = authRequestBuilder.buildObject();
        authnRequest.setID(authnId);
        authnRequest.setDestination(idpUrl);
        authnRequest.setVersion(SAMLVersion.VERSION_20);
        authnRequest.setForceAuthn(true);
        authnRequest.setIsPassive(false);
        authnRequest.setIssuer(issuer);
        authnRequest.setIssueInstant(new DateTime());
        authnRequest.setProviderName("Apache CloudStack");
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI); //SAML2_ARTIFACT_BINDING_URI);
        authnRequest.setAssertionConsumerServiceURL(consumerUrl);
        authnRequest.setNameIDPolicy(nameIdPolicy);
        authnRequest.setRequestedAuthnContext(requestedAuthnContext);

        return authnRequest;
    }

    private String encodeAuthnRequest(AuthnRequest authnRequest)
            throws MarshallingException, IOException {
        Marshaller marshaller = Configuration.getMarshallerFactory()
                .getMarshaller(authnRequest);
        Element authDOM = marshaller.marshall(authnRequest);
        StringWriter requestWriter = new StringWriter();
        XMLHelper.writeNode(authDOM, requestWriter);
        String requestMessage = requestWriter.toString();
        Deflater deflater = new Deflater(Deflater.DEFLATED, true);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream, deflater);
        deflaterOutputStream.write(requestMessage.getBytes());
        deflaterOutputStream.close();
        String encodedRequestMessage = Base64.encodeBytes(byteArrayOutputStream.toByteArray(), Base64.DONT_BREAK_LINES);
        encodedRequestMessage = URLEncoder.encode(encodedRequestMessage, "UTF-8").trim();
        return encodedRequestMessage;
    }

    public Response processSAMLResponse(String responseMessage) {
        XMLObject responseObject = null;
        try {
            responseObject = this.unmarshall(responseMessage);

        } catch (ConfigurationException | ParserConfigurationException | SAXException | IOException | UnmarshallingException e) {
            s_logger.error("SAMLResponse processing error: " + e.getMessage());
        }
        return (Response) responseObject;
    }

    private XMLObject unmarshall(String responseMessage)
            throws ConfigurationException, ParserConfigurationException,
            SAXException, IOException, UnmarshallingException {
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException | FactoryConfigurationError e) {
            s_logger.error("SAML response message decoding error: " + e.getMessage());
        }
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
        byte[] base64DecodedResponse = Base64.decode(responseMessage);
        Document document = docBuilder.parse(new ByteArrayInputStream(base64DecodedResponse));
        Element element = document.getDocumentElement();
        UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        return unmarshaller.unmarshall(element);
    }

    @Override
    public String authenticate(final String command, final Map<String, Object[]> params, final HttpSession session, final String remoteAddress, final String responseType, final StringBuilder auditTrailSb, final HttpServletResponse resp) throws ServerApiException {
        try {
            if (!params.containsKey("SAMLResponse")) {
                final String[] idps = (String[])params.get("idpurl");
                String redirectUrl = buildAuthnRequestUrl("http://localhost:8080/client/api?command=samlsso", idps[0]);
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
                    uniqueUserId = "saml-" + username;
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
                        uniqueUserId = "saml-" + username;
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
