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

package com.cloud.agent.api;

public class UpdateBcfAttachmentCommand extends BcfCommand {
    private final String _networkId;
    private final String _attachmentId;
    private final String _tenantId;
    private final String _attachmentName;

    public UpdateBcfAttachmentCommand(final String networkId, final String attachmentId,
            final String tenantId, final String attachmentName) {
        this._networkId = networkId;
        this._attachmentId = attachmentId;
        this._tenantId = tenantId;
        this._attachmentName = attachmentName;
    }

    public String getNetworkId() {
        return _networkId;
    }

    public String getAttachmentId() {
        return _attachmentId;
    }

    public String getTenantId() {
        return _tenantId;
    }

    public String getPortName() {
        return _attachmentName;
    }
}
