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

import com.cloud.agent.api.Command;

public class SyncNuageVspCmsIdCommand extends Command {

    public static enum SyncType { AUDIT, AUDIT_ONLY, REGISTER, UNREGISTER }

    private final SyncType _syncType;
    private final String _nuageVspCmsId;

    public SyncNuageVspCmsIdCommand(SyncType syncType, String nuageVspCmsId) {
        super();
        this._syncType = syncType;
        this._nuageVspCmsId = nuageVspCmsId;
    }

    public SyncType getSyncType() {
        return _syncType;
    }

    public String getNuageVspCmsId() {
        return _nuageVspCmsId;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SyncNuageVspCmsIdCommand)) return false;
        if (!super.equals(o)) return false;

        SyncNuageVspCmsIdCommand that = (SyncNuageVspCmsIdCommand) o;

        if (_nuageVspCmsId != null ? !_nuageVspCmsId.equals(that._nuageVspCmsId) : that._nuageVspCmsId != null)
            return false;
        if (_syncType != that._syncType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_syncType != null ? _syncType.hashCode() : 0);
        result = 31 * result + (_nuageVspCmsId != null ? _nuageVspCmsId.hashCode() : 0);
        return result;
    }
}
