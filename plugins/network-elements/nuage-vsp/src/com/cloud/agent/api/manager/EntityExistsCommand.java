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

import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Objects;

import com.cloud.agent.api.Command;

public class EntityExistsCommand<T> extends Command {

    private transient Class<T> _type;
    private final String _className;
    private final String _uuid;

    public EntityExistsCommand(Class<T> type, String uuid) {
        super();
        this._type = type;
        this._className = type.getName();
        this._uuid = uuid;
    }

    public Class<T> getType() {
        if (_type == null) {
            try {
                _type = (Class<T>)Class.forName(_className);
            } catch (ClassNotFoundException e) {
            }
        }
        return _type;
    }

    public String getUuid() {
        return _uuid;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof EntityExistsCommand)) {
            return false;
        }

        EntityExistsCommand that = (EntityExistsCommand) o;

        return super.equals(that)
                && Objects.equals(getType(), that.getType())
                && Objects.equals(_uuid, that._uuid);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(getType())
                .append(_uuid)
                .toHashCode();
    }
}