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
package org.apache.cloudstack.network.ssl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.command.user.loadbalancer.DeleteSslCertCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListSslCertsCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UploadSslCertCmd;
import org.apache.cloudstack.api.response.SslCertResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.tls.CertService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerCertMapVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.SslCertDao;
import com.cloud.network.dao.SslCertVO;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectService;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.security.CertificateHelper;
import com.google.common.base.Preconditions;

public class CertServiceImpl implements CertService {

    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    AccountManager _accountMgr;
    @Inject
    AccountDao _accountDao;
    @Inject
    ProjectService _projectMgr;
    @Inject
    DomainDao _domainDao;
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
    public SslCertResponse uploadSslCert(final UploadSslCertCmd certCmd) {
        Preconditions.checkNotNull(certCmd);

        final String cert = certCmd.getCert();
        final String key = certCmd.getKey();
        final String password = certCmd.getPassword();
        final String chain = certCmd.getChain();
        final String name = certCmd.getName();

        validate(cert, key, password, chain, certCmd.getEnabledRevocationCheck());
        logger.debug("Certificate Validation succeeded");

        final String fingerPrint = CertificateHelper.generateFingerPrint(parseCertificate(cert));

        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();

        Account owner = null;
        if (StringUtils.isNotEmpty(certCmd.getAccountName()) && certCmd.getDomainId() != null || certCmd.getProjectId() != null) {
            owner = _accountMgr.finalizeOwner(caller, certCmd.getAccountName(), certCmd.getDomainId(), certCmd.getProjectId());
        } else {
            owner = caller;
        }

        final Long accountId = owner.getId();
        final Long domainId = owner.getDomainId();

        final SslCertVO certVO = new SslCertVO(cert, key, password, chain, accountId, domainId, fingerPrint, name);
        _sslCertDao.persist(certVO);

        return createCertResponse(certVO, null);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LB_CERT_DELETE, eventDescription = "Deleting a certificate to cloudstack", async = false)
    public void deleteSslCert(final DeleteSslCertCmd deleteSslCertCmd) {
        Preconditions.checkNotNull(deleteSslCertCmd);

        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();

        final Long certId = deleteSslCertCmd.getId();
        final SslCertVO certVO = _sslCertDao.findById(certId);

        if (certVO == null) {
            throw new InvalidParameterValueException("Invalid certificate id: " + certId);
        }
        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.OperateEntry, true, certVO);

        final List<LoadBalancerCertMapVO> lbCertRule = _lbCertDao.listByCertId(certId);

        if (lbCertRule != null && !lbCertRule.isEmpty()) {
            StringBuilder lbNames = new StringBuilder();

            for (final LoadBalancerCertMapVO rule : lbCertRule) {
                final LoadBalancerVO lb = _entityMgr.findById(LoadBalancerVO.class, rule.getLbId());
                lbNames.append(lb.getName()).append(" ");
            }

            throw new CloudRuntimeException("Certificate in use by a loadbalancer(s) " + lbNames.toString());
        }

