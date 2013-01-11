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

import org.apache.cloudstack.api.command.user.ratelimit.GetApiLimitCmd;
import org.apache.cloudstack.api.commands.admin.ratelimit.ResetApiLimitCmd;
import org.apache.cloudstack.api.response.ApiLimitResponse;
import org.apache.cloudstack.ratelimit.ApiRateLimitServiceImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.UserContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ApiRateLimitTest {

	static ApiRateLimitServiceImpl _limitService = new ApiRateLimitServiceImpl();
	static ConfigurationDao _configDao = mock(ConfigurationDao.class);
	private static long acctIdSeq = 0L;

	@BeforeClass
	public static void setUp() throws ConfigurationException {
		_limitService._configDao = _configDao;

		// No global configuration set, will set in each test case
		when(_configDao.getValue(Config.ApiLimitInterval.key())).thenReturn(null);
        when(_configDao.getValue(Config.ApiLimitMax.key())).thenReturn(null);

		_limitService.configure("ApiRateLimitTest", Collections.<String, Object> emptyMap());
	}


	private Account createFakeAccount(){
	    return new AccountVO(acctIdSeq++);
	}

    @Test
    public void sequentialApiAccess() {
        int allowedRequests = 1;
        _limitService.setMaxAllowed(allowedRequests);
        _limitService.setTimeToLive(1);

        Account key = createFakeAccount();
        assertTrue("Allow for the first request", _limitService.isUnderLimit(key));

        assertFalse("Second request should be blocked, since we assume that the two api "
                + " accesses take less than a second to perform", _limitService.isUnderLimit(key));
    }

    @Test
    public void canDoReasonableNumberOfApiAccessPerSecond() throws Exception {
        int allowedRequests = 50000;
        _limitService.setMaxAllowed(allowedRequests);
        _limitService.setTimeToLive(1);

        Account key = createFakeAccount();

        for (int i = 0; i < allowedRequests; i++) {
            assertTrue("We should allow " + allowedRequests + " requests per second", _limitService.isUnderLimit(key));
        }


        assertFalse("We should block >" + allowedRequests + " requests per second", _limitService.isUnderLimit(key));
    }

    @Test
    public void multipleClientsCanAccessWithoutBlocking() throws Exception {
        int allowedRequests = 200;
        _limitService.setMaxAllowed(allowedRequests);
        _limitService.setTimeToLive(1);


        final Account key = createFakeAccount();

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

                        isUsable[j] = _limitService.isUnderLimit(key);

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

        Account key = this.createFakeAccount();

        assertTrue("The first request should be allowed", _limitService.isUnderLimit(key));

        // Allow the token to expire
        Thread.sleep(1001);

        assertTrue("Another request after interval should be allowed as well", _limitService.isUnderLimit(key));
    }

    @Test
    public void verifyResetCounters() throws Exception {
        int allowedRequests = 1;
        _limitService.setMaxAllowed(allowedRequests);
        _limitService.setTimeToLive(1);

        Account key = this.createFakeAccount();

        assertTrue("The first request should be allowed", _limitService.isUnderLimit(key));

        assertFalse("Another request should be blocked", _limitService.isUnderLimit(key));

        ResetApiLimitCmd cmd = new ResetApiLimitCmd();
        cmd.setAccountId(key.getId());

        _limitService.resetApiLimit(cmd);

        assertTrue("Another request should be allowed after reset counter", _limitService.isUnderLimit(key));
    }

    /* Disable this since I cannot mock Static method UserContext.current()
    @Test
    public void verifySearchCounter() throws Exception {
        int allowedRequests = 10;
        _limitService.setMaxAllowed(allowedRequests);
        _limitService.setTimeToLive(1);

        Account key = this.createFakeAccount();

        for ( int i = 0; i < 5; i++ ){
            assertTrue("Issued 5 requests", _limitService.isUnderLimit(key));
        }

        GetApiLimitCmd cmd = new GetApiLimitCmd();
        UserContext ctx = mock(UserContext.class);
        when(UserContext.current().getCaller()).thenReturn(key);
        ApiLimitResponse response = _limitService.searchApiLimit(cmd);
        assertEquals("apiIssued is incorrect", 5, response.getApiIssued());
        assertEquals("apiAllowed is incorrect", 5, response.getApiAllowed());
        assertTrue("expiredAfter is incorrect", response.getExpireAfter() < 1);

    }
    */
}
