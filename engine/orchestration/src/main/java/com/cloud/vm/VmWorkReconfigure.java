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
package com.cloud.vm;


import java.util.Map;

public class VmWorkReconfigure extends VmWork {
    private static final long serialVersionUID = -4517030323758086615L;

    Long oldServiceOfferingId;
    Long newServiceOfferingId;

    Map<String, String> customParameters;
    boolean sameHost;

    public VmWorkReconfigure(long userId, long accountId, long vmId, String handlerName, Long oldServiceOfferingId,
            Long newServiceOfferingId, Map<String, String> customParameters, boolean sameHost) {

        super(userId, accountId, vmId, handlerName);

        this.oldServiceOfferingId = oldServiceOfferingId;
        this.newServiceOfferingId = newServiceOfferingId;
        this.customParameters = customParameters;
        this.sameHost = sameHost;
    }

    public VmWorkReconfigure(VmWork vmWork, long oldServiceOfferingId, long newServiceOfferingId, Map<String, String> customParameters, boolean sameHost) {
        super(vmWork);
        this.oldServiceOfferingId = oldServiceOfferingId;
        this.newServiceOfferingId = newServiceOfferingId;
        this.customParameters = customParameters;
        this.sameHost = sameHost;
    }

    public Long getOldServiceOfferingId() {
        return oldServiceOfferingId;
    }

    public Long getNewServiceOfferingId() {
        return newServiceOfferingId;
    }

    public Map<String, String> getCustomParameters() {
        return customParameters;
    }

    public boolean isSameHost() {
        return sameHost;
    }
}
