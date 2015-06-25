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
package com.cloud.network.security;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.utils.Profiler;
import com.cloud.utils.component.ComponentContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/SecurityGroupManagerTestContext.xml")
public class SecurityGroupManagerImpl2Test extends TestCase {
    @Inject
    SecurityGroupManagerImpl2 _sgMgr = null;

    @Before
    public void setup() throws Exception {
        ComponentContext.initComponentsLifeCycle();
    }

    @Override
    @After
    public void tearDown() throws Exception {
    }

    protected void _schedule(final int numVms) {
        System.out.println("Starting");
        List<Long> work = new ArrayList<Long>();
        for (long i = 100; i <= 100 + numVms; i++) {
            work.add(i);
        }
        Profiler profiler = new Profiler();
        profiler.start();
        _sgMgr.scheduleRulesetUpdateToHosts(work, false, null);
        profiler.stop();

        System.out.println("Done " + numVms + " in " + profiler.getDurationInMillis() + " ms");
    }

    @Test
    public void testSchedule() throws ConfigurationException {
        _schedule(1000);
    }

    @Test
    public void testWork() throws ConfigurationException {
        _schedule(1000);
        _sgMgr.work();
    }
}
