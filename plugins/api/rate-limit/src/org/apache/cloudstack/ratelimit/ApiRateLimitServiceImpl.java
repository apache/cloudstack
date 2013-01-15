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

import java.util.Map;
import javax.ejb.Local;
import javax.naming.ConfigurationException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.APILimitChecker;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.ratelimit.ResetApiLimitCmd;
import org.apache.cloudstack.api.command.user.ratelimit.GetApiLimitCmd;
import org.apache.cloudstack.api.response.ApiLimitResponse;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.AdapterBase;

@Local(value = APILimitChecker.class)
public class ApiRateLimitServiceImpl extends AdapterBase implements APILimitChecker, ApiRateLimitService {
	private static final Logger s_logger = Logger.getLogger(ApiRateLimitServiceImpl.class);

	/**
	 * Fixed time duration where api rate limit is set, in seconds
	 */
	private int timeToLive = 1;

	/**
	 * Max number of api requests during timeToLive duration.
	 */
	private int maxAllowed = 30;

	private LimitStore _store = null;


	@Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        if (_store == null) {
            // not configured yet, note that since this class is both adapter
            // and pluggableService, so this method
            // may be invoked twice in ComponentLocator.
            // get global configured duration and max values
            Object duration = params.get("api.throttling.interval");
            if (duration != null) {
                timeToLive = Integer.parseInt((String) duration);
            }
            Object maxReqs = params.get("api.throttling.max");
            if (maxReqs != null) {
                maxAllowed = Integer.parseInt((String) maxReqs);
            }
            // create limit store
            EhcacheLimitStore cacheStore = new EhcacheLimitStore();
            int maxElements = 10000;
            Object cachesize = params.get("api.throttling.cachesize");
            if ( cachesize != null ){
                maxElements = Integer.parseInt((String)cachesize);
            }
            CacheManager cm = CacheManager.create();
            Cache cache = new Cache("api-limit-cache", maxElements, false, false, timeToLive, timeToLive);
            cm.addCache(cache);
            s_logger.info("Limit Cache created: " + cache.toString());
            cacheStore.setCache(cache);
            _store = cacheStore;
        }

        return true;

    }



    @Override
    public ApiLimitResponse searchApiLimit(GetApiLimitCmd cmd) {
        Account caller = UserContext.current().getCaller();
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
    public boolean resetApiLimit(ResetApiLimitCmd cmd) {
        if ( cmd.getAccountId() != null ){
            _store.create(cmd.getAccountId(), timeToLive);
        }
        else{
            _store.resetCounters();
        }
        return true;
    }


    @Override
    public void checkLimit(Account account) throws ServerApiException {

        Long accountId = account.getId();
        StoreEntry entry = _store.get(accountId);

        if (entry == null) {

            /* Populate the entry, thus unlocking any underlying mutex */
            entry = _store.create(accountId, timeToLive);
        }

        /* Increment the client count and see whether we have hit the maximum allowed clients yet. */
        int current = entry.incrementAndGet();

        if (current <= maxAllowed) {
            return;
        } else {
            long expireAfter = entry.getExpireDuration();
            s_logger.warn("The given user has reached his/her account api limit, please retry after " + expireAfter + " ms.");
            throw new ServerApiException(BaseCmd.API_LIMIT_EXCEED, "The given user has reached his/her account api limit, please retry after " +
                    expireAfter + " ms.");
        }
    }



    @Override
    public Map<String, String> getProperties() {
        return PropertiesUtil.processConfigFile(new String[]
                { "api-limit_commands.properties" });
    }



    @Override
    public void setTimeToLive(int timeToLive) {
        this.timeToLive = timeToLive;
    }



    @Override
    public void setMaxAllowed(int max) {
        this.maxAllowed = max;

    }


}
