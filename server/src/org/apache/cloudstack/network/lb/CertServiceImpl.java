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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.command.user.loadbalancer.DeleteSslCertCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListSslCertsCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UploadSslCertCmd;
import org.apache.cloudstack.api.response.SslCertResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import com.cloud.network.lb.CertService;
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

@Local(value = {CertService.class})
public class CertServiceImpl implements CertService {

    private static final Logger s_logger = Logger.getLogger(CertServiceImpl.class);

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
    public SslCertResponse uploadSslCert(UploadSslCertCmd certCmd) {
        try {
            final String cert = certCmd.getCert();
            final String key = certCmd.getKey();
            final String password = certCmd.getPassword();
            final String chain = certCmd.getChain();

            validate(cert, key, password, chain);
            s_logger.debug("Certificate Validation succeeded");

            final String fingerPrint = generateFingerPrint(parseCertificate(cert));

            final CallContext ctx = CallContext.current();
            final Account caller = ctx.getCallingAccount();

            Account owner = null;
            if (certCmd.getAccountName() != null && certCmd.getDomainId() != null || certCmd.getProjectId() != null) {
                owner = _accountMgr.finalizeOwner(caller, certCmd.getAccountName(), certCmd.getDomainId(), certCmd.getProjectId());
            } else {
                owner = caller;
            }

            final Long accountId = owner.getId();
            final Long domainId = owner.getDomainId();

            final SslCertVO certVO = new SslCertVO(cert, key, password, chain, accountId, domainId, fingerPrint);
            _sslCertDao.persist(certVO);

            return createCertResponse(certVO, null);

        } catch (final Exception e) {
            throw new CloudRuntimeException("Error parsing certificate data " + e.getMessage());
        }

    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_LB_CERT_DELETE, eventDescription = "Deleting a certificate to cloudstack", async = false)
    public void deleteSslCert(DeleteSslCertCmd deleteSslCertCmd) {

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
            String lbUuids = "";

            for (final LoadBalancerCertMapVO rule : lbCertRule) {
                final LoadBalancerVO lb = _entityMgr.findById(LoadBalancerVO.class, rule.getLbId());
                lbUuids += " " + lb.getUuid();
            }

            throw new CloudRuntimeException("Certificate in use by a loadbalancer(s)" + lbUuids);
        }

