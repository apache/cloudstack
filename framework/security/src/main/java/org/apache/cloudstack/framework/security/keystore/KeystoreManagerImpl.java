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
package org.apache.cloudstack.framework.security.keystore;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.security.CertificateHelper;

@Component
public class KeystoreManagerImpl extends ManagerBase implements KeystoreManager {
    private static final Logger s_logger = Logger.getLogger(KeystoreManagerImpl.class);

    @Inject
    private KeystoreDao _ksDao;

    @Override
    public boolean validateCertificate(String certificate, String key, String domainSuffix) {
        if (Strings.isNullOrEmpty(certificate) || Strings.isNullOrEmpty(key) || Strings.isNullOrEmpty(domainSuffix)) {
            s_logger.error("Invalid parameter found in (certificate, key, domainSuffix) tuple for domain: " + domainSuffix);
            return false;
        }

        try {
            String ksPassword = "passwordForValidation";
            byte[] ksBits = CertificateHelper.buildAndSaveKeystore(domainSuffix, certificate, getKeyContent(key), ksPassword);
            KeyStore ks = CertificateHelper.loadKeystore(ksBits, ksPassword);
            if (ks != null)
                return true;

            s_logger.error("Unabled to construct keystore for domain: " + domainSuffix);
        } catch (Exception e) {
            s_logger.error("Certificate validation failed due to exception for domain: " + domainSuffix, e);
        }
        return false;
    }

    @Override
    public void saveCertificate(String name, String certificate, String key, String domainSuffix) {
        if (name == null || name.isEmpty() || certificate == null || certificate.isEmpty() || key == null || key.isEmpty() || domainSuffix == null ||
            domainSuffix.isEmpty())
            throw new CloudRuntimeException("invalid parameter in saveCerticate");

        _ksDao.save(name, certificate, key, domainSuffix);
    }

    @Override
    public void saveCertificate(String name, String certificate, Integer index, String domainSuffix) {
        if (name == null || name.isEmpty() || certificate == null || certificate.isEmpty() || index == null || domainSuffix == null || domainSuffix.isEmpty())
            throw new CloudRuntimeException("invalid parameter in saveCerticate");

        _ksDao.save(name, certificate, index, domainSuffix);
    }

    @Override
    public byte[] getKeystoreBits(String name, String aliasForCertificateInStore, String storePassword) {
        assert (name != null);
        assert (aliasForCertificateInStore != null);
        assert (storePassword != null);

        KeystoreVO ksVo = _ksDao.findByName(name);
        if (ksVo == null)
            throw new CloudRuntimeException("Unable to find keystore " + name);

        List<Ternary<String, String, String>> certs = new ArrayList<Ternary<String, String, String>>();
        List<KeystoreVO> certChains = _ksDao.findCertChain();

        for (KeystoreVO ks : certChains) {
            Ternary<String, String, String> cert = new Ternary<String, String, String>(ks.getName(), ks.getCertificate(), null);
            certs.add(cert);
        }

        Ternary<String, String, String> cert = new Ternary<String, String, String>(ksVo.getName(), ksVo.getCertificate(), getKeyContent(ksVo.getKey()));
        certs.add(cert);

        try {
            return CertificateHelper.buildAndSaveKeystore(certs, storePassword);
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            String msg = String.format("Unable to build keystore for %s due to %s", name, e.getClass().getSimpleName());
            s_logger.warn(msg);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(msg, e);
            }
        }
        return null;
    }

    @Override
    public Certificates getCertificates(String name) {
        KeystoreVO ksVo = _ksDao.findByName(name);
        if (ksVo == null) {
            return null;
        }
        String prvKey = ksVo.getKey();
        String prvCert = ksVo.getCertificate();
        String domainSuffix = ksVo.getDomainSuffix();
        String certChain = null;
        String rootCert = null;
        List<KeystoreVO> certchains = _ksDao.findCertChain(domainSuffix);
        if (certchains.size() > 0) {
            ArrayList<String> chains = new ArrayList<String>();
            for (KeystoreVO cert : certchains) {
                if (chains.size() == 0) {// For the first time it will be length 0
                    rootCert = cert.getCertificate();
                }
                chains.add(cert.getCertificate());
            }
            Collections.reverse(chains);
            certChain = StringUtils.join(chains, "\n");
        }
        Certificates certs = new Certificates(prvKey, prvCert, certChain, rootCert);
        return certs;
    }

    private static String getKeyContent(String key) {
        Pattern regex = Pattern.compile("(^[\\-]+[^\\-]+[\\-]+[\\n]?)([^\\-]+)([\\-]+[^\\-]+[\\-]+$)");
        Matcher m = regex.matcher(key);
        if (m.find())
            return m.group(2);

        return key;
    }
}
