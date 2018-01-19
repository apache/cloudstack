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

import net.nuage.vsp.acs.client.api.model.VspDomain;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.cloud.agent.api.Command;

public class SyncDomainCommand extends Command {

    public enum Type { ADD, REMOVE }
    private final VspDomain _domain;
    private final Type _action;

    public SyncDomainCommand(VspDomain domain, Type action) {
        super();
        this._domain = domain;
        this._action = action;
    }

    public VspDomain getDomain() {
        return _domain;
    }

    public Type getAction() {
        return _action;
    }

    public boolean isToAdd() {
        return Type.ADD.equals(_action);
    }

    public boolean isToRemove() {
        return Type.REMOVE.equals(_action);
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

        return super.equals(that)
                && Objects.equals(_action, that._action)
                && Objects.equals(_domain, that._domain);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(_domain)
                .append(_action)
                .toHashCode();
    }
}
