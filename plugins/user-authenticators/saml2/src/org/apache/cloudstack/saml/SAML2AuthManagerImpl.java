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
package org.apache.cloudstack.saml;

import com.cloud.configuration.Config;
import com.cloud.utils.component.AdapterBase;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.api.command.GetServiceProviderMetaDataCmd;
import org.apache.cloudstack.api.command.SAML2LoginAPIAuthenticatorCmd;
import org.apache.cloudstack.api.command.SAML2LogoutAPIAuthenticatorCmd;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.security.keystore.KeystoreDao;
import org.apache.cloudstack.framework.security.keystore.KeystoreVO;
import org.apache.cloudstack.utils.auth.SAMLUtils;
import org.apache.log4j.Logger;
import org.apache.commons.codec.binary.Base64;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.keyinfo.KeyInfoHelper;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.xml.stream.FactoryConfigurationError;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Component
@Local(value = {SAML2AuthManager.class, PluggableAPIAuthenticator.class})
public class SAML2AuthManagerImpl extends AdapterBase implements SAML2AuthManager {
    private static final Logger s_logger = Logger.getLogger(SAML2AuthManagerImpl.class);

    private String serviceProviderId;
    private String identityProviderId;

    private X509Certificate idpSigningKey;
    private X509Certificate idpEncryptionKey;
    private X509Certificate spX509Key;
    private KeyPair spKeyPair;

    private String spSingleSignOnUrl;
    private String idpSingleSignOnUrl;

    private String spSingleLogOutUrl;
    private String idpSingleLogOutUrl;

    private HTTPMetadataProvider idpMetaDataProvider;

    @Inject
    ConfigurationDao _configDao;

    @Inject
    private KeystoreDao _ksDao;

    @Override
    public boolean start() {
        if (isSAMLPluginEnabled()) {
            setup();
        }
        return super.start();
    }

    private boolean setup() {
        KeystoreVO keyStoreVO = _ksDao.findByName(SAMLUtils.SAMLSP_KEYPAIR);
        if (keyStoreVO == null) {
            try {
                KeyPair keyPair = SAMLUtils.generateRandomKeyPair();
                _ksDao.save(SAMLUtils.SAMLSP_KEYPAIR, SAMLUtils.savePrivateKey(keyPair.getPrivate()), SAMLUtils.savePublicKey(keyPair.getPublic()), "samlsp-keypair");
                keyStoreVO = _ksDao.findByName(SAMLUtils.SAMLSP_KEYPAIR);
            } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
                s_logger.error("Unable to create and save SAML keypair");
            }
        }

        if (keyStoreVO != null) {
            PrivateKey privateKey = SAMLUtils.loadPrivateKey(keyStoreVO.getCertificate());
            PublicKey publicKey = SAMLUtils.loadPublicKey(keyStoreVO.getKey());
            if (privateKey != null && publicKey != null) {
                spKeyPair = new KeyPair(publicKey, privateKey);
                KeystoreVO x509VO = _ksDao.findByName(SAMLUtils.SAMLSP_X509CERT);
                if (x509VO == null) {
                    try {
                        spX509Key = SAMLUtils.generateRandomX509Certificate(spKeyPair);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutput out = new ObjectOutputStream(bos);
                        out.writeObject(spX509Key);
                        out.flush();
                        _ksDao.save(SAMLUtils.SAMLSP_X509CERT, Base64.encodeBase64String(bos.toByteArray()), "", "samlsp-x509cert");
                        bos.close();
                    } catch (NoSuchAlgorithmException | NoSuchProviderException | CertificateEncodingException | SignatureException | InvalidKeyException | IOException e) {
                        s_logger.error("SAML Plugin won't be able to use X509 signed authentication");
                    }
                } else {
                    try {
                        ByteArrayInputStream bi = new ByteArrayInputStream(Base64.decodeBase64(x509VO.getCertificate()));
                        ObjectInputStream si = new ObjectInputStream(bi);
                        spX509Key = (X509Certificate) si.readObject();
                        bi.close();
                    } catch (IOException | ClassNotFoundException ignored) {
                        s_logger.error("SAML Plugin won't be able to use X509 signed authentication. Failed to load X509 Certificate from Database.");
                    }
                }
            }
        }

        this.serviceProviderId = _configDao.getValue(Config.SAMLServiceProviderID.key());
        this.identityProviderId = _configDao.getValue(Config.SAMLIdentityProviderID.key());

        this.spSingleSignOnUrl = _configDao.getValue(Config.SAMLServiceProviderSingleSignOnURL.key());
        this.spSingleLogOutUrl = _configDao.getValue(Config.SAMLServiceProviderSingleLogOutURL.key());

