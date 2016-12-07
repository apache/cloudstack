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

import java.util.Objects;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

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

    public SyncNuageVspCmsIdAnswer(Command command, Exception e, SyncNuageVspCmsIdCommand.SyncType syncType) {
        super(command, e);
        this._nuageVspCmsId = null;
        this._success = false;
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
        if (this == o) {
            return true;
        }

        if (!(o instanceof SyncNuageVspCmsIdAnswer)) {
            return false;
        }

        SyncNuageVspCmsIdAnswer that = (SyncNuageVspCmsIdAnswer) o;

        return super.equals(that)
                && _success == that._success
                && Objects.equals(_syncType, that._syncType)
                && Objects.equals(_nuageVspCmsId, that._nuageVspCmsId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(_syncType)
                .append(_nuageVspCmsId)
                .append(_success)
                .toHashCode();
    }
}
