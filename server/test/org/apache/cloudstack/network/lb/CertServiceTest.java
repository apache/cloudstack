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

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.net.URLDecoder;

import org.apache.cloudstack.api.command.user.loadbalancer.DeleteSslCertCmd;
import com.cloud.user.User;
import org.apache.cloudstack.api.command.user.loadbalancer.UploadSslCertCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.domain.dao.DomainDao;
import com.cloud.domain.DomainVO;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerCertMapVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.SslCertDao;
import com.cloud.network.dao.SslCertVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.TransactionLegacy;
import java.nio.charset.Charset;

public class CertServiceTest {

    @Before
    public void setUp() {
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
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

        TransactionLegacy txn = TransactionLegacy.open("runUploadSslCertWithCAChain");

        String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.crt").getFile(),Charset.defaultCharset().name());
        String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.key").getFile(),Charset.defaultCharset().name());
        String chainFile = URLDecoder.decode(getClass().getResource("/certs/root_chain.crt").getFile(),Charset.defaultCharset().name());

        String cert = readFileToString(new File(certFile));
        String key = readFileToString(new File(keyFile));
        String chain = readFileToString(new File(chainFile));

        CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        certService._accountDao = Mockito.mock(AccountDao.class);
        when(certService._accountDao.findByIdIncludingRemoved(anyLong())).thenReturn((AccountVO)account);

        //creating the command
        UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        Class<?> _class = uploadCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        Field chainField = _class.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        certService.uploadSslCert(uploadCmd);
    }

    @Test
    /**
     * Given a Self-signed Certificate with encrypted key, upload should succeed
     */
    public void runUploadSslCertSelfSignedWithPassword() throws Exception {

        TransactionLegacy txn = TransactionLegacy.open("runUploadSslCertSelfSignedWithPassword");

        String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed_with_pwd.crt").getFile(),Charset.defaultCharset().name());
        String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed_with_pwd.key").getFile(),Charset.defaultCharset().name());
        String password = "test";

        String cert = readFileToString(new File(certFile));
        String key = readFileToString(new File(keyFile));

        CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        certService._accountDao = Mockito.mock(AccountDao.class);
        when(certService._accountDao.findByIdIncludingRemoved(anyLong())).thenReturn((AccountVO)account);

        //creating the command
        UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        Class<?> _class = uploadCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        Field passField = _class.getDeclaredField("password");
        passField.setAccessible(true);
        passField.set(uploadCmd, password);

        certService.uploadSslCert(uploadCmd);
    }

    @Test
    /**
     * Given a Self-signed Certificate with non-encrypted key, upload should succeed
     */
    public void runUploadSslCertSelfSignedNoPassword() throws Exception {

        TransactionLegacy txn = TransactionLegacy.open("runUploadSslCertSelfSignedNoPassword");

        String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.crt").getFile(),Charset.defaultCharset().name());
        String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.key").getFile(),Charset.defaultCharset().name());

        String cert = readFileToString(new File(certFile));
        String key = readFileToString(new File(keyFile));

        CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        certService._accountDao = Mockito.mock(AccountDao.class);
        when(certService._accountDao.findByIdIncludingRemoved(anyLong())).thenReturn((AccountVO)account);

        //creating the command
        UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        Class<?> _class = uploadCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        certService.uploadSslCert(uploadCmd);
    }


