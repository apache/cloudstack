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
package org.apache.cloudstack.network.lb;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.command.user.loadbalancer.DeleteSslCertCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListSslCertsCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UploadSslCertCmd;
import org.apache.cloudstack.api.response.SslCertResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerCertMapVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.SslCertDao;
import com.cloud.network.dao.SslCertVO;
import com.cloud.network.lb.CertService;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value = {CertService.class})
public class CertServiceImpl implements CertService {

    private static final Logger s_logger = Logger.getLogger(CertServiceImpl.class);

    @Inject
    AccountManager _accountMgr;
    @Inject
    AccountDao _accountDao;
    @Inject
    SslCertDao _sslCertDao;
    @Inject
    LoadBalancerCertMapDao _lbCertDao;
    @Inject
    EntityManager _entityMgr;

    public CertServiceImpl() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LB_CERT_UPLOAD, eventDescription = "Uploading a certificate to cloudstack", async = false)
    public SslCertResponse uploadSslCert(UploadSslCertCmd certCmd) {
        try {

            String cert = URLDecoder.decode(certCmd.getCert(), "UTF-8");
            String key = URLDecoder.decode(certCmd.getKey(), "UTF-8");
            String password = certCmd.getPassword();
            String chain = certCmd.getChain() == null ? null : URLDecoder.decode(certCmd.getChain(), "UTF-8");

            validate(cert, key, password, chain);
            s_logger.debug("Certificate Validation succeeded");

            String fingerPrint = generateFingerPrint(parseCertificate(cert));

            Long accountId = CallContext.current().getCallingAccount().getId();
            Long domainId = CallContext.current().getCallingAccount().getDomainId();

            SslCertVO certVO = new SslCertVO(cert, key, password, chain, accountId, domainId, fingerPrint);
            _sslCertDao.persist(certVO);

            return createCertResponse(certVO, null);

        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Error decoding certificate data");
        }

    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LB_CERT_DELETE, eventDescription = "Deleting a certificate to cloudstack", async = false)
    public void deleteSslCert(DeleteSslCertCmd deleteSslCertCmd) {

        CallContext ctx = CallContext.current();
        Account caller = ctx.getCallingAccount();

        Long certId = deleteSslCertCmd.getId();
        SslCertVO certVO = _sslCertDao.findById(certId);

        if (certVO == null) {
            throw new InvalidParameterValueException("Invalid certificate id: " + certId);
        }
        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.ModifyEntry, true, certVO);

        List<LoadBalancerCertMapVO> lbCertRule = _lbCertDao.listByCertId(certId);

        if ((lbCertRule != null) && (!lbCertRule.isEmpty())) {
            String lbUuids = "";

            for (LoadBalancerCertMapVO rule : lbCertRule) {
                LoadBalancerVO lb = _entityMgr.findById(LoadBalancerVO.class, rule.getLbId());
                lbUuids += " " + lb.getUuid();
            }

            throw new CloudRuntimeException("Certificate in use by a loadbalancer(s)" + lbUuids);
        }

        _sslCertDao.remove(certId);
    }

    @Override
    public List<SslCertResponse> listSslCerts(ListSslCertsCmd listSslCertCmd) {
        CallContext ctx = CallContext.current();
        Account caller = ctx.getCallingAccount();

        Long certId = listSslCertCmd.getCertId();
        Long accountId = listSslCertCmd.getAccountId();
        Long lbRuleId = listSslCertCmd.getLbId();

        List<SslCertResponse> certResponseList = new ArrayList<SslCertResponse>();

        if (certId == null && accountId == null && lbRuleId == null) {
            throw new InvalidParameterValueException("Invalid parameters either certificate ID or Account ID or Loadbalancer ID required");
        }

        List<LoadBalancerCertMapVO> certLbMap = null;
        SslCertVO certVO = null;

        if (certId != null) {

            certVO = _sslCertDao.findById(certId);

            if (certVO == null) {
                throw new InvalidParameterValueException("Invalid certificate id: " + certId);
            }

            _accountMgr.checkAccess(caller, SecurityChecker.AccessType.ListEntry, true, certVO);

            certLbMap = _lbCertDao.listByCertId(certId);

            certResponseList.add(createCertResponse(certVO, certLbMap));
            return certResponseList;
        }

        if (lbRuleId != null) {
            LoadBalancer lb = _entityMgr.findById(LoadBalancerVO.class, lbRuleId);

            if (lb == null) {
                throw new InvalidParameterValueException("found no loadbalancer  wth id: " + lbRuleId);
            }

            _accountMgr.checkAccess(caller, SecurityChecker.AccessType.ListEntry, true, lb);

            // get the cert id
            LoadBalancerCertMapVO lbCertMapRule;
            lbCertMapRule = _lbCertDao.findByLbRuleId(lbRuleId);

            if (lbCertMapRule == null) {
                throw new InvalidParameterValueException("No certificate bound to loadbalancer id: " + lbRuleId);
            }

            certVO = _sslCertDao.findById(lbCertMapRule.getCertId());
            certLbMap = _lbCertDao.listByCertId(lbCertMapRule.getCertId());

            certResponseList.add(createCertResponse(certVO, certLbMap));
            return certResponseList;

        }

        //reached here look by accountId
        List<SslCertVO> certVOList = _sslCertDao.listByAccountId(accountId);
        if (certVOList == null || certVOList.isEmpty())
            return certResponseList;
        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.ListEntry, true, certVOList.get(0));

        for (SslCertVO cert : certVOList) {
            certLbMap = _lbCertDao.listByCertId(cert.getId());
            certResponseList.add(createCertResponse(cert, certLbMap));
        }

        return certResponseList;
    }

    private void validate(String _cert, String _key, String _password, String _chain) {
        Certificate cert;
        PrivateKey key;
        List<Certificate> chain = null;

        try {
            cert = parseCertificate(_cert);
            key = parsePrivateKey(_key, _password);

            if (_chain != null) {
                chain = parseChain(_chain);
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Parsing certificate/key failed: " + e.getMessage(), e);
        }

        validateCert(cert, _chain != null ? true : false);
        validateKeys(cert.getPublicKey(), key);

        if (_chain != null)
            validateChain(chain, cert);
    }

    public SslCertResponse createCertResponse(SslCertVO cert, List<LoadBalancerCertMapVO> lbCertMap) {
        SslCertResponse response = new SslCertResponse();

        Account account = _accountDao.findByIdIncludingRemoved(cert.getAccountId());

        response.setObjectName("sslcert");
        response.setId(cert.getUuid());
        response.setCertificate(cert.getCertificate());
        response.setPrivatekey(cert.getKey());
        response.setFingerprint(cert.getFingerPrint());
        response.setAccountName(account.getAccountName());

        if (cert.getChain() != null)
            response.setCertchain(cert.getChain());

        if (lbCertMap != null && !lbCertMap.isEmpty()) {
            List<String> lbIds = new ArrayList<String>();
            for (LoadBalancerCertMapVO mapVO : lbCertMap) {
                LoadBalancer lb = _entityMgr.findById(LoadBalancerVO.class, mapVO.getLbId());
                lbIds.add(lb.getUuid());
            }
            response.setLbIds(lbIds);
        }

        return response;
    }

    private void validateCert(Certificate cert, boolean chain_present) {

        if (!(cert instanceof X509Certificate))
            throw new IllegalArgumentException("Invalid certificate format. Expected X509 certificate");

        try {
            ((X509Certificate)cert).checkValidity();
        } catch (Exception e) {
            throw new IllegalArgumentException("Certificate expired or not valid", e);
        }

        if (!chain_present) {
            PublicKey pubKey = cert.getPublicKey();
            try {
                cert.verify(pubKey);
            } catch (Exception e) {
                throw new IllegalArgumentException("No chain given and certificate not self signed", e);
            }
        }
    }

    private void validateKeys(PublicKey pubKey, PrivateKey privKey) {

        if (pubKey.getAlgorithm() != privKey.getAlgorithm())
            throw new IllegalArgumentException("Public and private key have different algorithms");

        // No encryption for DSA
        if (pubKey.getAlgorithm() != "RSA")
            return;

        try {

            String data = "ENCRYPT_DATA";
            SecureRandom random = new SecureRandom();
            Cipher cipher = Cipher.getInstance(pubKey.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, privKey, random);
            byte[] encryptedData = cipher.doFinal(data.getBytes());

            cipher.init(Cipher.DECRYPT_MODE, pubKey, random);
            String decreptedData = new String(cipher.doFinal(encryptedData));
            if (!decreptedData.equals(data))
                throw new IllegalArgumentException("Bad public-private key");

        } catch (BadPaddingException e) {
            throw new IllegalArgumentException("Bad public-private key", e);
        } catch (IllegalBlockSizeException e) {
            throw new IllegalArgumentException("Bad public-private key", e);
        } catch (NoSuchPaddingException e) {
            throw new IllegalArgumentException("Bad public-private key", e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid public-private key", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Invalid algorithm for public-private key", e);
        }
    }

    private void validateChain(List<Certificate> chain, Certificate cert) {

        List<Certificate> certs = new ArrayList<Certificate>();
        List<Certificate> root = new ArrayList<Certificate>();

        Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();

        certs.add(cert); // adding for self signed certs
        certs.addAll(chain);

        for (Certificate c : certs) {
            if (!(c instanceof X509Certificate))
                throw new IllegalArgumentException("Invalid chain format. Expected X509 certificate");

            X509Certificate xCert = (X509Certificate)c;

            Principal subject = xCert.getSubjectDN();
            Principal issuer = xCert.getIssuerDN();

            if (issuer != null && subject.equals(issuer)) {
                root.add(c);
                anchors.add(new TrustAnchor(xCert, null));
            }
        }

        if (root.size() == 0)
            throw new IllegalArgumentException("No root certificates found for certificate chain", null);

        X509CertSelector target = new X509CertSelector();
        target.setCertificate((X509Certificate)cert);

        PKIXBuilderParameters params = null;
        try {
            params = new PKIXBuilderParameters(anchors, target);
            params.setRevocationEnabled(false);
            params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(certs)));
            CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", "BC");
            builder.build(params);

        } catch (InvalidAlgorithmParameterException e) {
            throw new IllegalArgumentException("Invalid certificate chain", e);
        } catch (CertPathBuilderException e) {
            throw new IllegalArgumentException("Invalid certificate chain", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Invalid certificate chain", e);
        } catch (NoSuchProviderException e) {
            throw new CloudRuntimeException("No provider for certificate validation", e);
        }

    }

    public PrivateKey parsePrivateKey(String key, String password) throws IOException {

        PasswordFinder pGet = null;

        if (password != null)
            pGet = new KeyPassword(password.toCharArray());

        PEMReader privateKey = new PEMReader(new StringReader(key), pGet);
        Object obj = null;
        try {
            obj = privateKey.readObject();
        } finally {
            IOUtils.closeQuietly(privateKey);
        }

        try {

            if (obj instanceof KeyPair)
                return ((KeyPair)obj).getPrivate();

            return (PrivateKey)obj;

        } catch (Exception e) {
            throw new IOException("Invalid Key format or invalid password.", e);
        }
    }

    public Certificate parseCertificate(String cert) {
        PEMReader certPem = new PEMReader(new StringReader(cert));
        try {
            return (Certificate)certPem.readObject();
        } catch (Exception e) {
            throw new InvalidParameterValueException("Invalid Certificate format. Expected X509 certificate");
        } finally {
            IOUtils.closeQuietly(certPem);
        }
    }

    public List<Certificate> parseChain(String chain) throws IOException {

        List<Certificate> certs = new ArrayList<Certificate>();
        PEMReader reader = new PEMReader(new StringReader(chain));

        Certificate crt = null;

        while ((crt = (Certificate)reader.readObject()) != null) {
            if (crt instanceof X509Certificate) {
                certs.add(crt);
            }
        }
        if (certs.size() == 0)
            throw new IllegalArgumentException("Unable to decode certificate chain");

        return certs;
    }

    String generateFingerPrint(Certificate cert) {

        final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        StringBuilder buffer = new StringBuilder(60);
        try {

            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] data = md.digest(cert.getEncoded());

            for (int i = 0; i < data.length; i++) {
                if (buffer.length() > 0) {
                    buffer.append(":");
                }

                buffer.append(HEX[(0xF0 & data[i]) >>> 4]);
                buffer.append(HEX[0x0F & data[i]]);
            }

        } catch (CertificateEncodingException e) {
            throw new InvalidParameterValueException("Bad certificate encoding");
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidParameterValueException("Bad certificate algorithm");
        }

        return buffer.toString();
    }

    public static class KeyPassword implements PasswordFinder {

        boolean password_requested = false;
        char[] password;

        KeyPassword(char[] word) {
            this.password = word;
        }

        @Override
        public char[] getPassword() {
            password_requested = true;
            return password;
        }

        public boolean getPasswordRequested() {
            return password_requested;
        }
    }
}