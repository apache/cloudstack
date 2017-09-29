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

package org.apache.cloudstack.ca.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.framework.ca.CAProvider;
import org.apache.cloudstack.framework.ca.Certificate;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.utils.security.CertUtils;
import org.apache.cloudstack.utils.security.KeyStoreUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import com.cloud.certificate.dao.CrlDao;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DbProperties;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.nio.Link;
import com.google.common.base.Strings;

public final class RootCAProvider extends AdapterBase implements CAProvider, Configurable {
    private static final Logger LOG = Logger.getLogger(RootCAProvider.class);

    public static final Integer caValidityYears = 30;
    public static final String caAlias = "root";
    public static final String managementAlias = "management";

    private static KeyPair caKeyPair = null;
    private static X509Certificate caCertificate = null;

    @Inject
    private ConfigurationDao configDao;
    @Inject
    private CrlDao crlDao;

    ////////////////////////////////////////////////////
    /////////////// Root CA Settings ///////////////////
    ////////////////////////////////////////////////////

    private static ConfigKey<String> rootCAPrivateKey = new ConfigKey<>("Hidden", String.class,
            "ca.plugin.root.private.key",
            null,
            "The ROOT CA private key.", true);

    private static ConfigKey<String> rootCAPublicKey = new ConfigKey<>("Hidden", String.class,
            "ca.plugin.root.public.key",
            null,
            "The ROOT CA public key.", true);

    private static ConfigKey<String> rootCACertificate = new ConfigKey<>("Hidden", String.class,
            "ca.plugin.root.ca.certificate",
            null,
            "The ROOT CA certificate.", true);

    private static ConfigKey<String> rootCAIssuerDN = new ConfigKey<>("Advanced", String.class,
            "ca.plugin.root.issuer.dn",
            "CN=ca.cloudstack.apache.org",
            "The ROOT CA issuer distinguished name.", true);

    protected static ConfigKey<Boolean> rootCAAuthStrictness = new ConfigKey<>("Advanced", Boolean.class,
            "ca.plugin.root.auth.strictness",
            "false",
            "Set client authentication strictness, setting to true will enforce and require client certificate for authentication in applicable CA providers.", true);

    private static ConfigKey<Boolean> rootCAAllowExpiredCert = new ConfigKey<>("Advanced", Boolean.class,
            "ca.plugin.root.allow.expired.cert",
            "true",
            "When set to true, it will allow expired client certificate during SSL handshake.", true);


    ///////////////////////////////////////////////////////////
    /////////////// Root CA Private Methods ///////////////////
    ///////////////////////////////////////////////////////////

    private Certificate generateCertificate(final List<String> domainNames, final List<String> ipAddresses, final int validityDays) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, CertificateException, SignatureException, IOException, OperatorCreationException {
        if (domainNames == null || domainNames.size() < 1 || Strings.isNullOrEmpty(domainNames.get(0))) {
            throw new CloudRuntimeException("No domain name is specified, cannot generate certificate");
        }
        final String subject = "CN=" + domainNames.get(0);

        final KeyPair keyPair = CertUtils.generateRandomKeyPair(CAManager.CertKeySize.value());
        final X509Certificate clientCertificate = CertUtils.generateV3Certificate(
                caCertificate,
                caKeyPair.getPrivate(),
                keyPair.getPublic(),
                subject,
                CAManager.CertSignatureAlgorithm.value(),
                validityDays,
                domainNames,
                ipAddresses);
        return new Certificate(clientCertificate, keyPair.getPrivate(), Collections.singletonList(caCertificate));
    }

