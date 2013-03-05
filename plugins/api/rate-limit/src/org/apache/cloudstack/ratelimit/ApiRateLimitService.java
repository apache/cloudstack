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

import org.apache.cloudstack.api.response.ApiLimitResponse;
import com.cloud.user.Account;
import com.cloud.utils.component.PluggableService;

/**
 * Provide API rate limit service
 *
 */
public interface ApiRateLimitService extends PluggableService{

    public ApiLimitResponse searchApiLimit(Account caller);

    public boolean resetApiLimit(Long accountId);

    public void setTimeToLive(int timeToLive);

    public void setMaxAllowed(int max);

    public void setEnabled(boolean enabled);
}
