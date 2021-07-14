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

package org.apache.cloudstack.network.contrail.management;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.vm.UserVmVO;

public class ServiceVirtualMachine extends UserVmVO {
    public ServiceVirtualMachine(long id, String instanceName, String name, long templateId, long serviceOfferingId, HypervisorType hypervisorType, long guestOSId,
                                 long dataCenterId, long domainId, long accountId, long userId, boolean haEnabled) {
        super(id, instanceName, name, templateId, hypervisorType, guestOSId, false, false, domainId, accountId, userId, serviceOfferingId, null, name);
    }
}
