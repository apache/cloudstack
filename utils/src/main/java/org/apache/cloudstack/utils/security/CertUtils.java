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
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.common.base.Strings;

public class CertUtils {

    private static final Logger LOG = Logger.getLogger(CertUtils.class);

    public static KeyPair generateRandomKeyPair(final int keySize) throws NoSuchProviderException, NoSuchAlgorithmException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(keySize, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    private static KeyFactory getKeyFactory() {
        KeyFactory keyFactory = null;
        try {
            Security.addProvider(new BouncyCastleProvider());
            keyFactory = KeyFactory.getInstance("RSA", "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            LOG.error("Unable to create KeyFactory:" + e.getMessage());
        }
        return keyFactory;
    }

    public static X509Certificate pemToX509Certificate(final String pem) throws IOException {
        final PEMReader pr = new PEMReader(new StringReader(pem));
        return (X509Certificate) pr.readObject();
    }

    public static String x509CertificateToPem(final X509Certificate cert) throws IOException {
        final StringWriter sw = new StringWriter();
        try (final PEMWriter pw = new PEMWriter(sw)) {
            pw.writeObject(cert);
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
        final PEMReader pr = new PEMReader(new StringReader(pem));
        final PemObject pemObject = pr.readPemObject();
        final KeyFactory keyFactory = getKeyFactory();
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pemObject.getContent()));
    }

    public static String privateKeyToPem(final PrivateKey key) throws IOException {
        final PemObject pemObject = new PemObject("RSA PRIVATE KEY", key.getEncoded());
        final StringWriter sw = new StringWriter();
        try (final PEMWriter pw = new PEMWriter(sw)) {
            pw.writeObject(pemObject);
        }
        return sw.toString();
    }

    public static PublicKey pemToPublicKey(final String pem) throws InvalidKeySpecException, IOException {
        final PEMReader pr = new PEMReader(new StringReader(pem));
        final PemObject pemObject = pr.readPemObject();
        final KeyFactory keyFactory = getKeyFactory();
        return keyFactory.generatePublic(new X509EncodedKeySpec(pemObject.getContent()));
    }

    public static String publicKeyToPem(final PublicKey key) throws IOException {
        final PemObject pemObject = new PemObject("PUBLIC KEY", key.getEncoded());
        final StringWriter sw = new StringWriter();
        try (final PEMWriter pw = new PEMWriter(sw)) {
            pw.writeObject(pemObject);
        }
        return sw.toString();
    }

    public static BigInteger generateRandomBigInt() {
        return new BigInteger(64, new SecureRandom());
    }

    public static X509Certificate generateV1Certificate(final KeyPair keyPair,
                                                        final String subjectDN,
                                                        final String issuerDN,
                                                        final int validityYears,
                                                        final String signatureAlgorithm) throws NoSuchAlgorithmException, NoSuchProviderException, CertificateEncodingException, SignatureException, InvalidKeyException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final X500Principal subjectDn = new X500Principal(subjectDN);
        final X500Principal issuerDn = new X500Principal(issuerDN);
        final X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        certGen.setSerialNumber(generateRandomBigInt());
        certGen.setSubjectDN(subjectDn);
        certGen.setIssuerDN(issuerDn);
        certGen.setNotBefore(now.minusDays(1).toDate());
        certGen.setNotAfter(now.plusYears(validityYears).toDate());
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm(signatureAlgorithm);
        return certGen.generate(keyPair.getPrivate(), "BC");
    }

    public static X509Certificate generateV3Certificate(final X509Certificate caCert,
                                                        final PrivateKey caPrivateKey,
                                                        final PublicKey clientPublicKey,
                                                        final String subjectDN,
                                                        final String signatureAlgorithm,
                                                        final int validityDays,
                                                        final List<String> dnsNames,
                                                        final List<String> publicIPAddresses) throws IOException, NoSuchAlgorithmException, CertificateException, NoSuchProviderException, InvalidKeyException, SignatureException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final BigInteger serial = generateRandomBigInt();
        final X500Principal subject = new X500Principal(subjectDN);

        final X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();;
        certGen.setSerialNumber(serial);
        certGen.setIssuerDN(caCert.getSubjectX500Principal());
        certGen.setSubjectDN(subject);
        certGen.setNotBefore(now.minusHours(12).toDate());
        certGen.setNotAfter(now.plusDays(validityDays).toDate());
        certGen.setPublicKey(clientPublicKey);
        certGen.setSignatureAlgorithm(signatureAlgorithm);
        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(caCert));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(clientPublicKey));

        final List<ASN1Encodable> subjectAlternativeNames = new ArrayList<ASN1Encodable>();
        if (publicIPAddresses != null) {
            for (final String publicIPAddress: publicIPAddresses) {
                if (Strings.isNullOrEmpty(publicIPAddress)) {
                    continue;
                }
                subjectAlternativeNames.add(new GeneralName(GeneralName.iPAddress, publicIPAddress));
            }
        }
        if (dnsNames != null) {
            for (final String dnsName : dnsNames) {
                if (Strings.isNullOrEmpty(dnsName)) {
                    continue;
                }
                subjectAlternativeNames.add(new GeneralName(GeneralName.dNSName, dnsName));
            }
        }
        if (subjectAlternativeNames.size() > 0) {
            final DERSequence subjectAlternativeNamesExtension = new DERSequence(
                    subjectAlternativeNames.toArray(new ASN1Encodable[subjectAlternativeNames.size()]));
            certGen.addExtension(X509Extensions.SubjectAlternativeName, false,
                    subjectAlternativeNamesExtension);
        }
        final X509Certificate certificate = certGen.generate(caPrivateKey, "BC");
        certificate.verify(caCert.getPublicKey());
        return certificate;
    }
}
