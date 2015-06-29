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

import com.cloud.utils.HttpUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.LogoutRequestBuilder;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml2.core.impl.RequestedAuthnContextBuilder;
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

import javax.security.auth.x500.X500Principal;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

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

    public static String buildAuthnRequestUrl(final String authnId, final SAMLProviderMetadata spMetadata, final SAMLProviderMetadata idpMetadata, final String signatureAlgorithm) {
        String redirectUrl = "";
        try {
            DefaultBootstrap.bootstrap();
            AuthnRequest authnRequest = SAMLUtils.buildAuthnRequestObject(authnId, spMetadata.getEntityId(), idpMetadata.getSsoUrl(), spMetadata.getSsoUrl());
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

    public static LogoutRequest buildLogoutRequest(String logoutUrl, String spId, String nameIdString) {
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
        deflaterOutputStream.write(requestMessage.getBytes());
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
        signature.update(url.getBytes());
        String signatureString = Base64.encodeBytes(signature.sign(), Base64.DONT_BREAK_LINES);
        if (signatureString != null) {
            return url + "&Signature=" + URLEncoder.encode(signatureString, HttpUtils.UTF_8);
        }
        return url;
    }

    public static KeyFactory getKeyFactory() {
        KeyFactory keyFactory = null;
        try {
            Security.addProvider(new BouncyCastleProvider());
            keyFactory = KeyFactory.getInstance("RSA", "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            s_logger.error("Unable to create KeyFactory:" + e.getMessage());
        }
        return keyFactory;
    }

    public static String savePublicKey(PublicKey key) {
        try {
            KeyFactory keyFactory = SAMLUtils.getKeyFactory();
            if (keyFactory == null) return null;
            X509EncodedKeySpec spec = keyFactory.getKeySpec(key, X509EncodedKeySpec.class);
            return new String(org.bouncycastle.util.encoders.Base64.encode(spec.getEncoded()));
        } catch (InvalidKeySpecException e) {
            s_logger.error("Unable to create KeyFactory:" + e.getMessage());
        }
        return null;
    }

    public static String savePrivateKey(PrivateKey key) {
        try {
            KeyFactory keyFactory = SAMLUtils.getKeyFactory();
            if (keyFactory == null) return null;
            PKCS8EncodedKeySpec spec = keyFactory.getKeySpec(key,
                    PKCS8EncodedKeySpec.class);
            return new String(org.bouncycastle.util.encoders.Base64.encode(spec.getEncoded()));
        } catch (InvalidKeySpecException e) {
            s_logger.error("Unable to create KeyFactory:" + e.getMessage());
        }
        return null;
    }

    public static PublicKey loadPublicKey(String publicKey) {
        byte[] sigBytes = org.bouncycastle.util.encoders.Base64.decode(publicKey);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(sigBytes);
        KeyFactory keyFact = SAMLUtils.getKeyFactory();
        if (keyFact == null)
            return null;
        try {
            return keyFact.generatePublic(x509KeySpec);
        } catch (InvalidKeySpecException e) {
            s_logger.error("Unable to create PrivateKey from privateKey string:" + e.getMessage());
        }
        return null;
    }

    public static PrivateKey loadPrivateKey(String privateKey) {
        byte[] sigBytes = org.bouncycastle.util.encoders.Base64.decode(privateKey);
        PKCS8EncodedKeySpec pkscs8KeySpec = new PKCS8EncodedKeySpec(sigBytes);
        KeyFactory keyFact = SAMLUtils.getKeyFactory();
        if (keyFact == null)
            return null;
        try {
            return keyFact.generatePrivate(pkscs8KeySpec);
        } catch (InvalidKeySpecException e) {
            s_logger.error("Unable to create PrivateKey from privateKey string:" + e.getMessage());
        }
        return null;
    }

    public static KeyPair generateRandomKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(4096, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    public static X509Certificate generateRandomX509Certificate(KeyPair keyPair) throws NoSuchAlgorithmException, NoSuchProviderException, CertificateEncodingException, SignatureException, InvalidKeyException {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        X500Principal dnName = new X500Principal("CN=ApacheCloudStack");
        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setSubjectDN(dnName);
        certGen.setIssuerDN(dnName);
        certGen.setNotBefore(now.minusDays(1).toDate());
        certGen.setNotAfter(now.plusYears(3).toDate());
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        return certGen.generate(keyPair.getPrivate(), "BC");
    }

}
