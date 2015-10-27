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

package com.cloud.agent.api.element;

import com.cloud.agent.api.CmdBuilder;
import com.cloud.agent.api.Command;

public class ShutDownVpcVspCommand extends Command {

    private final String _domainUuid;
    private final String _vpcUuid;
    private final String _domainTemplateName;

    private ShutDownVpcVspCommand(String domainUuid, String vpcUuid, String domainTemplateName) {
        super();
        this._domainUuid = domainUuid;
        this._vpcUuid = vpcUuid;
        this._domainTemplateName = domainTemplateName;
    }

    public String getDomainUuid() {
        return _domainUuid;
    }

    public String getVpcUuid() {
        return _vpcUuid;
    }

    public String getDomainTemplateName() {
        return _domainTemplateName;
    }

    public static class Builder implements CmdBuilder<ShutDownVpcVspCommand> {
        private String _domainUuid;
        private String _vpcUuid;
        private String _domainTemplateName;

        public Builder domainUuid(String domainUuid) {
            this._domainUuid = domainUuid;
            return this;
        }

        public Builder vpcUuid(String vpcUuid) {
            this._vpcUuid = vpcUuid;
            return this;
        }

        public Builder domainTemplateName(String domainTemplateName) {
            this._domainTemplateName = domainTemplateName;
            return this;
        }

        @Override
        public ShutDownVpcVspCommand build() {
            return new ShutDownVpcVspCommand(_domainUuid, _vpcUuid, _domainTemplateName);
        }
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShutDownVpcVspCommand)) return false;
        if (!super.equals(o)) return false;

        ShutDownVpcVspCommand that = (ShutDownVpcVspCommand) o;

        if (_domainTemplateName != null ? !_domainTemplateName.equals(that._domainTemplateName) : that._domainTemplateName != null)
            return false;
        if (_domainUuid != null ? !_domainUuid.equals(that._domainUuid) : that._domainUuid != null) return false;
        if (_vpcUuid != null ? !_vpcUuid.equals(that._vpcUuid) : that._vpcUuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_domainUuid != null ? _domainUuid.hashCode() : 0);
        result = 31 * result + (_vpcUuid != null ? _vpcUuid.hashCode() : 0);
        result = 31 * result + (_domainTemplateName != null ? _domainTemplateName.hashCode() : 0);
        return result;
    }
}
