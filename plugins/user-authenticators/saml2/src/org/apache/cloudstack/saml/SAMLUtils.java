//
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
//

package org.apache.cloudstack.saml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.LoginCmdResponse;
import org.apache.cloudstack.utils.security.CertUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AuthnContext;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusMessage;
import org.opensaml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.LogoutRequestBuilder;
import org.opensaml.saml2.core.impl.LogoutResponseBuilder;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml2.core.impl.RequestedAuthnContextBuilder;
import org.opensaml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml2.core.impl.StatusMessageBuilder;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.cloud.utils.HttpUtils;
import com.google.common.base.Strings;

public class SAMLUtils {
    public static final Logger s_logger = Logger.getLogger(SAMLUtils.class);

    public static String generateSecureRandomId() {
        return new BigInteger(160, new SecureRandom()).toString(32);
    }

    public static String getValueFromAttributeStatements(final List<AttributeStatement> attributeStatements, final String attributeKey) {
        if (attributeStatements == null || attributeStatements.size() < 1 || attributeKey == null) {
            return null;
        }
        for (AttributeStatement attributeStatement : attributeStatements) {
            if (attributeStatement == null || attributeStatements.size() < 1) {
                continue;
            }
            for (Attribute attribute : attributeStatement.getAttributes()) {
                if (attribute.getAttributeValues() != null && attribute.getAttributeValues().size() > 0) {
                    String value = attribute.getAttributeValues().get(0).getDOM().getTextContent();
                    s_logger.debug("SAML attribute name: " + attribute.getName() + " friendly-name:" + attribute.getFriendlyName() + " value:" + value);
                    if (attributeKey.equals(attribute.getName()) || attributeKey.equals(attribute.getFriendlyName())) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    public static String getValueFromAssertions(final List<Assertion> assertions, final String attributeKey) {
        if (assertions == null || attributeKey == null) {
            return null;
        }
        for (Assertion assertion : assertions) {
            String value = getValueFromAttributeStatements(assertion.getAttributeStatements(), attributeKey);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static String getSessionIndexFromAssertion(final Assertion assertion) {
        String sessionIndex = null;

        if (assertion == null) {
            return sessionIndex;
        }

        final List<AuthnStatement> authnStatements = assertion.getAuthnStatements();
        for (AuthnStatement authnStatement : authnStatements) {
            sessionIndex = authnStatement.getSessionIndex();
            if (sessionIndex != null) {
                break;
            }
        }

        return sessionIndex;
    }

    public static String buildAuthnRequestUrl(final String authnId, final SAMLProviderMetadata spMetadata, final SAMLProviderMetadata idpMetadata, final String signatureAlgorithm, final HttpServletRequest req, final Boolean redirectOnError) {
        String redirectUrl = "";
        try {
            DefaultBootstrap.bootstrap();
            s_logger.debug("SAML Hostname Alias support is: " + SAML2AuthManager.SAMLSupportHostnameAliases.value());
            String spSsoUrl = (SAML2AuthManager.SAMLSupportHostnameAliases.value()) ? replaceBaseUrl(spMetadata.getSsoUrl(), getBaseUrl(req)) : spMetadata.getSsoUrl();
            if (redirectOnError != null && redirectOnError) {
                spSsoUrl += "&" + ApiConstants.REDIRECT_ON_ERROR + "=true";
            }
            s_logger.debug("SAML SP SSO Url: " + spSsoUrl);
            AuthnRequest authnRequest = SAMLUtils.buildAuthnRequestObject(authnId, spMetadata.getEntityId(), idpMetadata.getSsoUrl(), spSsoUrl);
            PrivateKey privateKey = null;
            if (spMetadata.getKeyPair() != null) {
                privateKey = spMetadata.getKeyPair().getPrivate();
            }
            redirectUrl = idpMetadata.getSsoUrl() + "?" + SAMLUtils.generateSAMLRequestSignature("SAMLRequest=" + SAMLUtils.encodeSAMLRequest(authnRequest), privateKey, signatureAlgorithm);
        } catch (ConfigurationException | FactoryConfigurationError | MarshallingException | IOException | NoSuchAlgorithmException | InvalidKeyException | java.security.SignatureException e) {
            s_logger.error("SAML AuthnRequest message building error: " + e.getMessage());
        }
        return redirectUrl;
    }

    public static AuthnRequest buildAuthnRequestObject(final String authnId, final String spId, final String idpUrl, final String consumerUrl) {
        // Issuer object
        IssuerBuilder issuerBuilder = new IssuerBuilder();
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(spId);

        // AuthnContextClass
        AuthnContextClassRefBuilder authnContextClassRefBuilder = new AuthnContextClassRefBuilder();
        AuthnContextClassRef authnContextClassRef = authnContextClassRefBuilder.buildObject(
                SAMLConstants.SAML20_NS,
                "AuthnContextClassRef", "saml");
        authnContextClassRef.setAuthnContextClassRef(AuthnContext.PPT_AUTHN_CTX);

        // AuthnContext
        RequestedAuthnContextBuilder requestedAuthnContextBuilder = new RequestedAuthnContextBuilder();
        RequestedAuthnContext requestedAuthnContext = requestedAuthnContextBuilder.buildObject();
        requestedAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.EXACT);
        requestedAuthnContext.getAuthnContextClassRefs().add(authnContextClassRef);

        // Creation of AuthRequestObject
        AuthnRequestBuilder authRequestBuilder = new AuthnRequestBuilder();
        AuthnRequest authnRequest = authRequestBuilder.buildObject();
        authnRequest.setID(authnId);
        authnRequest.setDestination(idpUrl);
        authnRequest.setVersion(SAMLVersion.VERSION_20);
        authnRequest.setForceAuthn(false);
        authnRequest.setIsPassive(false);
        authnRequest.setIssueInstant(new DateTime());
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        authnRequest.setAssertionConsumerServiceURL(consumerUrl);
        authnRequest.setProviderName(spId);
        authnRequest.setIssuer(issuer);
        authnRequest.setRequestedAuthnContext(requestedAuthnContext);

        return authnRequest;
    }


    public static String buildLogoutRequestUrl(final String nameId, final SAMLProviderMetadata spMetadata, final SAMLProviderMetadata idpMetadata, final String signatureAlgorithm) {
        String redirectUrl = "";
        try {
            DefaultBootstrap.bootstrap();
            LogoutRequest logoutRequest = SAMLUtils.buildLogoutRequestObject(idpMetadata.getSloUrl(), spMetadata.getEntityId(), nameId);
            PrivateKey privateKey = null;
            if (spMetadata.getKeyPair() != null) {
                privateKey = spMetadata.getKeyPair().getPrivate();
            }
            redirectUrl = idpMetadata.getSloUrl() + "?" + SAMLUtils.generateSAMLRequestSignature("SAMLRequest=" + SAMLUtils.encodeSAMLRequest(logoutRequest), privateKey, signatureAlgorithm);
        } catch (ConfigurationException | FactoryConfigurationError | MarshallingException | IOException | NoSuchAlgorithmException | InvalidKeyException | java.security.SignatureException e) {
            s_logger.error("SAML LogoutRequest message building error: " + e.getMessage());
        }

        return redirectUrl;
    }

    public static LogoutRequest buildLogoutRequestObject(String logoutUrl, String spId, String nameIdString) {
        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue(spId);
        NameID nameID = new NameIDBuilder().buildObject();
        nameID.setValue(nameIdString);
        LogoutRequest logoutRequest = new LogoutRequestBuilder().buildObject();
        logoutRequest.setID(generateSecureRandomId());
        logoutRequest.setDestination(logoutUrl);
        logoutRequest.setVersion(SAMLVersion.VERSION_20);
        logoutRequest.setIssueInstant(new DateTime());
        logoutRequest.setIssuer(issuer);
        logoutRequest.setNameID(nameID);

        return logoutRequest;
    }

    public static String buildLogoutResponseUrl(final String requestId, final SAMLProviderMetadata spMetadata, final SAMLProviderMetadata idpMetadata, final String status, final String statusMsg, final String signatureAlgorithm) {
        String responseUrl = "";
        try {
            DefaultBootstrap.bootstrap();
            LogoutResponse logoutResponse = SAMLUtils.buildLogoutResponseObject(idpMetadata.getSloUrl(), spMetadata.getEntityId(), requestId, status, statusMsg);
            PrivateKey privateKey = null;
            if (spMetadata.getKeyPair() != null) {
                privateKey = spMetadata.getKeyPair().getPrivate();
            }
            responseUrl = idpMetadata.getSloUrl() + "?" + SAMLUtils.generateSAMLRequestSignature("SAMLResponse=" + SAMLUtils.encodeSAMLRequest(logoutResponse), privateKey, signatureAlgorithm);
        } catch (ConfigurationException | FactoryConfigurationError | MarshallingException | IOException | NoSuchAlgorithmException | InvalidKeyException | java.security.SignatureException e) {
            s_logger.error("SAML LogoutResponse message building error: " + e.getMessage());
        }

        return responseUrl;
    }

    public static LogoutResponse buildLogoutResponseObject(final String idpUrl, final String spId, final String requestId, final String status, final String statusMsg) {
        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue(spId);
        LogoutResponse logoutResponse = new LogoutResponseBuilder().buildObject();
        logoutResponse.setID(generateSecureRandomId());
        logoutResponse.setDestination(idpUrl);
        logoutResponse.setInResponseTo(requestId);
        logoutResponse.setStatus(buildStatus(status, statusMsg));
        logoutResponse.setIssuer(issuer);
        logoutResponse.setIssueInstant(new DateTime());

        return logoutResponse;
    }

    public static boolean redirectToSloUrlViaPost(final HttpServletResponse resp, final String postUrl, final String currentUrl, final String samlRequest, final int attempt) {
        try {
            final String postRedirect = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
                    + "<html><head><meta name=\"robots\" content=\"noindex, nofollow\"><title>Logging Out</title></head>"
                    + "<body><p>Redirecting, please wait.</p>"
                    + "<script>window.onload = function() {document.forms[0].submit()};</script>"
                    + "<form name=\"saml-post-binding\" method=\"post\" action=\"" + postUrl + "\">"
                    + "<input type=\"hidden\" name=\"SAMLRequest\" value=\"" + samlRequest + "\"/>"
                    + "<input type=\"hidden\" name=\"prevUrl\" value=\"" + currentUrl + "\"/>"
                    + "<input type=\"hidden\" name=\"attempt\" value=\"" + attempt + "\"/>"
                    + "<noscript>"
                    + "<p>JavaScript is disabled. We strongly recommend to enable it. Click the button below to continue.</p>"
                    + "</noscript></form></body></html>";
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/html");
            resp.setContentLength(postRedirect.length());
            resp.setDateHeader("Date", new Date().getTime());
            resp.setHeader("Cache-Control", "no-cache");
            PrintWriter writer = resp.getWriter();
            writer.println(postRedirect);
            resp.flushBuffer();
        } catch (IOException e) {
            s_logger.error("Exception sending POST redirect to user's SAML Slo URL.");
            return false;
        }
        return true;
    }

    public static void redirectToSAMLCloudStackRedirectionUrl(final HttpServletResponse resp, final HttpServletRequest req) throws IOException {
        final String redirectUrl = (SAML2AuthManager.SAMLSupportHostnameAliases.value()) ?
                                SAMLUtils.replaceBaseUrl(SAML2AuthManager.SAMLCloudStackRedirectionUrl.value(), SAMLUtils.getBaseUrl(req))
                                : SAML2AuthManager.SAMLCloudStackRedirectionUrl.value();
        try {
            s_logger.debug("SAML Redirecting to " + redirectUrl);
            resp.sendRedirect(redirectUrl);
        } catch (IOException exception) {
            s_logger.debug("SAML failed to redirect to " + redirectUrl + " due to exception: " + exception.getMessage());
            throw exception;
        }
    }

    public static void redirectToSAMLCloudStackRedirectionUrl(final HttpServletResponse resp, final HttpServletRequest req, final String msg) throws IOException {
        try {
            resp.addCookie(new Cookie(SAMLPluginConstants.SAML_LOGIN_MSG_COOKIE, URLEncoder.encode(msg, HttpUtils.UTF_8).replace("+", "%20")));
            redirectToSAMLCloudStackRedirectionUrl(resp, req);
        } catch (IOException exception) {
            throw exception;
        }
    }
    private static Status buildStatus(final String statusUri, final String statusMsg) {
        Status status = new StatusBuilder().buildObject();
        StatusCode statusCode = new StatusCodeBuilder().buildObject();
        statusCode.setValue(statusUri);
        status.setStatusCode(statusCode);

        if (!Strings.isNullOrEmpty(statusMsg)) {
            StatusMessage statusMessage = new StatusMessageBuilder().buildObject();
            statusMessage.setMessage(statusMsg);
            status.setStatusMessage(statusMessage);
        }

        return status;
    }

    public static String encodeSAMLRequest(XMLObject authnRequest)
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
        deflaterOutputStream.write(requestMessage.getBytes(Charset.forName("UTF-8")));
        deflaterOutputStream.close();
        String encodedRequestMessage = Base64.encodeBytes(byteArrayOutputStream.toByteArray(), Base64.DONT_BREAK_LINES);
        encodedRequestMessage = URLEncoder.encode(encodedRequestMessage, HttpUtils.UTF_8).trim();
        return encodedRequestMessage;
    }

    public static Response decodeSAMLResponse(String responseMessage)
            throws ConfigurationException, ParserConfigurationException,
            SAXException, IOException, UnmarshallingException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
        byte[] base64DecodedResponse = Base64.decode(responseMessage);
        Document document = docBuilder.parse(new ByteArrayInputStream(base64DecodedResponse));
        Element element = document.getDocumentElement();
        UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        return (Response) unmarshaller.unmarshall(element);
    }

    public static LogoutRequest decodeSAMLLogoutRequest(String requestMessage)
            throws ConfigurationException, ParserConfigurationException,
            SAXException, IOException, UnmarshallingException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
        byte[] base64DecodedRequest = Base64.decode(requestMessage);
        Document document = docBuilder.parse(new ByteArrayInputStream(base64DecodedRequest));
        Element element = document.getDocumentElement();
        UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        return (LogoutRequest) unmarshaller.unmarshall(element);
    }

    public static String generateSAMLRequestSignature(final String urlEncodedString, final PrivateKey signingKey, final String sigAlgorithmName)
            throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, UnsupportedEncodingException {
        if (signingKey == null) {
            return urlEncodedString;
        }

        String opensamlAlgoIdSignature = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1;
        String javaSignatureAlgorithmName = "SHA1withRSA";

        if (sigAlgorithmName.equalsIgnoreCase("SHA256")) {
            opensamlAlgoIdSignature = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256;
            javaSignatureAlgorithmName = "SHA256withRSA";
        } else if (sigAlgorithmName.equalsIgnoreCase("SHA384")) {
            opensamlAlgoIdSignature = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA384;
            javaSignatureAlgorithmName = "SHA384withRSA";
        } else if (sigAlgorithmName.equalsIgnoreCase("SHA512")) {
            opensamlAlgoIdSignature = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512;
            javaSignatureAlgorithmName = "SHA512withRSA";
        }

        String url = urlEncodedString + "&SigAlg=" + URLEncoder.encode(opensamlAlgoIdSignature, HttpUtils.UTF_8);
        Signature signature = Signature.getInstance(javaSignatureAlgorithmName);
        signature.initSign(signingKey);
        signature.update(url.getBytes(Charset.forName("UTF-8")));
        String signatureString = Base64.encodeBytes(signature.sign(), Base64.DONT_BREAK_LINES);
        if (signatureString != null) {
            return url + "&Signature=" + URLEncoder.encode(signatureString, HttpUtils.UTF_8);
        }
        return url;
    }

    public static void setupSamlUserCookies(final LoginCmdResponse loginResponse, final HttpServletResponse resp) throws IOException {
        resp.addCookie(new Cookie("userid", URLEncoder.encode(loginResponse.getUserId(), HttpUtils.UTF_8)));
        resp.addCookie(new Cookie("domainid", URLEncoder.encode(loginResponse.getDomainId(), HttpUtils.UTF_8)));
        resp.addCookie(new Cookie("role", URLEncoder.encode(loginResponse.getType(), HttpUtils.UTF_8)));
        resp.addCookie(new Cookie("username", URLEncoder.encode(loginResponse.getUsername(), HttpUtils.UTF_8)));
        resp.addCookie(new Cookie("account", URLEncoder.encode(loginResponse.getAccount(), HttpUtils.UTF_8).replace("+", "%20")));
        String timezone = loginResponse.getTimeZone();
        if (timezone != null) {
            resp.addCookie(new Cookie("timezone", URLEncoder.encode(timezone, HttpUtils.UTF_8)));
        }
        resp.addCookie(new Cookie("userfullname", URLEncoder.encode(loginResponse.getFirstName() + " " + loginResponse.getLastName(), HttpUtils.UTF_8).replace("+", "%20")));
        resp.addHeader("SET-COOKIE", String.format("%s=%s;HttpOnly", ApiConstants.SESSIONKEY, loginResponse.getSessionKey()));
    }

    /**
     * Returns base64 encoded PublicKey
     * @param key PublicKey
     * @return public key encoded string
     */
    public static String encodePublicKey(PublicKey key) {
        try {
            KeyFactory keyFactory = CertUtils.getKeyFactory();
            if (keyFactory == null) return null;
            X509EncodedKeySpec spec = keyFactory.getKeySpec(key, X509EncodedKeySpec.class);
            return new String(org.bouncycastle.util.encoders.Base64.encode(spec.getEncoded()), Charset.forName("UTF-8"));
        } catch (InvalidKeySpecException e) {
            s_logger.error("Unable to create KeyFactory:" + e.getMessage());
        }
        return null;
    }

    /**
     * Returns base64 encoded PrivateKey
     * @param key PrivateKey
     * @return privatekey encoded string
     */
    public static String encodePrivateKey(PrivateKey key) {
        try {
            KeyFactory keyFactory = CertUtils.getKeyFactory();
            if (keyFactory == null) return null;
            PKCS8EncodedKeySpec spec = keyFactory.getKeySpec(key,
                    PKCS8EncodedKeySpec.class);
            return new String(org.bouncycastle.util.encoders.Base64.encode(spec.getEncoded()), Charset.forName("UTF-8"));
        } catch (InvalidKeySpecException e) {
            s_logger.error("Unable to create KeyFactory:" + e.getMessage());
        }
        return null;
    }

    /**
     * Decodes base64 encoded public key to PublicKey
     * @param publicKey encoded public key string
     * @return returns PublicKey
     */
    public static PublicKey decodePublicKey(String publicKey) {
        byte[] sigBytes = org.bouncycastle.util.encoders.Base64.decode(publicKey);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(sigBytes);
        KeyFactory keyFactory = CertUtils.getKeyFactory();
        if (keyFactory == null)
            return null;
        try {
            return keyFactory.generatePublic(x509KeySpec);
        } catch (InvalidKeySpecException e) {
            s_logger.error("Unable to create PrivateKey from privateKey string:" + e.getMessage());
        }
        return null;
    }

    /**
     * Decodes base64 encoded private key to PrivateKey
     * @param privateKey encoded private key string
     * @return returns PrivateKey
     */
    public static PrivateKey decodePrivateKey(String privateKey) {
        byte[] sigBytes = org.bouncycastle.util.encoders.Base64.decode(privateKey);
        PKCS8EncodedKeySpec pkscs8KeySpec = new PKCS8EncodedKeySpec(sigBytes);
        KeyFactory keyFactory = CertUtils.getKeyFactory();
        if (keyFactory == null)
            return null;
        try {
            return keyFactory.generatePrivate(pkscs8KeySpec);
        } catch (InvalidKeySpecException e) {
            s_logger.error("Unable to create PrivateKey from privateKey string:" + e.getMessage());
        }
        return null;
    }

    public static X509Certificate generateRandomX509Certificate(KeyPair keyPair) throws NoSuchAlgorithmException, NoSuchProviderException, CertificateException, SignatureException, InvalidKeyException, OperatorCreationException {
        return CertUtils.generateV1Certificate(keyPair,
                "CN=ApacheCloudStack", "CN=ApacheCloudStack",
                3, "SHA256WithRSA");
    }

    public static String getBaseUrl(final HttpServletRequest req) {
        String baseUrl = null;
        String scheme = null;
        Integer port = null;

        try {
            scheme = (!Strings.isNullOrEmpty(req.getHeader("X-Forwarded-Proto"))) ? req.getHeader("X-Forwarded-Proto") : req.getScheme();
            port = (!Strings.isNullOrEmpty(req.getHeader("X-Forwarded-Port"))) ? Integer.parseInt(req.getHeader("X-Forwarded-Port")) : req.getServerPort();
        } catch (final Exception ex) {
            s_logger.error("SAML Exception determining URL scheme or port.  Defaulting to request variables.", ex);
            scheme = (!Strings.isNullOrEmpty(scheme)) ? scheme : req.getScheme();
            port = (port != null) ? port : req.getServerPort();
        }
        try {
            baseUrl = scheme + "://" + req.getServerName();
            final URL url = new URL(baseUrl);
            if (url.getPort() != -1 && url.getDefaultPort() != url.getPort()) {
                baseUrl += ":" + url.getPort();
            }
        } catch (MalformedURLException ex) {
            s_logger.error("SAML could not determine base URL from HttpServletRequest, unable to parse: " + baseUrl, ex);
        }
        return baseUrl;
    }

    public static String getBaseUrl(final String urlString) {
        URL url = null;
        String baseUrl = null;

        try {
            url = new URL(urlString);
            if (!url.getProtocol().equalsIgnoreCase("http") && !url.getProtocol().equalsIgnoreCase("https")) {
                throw new MalformedURLException("urlString protocol " + url.getProtocol() + " is not http or https");
            }
            baseUrl = url.getProtocol() + "://" + url.getHost();
            if (url.getPort() != -1 && url.getDefaultPort() != url.getPort()) {
                baseUrl += ":" + url.getPort();
            }
        } catch (MalformedURLException ex) {
            s_logger.error("SAML could not convert " + urlString + " to a URL.", ex);
        }
        return baseUrl;
    }

    public static String replaceBaseUrl(final String urlString, final String newBaseUrl) {
        String newUrl = urlString;

        try {
            new URL(urlString);
        } catch (MalformedURLException ex) {
            s_logger.error("SAML could not convert " + urlString + " to a URL: ", ex);
            return null;
        }

        if (!Strings.isNullOrEmpty(newBaseUrl)) {
            newUrl = newUrl.replace(getBaseUrl(urlString), newBaseUrl);
        }
        return newUrl;
    }

    public static String getCurrentUrl(final HttpServletRequest req) {
        String absoluteUrl = getBaseUrl(req);

        if (req.getRequestURI() != null) {
            absoluteUrl += req.getRequestURI();
        }
        if (req.getQueryString() != null) {
            absoluteUrl += "?" + req.getQueryString();
        }
        return absoluteUrl;
    }

    public static String getErrorTextFromXml(final String xml)
            throws ConfigurationException, ParserConfigurationException,
            SAXException, UnmarshallingException, IOException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
        return document.getElementsByTagName("errortext").item(0).getTextContent();
    }
}