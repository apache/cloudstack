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

import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.LoginCmdResponse;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameIDPolicy;
import org.opensaml.saml2.core.RequestedAuthnContext;
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
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

    public String buildAuthnRequestUrl(String resourceUrl) {
        String randomId = new BigInteger(130, new SecureRandom()).toString(32);
        // TODO: Add method to get this url from metadata
        String identityProviderUrl = "https://idp.ssocircle.com:443/sso/SSORedirect/metaAlias/ssocircle";
        String encodedAuthRequest = "";

        try {
            DefaultBootstrap.bootstrap();
            AuthnRequest authnRequest = this.buildAuthnRequestObject(randomId, identityProviderUrl, resourceUrl); // SAML AuthRequest
            encodedAuthRequest = encodeAuthnRequest(authnRequest);
        } catch (ConfigurationException | FactoryConfigurationError | MarshallingException | IOException e) {
            s_logger.error("SAML AuthnRequest message building error: " + e.getMessage());
        }
        return identityProviderUrl + "?SAMLRequest=" + encodedAuthRequest; // + "&RelayState=" + relayState;
    }

    private AuthnRequest buildAuthnRequestObject(String authnId, String idpUrl, String consumerUrl) {
        // Issuer object
        IssuerBuilder issuerBuilder = new IssuerBuilder();
        Issuer issuer = issuerBuilder.buildObject();
        //SAMLConstants.SAML20_NS,
        //        "Issuer", "samlp");
        issuer.setValue("apache-cloudstack");

        // NameIDPolicy
        NameIDPolicyBuilder nameIdPolicyBuilder = new NameIDPolicyBuilder();
        NameIDPolicy nameIdPolicy = nameIdPolicyBuilder.buildObject();
        nameIdPolicy.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
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
                .setComparison(AuthnContextComparisonTypeEnumeration.EXACT);
        requestedAuthnContext.getAuthnContextClassRefs().add(
                authnContextClassRef);


        // Creation of AuthRequestObject
        AuthnRequestBuilder authRequestBuilder = new AuthnRequestBuilder();
        AuthnRequest authnRequest = authRequestBuilder.buildObject();
        //SAMLConstants.SAML20P_NS,
        //        "AuthnRequest", "samlp");
        authnRequest.setID(authnId);
        authnRequest.setDestination(idpUrl);
        authnRequest.setVersion(SAMLVersion.VERSION_20);
        authnRequest.setForceAuthn(true);
        authnRequest.setIsPassive(false);
        authnRequest.setIssuer(issuer);
        authnRequest.setIssueInstant(new DateTime());
        authnRequest.setProviderName("Apache CloudStack");
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        authnRequest.setAssertionConsumerServiceURL(consumerUrl);
        //authnRequest.setNameIDPolicy(nameIdPolicy);
        //authnRequest.setRequestedAuthnContext(requestedAuthnContext);

        return authnRequest;
    }

    private String encodeAuthnRequest(AuthnRequest authnRequest)
            throws MarshallingException, IOException {

        Marshaller marshaller = null;
        org.w3c.dom.Element authDOM = null;
        StringWriter requestWriter = null;
        String requestMessage = null;
        Deflater deflater = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        DeflaterOutputStream deflaterOutputStream = null;
        String encodedRequestMessage = null;

        marshaller = org.opensaml.Configuration.getMarshallerFactory()
                .getMarshaller(authnRequest); // object to DOM converter

        authDOM = marshaller.marshall(authnRequest); // converting to a DOM

        requestWriter = new StringWriter();
        XMLHelper.writeNode(authDOM, requestWriter);
        requestMessage = requestWriter.toString(); // DOM to string

        deflater = new Deflater(Deflater.DEFLATED, true);
        byteArrayOutputStream = new ByteArrayOutputStream();
        deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream,
                deflater);
        deflaterOutputStream.write(requestMessage.getBytes()); // compressing
        deflaterOutputStream.close();

        encodedRequestMessage = Base64.encodeBytes(byteArrayOutputStream
                .toByteArray(), Base64.DONT_BREAK_LINES);
        encodedRequestMessage = URLEncoder.encode(encodedRequestMessage,
                "UTF-8").trim(); // encoding string

        return encodedRequestMessage;
    }


    public String processResponseMessage(String responseMessage) {

        XMLObject responseObject = null;

        try {

            responseObject = this.unmarshall(responseMessage);

        } catch (ConfigurationException | ParserConfigurationException | SAXException | IOException | UnmarshallingException e) {
            e.printStackTrace();
        }

        return this.getResult(responseObject);
    }

    private XMLObject unmarshall(String responseMessage)
            throws ConfigurationException, ParserConfigurationException,
            SAXException, IOException, UnmarshallingException {

        DocumentBuilderFactory documentBuilderFactory = null;
        DocumentBuilder docBuilder = null;
        Document document = null;
        Element element = null;
        UnmarshallerFactory unmarshallerFactory = null;
        Unmarshaller unmarshaller = null;

        DefaultBootstrap.bootstrap();

        documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilderFactory.setNamespaceAware(true);

        docBuilder = documentBuilderFactory.newDocumentBuilder();

        document = docBuilder.parse(new ByteArrayInputStream(responseMessage
                .trim().getBytes())); // response to DOM

        element = document.getDocumentElement(); // the DOM element

        unmarshallerFactory = Configuration.getUnmarshallerFactory();

        unmarshaller = unmarshallerFactory.getUnmarshaller(element);

        return unmarshaller.unmarshall(element); // Response object

    }

    private String getResult(XMLObject responseObject) {

        Element ele = null;
        NodeList statusNodeList = null;
        Node statusNode = null;
        NamedNodeMap statusAttr = null;
        Node valueAtt = null;
        String statusValue = null;

        String[] word = null;
        String result = null;

        NodeList nameIDNodeList = null;
        Node nameIDNode = null;
        String nameID = null;

        // reading the Response Object
        ele = responseObject.getDOM();
        statusNodeList = ele.getElementsByTagName("samlp:StatusCode");
        statusNode = statusNodeList.item(0);
        statusAttr = statusNode.getAttributes();
        valueAtt = statusAttr.item(0);
        statusValue = valueAtt.getNodeValue();

        word = statusValue.split(":");
        result = word[word.length - 1];

        nameIDNodeList = ele.getElementsByTagNameNS(
                "urn:oasis:names:tc:SAML:2.0:assertion", "NameID");
        nameIDNode = nameIDNodeList.item(0);
        nameID = nameIDNode.getFirstChild().getNodeValue();

        result = nameID + ":" + result;

        return result;
    }



    @Override
    public String authenticate(String command, Map<String, Object[]> params, HttpSession session, String remoteAddress, String responseType, StringBuilder auditTrailSb, final HttpServletResponse resp) throws ServerApiException {
        String response = null;
        try {
            String redirectUrl = buildAuthnRequestUrl("http://localhost:8080/client/api?command=login");
            resp.sendRedirect(redirectUrl);

            //resp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            //resp.setHeader("Location", redirectUrl);

            // TODO: create and send assertion with the URL as GET params

        } catch (IOException e) {
            auditTrailSb.append("SP initiated SAML authentication using HTTP redirection failed:");
            auditTrailSb.append(e.getMessage());
        }
        return response;
    }

    @Override
    public APIAuthenticationType getAPIType() {
        return APIAuthenticationType.LOGIN_API;
    }
}
