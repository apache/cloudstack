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

package com.cloud.agent.api.sync;

import com.cloud.agent.api.Answer;

public class SyncDomainAnswer extends Answer {

    private final boolean _success;

    public SyncDomainAnswer(boolean success) {
        super();
        this._success = success;
    }

    public boolean getSuccess() {
        return _success;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SyncDomainAnswer)) return false;
        if (!super.equals(o)) return false;

        SyncDomainAnswer that = (SyncDomainAnswer) o;

        if (_success != that._success) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_success ? 1 : 0);
        return result;
    }
}
