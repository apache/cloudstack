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

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.api.command.admin.ratelimit.ResetApiLimitCmd;
import org.apache.cloudstack.api.command.user.ratelimit.GetApiLimitCmd;
import org.apache.cloudstack.api.response.ApiLimitResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.configuration.Config;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.RequestLimitException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.component.AdapterBase;

@Component
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
            if (isEnabled != null) {
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
            if (cachesize != null) {
                maxElements = Integer.parseInt(cachesize);
            }
            CacheManager cm = CacheManager.create();
            Cache cache = new Cache("api-limit-cache", maxElements, false, false, timeToLive, timeToLive);
            cm.addCache(cache);
            s_logger.info("Limit Cache created with timeToLive=" + timeToLive + ", maxAllowed=" + maxAllowed + ", maxElements=" + maxElements);
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
        } else {
            response.setApiIssued(entry.getCounter());
            response.setApiAllowed(maxAllowed - entry.getCounter());
            response.setExpireAfter(entry.getExpireDuration());
        }

        return response;
    }

    @Override
    public boolean resetApiLimit(Long accountId) {
        if (accountId != null) {
            _store.create(accountId, timeToLive);
        } else {
            _store.resetCounters();
        }
        return true;
    }

    @Override
    public List<String> getApisAllowedToUser(Role role, User user, List<String> apiNames) throws PermissionDeniedException {
        if (!isEnabled()) {
            return apiNames;
        }

        for (int i = 0; i < apiNames.size(); i++) {
            if (hasApiRateLimitBeenExceeded(user.getAccountId())) {
                throwExceptionDueToApiRateLimitReached(user.getAccountId());
            }
        }
        return apiNames;
    }

    public void throwExceptionDueToApiRateLimitReached(Long accountId) throws RequestLimitException {
        long expireAfter = _store.get(accountId).getExpireDuration();
        String msg = String.format("The given user has reached his/her account api limit, please retry after [%s] ms.", expireAfter);
        s_logger.warn(msg);
        throw new RequestLimitException(msg);
    }

    @Override
    public boolean checkAccess(User user, String apiCommandName) throws PermissionDeniedException {
        if (!isEnabled()) {
            return true;
        }

        Account account = _accountService.getAccount(user.getAccountId());
        return checkAccess(account, apiCommandName);
    }

    @Override
    public boolean checkAccess(Account account, String commandName) {
        Long accountId = account.getAccountId();
        if (_accountService.isRootAdmin(accountId)) {
            s_logger.info(String.format("Account [%s] is Root Admin, in this case, API limit does not apply.",
                    ReflectionToStringBuilderUtils.reflectOnlySelectedFields(account, "accountName", "uuid")));
            return true;
        }
        if (hasApiRateLimitBeenExceeded(accountId)) {
            throwExceptionDueToApiRateLimitReached(accountId);
        }
        return true;
    }

    /**
     * Verifies if the API limit was exceeded by the account.
     *
     * @param accountId the id of the account to be verified
     * @return if the API limit was exceeded by the account
     */
    public boolean hasApiRateLimitBeenExceeded(Long accountId) {
        Account account = _accountService.getAccount(accountId);
        StoreEntry entry = _store.get(accountId);

        if (entry == null) {
            entry = _store.create(account.getId(), timeToLive);
        }

        int current = entry.incrementAndGet();

        if (current <= maxAllowed) {
            s_logger.trace(String.format("Account %s has current count [%s].", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(account, "uuid", "accountName"), current));
            return false;
        }
        return true;
    }

    @Override
    public boolean isEnabled() {
        if (!enabled) {
            s_logger.debug("API rate limiting is disabled. We will not use ApiRateLimitService.");
        }
        return enabled;
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

    protected int getTimeToLive() {
        return this.timeToLive;
    }

    protected int getMaxAllowed() {
        return this.maxAllowed;
    }

    protected int getIssued(Long accountId) {
        int ammount = 0;
        StoreEntry entry = _store.get(accountId);
        if (entry != null) {
            ammount = entry.getCounter();
        }
        return ammount;
    }

    @Override
    public void setMaxAllowed(int max) {
        maxAllowed = max;

    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

    }
}
