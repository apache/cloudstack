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

public class SyncNuageVspCmsIdAnswer extends Answer {

    private final boolean _success;
    private final String _nuageVspCmsId;
    private final SyncNuageVspCmsIdCommand.SyncType _syncType;

    public SyncNuageVspCmsIdAnswer(boolean success, String nuageVspCmsId, SyncNuageVspCmsIdCommand.SyncType syncType) {
        super();
        this._success = success;
        this._nuageVspCmsId = nuageVspCmsId;
        this._syncType = syncType;
    }

    public boolean getSuccess() {
        return _success;
    }

    public String getNuageVspCmsId() {
        return _nuageVspCmsId;
    }

    public SyncNuageVspCmsIdCommand.SyncType getSyncType() {
        return _syncType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SyncNuageVspCmsIdAnswer)) return false;
        if (!super.equals(o)) return false;

        SyncNuageVspCmsIdAnswer that = (SyncNuageVspCmsIdAnswer) o;

        if (_success != that._success) return false;
        if (_nuageVspCmsId != null ? !_nuageVspCmsId.equals(that._nuageVspCmsId) : that._nuageVspCmsId != null)
            return false;
        if (_syncType != that._syncType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_success ? 1 : 0);
        result = 31 * result + (_nuageVspCmsId != null ? _nuageVspCmsId.hashCode() : 0);
        result = 31 * result + (_syncType != null ? _syncType.hashCode() : 0);
        return result;
    }
}
