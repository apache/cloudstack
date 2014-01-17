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
package org.apache.cloudstack.api.command.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.apache.cloudstack.api.command.admin.usage.GetUsageRecordsCmd;
import org.apache.cloudstack.usage.Usage;
import org.apache.cloudstack.usage.UsageService;

import com.cloud.utils.Pair;

public class UsageCmdTest extends TestCase {

    private GetUsageRecordsCmd getUsageRecordsCmd;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    @Before
    public void setUp() {

        getUsageRecordsCmd = new GetUsageRecordsCmd() {

        };
    }

    @Test
    public void testExecuteSuccess() {
        UsageService usageService = Mockito.mock(UsageService.class);
        getUsageRecordsCmd._usageService = usageService;
        getUsageRecordsCmd.execute();
    }

    @Test
    public void testExecuteEmptyResult() {

        UsageService usageService = Mockito.mock(UsageService.class);

        Pair<List<? extends Usage>, Integer> usageRecords = new Pair<List<? extends Usage>, Integer>(new ArrayList<Usage>(), new Integer(0));

        Mockito.when(usageService.getUsageRecords(getUsageRecordsCmd)).thenReturn(usageRecords);

        getUsageRecordsCmd._usageService = usageService;
        getUsageRecordsCmd.execute();

    }

}
