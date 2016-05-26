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
import net.nuage.vsp.acs.client.api.model.VspDomain;

public class SyncDomainCommand extends Command {

    private final VspDomain _domain;
    private final boolean _toAdd;
    private final boolean _toRemove;

    public SyncDomainCommand(VspDomain domain, boolean toAdd, boolean toRemove) {
        super();
        this._domain = domain;
        this._toAdd = toAdd;
        this._toRemove = toRemove;
    }

    public VspDomain getDomain() {
        return _domain;
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
        if (_domain != null ? !_domain.equals(that._domain) : that._domain != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_domain != null ? _domain.hashCode() : 0);
        result = 31 * result + (_toAdd ? 1 : 0);
        result = 31 * result + (_toRemove ? 1 : 0);
        return result;
    }
}
