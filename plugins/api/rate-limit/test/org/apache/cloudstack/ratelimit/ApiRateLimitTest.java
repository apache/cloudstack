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
package org.apache.cloudstack.ratelimit;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.response.ApiLimitResponse;
import org.apache.cloudstack.ratelimit.ApiRateLimitServiceImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloud.exception.RequestLimitException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ApiRateLimitTest {

	static ApiRateLimitServiceImpl _limitService = new ApiRateLimitServiceImpl();
	static AccountService _accountService = mock(AccountService.class);
	private static long acctIdSeq = 5L;
	private static Account testAccount;

	@BeforeClass
	public static void setUp() throws ConfigurationException {

		_limitService.configure("ApiRateLimitTest", Collections.<String, Object> emptyMap());

	    _limitService._accountService = _accountService;

	    // Standard responses
	    AccountVO acct = new AccountVO(acctIdSeq);
	    acct.setType(Account.ACCOUNT_TYPE_NORMAL);
	    acct.setAccountName("demo");
	    testAccount = acct;

	    when(_accountService.getAccount(5L)).thenReturn(testAccount);
	    when(_accountService.isRootAdmin(Account.ACCOUNT_TYPE_NORMAL)).thenReturn(false);
	}

    @Before
    public void testSetUp() {
        // reset counter for each test
        _limitService.resetApiLimit(null);
    }

	private User createFakeUser(){
	    UserVO user = new UserVO();
	    user.setAccountId(acctIdSeq);
	    return user;
	}

	private boolean isUnderLimit(User key){
	    try{
	       _limitService.checkAccess(key, null);
	        return true;
	    }
	    catch (RequestLimitException ex){
	        return false;
	    }
	}

    @Test
    public void sequentialApiAccess() {
        int allowedRequests = 1;
        _limitService.setMaxAllowed(allowedRequests);
        _limitService.setTimeToLive(1);

        User key = createFakeUser();
        assertTrue("Allow for the first request", isUnderLimit(key));

        assertFalse("Second request should be blocked, since we assume that the two api "
                + " accesses take less than a second to perform", isUnderLimit(key));
    }

    @Test
    public void canDoReasonableNumberOfApiAccessPerSecond() throws Exception {
        int allowedRequests = 200;
        _limitService.setMaxAllowed(allowedRequests);
        _limitService.setTimeToLive(1);

        User key = createFakeUser();

        for (int i = 0; i < allowedRequests; i++) {
            assertTrue("We should allow " + allowedRequests + " requests per second, but failed at request " + i, isUnderLimit(key));
        }


        assertFalse("We should block >" + allowedRequests + " requests per second", isUnderLimit(key));
    }

    @Test
    public void multipleClientsCanAccessWithoutBlocking() throws Exception {
        int allowedRequests = 200;
        _limitService.setMaxAllowed(allowedRequests);
        _limitService.setTimeToLive(1);


        final User key = createFakeUser();

        int clientCount = allowedRequests;
        Runnable[] clients = new Runnable[clientCount];
        final boolean[] isUsable = new boolean[clientCount];

        final CountDownLatch startGate = new CountDownLatch(1);

        final CountDownLatch endGate = new CountDownLatch(clientCount);


        for (int i = 0; i < isUsable.length; ++i) {
            final int j = i;
            clients[j] = new Runnable() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run() {
                    try {
                        startGate.await();

                        isUsable[j] = isUnderLimit(key);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        endGate.countDown();
                    }
                }
            };
        }

        ExecutorService executor = Executors.newFixedThreadPool(clientCount);

        for (Runnable runnable : clients) {
            executor.execute(runnable);
        }

        startGate.countDown();

        endGate.await();

        for (boolean b : isUsable) {
            assertTrue("Concurrent client request should be allowed within limit", b);
        }
    }

    @Test
    public void expiryOfCounterIsSupported() throws Exception {
        int allowedRequests = 1;
        _limitService.setMaxAllowed(allowedRequests);
        _limitService.setTimeToLive(1);

        User key = this.createFakeUser();

        assertTrue("The first request should be allowed", isUnderLimit(key));

        // Allow the token to expire
        Thread.sleep(1001);

        assertTrue("Another request after interval should be allowed as well", isUnderLimit(key));
    }

    @Test
    public void verifyResetCounters() throws Exception {
        int allowedRequests = 1;
        _limitService.setMaxAllowed(allowedRequests);
        _limitService.setTimeToLive(1);

        User key = this.createFakeUser();

        assertTrue("The first request should be allowed", isUnderLimit(key));

        assertFalse("Another request should be blocked", isUnderLimit(key));

        _limitService.resetApiLimit(key.getAccountId());

        assertTrue("Another request should be allowed after reset counter", isUnderLimit(key));
    }


    @Test
    public void verifySearchCounter() throws Exception {
        int allowedRequests = 10;
        _limitService.setMaxAllowed(allowedRequests);
        _limitService.setTimeToLive(1);

        User key = this.createFakeUser();

        for ( int i = 0; i < 5; i++ ){
            assertTrue("Issued 5 requests", isUnderLimit(key));
        }

        ApiLimitResponse response = _limitService.searchApiLimit(testAccount);
        assertEquals("apiIssued is incorrect", 5, response.getApiIssued());
        assertEquals("apiAllowed is incorrect", 5, response.getApiAllowed());
        assertTrue("expiredAfter is incorrect", response.getExpireAfter() < 1000);

    }

}
