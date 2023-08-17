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
package org.apache.cloudstack.direct.download;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class HttpsMultiTrustManager implements X509TrustManager {

    private final List<X509TrustManager> trustManagers;

    public HttpsMultiTrustManager(KeyStore... keystores) {
        List<X509TrustManager> trustManagers = new ArrayList<>();
        trustManagers.add(getTrustManager(null));
        for (KeyStore keystore : keystores) {
            trustManagers.add(getTrustManager(keystore));
        }
        this.trustManagers = ImmutableList.copyOf(trustManagers);
    }

    public static TrustManager[] getTrustManagersFromKeyStores(KeyStore... keyStore) {
        return new TrustManager[] { new HttpsMultiTrustManager(keyStore) };

    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        for (X509TrustManager trustManager : trustManagers) {
            try {
                trustManager.checkClientTrusted(chain, authType);
                return;
            } catch (CertificateException ignored) {}
        }
        throw new CertificateException("None of the TrustManagers trust this certificate chain");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        for (X509TrustManager trustManager : trustManagers) {
            try {
                trustManager.checkServerTrusted(chain, authType);
                return;
            } catch (CertificateException ignored) {}
        }
        throw new CertificateException("None of the TrustManagers trust this certificate chain");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        ImmutableList.Builder<X509Certificate> certificates = ImmutableList.builder();
        for (X509TrustManager trustManager : trustManagers) {
            for (X509Certificate cert : trustManager.getAcceptedIssuers()) {
                certificates.add(cert);
            }
        }
        return Iterables.toArray(certificates.build(), X509Certificate.class);
    }

    public X509TrustManager getTrustManager(KeyStore keystore) {
        return getTrustManager(TrustManagerFactory.getDefaultAlgorithm(), keystore);
    }

    public X509TrustManager getTrustManager(String algorithm, KeyStore keystore) {
        TrustManagerFactory factory;
        try {
            factory = TrustManagerFactory.getInstance(algorithm);
            factory.init(keystore);
            return Iterables.getFirst(Iterables.filter(
                    Arrays.asList(factory.getTrustManagers()), X509TrustManager.class), null);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
        }
        return null;
    }
}
