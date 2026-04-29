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

package com.cloud.utils.security;

import com.cloud.utils.Ternary;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public class CertificateHelper {
    public static byte[] buildAndSaveKeystore(final String alias, final String cert, final String privateKey, final String storePassword) throws KeyStoreException, CertificateException,
    NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        Preconditions.checkArgument(StringUtils.isNotEmpty(alias), "Certificate alias cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotEmpty(cert), "Certificate cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotEmpty(privateKey), "Private key cannot be blank");

        final KeyStore ks = buildKeystore(alias, cert, privateKey, storePassword);

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ks.store(os, storePassword != null ? storePassword.toCharArray() : null);
            return os.toByteArray();
        }
    }

    public static byte[] buildAndSaveKeystore(final List<Ternary<String, String, String>> certs, final String storePassword) throws KeyStoreException, NoSuchAlgorithmException,
    CertificateException, IOException, InvalidKeySpecException {
        Preconditions.checkNotNull(certs, "List of certificates to be saved in keystore cannot be null");
        char password[] = null;
        if (storePassword != null) {
            password = storePassword.toCharArray();
        }
        final KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, password);

        //name,cert,key
        for (final Ternary<String, String, String> cert : certs) {
            if (cert.third() == null) {
                final Certificate c = buildCertificate(cert.second());
                ks.setCertificateEntry(cert.first(), c);
            } else {
                final Certificate[] c = new Certificate[certs.size()];
                int i = certs.size();
                for (final Ternary<String, String, String> ct : certs) {
                    c[i - 1] = buildCertificate(ct.second());
                    i--;
                }
                ks.setKeyEntry(cert.first(), buildPrivateKey(cert.third()), password, c);
            }
        }

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ks.store(os, password);
            return os.toByteArray();
        }
    }

    public static KeyStore loadKeystore(final byte[] ksData, final String storePassword) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        Preconditions.checkNotNull(ksData, "Keystore data cannot be null");
        final KeyStore ks = KeyStore.getInstance("JKS");
        try (final ByteArrayInputStream is = new ByteArrayInputStream(ksData)) {
            ks.load(is, storePassword != null ? storePassword.toCharArray() : null);
        }

        return ks;
    }

    public static KeyStore buildKeystore(final String alias, final String cert, final String privateKey, final String storePassword) throws KeyStoreException, CertificateException,
    NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        Preconditions.checkArgument(StringUtils.isNotEmpty(alias), "Certificate alias cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotEmpty(cert), "Certificate cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotEmpty(privateKey), "Private key cannot be blank");

        char password[] = null;
        if (storePassword != null) {
            password = storePassword.toCharArray();
        }
        final KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, password);
        final Certificate[] certs = new Certificate[1];
        certs[0] = buildCertificate(cert);
        ks.setKeyEntry(alias, buildPrivateKey(privateKey), password, certs);
        return ks;
    }

    public static Certificate buildCertificate(final String content) throws CertificateException {
        Preconditions.checkNotNull(content, "Certificate content cannot be null");

        final BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(content.getBytes()));
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(bis);
    }

    public static Key buildPrivateKey(final String base64EncodedKeyContent) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        Preconditions.checkNotNull(base64EncodedKeyContent);

        final KeyFactory kf = KeyFactory.getInstance("RSA");
        final PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec(Base64.decodeBase64(base64EncodedKeyContent));
        return kf.generatePrivate(keysp);
    }

    public static List<Certificate> parseChain(final String chain) throws IOException, CertificateException {
        Preconditions.checkNotNull(chain);

        final List<Certificate> certs = new ArrayList<Certificate>();
        try(final PemReader pemReader = new PemReader(new StringReader(chain));)
        {
            final PemObject pemObject = pemReader.readPemObject();
            final CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
            final ByteArrayInputStream bais = new ByteArrayInputStream(pemObject.getContent());

            for (final Certificate cert : certificateFactory.generateCertificates(bais)) {
                if (cert instanceof X509Certificate) {
                    certs.add(cert);
                }
            }
            if (certs.isEmpty()) {
                throw new IllegalStateException("Unable to decode certificate chain");
            }
        }
        return certs;
    }

    public static String generateFingerPrint(final Certificate cert) {
        Preconditions.checkNotNull(cert, "Certificate cannot be null");

        final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        final StringBuilder buffer = new StringBuilder(60);
        try {

            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] data = md.digest(cert.getEncoded());

            for (final byte element : data) {
                if (buffer.length() > 0) {
                    buffer.append(":");
                }

                buffer.append(HEX[(0xF0 & element) >>> 4]);
                buffer.append(HEX[0x0F & element]);
            }

        } catch (final CertificateEncodingException e) {
            throw new IllegalStateException("Bad certificate encoding");
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Bad certificate algorithm");
        }

        return buffer.toString();
    }

}