    private Certificate generateCertificateUsingCsr(final String csr, final List<String> domainNames, final List<String> ipAddresses, final int validityDays) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, CertificateException, SignatureException, IOException, OperatorCreationException {
        PemObject pemObject = null;

        try {
            final PemReader pemReader = new PemReader(new StringReader(csr));
            pemObject = pemReader.readPemObject();
        } catch (IOException e) {
            LOG.error("Failed to read provided CSR string as a PEM object", e);
        }

        if (pemObject == null) {
            throw new CloudRuntimeException("Unable to read/process CSR: " + csr);
        }

        final PKCS10CertificationRequest request = new PKCS10CertificationRequest(pemObject.getContent());

        final X509Certificate clientCertificate = CertUtils.generateV3Certificate(
                caCertificate, caKeyPair.getPrivate(),
                request.getPublicKey(),
                request.getCertificationRequestInfo().getSubject().toString(),
                CAManager.CertSignatureAlgorithm.value(),
                validityDays,
                domainNames,
                ipAddresses);
        return new Certificate(clientCertificate, null, Collections.singletonList(caCertificate));

    }

    ////////////////////////////////////////////////////////
    /////////////// Root CA API Handlers ///////////////////
    ////////////////////////////////////////////////////////

    @Override
    public boolean canProvisionCertificates() {
        return true;
    }

    @Override
    public List<X509Certificate> getCaCertificate() {
        return Collections.singletonList(caCertificate);
    }

    @Override
    public Certificate issueCertificate(final List<String> domainNames, final List<String> ipAddresses, final int validityDays) {
        try {
            return generateCertificate(domainNames, ipAddresses, validityDays);
        } catch (final CertificateException | IOException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | OperatorCreationException e) {
            LOG.error("Failed to create client certificate, due to: ", e);
            throw new CloudRuntimeException("Failed to generate certificate due to:" + e.getMessage());
        }
    }

    @Override
    public Certificate issueCertificate(final String csr, final List<String> domainNames, final List<String> ipAddresses, final int validityDays) {
        try {
            return generateCertificateUsingCsr(csr, domainNames, ipAddresses, validityDays);
        } catch (final CertificateException | IOException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | OperatorCreationException e) {
            LOG.error("Failed to generate certificate from CSR: ", e);
            throw new CloudRuntimeException("Failed to generate certificate using CSR due to:" + e.getMessage());
        }
    }

    @Override
    public boolean revokeCertificate(final BigInteger certSerial, final String certCn) {
        return true;
    }

    ////////////////////////////////////////////////////////////
    /////////////// Root CA Trust Management ///////////////////
    ////////////////////////////////////////////////////////////

    private char[] getCaKeyStorePassphrase() {
        return KeyStoreUtils.defaultKeystorePassphrase;
    }

