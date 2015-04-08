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

import java.util.ArrayList;
import java.util.List;

import com.cloud.network.bigswitch.AclData;

public class UpdateBcfRouterCommand extends BcfCommand {
    private String tenantId;
    private String publicIp;
    private List<AclData> acls;

    public UpdateBcfRouterCommand(String tenantId){
        this.tenantId = tenantId;
        this.publicIp = null;
        this.acls = new ArrayList<AclData>();
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public List<AclData> getAcls() {
        return acls;
    }

    public void addAcl(AclData acl){
        this.acls.add(acl);
    }
}
