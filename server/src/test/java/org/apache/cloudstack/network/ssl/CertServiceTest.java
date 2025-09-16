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

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerCertMapVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.SslCertDao;
import com.cloud.network.dao.SslCertVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.api.command.user.loadbalancer.DeleteSslCertCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UploadSslCertCmd;
import org.apache.cloudstack.context.CallContext;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

public class CertServiceTest {

    @Before
    public void setUp() {
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        final UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    /**
     * JCE is known to be working fine without additional configuration in OpenJDK.
     * This checks if the tests are running in OpenJDK;
     * @return  true if openjdk environment
     */
    static boolean isOpenJdk() {
        //TODO: find a better way for OpenJDK detection
        return System.getProperty("java.home").toLowerCase().contains("openjdk");
    }

    /**
     * One can run the tests on Oracle JDK after installing JCE by specifying -Dcloudstack.jce.enabled=true
     * @return true if the jce enable property was set to true
     */
    static boolean isJCEInstalled() {
        return Boolean.getBoolean("cloudstack.jce.enabled");
    }

    @Test
    /**
     * Given a certificate signed by a CA and a valid CA chain, upload should succeed
     */
    public void runUploadSslCertWithCAChain() throws Exception {
        Assume.assumeTrue(isOpenJdk() || isJCEInstalled());

        TransactionLegacy.open("runUploadSslCertWithCAChain");

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.crt").getFile(),Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.key").getFile(),Charset.defaultCharset().name());
        final String chainFile = URLDecoder.decode(getClass().getResource("/certs/root_chain.crt").getFile(),Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));
        final String chain = readFileToString(new File(chainFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(ArgumentMatchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

        certService._accountDao = Mockito.mock(AccountDao.class);
        when(certService._accountDao.findByIdIncludingRemoved(anyLong())).thenReturn((AccountVO)account);

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> klazz = uploadCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = klazz.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        final Field chainField = klazz.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        final Field enabledRevocationCheckField = klazz.getDeclaredField("enabledRevocationCheck");
        enabledRevocationCheckField.setAccessible(true);
        enabledRevocationCheckField.set(uploadCmd, Boolean.FALSE);

        certService.uploadSslCert(uploadCmd);
    }

    @Test
    /**
     * Given a certificate signed by a CA and a valid CA chain, but without any info for revocation checking, upload should fail.
     */
    public void runUploadSslCertWithNoRevocationInfo() throws Exception {
        Assume.assumeTrue(isOpenJdk() || isJCEInstalled());

        TransactionLegacy.open("runUploadSslCertWithCAChain");

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.crt").getFile(),Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.key").getFile(),Charset.defaultCharset().name());
        final String chainFile = URLDecoder.decode(getClass().getResource("/certs/root_chain.crt").getFile(),Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));
        final String chain = readFileToString(new File(chainFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(ArgumentMatchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

        certService._accountDao = Mockito.mock(AccountDao.class);
        when(certService._accountDao.findByIdIncludingRemoved(anyLong())).thenReturn((AccountVO)account);

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> klazz = uploadCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = klazz.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        final Field chainField = klazz.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        try {
            certService.uploadSslCert(uploadCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Invalid certificate chain"));
            Throwable cause = e.getCause();
            Assert.assertTrue(cause.getMessage().contains("Certification path could not be validated"));
            cause = cause.getCause();
            Assert.assertTrue(cause.getMessage().contains("No CRLs found"));
        }
    }

    @Test
    /**
     * Given a Self-signed Certificate with encrypted key, upload should succeed
     */
    public void runUploadSslCertSelfSignedWithPassword() throws Exception {

        TransactionLegacy.open("runUploadSslCertSelfSignedWithPassword");

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed_with_pwd.crt").getFile(),Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed_with_pwd.key").getFile(),Charset.defaultCharset().name());
        final String password = "test";

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(ArgumentMatchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

        certService._accountDao = Mockito.mock(AccountDao.class);
        when(certService._accountDao.findByIdIncludingRemoved(anyLong())).thenReturn((AccountVO)account);

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> klazz = uploadCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = klazz.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        final Field passField = klazz.getDeclaredField("password");
        passField.setAccessible(true);
        passField.set(uploadCmd, password);

        certService.uploadSslCert(uploadCmd);
    }

    @Test
    /**
     * Given a Self-signed Certificate with non-encrypted key, upload should succeed
     */
    public void runUploadSslCertSelfSignedNoPassword() throws Exception {

        TransactionLegacy.open("runUploadSslCertSelfSignedNoPassword");

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.crt").getFile(),Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.key").getFile(),Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(ArgumentMatchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

        certService._accountDao = Mockito.mock(AccountDao.class);
        when(certService._accountDao.findByIdIncludingRemoved(anyLong())).thenReturn((AccountVO)account);

        //creating the command
        UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> klazz = uploadCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = klazz.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        uploadCmd = Mockito.spy(uploadCmd);
        certService.uploadSslCert(uploadCmd);
        Mockito.verify(uploadCmd, Mockito.atLeastOnce()).getAccountName();
        Mockito.verify(uploadCmd, Mockito.times(1)).getCert();
    }

    @Test
    public void runUploadSslCertBadChain() throws IOException, IllegalAccessException, NoSuchFieldException {
        Assume.assumeTrue(isOpenJdk() || isJCEInstalled());

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.crt").getFile(),Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.key").getFile(),Charset.defaultCharset().name());
        final String chainFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.crt").getFile(),Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));
        final String chain = readFileToString(new File(chainFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(ArgumentMatchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> klazz = uploadCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = klazz.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        final Field chainField = klazz.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        try {
            certService.uploadSslCert(uploadCmd);
            Assert.fail("The chain given is not the correct chain for the certificate");
        } catch (final Exception e) {
            Assert.assertTrue(e.getMessage().contains("Invalid certificate chain"));
        }
    }


    @Test
    public void runUploadSslCertNoRootCert() throws IOException, IllegalAccessException, NoSuchFieldException {

        Assume.assumeTrue(isOpenJdk() || isJCEInstalled());

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.crt").getFile(),Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.key").getFile(),Charset.defaultCharset().name());
        final String chainFile = URLDecoder.decode(getClass().getResource("/certs/non_root.crt").getFile(),Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));
        final String chain = readFileToString(new File(chainFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(ArgumentMatchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> klazz = uploadCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = klazz.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        final Field chainField = klazz.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        try {
            certService.uploadSslCert(uploadCmd);
            Assert.fail("Chain is given but does not link to the certificate");
        } catch (final Exception e) {
            Assert.assertTrue(e.getMessage().contains("Invalid certificate chain"));
        }

    }


    @Test
    public void runUploadSslCertBadPassword() throws IOException, IllegalAccessException, NoSuchFieldException {

        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed_with_pwd.crt").getFile(),Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed_with_pwd.key").getFile(),Charset.defaultCharset().name());
        final String password = "bad_password";

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(ArgumentMatchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> klazz = uploadCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = klazz.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        final Field passField = klazz.getDeclaredField("password");
        passField.setAccessible(true);
        passField.set(uploadCmd, password);

        try {
            certService.uploadSslCert(uploadCmd);
            Assert.fail("Given an encrypted private key with a bad password. Upload should fail.");
        } catch (final Exception e) {
            Assert.assertTrue("Did not expect message: " + e.getMessage(),
                    e.getMessage().contains("Parsing certificate/key failed: exception using cipher - please check password and data."));
        }

    }

    @Test
    public void runUploadSslCertBadkeyPair() throws IOException, IllegalAccessException, NoSuchFieldException {
        // Reading appropritate files
        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.crt").getFile(),Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/non_root.key").getFile(),Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(ArgumentMatchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> klazz = uploadCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = klazz.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
        } catch (final Exception e) {
            Assert.assertTrue(e.getMessage().contains("Bad public-private key"));
        }
    }

    @Test
    public void runUploadSslCertBadkeyAlgo() throws IOException, IllegalAccessException, NoSuchFieldException {

        // Reading appropritate files
        final String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.crt").getFile(),Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/dsa_self_signed.key").getFile(),Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(ArgumentMatchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> klazz = uploadCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = klazz.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
            Assert.fail("Given a private key which has a different algorithm than the certificate, upload should fail");
        } catch (final Exception e) {
            Assert.assertTrue("Did not expect message: " + e.getMessage(),
                    e.getMessage().contains("Public and private key have different algorithms"));
        }
    }

    @Test
    public void runUploadSslCertExpiredCert() throws IOException, IllegalAccessException, NoSuchFieldException {

        // Reading appropritate files
        final String certFile = URLDecoder.decode(getClass().getResource("/certs/expired_cert.crt").getFile(),Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.key").getFile(),Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(ArgumentMatchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> klazz = uploadCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = klazz.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
            Assert.fail("Given an expired certificate, upload should fail");
        } catch (final Exception e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(e.getMessage().contains("Parsing certificate/key failed: NotAfter:"));
        }
    }

    @Test
    public void runUploadSslCertNotX509() throws IOException, IllegalAccessException, NoSuchFieldException {
        // Reading appropritate files
        final String certFile = URLDecoder.decode(getClass().getResource("/certs/non_x509_pem.crt").getFile(),Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.key").getFile(),Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(ArgumentMatchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> klazz = uploadCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = klazz.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
            Assert.fail("Given a Certificate which is not X509, upload should fail");
        } catch (final Exception e) {
            Assert.assertTrue(e.getMessage().contains("Expected X509 certificate"));
        }
    }

    @Test(expected = NullPointerException.class)
    public void runUploadSslCertBadFormat() throws IOException, IllegalAccessException, NoSuchFieldException {

        // Reading appropritate files
        final String certFile = URLDecoder.decode(getClass().getResource("/certs/bad_format_cert.crt").getFile(),Charset.defaultCharset().name());
        final String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.key").getFile(),Charset.defaultCharset().name());

        final String cert = readFileToString(new File(certFile));
        final String key = readFileToString(new File(keyFile));

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(ArgumentMatchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        final UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        final Class<?> klazz = uploadCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        final Field keyField = klazz.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        certService.uploadSslCert(uploadCmd);
        Assert.fail("Given a Certificate in bad format (Not PEM), upload should fail");
    }

    @Test
    /**
     * Delete with a valid Id should succeed
     */
    public void runDeleteSslCertValid() throws Exception {

        TransactionLegacy.open("runDeleteSslCertValid");

        final CertServiceImpl certService = new CertServiceImpl();
        final long certId = 1;

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.remove(anyLong())).thenReturn(true);
        when(certService._sslCertDao.findById(anyLong())).thenReturn(new SslCertVO());

        // a rule holding the cert

        certService._lbCertDao = Mockito.mock(LoadBalancerCertMapDao.class);
        when(certService._lbCertDao.listByCertId(anyLong())).thenReturn(null);

        //creating the command
        final DeleteSslCertCmd deleteCmd = new DeleteSslCertCmdExtn();
        final Class<?> klazz = deleteCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("id");
        certField.setAccessible(true);
        certField.set(deleteCmd, certId);

        certService.deleteSslCert(deleteCmd);
    }

    @Test
    public void runDeleteSslCertBoundCert() throws NoSuchFieldException, IllegalAccessException {

        TransactionLegacy.open("runDeleteSslCertBoundCert");

        final CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        final long certId = 1;

        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.remove(anyLong())).thenReturn(true);
        when(certService._sslCertDao.findById(anyLong())).thenReturn(new SslCertVO());

        // rule holding the cert
        certService._lbCertDao = Mockito.mock(LoadBalancerCertMapDao.class);

        final List<LoadBalancerCertMapVO> lbMapList = new ArrayList<>();
        lbMapList.add(new LoadBalancerCertMapVO());

        certService._lbCertDao = Mockito.mock(LoadBalancerCertMapDao.class);
        when(certService._lbCertDao.listByCertId(anyLong())).thenReturn(lbMapList);


        certService._entityMgr = Mockito.mock(EntityManager.class);
        when(certService._entityMgr.findById(eq(LoadBalancerVO.class), nullable(Long.class))).thenReturn(new LoadBalancerVO());

        //creating the command
        final DeleteSslCertCmd deleteCmd = new DeleteSslCertCmdExtn();
        final Class<?> klazz = deleteCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("id");
        certField.setAccessible(true);
        certField.set(deleteCmd, certId);

        try {
            certService.deleteSslCert(deleteCmd);
            Assert.fail("Delete with a cert id bound to a lb should fail");
        } catch (final Exception e) {
            Assert.assertTrue(e.getMessage().contains("Certificate in use by a loadbalancer"));
        }

    }

    @Test
    public void runDeleteSslCertInvalidId() throws NoSuchFieldException, IllegalAccessException {
        TransactionLegacy.open("runDeleteSslCertInvalidId");

        final long certId = 1;
        final CertServiceImpl certService = new CertServiceImpl();

        certService._accountMgr = Mockito.mock(AccountManager.class);
        final Account account = new AccountVO("testaccount", 1, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        final DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.remove(anyLong())).thenReturn(true);
        when(certService._sslCertDao.findById(anyLong())).thenReturn(null);

        // no rule holding the cert
        certService._lbCertDao = Mockito.mock(LoadBalancerCertMapDao.class);
        when(certService._lbCertDao.listByCertId(anyLong())).thenReturn(null);

        //creating the command
        final DeleteSslCertCmd deleteCmd = new DeleteSslCertCmdExtn();
        final Class<?> klazz = deleteCmd.getClass().getSuperclass();

        final Field certField = klazz.getDeclaredField("id");
        certField.setAccessible(true);
        certField.set(deleteCmd, certId);

        try {
            certService.deleteSslCert(deleteCmd);
            Assert.fail("Delete with an invalid ID should fail");
        } catch (final Exception e) {
            Assert.assertTrue(e.getMessage().contains("Invalid certificate id"));
        }

    }

    public class UploadSslCertCmdExtn extends UploadSslCertCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }

    public class DeleteSslCertCmdExtn extends DeleteSslCertCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }

    private String generateEncryptedPrivateKey(String password) throws NoSuchAlgorithmException, OperatorCreationException, IOException {
        // Generate RSA key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        // Build encryptor (AES-256-CBC is FIPS-approved)
        OutputEncryptor encryptor = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
                .setPassword(password.toCharArray())
                .build();

        // Wrap the private key into PKCS#8 format and encrypt
        JcaPKCS8Generator gen = new JcaPKCS8Generator(keyPair.getPrivate(), encryptor);
        PemObject pemObject = gen.generate();

        StringWriter stringWriter = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(stringWriter)) {
            pemWriter.writeObject(pemObject);
        }
        return stringWriter.toString();
    }

    @Test
    public void parseEncryptedPrivateKey() throws Exception{
        String password = "strongpassword";
        String key = generateEncryptedPrivateKey(password);
        final CertServiceImpl certService = new CertServiceImpl();
        certService.parsePrivateKey(key, password);
    }

    @Test
    public void validateCertAndChainsWithEncryptedKey() {
        String password = "strongpassword";
        String key = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n" +
                "MIIFGzBVBgkqhkiG9w0BBQ0wSDAnBgkqhkiG9w0BBQwwGgQUiQiFcfHTx8EKYNHJ\n" +
                "zOqT8/9AkaQCAggAMB0GCWCGSAFlAwQBKgQQKXBglXgHYSWK20BxSFUVLQSCBMBr\n" +
                "ro2dXjsEoZfglccP5YWRPETSXntMdjAd39ftiWSXwQWZmht9/t+hSK+qZnGX/8VI\n" +
                "0OR7x+8SBDqZAb9mYZzPPcUd/k+KLpQAFBSFrWVle40MY1OyZqEdQe3ELDERS919\n" +
                "WRGmjTYUomL1zCAIrx27Woq5iiZkqsXmCcQwKRkCSNbTXjDe6gXtO9ePuMgvSiGg\n" +
                "q2rhBZv82AYoc/IHzftsoS53Sda96RE93MK12+L48E5gxbqeHUJeGhn1hxxkqFcj\n" +
                "cL/z817M6a9BEJkNlS4sZk3+Fg1RYBTx7CKYzR8WAf+LvasdO5ijPrNcqc6DzIIn\n" +
                "tL0Kj/Gjp6rFP83IfezCtVdYi/dRLR9dNROJt7aIaeXnYdYF8o+vmWZm5H4bZeun\n" +
                "czadKzd4EfvatHXi7Zq/cV/mh/NitUfnYMR5LUnX9pjNRkr2uqYx5AiO6aPQoR9G\n" +
                "Gv1ubkUtug/rDoywwol7XGWxnDNbB4fvXRIGsyZYDh9J1CX+sv693ZeRx1J48vhT\n" +
                "s+gZug8oG5DfSLCVaJDuIyHQGKuRLnh6LawUkyCA7Q/9vgmXnXo+0hJ5dYQw21fj\n" +
                "M5yHrOt/tway5tJgDwuD778r3Y4w1H9Yt42J3tZL3gOIOyYhHad2M/emh5Khh/m/\n" +
                "VK8eM86OQeo/zp+RddM4ckaUxKe/bFBqj9KvhzHsFTAuirT7be3+Ye1iBqKLvCgO\n" +
                "yOTY14J1NbrvGmUs6yq3JxTkzl4+A23SPlHQE16j3UzCz0qnNTYLruUibL940uXu\n" +
                "rnBcOuW6uM4yc+X3Aqo7xL3kzW/9waCd/VG/btJLNPKSDDRuuKQ7NEPjS+xajmqh\n" +
                "WVMzzcMj4wVvTz6vnZNm9u9Yu/ACpzHTD+hVeFZhIscVCdT+LncEumLHHhrLQS3h\n" +
                "9gVlv0MvSrWH6sl3oQEnA5ceEI4LfH6eT++IGAdKJTqkpAwSEtSEV+P/dETRNnsH\n" +
                "TsKNEdylH++9Ljhkt68971cLGHf9yuzVU75BPFybngcNFZu3+YUDWY0fBwqwE0OI\n" +
                "FXeqPhnN2UfAoeqCwz2KtPf2ig0a34S6Rxne9/XewlCsKEGSrdYG8mm4eJzsP69/\n" +
                "5qw1MDO1nvt0B5jSly3vHcHGvgiDtG+vsfGqC1TA8eaTSq/UkUAKfoGg1DkL8olz\n" +
                "b7jB24748Oh87Ksz12yeyY5T1edpoDcScCRLwIb0vNMKqIUe1aCEdTl08UHV3CbG\n" +
                "7rnRLWE+9/Csij2fpkx0mEDeXdLxeSvkw5K8ha26s52MR4WhW0EUN74FJOMrTej3\n" +
                "0jtcTC/bThc5jmQDaSQJbaiSIEKl8sdA0u8oTzBD2B1F9gkrZNZpE7hz670tysQs\n" +
                "2Z0AxDcxQ7Qfkytg52MfJvLf0jxuNqjfbmQqkQsT+yUkjT6AmOgUMGP4zojP8ErY\n" +
                "AvAqgurefHMS/HA8BUT7qxt300cTYaAONUlAJ/qAJ/YoHOI5yqWzBFJsr95NC13t\n" +
                "rGqiOOLGtSIxk4WwdUX0u9TW8Hk6pWnl6MkyAn+a3RqKfrJ2tfKMjsO3iqu3Dlvz\n" +
                "72RD5LsGcnhfKQ/TdswEA1EKdHBBjnDQOGdWNNTXnn41XoNNKneFjlFgJc8AXyoN\n" +
                "fHvkc2aKb86WdpcANxK3\n" +
                "-----END ENCRYPTED PRIVATE KEY-----";
        String certificate = "-----BEGIN CERTIFICATE-----\n" +
                "MIIERTCCAi0CFF2Ro8QjYcOCALfEqL1zs2T0cQzyMA0GCSqGSIb3DQEBCwUAMF4x\n" +
                "CzAJBgNVBAYTAlhYMQswCQYDVQQIDAJYWDELMAkGA1UEBwwCWFgxEzARBgNVBAoM\n" +
                "CkNsb3VkU3RhY2sxDzANBgNVBAsMBkFwYWNoZTEPMA0GA1UEAwwGQXBhY2hlMCAX\n" +
                "DTI1MDYxNzA3MzQxOFoYDzIxMjUwNTI0MDczNDE4WjBeMQswCQYDVQQGEwJYWDEL\n" +
                "MAkGA1UECAwCWFgxCzAJBgNVBAcMAlhYMRMwEQYDVQQKDApDbG91ZFN0YWNrMQ8w\n" +
                "DQYDVQQLDAZBcGFjaGUxDzANBgNVBAMMBkFwYWNoZTCCASIwDQYJKoZIhvcNAQEB\n" +
                "BQADggEPADCCAQoCggEBAMXpfAyO1m+YolspmNL64cMJ0mW4QiJUrrNxYyIaakfW\n" +
                "/qs78hMlf8V82T94ayoMs2fpkjf69QsXTZoOZoUkaz58Wz9Z860OMAD/wguGz7EX\n" +
                "Bk+OTEDhXP9NAkY99TqscWS3bm6XSu3w0cOwjwLtV72VsT2UA1d0hpVI4kVTbI56\n" +
                "RZ1ymboyu/mhp2dqZu+Ewh8n7PMYvDO6hGuqsM5We2WLdSCmPZKtmbQ8CRj0fwJI\n" +
                "CZZEafFEBwLhW3F15SRZLxQApzqMTlmbk9edEgOfJZqMrr+F8jguce7Qry6FcbkU\n" +
                "6x4oRyykuz5pi5mPjaTxQyY4NWsCHojlQ0kz0VeBUX0CAwEAATANBgkqhkiG9w0B\n" +
                "AQsFAAOCAgEAJAUldK70IoyA0jokXfAyLXNRX43/UfmQMu3xvVYI9OPk8f6CrBIm\n" +
                "g79cA3pGPNxyIReqFxDk+wXW+/iPCgOwv+YYODPEMZi1Cc8WQJ4OGzovD5hep7TA\n" +
                "pg6jo16LdKpOQM6C9XUce3vZf6t487PCgg8SzldqhMMC97Kw+DAxYg+JRd28jfIB\n" +
                "RAtpOCzqKqWp7lQ1YwS9M/VI0mYtmiuQbaz1to4qBPcCbR1GsLsmqMmTUkbYYyFF\n" +
                "fgvInITyW+0NV/UwgiNFxU+k9T2H1lfvqj6hVRwwj7i84xAu4Y/N9zP/UKXxU93N\n" +
                "ogoHabfGcsFEygyTkFuI4XG/Ppc3c8CJV2NbVQixe5Wdt1Yc9qMkbq+OdGvsOhbt\n" +
                "T2+Qz5JZ7w0LsYONzuCRbaDpJiAg2MiALe3L1RzEya57/PylgUeH6gMbPyuQ2EyL\n" +
                "pTUQ1imV3tTlkxjy7niu/IeqgcQOA2cx8Fwok+ECLvxc47noUlgPcROz5i43+IYA\n" +
                "frvGqDfZCeKXKuAi//8wBl2tptMMmLpkS4mW/8Pijcx3JuxC6ySeOFAVgPjq4krw\n" +
                "dGl+IBNwKNcsUu5/3uj/2h85w56Ys8uxeLkLqEq+9yHlwxexGJG0qJ2QcXFnOxCC\n" +
                "qz+L2k3m0+Yu5zUFsMCTgEwQeR6CUfW9/GtPunZtvwHOSbVus0DvnSE=\n" +
                "-----END CERTIFICATE-----";
        String certChains = "-----BEGIN CERTIFICATE-----\n" +
                "MIIFQzCCAysCFEVQffqr0ScjpyZ6pmDsOOu71t70MA0GCSqGSIb3DQEBCwUAMF4x\n" +
                "CzAJBgNVBAYTAlhYMQswCQYDVQQIDAJYWDELMAkGA1UEBwwCWFgxEzARBgNVBAoM\n" +
                "CkNsb3VkU3RhY2sxDzANBgNVBAsMBkFwYWNoZTEPMA0GA1UEAwwGQXBhY2hlMB4X\n" +
                "DTI1MDYxNjEwMjc1NloXDTMwMDYxNTEwMjc1NlowXjELMAkGA1UEBhMCWFgxCzAJ\n" +
                "BgNVBAgMAlhYMQswCQYDVQQHDAJYWDETMBEGA1UECgwKQ2xvdWRTdGFjazEPMA0G\n" +
                "A1UECwwGQXBhY2hlMQ8wDQYDVQQDDAZBcGFjaGUwggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQCLiQmSjrht15R1F+r79m/LZN5hsfQBGp+dy+yrtsWfOOur\n" +
                "RdXAwgbLxxsyKMQKWCQxlRI7wdhqh0L0ZBrIr9MjltYqsqLAoLmgY4eG/f6G8YGr\n" +
                "O/rxzfwTLbCeaIseF/OMA6Sz125HXYp1bltYK4LsuC7tihZXbeVa5pUGs3Jwgcfx\n" +
                "LYm4eB42Hp7Eg05uL8LbwT/1AjcwoWkTewKAWXA83zgLRDFDbl1t0IPHI4cdVvia\n" +
                "BNwNbG49ZCF6OgmokSarQSe4Vbems1u9T9pAySXAVjEYBqFjKWyswpdr782uNLmB\n" +
                "lCGm0pDeJ9/WASxbTJr7k9H6ZpnaHr54DG6ZqennWMz8w6r2pf7bp/EGZ3mZQ4s3\n" +
                "5ylSP4cQt8CSSI8k2CflPGUyytUAiWlDS3qSyIuAOPKXDg7wIpcbwcu4VMeKnH0Z\n" +
                "x7Uu9j1UDZEZoSu6UI/VInTl47k1/ECD+AO9yBzZSv+pTQmO3/Im3CcxsTHmVd5s\n" +
                "Tl0CJ/jWNpo9DAMtmGvt6CBWBXGRsO2XNk7djRcq2CubiCpvODg+7CcR6CiZK73L\n" +
                "1aOisLiq3+ofiJSSXRRuKtJlkQ4eSPSbYWkNJcKmIhbCoYOdH/Pe3/+RHjvNc1kO\n" +
                "OUb+icmfzcMVAs3C5jybpazsfjDNQZXWAFx4FLDcqOVbrCwom+tMukw+hzlZnwID\n" +
                "AQABMA0GCSqGSIb3DQEBCwUAA4ICAQAdexoMwn+Ol1A3+NOHk9WJZX+t2Q8/9wWb\n" +
                "K+jSVleSfXXWsB1mC3fABVJQdCxtXCH3rpt4w7FK6aUes9VjqAHap4tt9WBq0Wqq\n" +
                "vvMURFHfllxEM31Z35kBOCSQY9xwpGV1rRh/zYs4h55YixomR3nXNZ9cI8xzlSCi\n" +
                "sMG0mv0y+yxPohKrZj3AzLYz/M11SimSoyRPIANI+cUg1nJXyQoHzVHWEp1Nj0HB\n" +
                "M/GW05cxsWea7f5YcAW1JQI3FOkpwb72fIZOtMDa4PO8IYWXJAeAc/chw745/MTi\n" +
                "Rvl2NT4RZBAcrSNbhCOzRPG/ZiG+ArQuCluZ9HHAXRBMTtlLk5DO4+XxZlyGpjwf\n" +
                "uKniK8dccy9uU0ho73p9SNDhXH0yb9Naj8vd9NWzCUYaaBXt/92cIyhaAHAVFxJu\n" +
                "o6jr2FLbnhSGF9EO/tHvF7LxZv1dnbInvlWHwoFQjwmoeB+e17lHBdPMnWnPKBZe\n" +
                "jA2VH/IzGCucWuWQhruummO5GT8Z6F4jBwvafBo+QARKPZgEBpx3LycXrpkYI3LT\n" +
                "GGOpGCxFt5tVZOEsC/jQ5rIljNSeTzWmzfNRn/yRUW97uWsrzcQIBAUtu/pQnyFQ\n" +
                "WCnC1ipCp1zhJsXAFUKuqEfLngXodOvC4tAOr76h11S57o5lN4506Poq2mWgAZe/\n" +
                "JZr9MEn1+w==\n" +
                "-----END CERTIFICATE-----\n" +
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIFnzCCA4egAwIBAgIUcUNMqgWoDLsvMj0YmEudj60EG5swDQYJKoZIhvcNAQEL\n" +
                "BQAwXjELMAkGA1UEBhMCWFgxCzAJBgNVBAgMAlhYMQswCQYDVQQHDAJYWDETMBEG\n" +
                "A1UECgwKQ2xvdWRTdGFjazEPMA0GA1UECwwGQXBhY2hlMQ8wDQYDVQQDDAZBcGFj\n" +
                "aGUwIBcNMjUwNjE2MTAyNzM2WhgPMjEyNTA1MjMxMDI3MzZaMF4xCzAJBgNVBAYT\n" +
                "AlhYMQswCQYDVQQIDAJYWDELMAkGA1UEBwwCWFgxEzARBgNVBAoMCkNsb3VkU3Rh\n" +
                "Y2sxDzANBgNVBAsMBkFwYWNoZTEPMA0GA1UEAwwGQXBhY2hlMIICIjANBgkqhkiG\n" +
                "9w0BAQEFAAOCAg8AMIICCgKCAgEAwVQaePulUM523gKw168ToYp+gt05bXbu4Gg8\n" +
                "uaRDKhnRAX1sEgYwkQ36Q+iTDEM9sKRma8lMNMIqkZMQdk6sIGX6BL+6wUOb7mL0\n" +
                "5+I0yO9i8ooaGgNaeNvZftNIRlLsnPMGJaeom2/66XV4CsMqoZKaJ1H/I8N+bAeD\n" +
                "GvrBx+B4l9D3G390nQvot9JUzrJgGuLl0KDHapvhlR39cCgEfIii02uX1iy0qXlV\n" +
                "b+G1kLvpeC7T+lsJxondPJ69aO3lbDv/izyWw7qqBC57UhT/oKDxJmjQqklqzhgt\n" +
                "nM/p3YE7M0nkRi3LnRmsZBz7o1DRf+M29zypKzXVk1aJflL46AtLMmpDIzVrEB2M\n" +
                "q7o47rstXusYRYsBCqGTgdI1fV/CkDsZY5XkPZh2dsjZCHIS4P03OqFGsc6PQha2\n" +
                "+y2AhV1pvywkDl48kPKSukHfV1RtaPZUZtcQKztwHH+aFfo9mD8z0H2HcExdXKzd\n" +
                "jhRhI9ZSwFj3HEN9f5P8fS3lf5+fV7EEbG4NisieBj/UivW6QiTHpLD7wRLIUt2g\n" +
                "XgXNF0lfJzYHbIcxQ6kfC5McU2fu6mUC+p/pNN8G0POS3S2T55tEUqLL4N0SadQy\n" +
                "N1TZlTd2xTn+Hb6WlG0f5m97xGcNlGHKBvntFrHvOIfkEQ9ne3MlOO1Gjlintowo\n" +
                "fRGf15kCAwEAAaNTMFEwHQYDVR0OBBYEFM4WEQJpN9M07Q8CHq+5owG93Dj8MB8G\n" +
                "A1UdIwQYMBaAFM4WEQJpN9M07Q8CHq+5owG93Dj8MA8GA1UdEwEB/wQFMAMBAf8w\n" +
                "DQYJKoZIhvcNAQELBQADggIBABr5RKGc/rKx4fOgyXNJR4aCJCTtPZKs4AUCCBYz\n" +
                "cWOjJYGNvThOPSVx51nAD8+YP2BwkOIPfhl2u/+vQSSbz4OlauXLki2DUN8E2OFe\n" +
                "gJfxPDzWOfAo/QOJcyHwSlnIQjiZzG2lK3eyf5IFnfQILQzDvXaUYvMBkl2hb5Q7\n" +
                "44H6tRw78uuf/KsT4rY0bBFMN5DayjiyvoIDUvzCRqcb2KOi9DnZ7pXjduL7tO0j\n" +
                "PhlQ24B77LVUUAvydIGUzmbhGC2VvY1qE7uaYgYtgSUZ0zSjJrHjUjVLMzRouNP7\n" +
                "jpbBQRAcP4FDcOFZBHogunA0hxQdm0d8u3LqDYPNS0rpfW0ddU/72nfBX4bnoDEN\n" +
                "+anw4wOgFuUcoEThALWZ9ESVKxXQ9Fpvd6FRW8fLLqhXAuli1BqP1c1WRxagldYe\n" +
                "nPGm/FGZyJ2xOak9Uigi9NAQ/vX6CEfgcJgFZmCo8EKH0d4Ut72vGUcPqiUhT2EI\n" +
                "AFAd6drSyoUdXXniSMWky9Vrt+qtLuAD1nhHTv8ZPdItXokoiD6ea/4xrbUZn0qY\n" +
                "lLMDyfY76UVF0ruTR2Q6IdSq/zSggdwgkTooOW4XZcRf5l/ZnoeVQ1QH9C85SIKH\n" +
                "IKZwPeGUm+EntmpuCBDmQSHLRCGEThd64iOAjqLR6arLj4TBJzBrZsGHFJbm0OcI\n" +
                "dwa9\n" +
                "-----END CERTIFICATE-----";
        final CertServiceImpl certService = new CertServiceImpl();
        certService.validate(certificate, key, password, certChains, false);
    }

    @Test
    public void validateCertAndChainsWithUnencryptedKey() {
        String key = "-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCph7jsoMCQirRn\n" +
                "3obuvgnnefTXRQYd9tF9k2aCVkTiiisvC39px7MGdgvDXADhD9fmR7oyXVQlfNu0\n" +
                "rXjjgsVT3r4bv+DVi81YGXnuU7h10yCOZJt21i6QGHN1CS0/TAfg0UhlACCEYNRx\n" +
                "kB0klwUcj/jk/AKil1DoUGpvAm2gZsek/njb76/AeIfxc+Es4ZOPCVqQOHp6gI0q\n" +
                "t6KDMkUwv8fyzrpScygMUPVYrLmm6D0pn8yd3ihW07wGxMjND6UgOnao8t6H3LaM\n" +
                "Pe7eqSFzxunF9NFFjnUrKcHZZSledDM/37Kbqb/8T5f+4SwjioS1OdPCh8ApdiXq\n" +
                "HNUwYkALAgMBAAECggEAK5JiiQ7X7053B6s96uaVDRVfRGTNKa5iMXBNDHq3wbHZ\n" +
                "X4IJAVr+PE7ivxdKco3r45fT11X9ZpUsssdTJsZZiTDak69BTiFcaaRCnmqOIlpd\n" +
                "J7vb6TMrTIW8RvxQ0M/txm6DuNHLibqJX5a2pszZ13l5cwECfF9/v/XLJTTukCbu\n" +
                "6D/f3fBVFl1tM8y9saOEYLkdb4dILWY61bVSDNswgprz2EV1SFnk5jxz2FuBrM/Q\n" +
                "+7hINvjDcaRvcm59hRb1rkljv7S10VoNw/CFkU451csJkUe4vWZwB8lZK/XxLQG0\n" +
                "HEdS1zU1XY8H8Y1RCrxjGRyiiWsBtUThhWYlPrGCoQKBgQDkP09YAlKqXhT69Kx5\n" +
                "keg2i1jV2hA73zWbWXt9xp5jG5r3pl3m170DvKL93YIDnHtpTC56mlzGrzS7DSTN\n" +
                "p0buY9Qb3fkJxunCpPVFo0HMFkpeR77ax0v34NzSohlRLKFo5R2M1cmDfbVbnSSl\n" +
                "MB57FfRRMxzjrk+dJvjOeJsxjwKBgQC+JLb4B8CZjpurXYg3ySiRqFsCqkqob+kf\n" +
                "9dR+rWvcR6vMTEyha0hUlDvTikDepU2smYR4oPHfdcXF9lAJ7T02UmQDeizAqR68\n" +
                "u9e+yS0q3tdRnPPZmXJfaDCXG1hKMqF4YA5Vs0XAjleF3zHB+vBLrnlPpShtd/Mu\n" +
                "sWTpxICTxQKBgQDSr/n+pE5IQwYczOO0aFGwn5pF9L9NdPHXz5aleETV+TJn7WL6\n" +
                "ZiRsoaDWs7SCvtxQS2kP9RM0t5/2FeDmEMXx4aZ2fsSWGM3IxVo+iL+Aswa81n8/\n" +
                "Ff5y9lb/+29hNdBcsjk/ukwEG3Lf+UNNVAie15oppgPByzJkPwgmFsAy0wKBgHDX\n" +
                "/TZp82WuerhSw/rHiSoYjhqg0bnw4Ju1Gy0q4q5SYqTWS0wpDT4U0wSSMjlwRQ6/\n" +
                "9RxZ9/G0RXFc4tdhUkig0PY3VcPpGnLL0BhL8GBW69ZlnVpwdK4meV/UPKucLLPx\n" +
                "3dACmszSLSMn+LG0qVNg8mHQFJQS8eGuKcOKePw5AoGACuxtefROKdKOALh4lTi2\n" +
                "VOwPZ+1jxsm6lKNccIEvbUpe3UXPgNWpJiDX8mUcob4/NBLzmV3BUVKbG7Exbo5J\n" +
                "LoMfp7OsztWUFwt7YAvRfS8fHdhkEsxEf3T72ADieH5ZAuXFF+K0H3r6HtWPD4ws\n" +
                "mTJjGP4+Bl/dFakA5FJcjHg=\n" +
                "-----END PRIVATE KEY-----";
        String certificate = "-----BEGIN CERTIFICATE-----\n" +
                "MIIERTCCAi0CFF2Ro8QjYcOCALfEqL1zs2T0cQzzMA0GCSqGSIb3DQEBCwUAMF4x\n" +
                "CzAJBgNVBAYTAlhYMQswCQYDVQQIDAJYWDELMAkGA1UEBwwCWFgxEzARBgNVBAoM\n" +
                "CkNsb3VkU3RhY2sxDzANBgNVBAsMBkFwYWNoZTEPMA0GA1UEAwwGQXBhY2hlMCAX\n" +
                "DTI1MDYxNzA5MTE0N1oYDzIxMjUwNTI0MDkxMTQ3WjBeMQswCQYDVQQGEwJYWDEL\n" +
                "MAkGA1UECAwCWFgxCzAJBgNVBAcMAlhYMRMwEQYDVQQKDApDbG91ZFN0YWNrMQ8w\n" +
                "DQYDVQQLDAZBcGFjaGUxDzANBgNVBAMMBkFwYWNoZTCCASIwDQYJKoZIhvcNAQEB\n" +
                "BQADggEPADCCAQoCggEBAKmHuOygwJCKtGfehu6+Ced59NdFBh320X2TZoJWROKK\n" +
                "Ky8Lf2nHswZ2C8NcAOEP1+ZHujJdVCV827SteOOCxVPevhu/4NWLzVgZee5TuHXT\n" +
                "II5km3bWLpAYc3UJLT9MB+DRSGUAIIRg1HGQHSSXBRyP+OT8AqKXUOhQam8CbaBm\n" +
                "x6T+eNvvr8B4h/Fz4Szhk48JWpA4enqAjSq3ooMyRTC/x/LOulJzKAxQ9Visuabo\n" +
                "PSmfzJ3eKFbTvAbEyM0PpSA6dqjy3ofctow97t6pIXPG6cX00UWOdSspwdllKV50\n" +
                "Mz/fspupv/xPl/7hLCOKhLU508KHwCl2Jeoc1TBiQAsCAwEAATANBgkqhkiG9w0B\n" +
                "AQsFAAOCAgEAOKaT7cp1P/B67cT0pQ+ZO7dazoomvwbznpUDPlX+h2f9pPYvBoOJ\n" +
                "qul0Np3zft3sR4M1uxRNuayhd+oFMNx0J3CJVxc6fpUvc0IvNAgy0C6IeAlTTH6V\n" +
                "Tiy8X5YeD1SAg0wJkqZQzXC+8Ao+LPacdhnz7wUSV1j4ILlVZcfvISaaZUFidERT\n" +
                "nP18syUWSodTULXTKB8M8z/9t6KFWXJDJGXLKBMoX3DCSx9QG5GDMuyu9XWf3bBH\n" +
                "ZHZse02mh0x83hV34Bpa1Yr98PsGvQm7GUXiLenFO57wzWaInxBkS6sF4OWreiMI\n" +
                "lN94CtBXtMxtC5C50WthNGBJHg3dXKeF3O6F8z8EkkqpKyJtJ3IoAXTHGEh5fxp0\n" +
                "tsbOEqJ540XbtD82UWYA4bVY1h0Tb1SaV7fylZkuYXZ+rl6G0S7roPVYbrjRsP9t\n" +
                "FCGko35WkhkI0OpNoTremH+H1U/nBowMm6tSfZ0ZWa/4NnLacXhPjDJkEhu7RlA4\n" +
                "JYeYKe4dj4hLdcHCUFuP8Tdv1P20SGQQOaHUXYbHP5Er3EHZxzI13JwHiO+FKuYP\n" +
                "igIqbCdBd8smTzdbit0f6OfKOyNXDDxN+E1VKAHSquYuxMcj+njKTQ1ihpXnTLpo\n" +
                "ZP3NoLZ6gAQIjEgHHsLeZ24HCbiFfUpwWSPNNcr6X5qQelt5leNGsIU=\n" +
                "-----END CERTIFICATE-----";
        String certChains = "-----BEGIN CERTIFICATE-----\n" +
                "MIIFQzCCAysCFEVQffqr0ScjpyZ6pmDsOOu71t70MA0GCSqGSIb3DQEBCwUAMF4x\n" +
                "CzAJBgNVBAYTAlhYMQswCQYDVQQIDAJYWDELMAkGA1UEBwwCWFgxEzARBgNVBAoM\n" +
                "CkNsb3VkU3RhY2sxDzANBgNVBAsMBkFwYWNoZTEPMA0GA1UEAwwGQXBhY2hlMB4X\n" +
                "DTI1MDYxNjEwMjc1NloXDTMwMDYxNTEwMjc1NlowXjELMAkGA1UEBhMCWFgxCzAJ\n" +
                "BgNVBAgMAlhYMQswCQYDVQQHDAJYWDETMBEGA1UECgwKQ2xvdWRTdGFjazEPMA0G\n" +
                "A1UECwwGQXBhY2hlMQ8wDQYDVQQDDAZBcGFjaGUwggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQCLiQmSjrht15R1F+r79m/LZN5hsfQBGp+dy+yrtsWfOOur\n" +
                "RdXAwgbLxxsyKMQKWCQxlRI7wdhqh0L0ZBrIr9MjltYqsqLAoLmgY4eG/f6G8YGr\n" +
                "O/rxzfwTLbCeaIseF/OMA6Sz125HXYp1bltYK4LsuC7tihZXbeVa5pUGs3Jwgcfx\n" +
                "LYm4eB42Hp7Eg05uL8LbwT/1AjcwoWkTewKAWXA83zgLRDFDbl1t0IPHI4cdVvia\n" +
                "BNwNbG49ZCF6OgmokSarQSe4Vbems1u9T9pAySXAVjEYBqFjKWyswpdr782uNLmB\n" +
                "lCGm0pDeJ9/WASxbTJr7k9H6ZpnaHr54DG6ZqennWMz8w6r2pf7bp/EGZ3mZQ4s3\n" +
                "5ylSP4cQt8CSSI8k2CflPGUyytUAiWlDS3qSyIuAOPKXDg7wIpcbwcu4VMeKnH0Z\n" +
                "x7Uu9j1UDZEZoSu6UI/VInTl47k1/ECD+AO9yBzZSv+pTQmO3/Im3CcxsTHmVd5s\n" +
                "Tl0CJ/jWNpo9DAMtmGvt6CBWBXGRsO2XNk7djRcq2CubiCpvODg+7CcR6CiZK73L\n" +
                "1aOisLiq3+ofiJSSXRRuKtJlkQ4eSPSbYWkNJcKmIhbCoYOdH/Pe3/+RHjvNc1kO\n" +
                "OUb+icmfzcMVAs3C5jybpazsfjDNQZXWAFx4FLDcqOVbrCwom+tMukw+hzlZnwID\n" +
                "AQABMA0GCSqGSIb3DQEBCwUAA4ICAQAdexoMwn+Ol1A3+NOHk9WJZX+t2Q8/9wWb\n" +
                "K+jSVleSfXXWsB1mC3fABVJQdCxtXCH3rpt4w7FK6aUes9VjqAHap4tt9WBq0Wqq\n" +
                "vvMURFHfllxEM31Z35kBOCSQY9xwpGV1rRh/zYs4h55YixomR3nXNZ9cI8xzlSCi\n" +
                "sMG0mv0y+yxPohKrZj3AzLYz/M11SimSoyRPIANI+cUg1nJXyQoHzVHWEp1Nj0HB\n" +
                "M/GW05cxsWea7f5YcAW1JQI3FOkpwb72fIZOtMDa4PO8IYWXJAeAc/chw745/MTi\n" +
                "Rvl2NT4RZBAcrSNbhCOzRPG/ZiG+ArQuCluZ9HHAXRBMTtlLk5DO4+XxZlyGpjwf\n" +
                "uKniK8dccy9uU0ho73p9SNDhXH0yb9Naj8vd9NWzCUYaaBXt/92cIyhaAHAVFxJu\n" +
                "o6jr2FLbnhSGF9EO/tHvF7LxZv1dnbInvlWHwoFQjwmoeB+e17lHBdPMnWnPKBZe\n" +
                "jA2VH/IzGCucWuWQhruummO5GT8Z6F4jBwvafBo+QARKPZgEBpx3LycXrpkYI3LT\n" +
                "GGOpGCxFt5tVZOEsC/jQ5rIljNSeTzWmzfNRn/yRUW97uWsrzcQIBAUtu/pQnyFQ\n" +
                "WCnC1ipCp1zhJsXAFUKuqEfLngXodOvC4tAOr76h11S57o5lN4506Poq2mWgAZe/\n" +
                "JZr9MEn1+w==\n" +
                "-----END CERTIFICATE-----\n" +
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIFnzCCA4egAwIBAgIUcUNMqgWoDLsvMj0YmEudj60EG5swDQYJKoZIhvcNAQEL\n" +
                "BQAwXjELMAkGA1UEBhMCWFgxCzAJBgNVBAgMAlhYMQswCQYDVQQHDAJYWDETMBEG\n" +
                "A1UECgwKQ2xvdWRTdGFjazEPMA0GA1UECwwGQXBhY2hlMQ8wDQYDVQQDDAZBcGFj\n" +
                "aGUwIBcNMjUwNjE2MTAyNzM2WhgPMjEyNTA1MjMxMDI3MzZaMF4xCzAJBgNVBAYT\n" +
                "AlhYMQswCQYDVQQIDAJYWDELMAkGA1UEBwwCWFgxEzARBgNVBAoMCkNsb3VkU3Rh\n" +
                "Y2sxDzANBgNVBAsMBkFwYWNoZTEPMA0GA1UEAwwGQXBhY2hlMIICIjANBgkqhkiG\n" +
                "9w0BAQEFAAOCAg8AMIICCgKCAgEAwVQaePulUM523gKw168ToYp+gt05bXbu4Gg8\n" +
                "uaRDKhnRAX1sEgYwkQ36Q+iTDEM9sKRma8lMNMIqkZMQdk6sIGX6BL+6wUOb7mL0\n" +
                "5+I0yO9i8ooaGgNaeNvZftNIRlLsnPMGJaeom2/66XV4CsMqoZKaJ1H/I8N+bAeD\n" +
                "GvrBx+B4l9D3G390nQvot9JUzrJgGuLl0KDHapvhlR39cCgEfIii02uX1iy0qXlV\n" +
                "b+G1kLvpeC7T+lsJxondPJ69aO3lbDv/izyWw7qqBC57UhT/oKDxJmjQqklqzhgt\n" +
                "nM/p3YE7M0nkRi3LnRmsZBz7o1DRf+M29zypKzXVk1aJflL46AtLMmpDIzVrEB2M\n" +
                "q7o47rstXusYRYsBCqGTgdI1fV/CkDsZY5XkPZh2dsjZCHIS4P03OqFGsc6PQha2\n" +
                "+y2AhV1pvywkDl48kPKSukHfV1RtaPZUZtcQKztwHH+aFfo9mD8z0H2HcExdXKzd\n" +
                "jhRhI9ZSwFj3HEN9f5P8fS3lf5+fV7EEbG4NisieBj/UivW6QiTHpLD7wRLIUt2g\n" +
                "XgXNF0lfJzYHbIcxQ6kfC5McU2fu6mUC+p/pNN8G0POS3S2T55tEUqLL4N0SadQy\n" +
                "N1TZlTd2xTn+Hb6WlG0f5m97xGcNlGHKBvntFrHvOIfkEQ9ne3MlOO1Gjlintowo\n" +
                "fRGf15kCAwEAAaNTMFEwHQYDVR0OBBYEFM4WEQJpN9M07Q8CHq+5owG93Dj8MB8G\n" +
                "A1UdIwQYMBaAFM4WEQJpN9M07Q8CHq+5owG93Dj8MA8GA1UdEwEB/wQFMAMBAf8w\n" +
                "DQYJKoZIhvcNAQELBQADggIBABr5RKGc/rKx4fOgyXNJR4aCJCTtPZKs4AUCCBYz\n" +
                "cWOjJYGNvThOPSVx51nAD8+YP2BwkOIPfhl2u/+vQSSbz4OlauXLki2DUN8E2OFe\n" +
                "gJfxPDzWOfAo/QOJcyHwSlnIQjiZzG2lK3eyf5IFnfQILQzDvXaUYvMBkl2hb5Q7\n" +
                "44H6tRw78uuf/KsT4rY0bBFMN5DayjiyvoIDUvzCRqcb2KOi9DnZ7pXjduL7tO0j\n" +
                "PhlQ24B77LVUUAvydIGUzmbhGC2VvY1qE7uaYgYtgSUZ0zSjJrHjUjVLMzRouNP7\n" +
                "jpbBQRAcP4FDcOFZBHogunA0hxQdm0d8u3LqDYPNS0rpfW0ddU/72nfBX4bnoDEN\n" +
                "+anw4wOgFuUcoEThALWZ9ESVKxXQ9Fpvd6FRW8fLLqhXAuli1BqP1c1WRxagldYe\n" +
                "nPGm/FGZyJ2xOak9Uigi9NAQ/vX6CEfgcJgFZmCo8EKH0d4Ut72vGUcPqiUhT2EI\n" +
                "AFAd6drSyoUdXXniSMWky9Vrt+qtLuAD1nhHTv8ZPdItXokoiD6ea/4xrbUZn0qY\n" +
                "lLMDyfY76UVF0ruTR2Q6IdSq/zSggdwgkTooOW4XZcRf5l/ZnoeVQ1QH9C85SIKH\n" +
                "IKZwPeGUm+EntmpuCBDmQSHLRCGEThd64iOAjqLR6arLj4TBJzBrZsGHFJbm0OcI\n" +
                "dwa9\n" +
                "-----END CERTIFICATE-----";
        final CertServiceImpl certService = new CertServiceImpl();
        certService.validate(certificate, key, null, certChains, false);
    }
}
