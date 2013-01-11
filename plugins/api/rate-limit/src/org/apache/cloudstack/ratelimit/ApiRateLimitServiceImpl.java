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

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import org.apache.cloudstack.acl.APILimitChecker;
import org.apache.cloudstack.api.command.user.ratelimit.GetApiLimitCmd;
import org.apache.cloudstack.api.commands.admin.ratelimit.ResetApiLimitCmd;
import org.apache.cloudstack.api.response.ApiLimitResponse;
import com.cloud.network.element.NetworkElement;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;

@Local(value = NetworkElement.class)
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

	@Inject
	ConfigurationDao _configDao;

	private LimitStore _store;


	@Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        // get global configured duration and max values
        String duration = _configDao.getValue(Config.ApiLimitInterval.key());
        if (duration != null ){
            timeToLive = Integer.parseInt(duration);
        }
        String maxReqs = _configDao.getValue(Config.ApiLimitMax.key());
        if ( maxReqs != null){
            maxAllowed = Integer.parseInt(maxReqs);
        }
        // create limit store
        EhcacheLimitStore cacheStore = new EhcacheLimitStore();
        int maxElements = 10000;  //TODO: what should be the proper number here?
        CacheManager cm = CacheManager.create();
        Cache cache = new Cache("api-limit-cache", maxElements, true, false, timeToLive, timeToLive);
        cm.addCache(cache);
        s_logger.info("Limit Cache created: " + cache.toString());
        cacheStore.setCache(cache);
        _store = cacheStore;

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
    public boolean isUnderLimit(Account account) {

        Long accountId = account.getId();
        StoreEntry entry = _store.get(accountId);

        if (entry == null) {

            /* Populate the entry, thus unlocking any underlying mutex */
            entry = _store.create(accountId, timeToLive);
        }

        /* Increment the client count and see whether we have hit the maximum allowed clients yet. */
        int current = entry.incrementAndGet();

        if (current <= maxAllowed) {
            return true;
        } else {
            return false;
        }
    }



    @Override
    public String[] getPropertiesFiles() {
        return new String[] { "api-limit_commands.properties" };
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