    private KeyStore getCaKeyStore() throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        final KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        if (caKeyPair != null && caCertificate != null) {
            ks.setKeyEntry(caAlias, caKeyPair.getPrivate(), getCaKeyStorePassphrase(), new X509Certificate[]{caCertificate});
        } else {
            return null;
        }
        return ks;
    }

    @Override
    public SSLEngine createSSLEngine(final SSLContext sslContext, final String remoteAddress, final Map<String, X509Certificate> certMap) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException {
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");

        final KeyStore ks = getCaKeyStore();
        kmf.init(ks, getCaKeyStorePassphrase());
        tmf.init(ks);

        final boolean authStrictness = rootCAAuthStrictness.value();
        final boolean allowExpiredCertificate = rootCAAllowExpiredCert.value();

        TrustManager[] tms = new TrustManager[]{new RootCACustomTrustManager(remoteAddress, authStrictness, allowExpiredCertificate, certMap, caCertificate, crlDao)};
        sslContext.init(kmf.getKeyManagers(), tms, new SecureRandom());
        final SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setNeedClientAuth(authStrictness);
        return sslEngine;
    }

    //////////////////////////////////////////////////
    /////////////// Root CA Config ///////////////////
    //////////////////////////////////////////////////

    private char[] findKeyStorePassphrase() {
        char[] passphrase = KeyStoreUtils.defaultKeystorePassphrase;
        final String configuredPassphrase = DbProperties.getDbProperties().getProperty("db.cloud.keyStorePassphrase");
        if (configuredPassphrase != null) {
            passphrase = configuredPassphrase.toCharArray();
        }
        return passphrase;
    }

    private boolean createManagementServerKeystore(final String keyStoreFilePath, final char[] passphrase) {
        final Certificate managementServerCertificate = issueCertificate(Collections.singletonList(NetUtils.getHostName()),
                Collections.singletonList(NetUtils.getDefaultHostIp()), caValidityYears * 365);
        if (managementServerCertificate == null || managementServerCertificate.getPrivateKey() == null) {
            throw new CloudRuntimeException("Failed to generate certificate and setup management server keystore");
        }
        LOG.info("Creating new management server certificate and keystore");
        try {
            final KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            keyStore.setCertificateEntry(caAlias, caCertificate);
            keyStore.setKeyEntry(managementAlias, managementServerCertificate.getPrivateKey(), passphrase,
                    new X509Certificate[]{managementServerCertificate.getClientCertificate(), caCertificate});
            final String tmpFile = KeyStoreUtils.defaultTmpKeyStoreFile;
            final FileOutputStream stream = new FileOutputStream(tmpFile);
            keyStore.store(stream, passphrase);
            stream.close();
            KeyStoreUtils.copyKeystore(keyStoreFilePath, tmpFile);
            LOG.debug("Saved default root CA (server) keystore file at:" + keyStoreFilePath);
        } catch (final CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException  e) {
            LOG.error("Failed to save root CA (server) keystore due to exception: ", e);
            return false;
        }
        return true;
    }

    private boolean checkManagementServerKeystore() {
        final File confFile = PropertiesUtil.findConfigFile("db.properties");
        if (confFile == null) {
            return false;
        }
        final char[] passphrase = findKeyStorePassphrase();
        final String keystorePath = confFile.getParent() + "/" + KeyStoreUtils.defaultKeystoreFile;
        final File keystoreFile = new File(keystorePath);
        if (keystoreFile.exists()) {
            try {
                final KeyStore msKeystore = Link.loadKeyStore(new FileInputStream(keystorePath), passphrase);
                try {
                    final java.security.cert.Certificate[] msCertificates = msKeystore.getCertificateChain(managementAlias);
                    if (msCertificates != null && msCertificates.length > 1) {
                        msCertificates[0].verify(caKeyPair.getPublic());
                        ((X509Certificate)msCertificates[0]).checkValidity();
                        return true;
                    }
                } catch (final CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
                    LOG.info("Renewing management server keystore, current certificate has expired");
                    return createManagementServerKeystore(keystoreFile.getAbsolutePath(), passphrase);
                }
            } catch (final GeneralSecurityException | IOException e) {
                LOG.error("Failed to read current management server keystore, renewing keystore!");
            }
        }
        return createManagementServerKeystore(keystoreFile.getAbsolutePath(), passphrase);
    }

    /////////////////////////////////////////////////
    /////////////// Root CA Setup ///////////////////
    /////////////////////////////////////////////////

    private boolean saveNewRootCAKeypair() {
        try {
            LOG.debug("Generating root CA public/private keys");
            final KeyPair keyPair = CertUtils.generateRandomKeyPair(2 * CAManager.CertKeySize.value());
            if (!configDao.update(rootCAPublicKey.key(), rootCAPublicKey.category(), CertUtils.publicKeyToPem(keyPair.getPublic()))) {
                LOG.error("Failed to save RootCA public key");
            }
            if (!configDao.update(rootCAPrivateKey.key(), rootCAPrivateKey.category(), CertUtils.privateKeyToPem(keyPair.getPrivate()))) {
                LOG.error("Failed to save RootCA private key");
            }
        } catch (final NoSuchProviderException | NoSuchAlgorithmException | IOException e) {
            LOG.error("Failed to generate/save RootCA private/public keys due to exception:", e);
        }
        return loadRootCAKeyPair();
    }

    private boolean saveNewRootCACertificate() {
        if (caKeyPair == null) {
            throw new CloudRuntimeException("Cannot issue self-signed root CA certificate as CA keypair is not initialized");
        }
        try {
            LOG.debug("Generating root CA certificate");
            final X509Certificate rootCaCertificate = CertUtils.generateV1Certificate(
                    caKeyPair,
                    rootCAIssuerDN.value(),
                    rootCAIssuerDN.value(),
                    caValidityYears,
                    CAManager.CertSignatureAlgorithm.value());
            if (!configDao.update(rootCACertificate.key(), rootCACertificate.category(), CertUtils.x509CertificateToPem(rootCaCertificate))) {
                LOG.error("Failed to update RootCA public/x509 certificate");
            }
        } catch (final CertificateException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException | InvalidKeyException | OperatorCreationException | IOException e) {
            LOG.error("Failed to generate RootCA certificate from private/public keys due to exception:", e);
            return false;
        }
        return loadRootCACertificate();
    }

    private boolean loadRootCAKeyPair() {
        if (Strings.isNullOrEmpty(rootCAPublicKey.value()) || Strings.isNullOrEmpty(rootCAPrivateKey.value())) {
            return false;
        }
        try {
            caKeyPair = new KeyPair(CertUtils.pemToPublicKey(rootCAPublicKey.value()), CertUtils.pemToPrivateKey(rootCAPrivateKey.value()));
        } catch (InvalidKeySpecException | IOException e) {
            LOG.error("Failed to load saved RootCA private/public keys due to exception:", e);
            return false;
        }
        return caKeyPair.getPrivate() != null && caKeyPair.getPublic() != null;
    }

    private boolean loadRootCACertificate() {
        if (Strings.isNullOrEmpty(rootCACertificate.value())) {
            return false;
        }
        try {
            caCertificate = CertUtils.pemToX509Certificate(rootCACertificate.value());
            caCertificate.verify(caKeyPair.getPublic());
        } catch (final IOException | CertificateException | NoSuchAlgorithmException | InvalidKeyException | SignatureException | NoSuchProviderException e) {
            LOG.error("Failed to load saved RootCA certificate due to exception:", e);
            return false;
        }
        return caCertificate != null;
    }

    private boolean setupCA() {
        if (!loadRootCAKeyPair() && !saveNewRootCAKeypair()) {
            LOG.error("Failed to save and load root CA keypair");
            return false;
        }
        if (!loadRootCACertificate() && !saveNewRootCACertificate()) {
            LOG.error("Failed to save and load root CA certificate");
            return false;
        }
        if (!checkManagementServerKeystore()) {
            LOG.error("Failed to check and configure management server keystore");
            return false;
        }
        return true;
    }

    @Override
    public boolean start() {
        return loadRootCAKeyPair() && loadRootCAKeyPair() && checkManagementServerKeystore();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        Security.addProvider(new BouncyCastleProvider());
        final GlobalLock caLock = GlobalLock.getInternLock("RootCAProviderSetup");
        try {
            if (caLock.lock(5 * 60)) {
                try {
                    return setupCA();
                } finally {
                    caLock.unlock();
                }
            } else {
                LOG.error("Failed to grab lock and setup CA, startup method will try to load the CA certificate and keypair.");
            }
        } finally {
            caLock.releaseRef();
        }
        return true;
    }

    ///////////////////////////////////////////////////////
    /////////////// Root CA Descriptors ///////////////////
    ///////////////////////////////////////////////////////

    @Override
    public String getConfigComponentName() {
        return RootCAProvider.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{
                rootCAPrivateKey,
                rootCAPublicKey,
                rootCACertificate,
                rootCAIssuerDN,
                rootCAAuthStrictness,
                rootCAAllowExpiredCert
        };
    }

    @Override
    public String getProviderName() {
        return "root";
    }

    @Override
    public String getDescription() {
        return "CloudStack's Root CA provider plugin";
    }
}
