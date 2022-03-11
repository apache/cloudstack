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

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.api.command.user.loadbalancer.DeleteSslCertCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UploadSslCertCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

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
        when(certService._sslCertDao.persist(Matchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

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
        when(certService._sslCertDao.persist(Matchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

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

    //    @Test
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
        when(certService._sslCertDao.persist(Matchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

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
        when(certService._sslCertDao.persist(Matchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

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
        when(certService._sslCertDao.persist(Matchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

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
        when(certService._sslCertDao.persist(Matchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

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
        when(certService._sslCertDao.persist(Matchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

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
                    e.getMessage().contains("Parsing certificate/key failed: Invalid Key format."));
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
        when(certService._sslCertDao.persist(Matchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

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
        when(certService._sslCertDao.persist(Matchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

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
                    e.getMessage().contains("Parsing certificate/key failed: Invalid Key format."));
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
        when(certService._sslCertDao.persist(Matchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

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
        when(certService._sslCertDao.persist(Matchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

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
        when(certService._sslCertDao.persist(Matchers.any(SslCertVO.class))).thenReturn(new SslCertVO());

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
}
