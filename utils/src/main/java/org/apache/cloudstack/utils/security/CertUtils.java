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

package org.apache.cloudstack.utils.security;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
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
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import org.apache.commons.lang3.StringUtils;

public class CertUtils {

    private static final Logger LOG = Logger.getLogger(CertUtils.class);

    public static KeyPair generateRandomKeyPair(final int keySize) throws NoSuchProviderException, NoSuchAlgorithmException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(keySize, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    public static KeyFactory getKeyFactory() {
        KeyFactory keyFactory = null;
        try {
            Security.addProvider(new BouncyCastleProvider());
            keyFactory = KeyFactory.getInstance("RSA", "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            LOG.error("Unable to create KeyFactory:" + e.getMessage());
        }
        return keyFactory;
    }

    public static X509Certificate pemToX509Certificate(final String pem) throws CertificateException, IOException {
        final PEMParser pemParser = new PEMParser(new StringReader(pem));
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate((X509CertificateHolder) pemParser.readObject());
    }

    public static String x509CertificateToPem(final X509Certificate cert) throws IOException {
        final StringWriter sw = new StringWriter();
        try (final JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(cert);
            pw.flush();
        }
        return sw.toString();
    }

    public static String x509CertificatesToPem(final List<X509Certificate> certificates) throws IOException {
        if (certificates == null) {
            return "";
        }
        final StringBuilder buffer = new StringBuilder();
        for (final X509Certificate certificate: certificates) {
            buffer.append(CertUtils.x509CertificateToPem(certificate));
        }
        return buffer.toString();
    }

    public static PrivateKey pemToPrivateKey(final String pem) throws InvalidKeySpecException, IOException {
        final PemReader pr = new PemReader(new StringReader(pem));
        final PemObject pemObject = pr.readPemObject();
        final KeyFactory keyFactory = getKeyFactory();
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pemObject.getContent()));
    }

    public static String privateKeyToPem(final PrivateKey key) throws IOException {
        final PemObject pemObject = new PemObject("PRIVATE KEY", key.getEncoded());
        final StringWriter sw = new StringWriter();
        try (final PemWriter pw = new PemWriter(sw)) {
            pw.writeObject(pemObject);
        }
        return sw.toString();
    }

    public static PublicKey pemToPublicKey(final String pem) throws InvalidKeySpecException, IOException {
        final PemReader pr = new PemReader(new StringReader(pem));
        final PemObject pemObject = pr.readPemObject();
        final KeyFactory keyFactory = getKeyFactory();
        return keyFactory.generatePublic(new X509EncodedKeySpec(pemObject.getContent()));
    }

    public static String publicKeyToPem(final PublicKey key) throws IOException {
        final PemObject pemObject = new PemObject("PUBLIC KEY", key.getEncoded());
        final StringWriter sw = new StringWriter();
        try (final PemWriter pw = new PemWriter(sw)) {
            pw.writeObject(pemObject);
        }
        return sw.toString();
    }

    public static BigInteger generateRandomBigInt() {
        return new BigInteger(64, new SecureRandom());
    }

    public static X509Certificate generateV1Certificate(final KeyPair keyPair,
                                                        final String subject,
                                                        final String issuer,
                                                        final int validityYears,
                                                        final String signatureAlgorithm) throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, InvalidKeyException, OperatorCreationException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final X509v1CertificateBuilder certBuilder = new JcaX509v1CertificateBuilder(
                new X500Name(issuer),
                generateRandomBigInt(),
                now.minusDays(1).toDate(),
                now.plusYears(validityYears).toDate(),
                new X500Name(subject),
                keyPair.getPublic());
        final ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).setProvider("BC").build(keyPair.getPrivate());
        final X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }

    public static X509Certificate generateV3Certificate(final X509Certificate caCert,
                                                        final KeyPair caKeyPair,
                                                        final PublicKey clientPublicKey,
                                                        final String subject,
                                                        final String signatureAlgorithm,
                                                        final int validityDays,
                                                        final List<String> dnsNames,
                                                        final List<String> publicIPAddresses) throws IOException, NoSuchAlgorithmException, CertificateException, NoSuchProviderException, InvalidKeyException, SignatureException, OperatorCreationException {

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final BigInteger serial = generateRandomBigInt();
        final JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        final X509v3CertificateBuilder certBuilder;
        if (caCert == null) {
            // Generate CA certificate
            certBuilder = new JcaX509v3CertificateBuilder(
                    new X500Name(subject),
                    serial,
                    now.minusHours(12).toDate(),
                    now.plusDays(validityDays).toDate(),
                    new X500Name(subject),
                    clientPublicKey);

            certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        } else {
            // Generate client certificate
            certBuilder = new JcaX509v3CertificateBuilder(
                    caCert,
                    serial,
                    now.minusHours(12).toDate(),
                    now.plusDays(validityDays).toDate(),
                    new X500Principal(subject),
                    clientPublicKey);

            certBuilder.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(caCert));
        }

        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(clientPublicKey));

        final List<ASN1Encodable> subjectAlternativeNames = new ArrayList<ASN1Encodable>();
        if (publicIPAddresses != null) {
            for (final String publicIPAddress: new HashSet<>(publicIPAddresses)) {
                if (StringUtils.isEmpty(publicIPAddress)) {
                    continue;
                }
                subjectAlternativeNames.add(new GeneralName(GeneralName.iPAddress, publicIPAddress));
            }
        }
        if (dnsNames != null) {
            for (final String dnsName : new HashSet<>(dnsNames)) {
                if (StringUtils.isEmpty(dnsName)) {
                    continue;
                }
                subjectAlternativeNames.add(new GeneralName(GeneralName.dNSName, dnsName));
            }
        }
        if (subjectAlternativeNames.size() > 0) {
            final GeneralNames subjectAltNames = GeneralNames.getInstance(new DERSequence(subjectAlternativeNames.toArray(new ASN1Encodable[] {})));
            certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
        }

        final ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).setProvider("BC").build(caKeyPair.getPrivate());
        final X509CertificateHolder certHolder = certBuilder.build(signer);
        final X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
        if (caCert != null) {
            cert.verify(caCert.getPublicKey());
        } else {
            cert.verify(caKeyPair.getPublic());
        }
        return cert;
    }
}
