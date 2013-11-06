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

import com.cloud.network.dao.*;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionLegacy;
import junit.framework.TestCase;
import org.apache.cloudstack.api.command.user.loadbalancer.DeleteSslCertCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UploadSslCertCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

public class CertServiceTest extends TestCase {

    private static final Logger s_logger = Logger.getLogger( CertServiceTest.class);

    @Override
    @Before
    public void setUp() {
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());
        CallContext.register(user, account);
    }

    @Override
    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void testUploadSslCert() throws Exception {

        /* Test1 : Given a Certificate in bad format (Not PEM), upload should fail */
        runUploadSslCertBadFormat();

        /* Test2: Given a Certificate which is not X509, upload should fail */
        runUploadSslCertNotX509();

        /* Test3: Given an expired certificate, upload should fail */
        runUploadSslCertExpiredCert();

        /* Test4: Given a private key which has a different algorithm than the certificate,
           upload should fail.
         */
        runUploadSslCertBadkeyAlgo();

        /* Test5: Given a private key which does not match the public key in the certificate,
           upload should fail
         */
        runUploadSslCertBadkeyPair();

        /* Test6: Given an encrypted private key with a bad password. Upload should fail. */
        runUploadSslCertBadPassword();

        /* Test7: If no chain is given, the certificate should be self signed. Else, uploadShould Fail */
        runUploadSslCertNoChain();

        /* Test8: Chain is given but does not have root certificate */
        runUploadSslCertNoRootCert();

        /* Test9: The chain given is not the correct chain for the certificate */
        runUploadSslCertBadChain();

        /* Test10: Given a Self-signed Certificate with encrypted key, upload should succeed */
        runUploadSslCertSelfSignedNoPassword();

        /* Test11: Given a Self-signed Certificate with non-encrypted key, upload should succeed */
        runUploadSslCertSelfSignedWithPassword();

        /* Test12: Given a certificate signed by a CA and a valid CA chain, upload should succeed */
        runUploadSslCertWithCAChain();

    }

    private void runUploadSslCertWithCAChain() throws Exception {
        //To change body of created methods use File | Settings | File Templates.

        TransactionLegacy txn = TransactionLegacy.open("runUploadSslCertWithCAChain");

        String certFile  = getClass().getResource("/certs/rsa_ca_signed.crt").getFile();
        String keyFile   = getClass().getResource("/certs/rsa_ca_signed.key").getFile();
        String chainFile = getClass().getResource("/certs/root_chain.crt").getFile();
        String password = "user";


        String cert = URLEncoder.encode(readFileToString(new File(certFile)), "UTF-8");
        String key = URLEncoder.encode(readFileToString(new File(keyFile)), "UTF-8");
        String chain = URLEncoder.encode(readFileToString(new File(chainFile)), "UTF-8");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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

        Field chainField = _class.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        certService.uploadSslCert(uploadCmd);
    }

    private void runUploadSslCertSelfSignedWithPassword() throws Exception {

        TransactionLegacy txn = TransactionLegacy.open("runUploadSslCertSelfSignedWithPassword");

        String certFile  = getClass().getResource("/certs/rsa_self_signed_with_pwd.crt").getFile();
        String keyFile   = getClass().getResource("/certs/rsa_self_signed_with_pwd.key").getFile();
        String password = "test";


        String cert = URLEncoder.encode(readFileToString(new File(certFile)), "UTF-8");
        String key = URLEncoder.encode(readFileToString(new File(keyFile)), "UTF-8");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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

    private void runUploadSslCertSelfSignedNoPassword() throws Exception {

        TransactionLegacy txn = TransactionLegacy.open("runUploadSslCertSelfSignedNoPassword");

        String certFile  = getClass().getResource("/certs/rsa_self_signed.crt").getFile();
        String keyFile   = getClass().getResource("/certs/rsa_self_signed.key").getFile();


        String cert = URLEncoder.encode(readFileToString(new File(certFile)), "UTF-8");
        String key = URLEncoder.encode(readFileToString(new File(keyFile)), "UTF-8");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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


    private void runUploadSslCertBadChain()  throws IOException, IllegalAccessException, NoSuchFieldException {
        //To change body of created methods use File | Settings | File Templates.
        String certFile  = getClass().getResource("/certs/rsa_ca_signed.crt").getFile();
        String keyFile   = getClass().getResource("/certs/rsa_ca_signed.key").getFile();
        String chainFile = getClass().getResource("/certs/rsa_self_signed.crt").getFile();
        String password = "user";


        String cert = URLEncoder.encode(readFileToString(new File(certFile)), "UTF-8");
        String key = URLEncoder.encode(readFileToString(new File(keyFile)), "UTF-8");
        String chain = URLEncoder.encode(readFileToString(new File(chainFile)), "UTF-8");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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

        Field chainField = _class.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        try {
            certService.uploadSslCert(uploadCmd);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid certificate chain"));
        }
    }

    private void runUploadSslCertNoRootCert()  throws IOException, IllegalAccessException, NoSuchFieldException {

                //To change body of created methods use File | Settings | File Templates.
        String certFile  = getClass().getResource("/certs/rsa_ca_signed.crt").getFile();
        String keyFile   = getClass().getResource("/certs/rsa_ca_signed.key").getFile();
        String chainFile = getClass().getResource("/certs/rsa_ca_signed2.crt").getFile();
        String password = "user";


        String cert = URLEncoder.encode(readFileToString(new File(certFile)), "UTF-8");
        String key = URLEncoder.encode(readFileToString(new File(keyFile)), "UTF-8");
        String chain = URLEncoder.encode(readFileToString(new File(chainFile)), "UTF-8");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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

        Field chainField = _class.getDeclaredField("chain");
        chainField.setAccessible(true);
        chainField.set(uploadCmd, chain);

        try {
            certService.uploadSslCert(uploadCmd);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("No root certificates found"));
        }

    }

    private void runUploadSslCertNoChain() throws IOException, IllegalAccessException, NoSuchFieldException {

        String certFile = getClass().getResource("/certs/rsa_ca_signed.crt").getFile();
        String keyFile  = getClass().getResource("/certs/rsa_ca_signed.key").getFile();
        String password = "user";


        String cert = URLEncoder.encode(readFileToString(new File(certFile)), "UTF-8");
        String key = URLEncoder.encode(readFileToString(new File(keyFile)), "UTF-8");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("No chain given and certificate not self signed"));
        }

    }

    private void runUploadSslCertBadPassword() throws IOException, IllegalAccessException, NoSuchFieldException {

        String certFile = getClass().getResource("/certs/rsa_self_signed_with_pwd.crt").getFile();
        String keyFile  = getClass().getResource("/certs/rsa_self_signed_with_pwd.key").getFile();
        String password = "bad_password";


        String cert = URLEncoder.encode(readFileToString(new File(certFile)), "UTF-8");
        String key = URLEncoder.encode(readFileToString(new File(keyFile)), "UTF-8");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("please check password and data"));
        }

    }

    private void runUploadSslCertBadkeyPair() throws IOException, IllegalAccessException, NoSuchFieldException {
        // Reading appropritate files
        String certFile = getClass().getResource("/certs/rsa_self_signed.crt").getFile();
        String keyFile  = getClass().getResource("/certs/rsa_random_pkey.key").getFile();

        String cert = URLEncoder.encode(readFileToString(new File(certFile)), "UTF-8");
        String key = URLEncoder.encode(readFileToString(new File(keyFile)), "UTF-8");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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

    private void runUploadSslCertBadkeyAlgo() throws IOException, IllegalAccessException, NoSuchFieldException {

        // Reading appropritate files
        String certFile = getClass().getResource("/certs/rsa_self_signed.crt").getFile();
        String keyFile  = getClass().getResource("/certs/dsa_self_signed.key").getFile();

        String cert = URLEncoder.encode(readFileToString(new File(certFile)), "UTF-8");
        String key = URLEncoder.encode(readFileToString(new File(keyFile)), "UTF-8");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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
            assertTrue(e.getMessage().contains("Public and private key have different algorithms"));
        }
    }

    private void runUploadSslCertExpiredCert() throws IOException, IllegalAccessException, NoSuchFieldException {

        // Reading appropritate files
        String certFile = getClass().getResource("/certs/expired_cert.crt").getFile();
        String keyFile  = getClass().getResource("/certs/rsa_self_signed.key").getFile();

        String cert = URLEncoder.encode(readFileToString(new File(certFile)), "UTF-8");
        String key = URLEncoder.encode(readFileToString(new File(keyFile)), "UTF-8");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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
            assertTrue(e.getMessage().contains("Certificate expired"));
        }
    }

    private void runUploadSslCertNotX509() throws IOException, IllegalAccessException, NoSuchFieldException {
        // Reading appropritate files
        String certFile = getClass().getResource("/certs/non_x509_pem.crt").getFile();
        String keyFile  = getClass().getResource("/certs/rsa_self_signed.key").getFile();

        String cert = URLEncoder.encode(readFileToString(new File(certFile)), "UTF-8");
        String key = URLEncoder.encode(readFileToString(new File(keyFile)), "UTF-8");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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
            assertTrue(e.getMessage().contains("Expected X509 certificate"));
        }
    }

    private void runUploadSslCertBadFormat() throws IOException, IllegalAccessException, NoSuchFieldException {

        // Reading appropritate files
        String certFile = getClass().getResource("/certs/bad_format_cert.crt").getFile();
        String keyFile  = getClass().getResource("/certs/rsa_self_signed.key").getFile();

        String cert = URLEncoder.encode(readFileToString(new File(certFile)), "UTF-8");
        String key = URLEncoder.encode(readFileToString(new File(keyFile)), "UTF-8");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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
            assertTrue(e.getMessage().contains("Invalid certificate format"));
        }
    }


    @Test
    public void testDeleteSslCert() throws Exception {
        /* Test1: Delete with an invalid ID should fail */
        runDeleteSslCertInvalidId();

        /* Test2: Delete with a cert id bound to a lb should fail */
        runDeleteSslCertBoundCert();

        /* Test3: Delete with a valid Id should succeed */
        runDeleteSslCertValid();


    }

    private void runDeleteSslCertValid() throws Exception {

        TransactionLegacy txn = TransactionLegacy.open("runDeleteSslCertValid");

        CertServiceImpl certService =  new CertServiceImpl();
        long certId = 1;

        //setting mock objects
        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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

    private void runDeleteSslCertBoundCert() throws NoSuchFieldException, IllegalAccessException {

        TransactionLegacy txn = TransactionLegacy.open("runDeleteSslCertBoundCert");

        CertServiceImpl certService =  new CertServiceImpl();

        //setting mock objects
        long certId = 1;

        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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
        }catch (Exception e){
            assertTrue(e.getMessage().contains("Certificate in use by a loadbalancer"));
        }


    }

    private void runDeleteSslCertInvalidId() throws NoSuchFieldException, IllegalAccessException {

        TransactionLegacy txn = TransactionLegacy.open("runDeleteSslCertInvalidId");

        long certId = 1;
        CertServiceImpl certService =  new CertServiceImpl();

        certService._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(certService._accountMgr.getAccount(anyLong())).thenReturn(account);

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
        }catch (Exception e){
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
