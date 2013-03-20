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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.api.ApiConstants.LDAPParams;
import org.apache.cloudstack.api.command.admin.ratelimit.ResetApiLimitCmd;
import org.apache.cloudstack.api.command.user.ratelimit.GetApiLimitCmd;
import org.apache.cloudstack.api.response.ApiLimitResponse;

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.RequestLimitException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.component.AdapterBase;
import org.springframework.stereotype.Component;

@Component
@Local(value = APIChecker.class)
public class ApiRateLimitServiceImpl extends AdapterBase implements APIChecker, ApiRateLimitService {
	private static final Logger s_logger = Logger.getLogger(ApiRateLimitServiceImpl.class);

	/**
	 * True if api rate limiting is enabled
	 */
	private boolean enabled = false;

	/**
	 * Fixed time duration where api rate limit is set, in seconds
	 */
	private int timeToLive = 1;

	/**
	 * Max number of api requests during timeToLive duration.
	 */
	private int maxAllowed = 30;

	private LimitStore _store = null;

	@Inject
	AccountService _accountService;

    @Inject
    ConfigurationDao _configDao;

	@Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        if (_store == null) {
            // get global configured duration and max values
            String isEnabled = _configDao.getValue(Config.ApiLimitEnabled.key());
            if ( isEnabled != null ){
                enabled = Boolean.parseBoolean(isEnabled);
            }
            String duration = _configDao.getValue(Config.ApiLimitInterval.key());
            if (duration != null) {
                timeToLive = Integer.parseInt(duration);
            }
            String maxReqs = _configDao.getValue(Config.ApiLimitMax.key());
            if (maxReqs != null) {
                maxAllowed = Integer.parseInt(maxReqs);
            }
            // create limit store
            EhcacheLimitStore cacheStore = new EhcacheLimitStore();
            int maxElements = 10000;
            String cachesize = _configDao.getValue(Config.ApiLimitCacheSize.key());
            if ( cachesize != null ){
                maxElements = Integer.parseInt(cachesize);
            }
            CacheManager cm = CacheManager.create();
            Cache cache = new Cache("api-limit-cache", maxElements, false, false, timeToLive, timeToLive);
            cm.addCache(cache);
            s_logger.info("Limit Cache created with timeToLive=" + timeToLive + ", maxAllowed=" + maxAllowed + ", maxElements=" + maxElements );
            cacheStore.setCache(cache);
            _store = cacheStore;

        }

        return true;
    }

    @Override
    public ApiLimitResponse searchApiLimit(Account caller) {
        ApiLimitResponse response = new ApiLimitResponse();
        response.setAccountId(caller.getUuid());
        response.setAccountName(caller.getAccountName());
        StoreEntry entry = _store.get(caller.getId());
        if (entry == null) {

            /* Populate the entry, thus unlocking any underlying mutex */
            entry = _store.create(caller.getId(), timeToLive);
            response.setApiIssued(0);
            response.setApiAllowed(maxAllowed);
            response.setExpireAfter(timeToLive);
        }
        else{
            response.setApiIssued(entry.getCounter());
            response.setApiAllowed(maxAllowed - entry.getCounter());
            response.setExpireAfter(entry.getExpireDuration());
        }

        return response;
    }



    @Override
    public boolean resetApiLimit(Long accountId) {
        if ( accountId != null ){
            _store.create(accountId, timeToLive);
        }
        else{
            _store.resetCounters();
        }
        return true;
    }



    @Override
    public boolean checkAccess(User user, String apiCommandName) throws PermissionDeniedException {
        // check if api rate limiting is enabled or not
        if (!enabled){
            return true;
        }
        Long accountId = user.getAccountId();
        Account account = _accountService.getAccount(accountId);
        if ( _accountService.isRootAdmin(account.getType())){
            // no API throttling on root admin
            return true;
        }
        StoreEntry entry = _store.get(accountId);

        if (entry == null) {

            /* Populate the entry, thus unlocking any underlying mutex */
            entry = _store.create(accountId, timeToLive);
        }

        /* Increment the client count and see whether we have hit the maximum allowed clients yet. */
        int current = entry.incrementAndGet();

        if (current <= maxAllowed) {
            s_logger.trace("account (" + account.getAccountId() + "," + account.getAccountName() + ") has current count = " + current);
            return true;
        } else {
            long expireAfter = entry.getExpireDuration();
            // for this exception, we can just show the same message to user and admin users.
            String msg = "The given user has reached his/her account api limit, please retry after " + expireAfter + " ms.";
            s_logger.warn(msg);
            throw new RequestLimitException(msg);
        }
    }


    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ResetApiLimitCmd.class);
        cmdList.add(GetApiLimitCmd.class);
        return cmdList;
    }


    @Override
    public void setTimeToLive(int timeToLive) {
        this.timeToLive = timeToLive;
    }



    @Override
    public void setMaxAllowed(int max) {
        this.maxAllowed = max;

    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

    }


}
