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
package com.cloud.usage;

import com.cloud.usage.parser.*;
import com.cloud.user.AccountVO;
import com.cloud.utils.component.ComponentContext;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.Date;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/UsageManagerTestContext.xml")
public class UsageManagerTest extends TestCase {
    @Inject
    UsageManagerImpl _usageMgr = null;
    @Inject
    VMInstanceUsageParser vmParser = null;
    @Inject
    IPAddressUsageParser ipParser = null;
    @Inject
    LoadBalancerUsageParser lbParser = null;
    @Inject
    NetworkOfferingUsageParser noParser = null;
    @Inject
    NetworkUsageParser netParser = null;
    @Inject
    VmDiskUsageParser vmdiskParser = null;
    @Inject
    PortForwardingUsageParser pfParser = null;
    @Inject
    SecurityGroupUsageParser sgParser = null;
    @Inject
    StorageUsageParser stParser = null;
    @Inject
    VolumeUsageParser volParser = null;
    @Inject
    VPNUserUsageParser vpnParser = null;

    Date startDate = null;
    Date endDate = null;

    @Before
    public void setup() throws Exception {
        System.setProperty("pid", "5678");
        ComponentContext.initComponentsLifeCycle();
        startDate = new Date();
        endDate = new Date(100000L +  System.currentTimeMillis());
    }

    @Test
    public void testParse() throws ConfigurationException {
        UsageJobVO job = new UsageJobVO();
        _usageMgr.parse(job, System.currentTimeMillis(), 100000L +  System.currentTimeMillis());
    }

    @Test
    public void testSchedule() throws ConfigurationException {
        _usageMgr.scheduleParse();
    }

    @Test
    public void testParsers() throws ConfigurationException {
        AccountVO account = new AccountVO();
        account.setId(2L);
        vmParser.parse(account, startDate, endDate);
        ipParser.parse(account, startDate, endDate);
        lbParser.parse(account, startDate, endDate);
        noParser.parse(account, startDate, endDate);
        netParser.parse(account, startDate, endDate);
        vmdiskParser.parse(account, startDate, endDate);
        pfParser.parse(account, startDate, endDate);
        sgParser.parse(account, startDate, endDate);
        stParser.parse(account, startDate, endDate);
        volParser.parse(account, startDate, endDate);
        vpnParser.parse(account, startDate, endDate);
    }

}