        String idpMetaDataUrl = _configDao.getValue(Config.SAMLIdentityProviderMetadataURL.key());

        int tolerance = 30000;
        String timeout = _configDao.getValue(Config.SAMLTimeout.key());
        if (timeout != null) {
            tolerance = Integer.parseInt(timeout);
        }

        try {
            DefaultBootstrap.bootstrap();
            idpMetaDataProvider = new HTTPMetadataProvider(idpMetaDataUrl, tolerance);
            idpMetaDataProvider.setRequireValidMetadata(true);
            idpMetaDataProvider.setParserPool(new BasicParserPool());
            idpMetaDataProvider.initialize();

            EntityDescriptor idpEntityDescriptor = idpMetaDataProvider.getEntityDescriptor(this.identityProviderId);

            IDPSSODescriptor idpssoDescriptor = idpEntityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
            if (idpssoDescriptor != null) {
                for (SingleSignOnService ssos: idpssoDescriptor.getSingleSignOnServices()) {
                    if (ssos.getBinding().equals(SAMLConstants.SAML2_REDIRECT_BINDING_URI)) {
                        this.idpSingleSignOnUrl = ssos.getLocation();
                    }
                }

                for (SingleLogoutService slos: idpssoDescriptor.getSingleLogoutServices()) {
                    if (slos.getBinding().equals(SAMLConstants.SAML2_REDIRECT_BINDING_URI)) {
                        this.idpSingleLogOutUrl = slos.getLocation();
                    }
                }

                for (KeyDescriptor kd: idpssoDescriptor.getKeyDescriptors()) {
                    if (kd.getUse() == UsageType.SIGNING) {
                        try {
                            this.idpSigningKey = KeyInfoHelper.getCertificates(kd.getKeyInfo()).get(0);
                        } catch (CertificateException ignored) {
                        }
                    }
                    if (kd.getUse() == UsageType.ENCRYPTION) {
                        try {
                            this.idpEncryptionKey = KeyInfoHelper.getCertificates(kd.getKeyInfo()).get(0);
                        } catch (CertificateException ignored) {
                        }
                    }
                }
            } else {
                s_logger.warn("Provided IDP XML Metadata does not contain IDPSSODescriptor, SAML authentication may not work");
            }
        } catch (MetadataProviderException e) {
            s_logger.error("Unable to read SAML2 IDP MetaData URL, error:" + e.getMessage());
            s_logger.error("SAML2 Authentication may be unavailable");
        } catch (ConfigurationException | FactoryConfigurationError e) {
            s_logger.error("OpenSAML bootstrapping failed: error: " + e.getMessage());
        } catch (NullPointerException e) {
            s_logger.error("Unable to setup SAML Auth Plugin due to NullPointerException" +
                    " please check the SAML IDP metadata URL and entity ID in global settings: " + e.getMessage());
        }

        if (this.idpSingleLogOutUrl == null || this.idpSingleSignOnUrl == null) {
            s_logger.error("SAML based authentication won't work");
        }

        return true;
    }

    @Override
    public List<Class<?>> getAuthCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        if (!isSAMLPluginEnabled()) {
            return cmdList;
        }
        cmdList.add(SAML2LoginAPIAuthenticatorCmd.class);
        cmdList.add(SAML2LogoutAPIAuthenticatorCmd.class);
        cmdList.add(GetServiceProviderMetaDataCmd.class);
        return cmdList;
    }

    public String getServiceProviderId() {
        return serviceProviderId;
    }

    public String getIdpSingleSignOnUrl() {
        return this.idpSingleSignOnUrl;
    }

    public String getIdpSingleLogOutUrl() {
        return this.idpSingleLogOutUrl;
    }

    public String getSpSingleSignOnUrl() {
        return spSingleSignOnUrl;
    }

    public String getSpSingleLogOutUrl() {
        return spSingleLogOutUrl;
    }

    public String getIdentityProviderId() {
        return identityProviderId;
    }

    public X509Certificate getIdpSigningKey() {
        return idpSigningKey;
    }

    public X509Certificate getIdpEncryptionKey() {
        return idpEncryptionKey;
    }

    public Boolean isSAMLPluginEnabled() {
        return Boolean.valueOf(_configDao.getValue(Config.SAMLIsPluginEnabled.key()));
    }

    public X509Certificate getSpX509Certificate() {
        return spX509Key;
    }

    @Override
    public KeyPair getSpKeyPair() {
        return spKeyPair;
    }
}
