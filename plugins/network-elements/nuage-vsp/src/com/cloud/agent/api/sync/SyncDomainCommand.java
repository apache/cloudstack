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

public class SyncDomainCommand extends Command {

    private final String _domainUuid;
    private final String _domainName;
    private final String _domainPath;
    private final boolean _toAdd;
    private final boolean _toRemove;

    public SyncDomainCommand(String domainUuid, String domainName, String domainPath, boolean toAdd, boolean toRemove) {
        super();
        this._domainUuid = domainUuid;
        this._domainName = domainName;
        this._domainPath = domainPath;
        this._toAdd = toAdd;
        this._toRemove = toRemove;
    }

    public String getDomainUuid() {
        return _domainUuid;
    }

    public String getDomainName() {
        return _domainName;
    }

    public String getDomainPath() {
        return _domainPath;
    }

    public boolean isToAdd() {
        return _toAdd;
    }

    public boolean isToRemove() {
        return _toRemove;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SyncDomainCommand)) return false;
        if (!super.equals(o)) return false;

        SyncDomainCommand that = (SyncDomainCommand) o;

        if (_toAdd != that._toAdd) return false;
        if (_toRemove != that._toRemove) return false;
        if (_domainName != null ? !_domainName.equals(that._domainName) : that._domainName != null) return false;
        if (_domainPath != null ? !_domainPath.equals(that._domainPath) : that._domainPath != null) return false;
        if (_domainUuid != null ? !_domainUuid.equals(that._domainUuid) : that._domainUuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_domainUuid != null ? _domainUuid.hashCode() : 0);
        result = 31 * result + (_domainName != null ? _domainName.hashCode() : 0);
        result = 31 * result + (_domainPath != null ? _domainPath.hashCode() : 0);
        result = 31 * result + (_toAdd ? 1 : 0);
        result = 31 * result + (_toRemove ? 1 : 0);
        return result;
    }
}