    @Test
    public void runUploadSslCertBadChain() throws IOException, IllegalAccessException, NoSuchFieldException {
        Assume.assumeTrue(isOpenJdk() || isJCEInstalled());

        String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.crt").getFile(),Charset.defaultCharset().name());
        String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.key").getFile(),Charset.defaultCharset().name());
        String chainFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.crt").getFile(),Charset.defaultCharset().name());

        String cert = readFileToString(new File(certFile));
        String key = readFileToString(new File(keyFile));
        String chain = readFileToString(new File(chainFile));

        CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        Class<?> _class = uploadCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        Field chainField = _class.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("The chain given is not the correct chain for the certificate");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid certificate chain"));
        }
    }


    @Test
    public void runUploadSslCertNoRootCert() throws IOException, IllegalAccessException, NoSuchFieldException {

        Assume.assumeTrue(isOpenJdk() || isJCEInstalled());

        String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.crt").getFile(),Charset.defaultCharset().name());
        String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_ca_signed.key").getFile(),Charset.defaultCharset().name());
        String chainFile = URLDecoder.decode(getClass().getResource("/certs/non_root.crt").getFile(),Charset.defaultCharset().name());

        String cert = readFileToString(new File(certFile));
        String key = readFileToString(new File(keyFile));
        String chain = readFileToString(new File(chainFile));

        CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        Class<?> _class = uploadCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        Field chainField = _class.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("Chain is given but does not link to the certificate");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid certificate chain"));
        }

    }


    @Test
    public void runUploadSslCertBadPassword() throws IOException, IllegalAccessException, NoSuchFieldException {

        String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed_with_pwd.crt").getFile(),Charset.defaultCharset().name());
        String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed_with_pwd.key").getFile(),Charset.defaultCharset().name());
        String password = "bad_password";

        String cert = readFileToString(new File(certFile));
        String key = readFileToString(new File(keyFile));

        CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        Class<?> _class = uploadCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        Field passField = _class.getDeclaredField("password");
        passField.setAccessible(true);
        passField.set(uploadCmd, password);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("Given an encrypted private key with a bad password. Upload should fail.");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("please check password and data"));
        }

    }

    @Test
    public void runUploadSslCertBadkeyPair() throws IOException, IllegalAccessException, NoSuchFieldException {
        // Reading appropritate files
        String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.crt").getFile(),Charset.defaultCharset().name());
        String keyFile = URLDecoder.decode(getClass().getResource("/certs/non_root.key").getFile(),Charset.defaultCharset().name());

        String cert = readFileToString(new File(certFile));
        String key = readFileToString(new File(keyFile));

        CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        Class<?> _class = uploadCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Bad public-private key"));
        }
    }

    @Test
    public void runUploadSslCertBadkeyAlgo() throws IOException, IllegalAccessException, NoSuchFieldException {

        // Reading appropritate files
        String certFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.crt").getFile(),Charset.defaultCharset().name());
        String keyFile = URLDecoder.decode(getClass().getResource("/certs/dsa_self_signed.key").getFile(),Charset.defaultCharset().name());

        String cert = readFileToString(new File(certFile));
        String key = readFileToString(new File(keyFile));

        CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        Class<?> _class = uploadCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("Given a private key which has a different algorithm than the certificate, upload should fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Public and private key have different algorithms"));
        }
    }

    @Test
    public void runUploadSslCertExpiredCert() throws IOException, IllegalAccessException, NoSuchFieldException {

        // Reading appropritate files
        String certFile = URLDecoder.decode(getClass().getResource("/certs/expired_cert.crt").getFile(),Charset.defaultCharset().name());
        String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.key").getFile(),Charset.defaultCharset().name());

        String cert = readFileToString(new File(certFile));
        String key = readFileToString(new File(keyFile));

        CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        Class<?> _class = uploadCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("Given an expired certificate, upload should fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Certificate expired"));
        }
    }

    @Test
    public void runUploadSslCertNotX509() throws IOException, IllegalAccessException, NoSuchFieldException {
        // Reading appropritate files
        String certFile = URLDecoder.decode(getClass().getResource("/certs/non_x509_pem.crt").getFile(),Charset.defaultCharset().name());
        String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.key").getFile(),Charset.defaultCharset().name());

        String cert = readFileToString(new File(certFile));
        String key = readFileToString(new File(keyFile));

        CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        Class<?> _class = uploadCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("Given a Certificate which is not X509, upload should fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Expected X509 certificate"));
        }
    }

    @Test
    public void runUploadSslCertBadFormat() throws IOException, IllegalAccessException, NoSuchFieldException {

        // Reading appropritate files
        String certFile = URLDecoder.decode(getClass().getResource("/certs/bad_format_cert.crt").getFile(),Charset.defaultCharset().name());
        String keyFile = URLDecoder.decode(getClass().getResource("/certs/rsa_self_signed.key").getFile(),Charset.defaultCharset().name());

        String cert = readFileToString(new File(certFile));
        String key = readFileToString(new File(keyFile));

        CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.persist(any(SslCertVO.class))).thenReturn(new SslCertVO());

        //creating the command
        UploadSslCertCmd uploadCmd = new UploadSslCertCmdExtn();
        Class<?> _class = uploadCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("cert");
        certField.setAccessible(true);
        certField.set(uploadCmd, cert);

        Field keyField = _class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(uploadCmd, key);

        try {
            certService.uploadSslCert(uploadCmd);
            fail("Given a Certificate in bad format (Not PEM), upload should fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid certificate format"));
        }
    }

    @Test
    /**
     * Delete with a valid Id should succeed
     */
    public void runDeleteSslCertValid() throws Exception {

        TransactionLegacy txn = TransactionLegacy.open("runDeleteSslCertValid");

        CertServiceImpl certService = new CertServiceImpl();
        long certId = 1;

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.remove(anyLong())).thenReturn(true);
        when(certService._sslCertDao.findById(anyLong())).thenReturn(new SslCertVO());

        // a rule holding the cert

        certService._lbCertDao = Mockito.mock(LoadBalancerCertMapDao.class);
        when(certService._lbCertDao.listByCertId(anyLong())).thenReturn(null);

        //creating the command
        DeleteSslCertCmd deleteCmd = new DeleteSslCertCmdExtn();
        Class<?> _class = deleteCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("id");
        certField.setAccessible(true);
        certField.set(deleteCmd, certId);

        certService.deleteSslCert(deleteCmd);
    }

    @Test
    public void runDeleteSslCertBoundCert() throws NoSuchFieldException, IllegalAccessException {

        TransactionLegacy txn = TransactionLegacy.open("runDeleteSslCertBoundCert");

        CertServiceImpl certService = new CertServiceImpl();

        //setting mock objects
        long certId = 1;

        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.remove(anyLong())).thenReturn(true);
        when(certService._sslCertDao.findById(anyLong())).thenReturn(new SslCertVO());

        // rule holding the cert
        certService._lbCertDao = Mockito.mock(LoadBalancerCertMapDao.class);

        List<LoadBalancerCertMapVO> lbMapList = new ArrayList<LoadBalancerCertMapVO>();
        lbMapList.add(new LoadBalancerCertMapVO());

        certService._lbCertDao = Mockito.mock(LoadBalancerCertMapDao.class);
        when(certService._lbCertDao.listByCertId(anyLong())).thenReturn(lbMapList);

        certService._entityMgr = Mockito.mock(EntityManager.class);
        when(certService._entityMgr.findById(eq(LoadBalancerVO.class), anyLong())).thenReturn(new LoadBalancerVO());

        //creating the command
        DeleteSslCertCmd deleteCmd = new DeleteSslCertCmdExtn();
        Class<?> _class = deleteCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("id");
        certField.setAccessible(true);
        certField.set(deleteCmd, certId);

        try {
            certService.deleteSslCert(deleteCmd);
            fail("Delete with a cert id bound to a lb should fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Certificate in use by a loadbalancer"));
        }

    }

    @Test
    public void runDeleteSslCertInvalidId() throws NoSuchFieldException, IllegalAccessException {
        TransactionLegacy txn = TransactionLegacy.open("runDeleteSslCertInvalidId");

        long certId = 1;
        CertServiceImpl certService = new CertServiceImpl();

        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

        certService._domainDao = Mockito.mock(DomainDao.class);
        DomainVO domain = new DomainVO("networkdomain", 1L, 1L, "networkdomain");
        when(certService._domainDao.findByIdIncludingRemoved(anyLong())).thenReturn(domain);

        certService._sslCertDao = Mockito.mock(SslCertDao.class);
        when(certService._sslCertDao.remove(anyLong())).thenReturn(true);
        when(certService._sslCertDao.findById(anyLong())).thenReturn(null);

        // no rule holding the cert
        certService._lbCertDao = Mockito.mock(LoadBalancerCertMapDao.class);
        when(certService._lbCertDao.listByCertId(anyLong())).thenReturn(null);

        //creating the command
        DeleteSslCertCmd deleteCmd = new DeleteSslCertCmdExtn();
        Class<?> _class = deleteCmd.getClass().getSuperclass();

        Field certField = _class.getDeclaredField("id");
        certField.setAccessible(true);
        certField.set(deleteCmd, certId);

        try {
            certService.deleteSslCert(deleteCmd);
            fail("Delete with an invalid ID should fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid certificate id"));
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