        _sslCertDao.remove(certId);
    }

    @Override
    public List<SslCertResponse> listSslCerts(final ListSslCertsCmd listSslCertCmd) {
        Preconditions.checkNotNull(listSslCertCmd);

        final CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();

        final Long certId = listSslCertCmd.getCertId();
        final Long accountId = listSslCertCmd.getAccountId();
        final Long lbRuleId = listSslCertCmd.getLbId();
        final Long projectId = listSslCertCmd.getProjectId();

        final List<SslCertResponse> certResponseList = new ArrayList<SslCertResponse>();

        if (certId == null && accountId == null && lbRuleId == null && projectId == null) {
            throw new InvalidParameterValueException("Invalid parameters either certificate ID or Account ID or Loadbalancer ID or Project ID required");
        }

        List<LoadBalancerCertMapVO> certLbMap = null;
        SslCertVO certVO = null;

        if (certId != null) {
            certVO = _sslCertDao.findById(certId);

            if (certVO == null) {
                throw new InvalidParameterValueException("Invalid certificate id: " + certId);
            }

            _accountMgr.checkAccess(caller, SecurityChecker.AccessType.UseEntry, true, certVO);

            certLbMap = _lbCertDao.listByCertId(certId);

            certResponseList.add(createCertResponse(certVO, certLbMap));
            return certResponseList;
        }

        if (lbRuleId != null) {
            final LoadBalancer lb = _entityMgr.findById(LoadBalancerVO.class, lbRuleId);

            if (lb == null) {
                throw new InvalidParameterValueException("Found no loadbalancer with id: " + lbRuleId);
            }

            _accountMgr.checkAccess(caller, SecurityChecker.AccessType.UseEntry, true, lb);

            // get the cert id
            LoadBalancerCertMapVO lbCertMapRule;
            lbCertMapRule = _lbCertDao.findByLbRuleId(lbRuleId);

            if (lbCertMapRule == null) {
                logger.debug("No certificate bound to loadbalancer id: " + lbRuleId);
                return certResponseList;
            }

            certVO = _sslCertDao.findById(lbCertMapRule.getCertId());
            certLbMap = _lbCertDao.listByCertId(lbCertMapRule.getCertId());

            certResponseList.add(createCertResponse(certVO, certLbMap));
            return certResponseList;

        }

        if (projectId != null) {
            final Project project = _projectMgr.getProject(projectId);

            if (project == null) {
                throw new InvalidParameterValueException("Found no project with id: " + projectId);
            }

            final List<SslCertVO> projectCertVOList = _sslCertDao.listByAccountId(project.getProjectAccountId());
            if (projectCertVOList == null || projectCertVOList.isEmpty()) {
                return certResponseList;
            }
            _accountMgr.checkAccess(caller, SecurityChecker.AccessType.UseEntry, true, projectCertVOList.get(0));

            for (final SslCertVO cert : projectCertVOList) {
                certLbMap = _lbCertDao.listByCertId(cert.getId());
                certResponseList.add(createCertResponse(cert, certLbMap));
            }
            return certResponseList;
        }

        //reached here look by accountId
        final List<SslCertVO> certVOList = _sslCertDao.listByAccountId(accountId);
        if (certVOList == null || certVOList.isEmpty()) {
            return certResponseList;
        }
        _accountMgr.checkAccess(caller, SecurityChecker.AccessType.UseEntry, true, certVOList.get(0));

        for (final SslCertVO cert : certVOList) {
            certLbMap = _lbCertDao.listByCertId(cert.getId());
            certResponseList.add(createCertResponse(cert, certLbMap));
        }
        return certResponseList;
    }

    protected void validate(final String certInput, final String keyInput, final String password, final String chainInput, boolean revocationEnabled) {
        try {
            List<Certificate> chain = null;
            final Certificate cert = parseCertificate(certInput);
            final PrivateKey key = parsePrivateKey(keyInput, password);

            if (chainInput != null) {
                chain = CertificateHelper.parseChain(chainInput);
            }

            validateCert(cert);
            validateKeys(cert.getPublicKey(), key);

            if (chainInput != null) {
                validateChain(chain, cert, revocationEnabled);
            }
        } catch (final IOException | CertificateException | OperatorCreationException | PKCSException |
                       NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.warn("Failed to validate certificate", e);
            throw new IllegalStateException("Parsing certificate/key failed: " + e.getMessage(), e);
        }
    }

    public SslCertResponse createCertResponse(final SslCertVO cert, final List<LoadBalancerCertMapVO> lbCertMap) {
        Preconditions.checkNotNull(cert);

        final SslCertResponse response = new SslCertResponse();
        final Account account = _accountDao.findByIdIncludingRemoved(cert.getAccountId());
        if (account.getType() == Account.Type.PROJECT) {
            // find the project
            final Project project = _projectMgr.findByProjectAccountIdIncludingRemoved(account.getId());
            if (project != null)
            {
                response.setProjectId(project.getUuid());
                response.setProjectName(project.getName());
            }
            response.setAccountName(account.getAccountName());
        } else {
            response.setAccountName(account.getAccountName());
        }

        final DomainVO domain = _domainDao.findByIdIncludingRemoved(cert.getDomainId());
        response.setDomainId(domain.getUuid());
        response.setDomainName(domain.getName());

        response.setObjectName("sslcert");
        response.setId(cert.getUuid());
        response.setCertificate(cert.getCertificate());
        response.setFingerprint(cert.getFingerPrint());
        response.setName(cert.getName());

        if (cert.getChain() != null) {
            response.setCertchain(cert.getChain());
        }

        if (lbCertMap != null && !lbCertMap.isEmpty()) {
            final List<String> lbIds = new ArrayList<String>();
            for (final LoadBalancerCertMapVO mapVO : lbCertMap) {
                final LoadBalancer lb = _entityMgr.findById(LoadBalancerVO.class, mapVO.getLbId());
                if (lb != null) {
                    lbIds.add(lb.getUuid());
                }
            }
            response.setLbIds(lbIds);
        }

        return response;
    }

    private void validateCert(final Certificate cert) throws CertificateNotYetValidException, CertificateExpiredException {
        Preconditions.checkNotNull(cert);

        if (!(cert instanceof X509Certificate)) {
            throw new IllegalArgumentException("Invalid certificate format. Expected X509 certificate");
        }
        ((X509Certificate)cert).checkValidity();
    }

    private void validateKeys(final PublicKey pubKey, final PrivateKey privKey) {
        Preconditions.checkNotNull(pubKey);
        Preconditions.checkNotNull(privKey);

        if (!pubKey.getAlgorithm().equals(privKey.getAlgorithm())) {
            throw new IllegalArgumentException("Public and private key have different algorithms");
        }

        // No encryption for DSA
        if (pubKey.getAlgorithm() != "RSA") {
            return;
        }

        try {
            final String data = "ENCRYPT_DATA";
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privKey);
            sig.update(data.getBytes());
            byte[] signature = sig.sign();

            sig.initVerify(pubKey);
            sig.update(data.getBytes());
            if (!sig.verify(signature)) {
                throw new IllegalStateException("Bad public-private key");
            }
        } catch (final InvalidKeyException | SignatureException e) {
            throw new IllegalStateException("Bad public-private key", e);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Invalid algorithm for public-private key", e);
        }
    }

    private void validateChain(final List<Certificate> chain, final Certificate cert, boolean revocationEnabled) {

        final List<Certificate> certs = new ArrayList<Certificate>();
        final Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();

        certs.add(cert); // adding for self signed certs
        certs.addAll(chain);

        for (final Certificate c : certs) {
            if (!(c instanceof X509Certificate)) {
                throw new IllegalArgumentException("Invalid chain format. Expected X509 certificate");
            }
            final X509Certificate xCert = (X509Certificate)c;
            anchors.add(new TrustAnchor(xCert, null));
        }

        final X509CertSelector target = new X509CertSelector();
        target.setCertificate((X509Certificate)cert);

        PKIXBuilderParameters params = null;
        try {
            params = new PKIXBuilderParameters(anchors, target);
            params.setRevocationEnabled(revocationEnabled);
            params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(certs)));
            final CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", "BC");
            builder.build(params);

        } catch (final InvalidAlgorithmParameterException | CertPathBuilderException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Invalid certificate chain", e);
        } catch (final NoSuchProviderException e) {
            throw new CloudRuntimeException("No provider for certificate validation", e);
        }

    }

    public PrivateKey parsePrivateKey(final String key, String password) throws IOException, OperatorCreationException, PKCSException, NoSuchAlgorithmException, InvalidKeySpecException {
        Preconditions.checkArgument(StringUtils.isNotEmpty(key));
        PEMParser pemParser = new PEMParser(new StringReader(key));
        Object privateKeyObj = pemParser.readObject();
        if (privateKeyObj == null) {
            throw new CloudRuntimeException("Cannot parse private key");
        }
        PrivateKey privateKey;
        if (privateKeyObj instanceof PKCS8EncryptedPrivateKeyInfo) {
            privateKey = parsePKCS8EncryptedPrivateKeyInfo((PKCS8EncryptedPrivateKeyInfo)privateKeyObj, password);
        } else if (privateKeyObj instanceof PEMEncryptedKeyPair) {
            privateKey = parsePEMEncryptedKeyPair((PEMEncryptedKeyPair)privateKeyObj, password);
        } else if (privateKeyObj instanceof PEMKeyPair) {
            // Key pair
            PEMKeyPair pemKeyPair = (PEMKeyPair) privateKeyObj;
            privateKey = new JcaPEMKeyConverter().getKeyPair(pemKeyPair).getPrivate();
        } else if (privateKeyObj instanceof PrivateKeyInfo) {
            // Private key only
            PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) privateKeyObj;
            privateKey = new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);
        } else {
            throw new IllegalArgumentException("Unsupported PEM object: " + privateKeyObj.getClass());
        }
        pemParser.close();
        return privateKey;
    }

    private PrivateKey parsePKCS8EncryptedPrivateKeyInfo(PKCS8EncryptedPrivateKeyInfo privateKeyObj, String password)
            throws IOException, OperatorCreationException, PKCSException, NoSuchAlgorithmException, InvalidKeySpecException {
        if (password == null) {
            throw new CloudRuntimeException("Key is encrypted by PKCS#8 but password is null");
        }
        PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo)privateKeyObj;
        JceOpenSSLPKCS8DecryptorProviderBuilder builder = new JceOpenSSLPKCS8DecryptorProviderBuilder();
        InputDecryptorProvider decryptor = builder.build(password.toCharArray());

        PrivateKeyInfo privateKeyInfo = encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptor);
        String algorithm = privateKeyInfo.getPrivateKeyAlgorithm().getAlgorithm().getId();
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded());
        return keyFactory.generatePrivate(keySpec);
    }

    private PrivateKey parsePEMEncryptedKeyPair(PEMEncryptedKeyPair encryptedKeyPair, String password) throws IOException {
        if (password == null) {
            throw new CloudRuntimeException("Key is encrypted but password is null");
        }
        return new JcaPEMKeyConverter().getKeyPair(
                encryptedKeyPair.decryptKeyPair(new JcePEMDecryptorProviderBuilder().build(password.toCharArray()))).getPrivate();
    }

    @Override
    public Certificate parseCertificate(final String cert) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(cert));
        final PemReader certPem = new PemReader(new StringReader(cert));
        try {
            return readCertificateFromPemObject(certPem.readPemObject());
        } catch (final CertificateException | IOException e) {
            throw new InvalidParameterValueException("Invalid Certificate format. Expected X509 certificate. Failed due to " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(certPem);
        }
    }

    private Certificate readCertificateFromPemObject(final PemObject pemObject) throws CertificateException {
        Preconditions.checkNotNull(pemObject);
        final ByteArrayInputStream bais = new ByteArrayInputStream(pemObject.getContent());
        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");

        return certificateFactory.generateCertificate(bais);
    }
}