        _sslCertDao.remove(certId);
    }

    @Override
    public List<SslCertResponse> listSslCerts(ListSslCertsCmd listSslCertCmd) {
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
                s_logger.debug("No certificate bound to loadbalancer id: " + lbRuleId);
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

    private void validate(String certInput, String keyInput, String password, String chainInput) {
        Certificate cert;
        PrivateKey key;
        List<Certificate> chain = null;

        try {
            cert = parseCertificate(certInput);
            key = parsePrivateKey(keyInput, password);

            if (chainInput != null) {
                chain = CertificateHelper.parseChain(chainInput);
            }

        } catch (final IOException | CertificateException e) {
            throw new IllegalArgumentException("Parsing certificate/key failed: " + e.getMessage(), e);
        }

        validateCert(cert, chainInput != null ? true : false);
        validateKeys(cert.getPublicKey(), key);

        if (chainInput != null) {
            validateChain(chain, cert);
        }
    }

    public SslCertResponse createCertResponse(SslCertVO cert, List<LoadBalancerCertMapVO> lbCertMap) {
        final SslCertResponse response = new SslCertResponse();

        final Account account = _accountDao.findByIdIncludingRemoved(cert.getAccountId());
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            // find the project
            final Project project = _projectMgr.findByProjectAccountIdIncludingRemoved(account.getId());
            if (project != null)
            {
                response.setProjectId(project.getUuid());
                response.setProjectName(project.getName());
            } else {
                response.setAccountName(account.getAccountName());
            }
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

    private void validateCert(Certificate cert, boolean chainPresent) {

        if (!(cert instanceof X509Certificate)) {
            throw new IllegalArgumentException("Invalid certificate format. Expected X509 certificate");
        }

        try {
            ((X509Certificate)cert).checkValidity();
        } catch (final Exception e) {
            throw new IllegalArgumentException("Certificate expired or not valid", e);
        }
    }

    private void validateKeys(PublicKey pubKey, PrivateKey privKey) {

        if (pubKey.getAlgorithm() != privKey.getAlgorithm()) {
            throw new IllegalArgumentException("Public and private key have different algorithms");
        }

        // No encryption for DSA
        if (pubKey.getAlgorithm() != "RSA") {
            return;
        }

        try {

            final String data = "ENCRYPT_DATA";
            final SecureRandom random = new SecureRandom();
            final Cipher cipher = Cipher.getInstance(pubKey.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, privKey, random);
            final byte[] encryptedData = cipher.doFinal(data.getBytes());

            cipher.init(Cipher.DECRYPT_MODE, pubKey, random);
            final String decreptedData = new String(cipher.doFinal(encryptedData));
            if (!decreptedData.equals(data)) {
                throw new IllegalArgumentException("Bad public-private key");
            }

        } catch (final BadPaddingException e) {
            throw new IllegalArgumentException("Bad public-private key", e);
        } catch (final IllegalBlockSizeException e) {
            throw new IllegalArgumentException("Bad public-private key", e);
        } catch (final NoSuchPaddingException e) {
            throw new IllegalArgumentException("Bad public-private key", e);
        } catch (final InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid public-private key", e);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Invalid algorithm for public-private key", e);
        }
    }

    private void validateChain(List<Certificate> chain, Certificate cert) {

        final List<Certificate> certs = new ArrayList<Certificate>();
        final Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();

        certs.add(cert); // adding for self signed certs
        certs.addAll(chain);

        for (final Certificate c : certs) {
            if (!(c instanceof X509Certificate)) {
                throw new IllegalArgumentException("Invalid chain format. Expected X509 certificate");
            }

            final X509Certificate xCert = (X509Certificate)c;

            xCert.getSubjectDN();
            xCert.getIssuerDN();

           anchors.add(new TrustAnchor(xCert, null));
        }

        final X509CertSelector target = new X509CertSelector();
        target.setCertificate((X509Certificate)cert);

        PKIXBuilderParameters params = null;
        try {
            params = new PKIXBuilderParameters(anchors, target);
            params.setRevocationEnabled(false);
            params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(certs)));
            final CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", "BC");
            builder.build(params);

        } catch (final InvalidAlgorithmParameterException e) {
            throw new IllegalArgumentException("Invalid certificate chain", e);
        } catch (final CertPathBuilderException e) {
            throw new IllegalArgumentException("Invalid certificate chain", e);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Invalid certificate chain", e);
        } catch (final NoSuchProviderException e) {
            throw new CloudRuntimeException("No provider for certificate validation", e);
        }

    }

    public PrivateKey parsePrivateKey(final String key, final String password) throws IOException {
        try (final PemReader pemReader = new PemReader(new StringReader(key));) {
            final PemObject pemObject = pemReader.readPemObject();
            final byte[] content = pemObject.getContent();
            final PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
            final KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
            return factory.generatePrivate(privKeySpec);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new IOException("No encryption provider available.", e);
        } catch (final InvalidKeySpecException e) {
            throw new IOException("Invalid Key format or invalid password.", e);
        }
    }

    public Certificate parseCertificate(String cert) {
        final PemReader certPem = new PemReader(new StringReader(cert));
        try {
            return readCertificateFromPemObject(certPem.readPemObject());
        } catch (final Exception e) {
            throw new InvalidParameterValueException("Invalid Certificate format. Expected X509 certificate. Failed due to " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(certPem);
        }
    }

    private Certificate readCertificateFromPemObject(PemObject pemObject) throws CertificateException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(pemObject.getContent());
        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");

        return certificateFactory.generateCertificate(bais);
    }


    public List<Certificate> parseChain(String chain) throws IOException, CertificateException {

        return CertificateHelper.parseChain(chain);
    }

    String generateFingerPrint(Certificate cert) {

        final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        final StringBuilder buffer = new StringBuilder(60);
        try {

            final MessageDigest md = MessageDigest.getInstance("SHA-1");
            final byte[] data = md.digest(cert.getEncoded());

            for (final byte element : data) {
                if (buffer.length() > 0) {
                    buffer.append(":");
                }

                buffer.append(HEX[(0xF0 & element) >>> 4]);
                buffer.append(HEX[0x0F & element]);
            }

        } catch (final CertificateEncodingException e) {
            throw new InvalidParameterValueException("Bad certificate encoding");
        } catch (final NoSuchAlgorithmException e) {
            throw new InvalidParameterValueException("Bad certificate algorithm");
        }

        return buffer.toString();
    }

    /**
     *
     * @deprecated this is only for bcprov-jdk16
     *
     */
    @Deprecated
    public static class KeyPassword {

        boolean passwordRequested = false;
        char[] password;

        KeyPassword(char[] word) {
            password = word;
        }

        public char[] getPassword() {
            passwordRequested = true;
            return password;
        }

        public boolean getPasswordRequested() {
            return passwordRequested;
        }
    }
}