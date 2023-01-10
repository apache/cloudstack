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

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.InetAddress;
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
import java.util.ArrayList;
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
import javax.xml.bind.DatatypeConverter;

import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.framework.ca.CAProvider;
import org.apache.cloudstack.framework.ca.Certificate;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.utils.security.CertUtils;
import org.apache.cloudstack.utils.security.KeyStoreUtils;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import com.cloud.certificate.dao.CrlDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import org.apache.commons.lang3.StringUtils;

public final class RootCAProvider extends AdapterBase implements CAProvider, Configurable {

    public static final Integer caValidityYears = 30;
    public static final String caAlias = "root";
    public static final String managementAlias = "management";

    private static KeyPair caKeyPair = null;
    private static X509Certificate caCertificate = null;
    private static KeyStore managementKeyStore = null;

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
        if (domainNames == null || domainNames.size() < 1 || StringUtils.isEmpty(domainNames.get(0))) {
            throw new CloudRuntimeException("No domain name is specified, cannot generate certificate");
        }
        final String subject = "CN=" + domainNames.get(0);

        final KeyPair keyPair = CertUtils.generateRandomKeyPair(CAManager.CertKeySize.value());
        final X509Certificate clientCertificate = CertUtils.generateV3Certificate(
                caCertificate, caKeyPair, keyPair.getPublic(),
                subject, CAManager.CertSignatureAlgorithm.value(),
                validityDays, domainNames, ipAddresses);
        return new Certificate(clientCertificate, keyPair.getPrivate(), Collections.singletonList(caCertificate));
    }

    private Certificate generateCertificateUsingCsr(final String csr, final List<String> names, final List<String> ips, final int validityDays) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, CertificateException, SignatureException, IOException, OperatorCreationException {
        final List<String> dnsNames = new ArrayList<>();
        final List<String> ipAddresses = new ArrayList<>();

        if (names != null) {
            dnsNames.addAll(names);
        }
        if (ips != null) {
            ipAddresses.addAll(ips);
        }

        PemObject pemObject = null;

        try {
            final PemReader pemReader = new PemReader(new StringReader(csr));
            pemObject = pemReader.readPemObject();
        } catch (IOException e) {
            logger.error("Failed to read provided CSR string as a PEM object", e);
        }

        if (pemObject == null) {
            throw new CloudRuntimeException("Unable to read/process CSR: " + csr);
        }

        final JcaPKCS10CertificationRequest request = new JcaPKCS10CertificationRequest(pemObject.getContent());
        final String subject = request.getSubject().toString();
        for (final Attribute attribute : request.getAttributes()) {
            if (attribute == null) {
                continue;
            }
            if (attribute.getAttrType().equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
                final Extensions extensions = Extensions.getInstance(attribute.getAttrValues().getObjectAt(0));
                final GeneralNames gns = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
                if (gns != null && gns.getNames() != null && gns.getNames().length > 0) {
                    for (final GeneralName name : gns.getNames()) {
                        if (name.getTagNo() == GeneralName.dNSName) {
                            dnsNames.add(name.getName().toString());
                        }
                        if (name.getTagNo() == GeneralName.iPAddress) {
                            final InetAddress address = InetAddress.getByAddress(DatatypeConverter.parseHexBinary(name.getName().toString().substring(1)));
                            ipAddresses.add(address.toString().replace("/", ""));
                        }
                    }
                }
            }
        }

        final X509Certificate clientCertificate = CertUtils.generateV3Certificate(
                caCertificate, caKeyPair, request.getPublicKey(),
                subject, CAManager.CertSignatureAlgorithm.value(),
                validityDays, dnsNames, ipAddresses);
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
            logger.error("Failed to create client certificate, due to: ", e);
            throw new CloudRuntimeException("Failed to generate certificate due to:" + e.getMessage());
        }
    }

    @Override
    public Certificate issueCertificate(final String csr, final List<String> domainNames, final List<String> ipAddresses, final int validityDays) {
        try {
            return generateCertificateUsingCsr(csr, domainNames, ipAddresses, validityDays);
        } catch (final CertificateException | IOException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | OperatorCreationException e) {
            logger.error("Failed to generate certificate from CSR: ", e);
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

    private KeyStore getCaKeyStore() throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        final KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        if (caKeyPair != null && caCertificate != null) {
            ks.setKeyEntry(caAlias, caKeyPair.getPrivate(), getKeyStorePassphrase(), new X509Certificate[]{caCertificate});
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
        kmf.init(ks, getKeyStorePassphrase());
        tmf.init(ks);

        final boolean authStrictness = rootCAAuthStrictness.value();
        final boolean allowExpiredCertificate = rootCAAllowExpiredCert.value();

        TrustManager[] tms = new TrustManager[]{new RootCACustomTrustManager(remoteAddress, authStrictness, allowExpiredCertificate, certMap, caCertificate, crlDao)};

        sslContext.init(kmf.getKeyManagers(), tms, new SecureRandom());
        final SSLEngine sslEngine = sslContext.createSSLEngine();
        // If authStrictness require SSL and validate client cert, otherwise prefer SSL but don't validate client cert
        if (authStrictness) {
            sslEngine.setNeedClientAuth(true);  // Require SSL and client cert validation
        } else {
            sslEngine.setWantClientAuth(true);  // Prefer SSL but don't validate client cert
        }

        return sslEngine;
    }

    @Override
    public KeyStore getManagementKeyStore() throws KeyStoreException {
        return managementKeyStore;
    }

    @Override
    public char[] getKeyStorePassphrase() {
        return KeyStoreUtils.DEFAULT_KS_PASSPHRASE;
    }

    /////////////////////////////////////////////////
    /////////////// Root CA Setup ///////////////////
    /////////////////////////////////////////////////

    private int getCaValidityDays() {
        return 365 * caValidityYears;
    }

    private boolean saveNewRootCAKeypair() {
        try {
            logger.debug("Generating root CA public/private keys");
            final KeyPair keyPair = CertUtils.generateRandomKeyPair(2 * CAManager.CertKeySize.value());
            if (!configDao.update(rootCAPublicKey.key(), rootCAPublicKey.category(), CertUtils.publicKeyToPem(keyPair.getPublic()))) {
                logger.error("Failed to save RootCA public key");
            }
            if (!configDao.update(rootCAPrivateKey.key(), rootCAPrivateKey.category(), CertUtils.privateKeyToPem(keyPair.getPrivate()))) {
                logger.error("Failed to save RootCA private key");
            }
        } catch (final NoSuchProviderException | NoSuchAlgorithmException | IOException e) {
            logger.error("Failed to generate/save RootCA private/public keys due to exception:", e);
        }
        return loadRootCAKeyPair();
    }

    private boolean saveNewRootCACertificate() {
        if (caKeyPair == null) {
            throw new CloudRuntimeException("Cannot issue self-signed root CA certificate as CA keypair is not initialized");
        }
        try {
            logger.debug("Generating root CA certificate");
            final X509Certificate rootCaCertificate = CertUtils.generateV3Certificate(
                    null, caKeyPair, caKeyPair.getPublic(),
                    rootCAIssuerDN.value(), CAManager.CertSignatureAlgorithm.value(),
                    getCaValidityDays(), null, null);
            if (!configDao.update(rootCACertificate.key(), rootCACertificate.category(), CertUtils.x509CertificateToPem(rootCaCertificate))) {
                logger.error("Failed to update RootCA public/x509 certificate");
            }
        } catch (final CertificateException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException | InvalidKeyException | OperatorCreationException | IOException e) {
            logger.error("Failed to generate RootCA certificate from private/public keys due to exception:", e);
            return false;
        }
        return loadRootCACertificate();
    }

    private boolean loadRootCAKeyPair() {
        if (StringUtils.isAnyEmpty(rootCAPublicKey.value(), rootCAPrivateKey.value())) {
            return false;
        }
        try {
            caKeyPair = new KeyPair(CertUtils.pemToPublicKey(rootCAPublicKey.value()), CertUtils.pemToPrivateKey(rootCAPrivateKey.value()));
        } catch (InvalidKeySpecException | IOException e) {
            logger.error("Failed to load saved RootCA private/public keys due to exception:", e);
            return false;
        }
        return caKeyPair.getPrivate() != null && caKeyPair.getPublic() != null;
    }

    private boolean loadRootCACertificate() {
        if (StringUtils.isEmpty(rootCACertificate.value())) {
            return false;
        }
        try {
            caCertificate = CertUtils.pemToX509Certificate(rootCACertificate.value());
            caCertificate.verify(caKeyPair.getPublic());
        } catch (final IOException | CertificateException | NoSuchAlgorithmException | InvalidKeyException | SignatureException | NoSuchProviderException e) {
            logger.error("Failed to load saved RootCA certificate due to exception:", e);
            return false;
        }
        return caCertificate != null;
    }

    private boolean loadManagementKeyStore() {
        if (managementKeyStore != null) {
            return true;
        }
        final Certificate serverCertificate = issueCertificate(Collections.singletonList(NetUtils.getHostName()),
                NetUtils.getAllDefaultNicIps(), getCaValidityDays());
        if (serverCertificate == null || serverCertificate.getPrivateKey() == null) {
            throw new CloudRuntimeException("Failed to generate management server certificate and load management server keystore");
        }
        logger.info("Creating new management server certificate and keystore");
        try {
            managementKeyStore = KeyStore.getInstance("JKS");
            managementKeyStore.load(null, null);
            managementKeyStore.setCertificateEntry(caAlias, caCertificate);
            managementKeyStore.setKeyEntry(managementAlias, serverCertificate.getPrivateKey(), getKeyStorePassphrase(),
                    new X509Certificate[]{serverCertificate.getClientCertificate(), caCertificate});
        } catch (final CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException  e) {
            logger.error("Failed to load root CA management-server keystore due to exception: ", e);
            return false;
        }
        return managementKeyStore != null;
    }

    private boolean setupCA() {
        if (!loadRootCAKeyPair() && !saveNewRootCAKeypair()) {
            logger.error("Failed to save and load root CA keypair");
            return false;
        }
        if (!loadRootCACertificate() && !saveNewRootCACertificate()) {
            logger.error("Failed to save and load root CA certificate");
            return false;
        }
        if (!loadManagementKeyStore()) {
            logger.error("Failed to check and configure management server keystore");
            return false;
        }
        return true;
    }

    @Override
    public boolean start() {
        return loadRootCAKeyPair() && loadRootCAKeyPair() && loadManagementKeyStore();
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
                logger.error("Failed to grab lock and setup CA, startup method will try to load the CA certificate and keypair.");
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
