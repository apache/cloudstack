//
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
//

package com.cloud.agent.api.manager;

import com.cloud.agent.api.Answer;

import java.util.Map;

public class GetClientDefaultsAnswer extends Answer {

    private String _currentApiVersion;
    private Integer _apiRetryCount;
    private Long _apiRetryInterval;

    public GetClientDefaultsAnswer(GetClientDefaultsCommand cmd, Map<String, Object> defaults) {
        super(cmd);
        this._currentApiVersion = (String) defaults.get("CURRENT_API_VERSION");
        this._apiRetryCount = (Integer) defaults.get("DEFAULT_API_RETRY_COUNT");
        this._apiRetryInterval = (Long) defaults.get("DEFAULT_API_RETRY_INTERVAL");
    }

    public GetClientDefaultsAnswer(GetClientDefaultsCommand cmd, Exception e) {
        super(cmd, e);
    }

    public String getCurrentApiVersion() {
        return _currentApiVersion;
    }

    public Integer getApiRetryCount() {
        return _apiRetryCount;
    }

    public Long getApiRetryInterval() {
        return _apiRetryInterval;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetClientDefaultsAnswer)) return false;
        if (!super.equals(o)) return false;

        GetClientDefaultsAnswer that = (GetClientDefaultsAnswer) o;

        if (_apiRetryCount != null ? !_apiRetryCount.equals(that._apiRetryCount) : that._apiRetryCount != null)
            return false;
        if (_apiRetryInterval != null ? !_apiRetryInterval.equals(that._apiRetryInterval) : that._apiRetryInterval != null)
            return false;
        if (_currentApiVersion != null ? !_currentApiVersion.equals(that._currentApiVersion) : that._currentApiVersion != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_currentApiVersion != null ? _currentApiVersion.hashCode() : 0);
        result = 31 * result + (_apiRetryCount != null ? _apiRetryCount.hashCode() : 0);
        result = 31 * result + (_apiRetryInterval != null ? _apiRetryInterval.hashCode() : 0);
        return result;
    }
}
