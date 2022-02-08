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
package com.cloud.vm.dao;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;

public class UserVmDaoImplTest extends TestCase {
    @Inject
    UserVmDao dao;

    public void makeAndVerifyEntry(Long vmId, String instanceName, String displayName, long templateId, boolean userdataFlag, Hypervisor.HypervisorType hypervisor,
        long guestOsId, boolean haEnabled, boolean limitCpuUse, long domainId, long accountId, long serviceOfferingId, String name, Long diskOfferingId) {

        dao.expunge(vmId);
        String userdata;

        if (userdataFlag) {
            // Generate large userdata to simulate 32k of random string data for userdata submitted through HTTP POST requests.
            userdata = RandomStringUtils.randomAlphanumeric(32 * 1024);
        } else {
            // Generate normal sized userdata to simulate 2k of random string data.
            userdata = RandomStringUtils.randomAlphanumeric(2 * 1024);
        }

        // Persist the data.
        UserVmVO vo =
            new UserVmVO(vmId, instanceName, displayName, templateId, hypervisor, guestOsId, haEnabled, limitCpuUse, domainId, accountId, 1, serviceOfferingId, userdata,
                name);
        dao.persist(vo);

        vo = dao.findById(vmId);
        assert (vo.getType() == VirtualMachine.Type.User) : "Incorrect type " + vo.getType();

        // Check whether the userdata matches what we generated.
        assert (vo.getUserData().equals(userdata)) : "User data retrieved does not match userdata generated as input";

    }

    @Test
    public void testPersist() {
        Long vmId = 2222l;
        makeAndVerifyEntry(vmId, "vm1", "vmdisp1", 1l, false, Hypervisor.HypervisorType.KVM, 1l, false, true, 1l, 1l, 1l, "uservm1", 1l);
        makeAndVerifyEntry(vmId, "vm1", "vmdisp1", 1l, true, Hypervisor.HypervisorType.KVM, 1l, false, true, 1l, 1l, 1l, "uservm1", 1l);
    }

}
