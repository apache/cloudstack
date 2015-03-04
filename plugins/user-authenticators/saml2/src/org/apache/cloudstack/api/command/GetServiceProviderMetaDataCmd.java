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
import com.cloud.user.Account;
import com.cloud.utils.HttpUtils;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.api.auth.APIAuthenticator;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.api.response.SAMLMetaDataResponse;
import org.apache.cloudstack.saml.SAML2AuthManager;
import org.apache.log4j.Logger;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.NameIDFormat;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml2.metadata.impl.AssertionConsumerServiceBuilder;
import org.opensaml.saml2.metadata.impl.EntityDescriptorBuilder;
import org.opensaml.saml2.metadata.impl.KeyDescriptorBuilder;
import org.opensaml.saml2.metadata.impl.NameIDFormatBuilder;
import org.opensaml.saml2.metadata.impl.SPSSODescriptorBuilder;
import org.opensaml.saml2.metadata.impl.SingleLogoutServiceBuilder;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.keyinfo.KeyInfoGenerator;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.security.x509.X509KeyInfoGeneratorFactory;
import org.w3c.dom.Document;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;

@APICommand(name = "getSPMetadata", description = "Returns SAML2 CloudStack Service Provider MetaData", responseObject = SAMLMetaDataResponse.class, entityType = {})
public class GetServiceProviderMetaDataCmd extends BaseCmd implements APIAuthenticator {
    public static final Logger s_logger = Logger.getLogger(GetServiceProviderMetaDataCmd.class.getName());
    private static final String s_name = "spmetadataresponse";

    @Inject
    ApiServerService _apiServer;

    SAML2AuthManager _samlAuthManager;

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
        throw new ServerApiException(ApiErrorCode.METHOD_NOT_ALLOWED, "This is an authentication plugin api, cannot be used directly");
    }

    @Override
    public String authenticate(String command, Map<String, Object[]> params, HttpSession session, InetAddress remoteAddress, String responseType, StringBuilder auditTrailSb, HttpServletResponse resp) throws ServerApiException {
        SAMLMetaDataResponse response = new SAMLMetaDataResponse();
        response.setResponseName(getCommandName());

        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException | FactoryConfigurationError e) {
            s_logger.error("OpenSAML Bootstrapping error: " + e.getMessage());
            throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, _apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                    "OpenSAML Bootstrapping error while creating SP MetaData",
                    params, responseType));
        }

        EntityDescriptor spEntityDescriptor = new EntityDescriptorBuilder().buildObject();
        spEntityDescriptor.setEntityID(_samlAuthManager.getServiceProviderId());

        SPSSODescriptor spSSODescriptor = new SPSSODescriptorBuilder().buildObject();
        spSSODescriptor.setWantAssertionsSigned(true);
        spSSODescriptor.setAuthnRequestsSigned(true);

        X509KeyInfoGeneratorFactory keyInfoGeneratorFactory = new X509KeyInfoGeneratorFactory();
        keyInfoGeneratorFactory.setEmitEntityCertificate(true);
        KeyInfoGenerator keyInfoGenerator = keyInfoGeneratorFactory.newInstance();

        KeyDescriptor encKeyDescriptor = new KeyDescriptorBuilder().buildObject();
        encKeyDescriptor.setUse(UsageType.ENCRYPTION);

        KeyDescriptor signKeyDescriptor = new KeyDescriptorBuilder().buildObject();
        signKeyDescriptor.setUse(UsageType.SIGNING);

        BasicX509Credential credential = new BasicX509Credential();
        credential.setEntityCertificate(_samlAuthManager.getSpX509Certificate());
        try {
            encKeyDescriptor.setKeyInfo(keyInfoGenerator.generate(credential));
            signKeyDescriptor.setKeyInfo(keyInfoGenerator.generate(credential));
            spSSODescriptor.getKeyDescriptors().add(encKeyDescriptor);
            spSSODescriptor.getKeyDescriptors().add(signKeyDescriptor);
        } catch (SecurityException e) {
            s_logger.warn("Unable to add SP X509 descriptors:" + e.getMessage());
        }

        NameIDFormat nameIDFormat = new NameIDFormatBuilder().buildObject();
        nameIDFormat.setFormat(NameIDType.PERSISTENT);
        spSSODescriptor.getNameIDFormats().add(nameIDFormat);

        NameIDFormat emailNameIDFormat = new NameIDFormatBuilder().buildObject();
        emailNameIDFormat.setFormat(NameIDType.EMAIL);
        spSSODescriptor.getNameIDFormats().add(emailNameIDFormat);

        NameIDFormat transientNameIDFormat = new NameIDFormatBuilder().buildObject();
        transientNameIDFormat.setFormat(NameIDType.TRANSIENT);
        spSSODescriptor.getNameIDFormats().add(transientNameIDFormat);

        AssertionConsumerService assertionConsumerService = new AssertionConsumerServiceBuilder().buildObject();
        assertionConsumerService.setIndex(0);
        assertionConsumerService.setBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        assertionConsumerService.setLocation(_samlAuthManager.getSpSingleSignOnUrl());

        SingleLogoutService ssoService = new SingleLogoutServiceBuilder().buildObject();
        ssoService.setBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        ssoService.setLocation(_samlAuthManager.getSpSingleLogOutUrl());

        spSSODescriptor.getSingleLogoutServices().add(ssoService);
        spSSODescriptor.getAssertionConsumerServices().add(assertionConsumerService);
        spSSODescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);
        spEntityDescriptor.getRoleDescriptors().add(spSSODescriptor);

        StringWriter stringWriter = new StringWriter();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Marshaller out = Configuration.getMarshallerFactory().getMarshaller(spEntityDescriptor);
            out.marshall(spEntityDescriptor, document);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            StreamResult streamResult = new StreamResult(stringWriter);
            DOMSource source = new DOMSource(document);
            transformer.transform(source, streamResult);
            stringWriter.close();
            response.setMetadata(stringWriter.toString());
        } catch (ParserConfigurationException | IOException | MarshallingException | TransformerException e) {
            if (responseType.equals(HttpUtils.JSON_CONTENT_TYPE)) {
                response.setMetadata("Error creating Service Provider MetaData XML: " + e.getMessage());
            } else {
                return "Error creating Service Provider MetaData XML: " + e.getMessage();
            }
        }
        // For JSON type return serialized response object
        if (responseType.equals(HttpUtils.RESPONSE_TYPE_JSON)) {
            return ApiResponseSerializer.toSerializedString(response, responseType);
        }
        // For other response types return XML
        return stringWriter.toString();
    }

    @Override
    public APIAuthenticationType getAPIType() {
        return APIAuthenticationType.LOGIN_API;
    }

    @Override
    public void setAuthenticators(List<PluggableAPIAuthenticator> authenticators) {
        for (PluggableAPIAuthenticator authManager: authenticators) {
            if (authManager != null && authManager instanceof SAML2AuthManager) {
                _samlAuthManager = (SAML2AuthManager) authManager;
            }
        }
        if (_samlAuthManager == null) {
            s_logger.error("No suitable Pluggable Authentication Manager found for SAML2 Login Cmd");
        }
    }
}
