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
package com.cloud.hypervisor.vmware.mo;

import java.util.GregorianCalendar;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.mo.SnapshotDescriptor.SnapshotInfo;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.utils.Pair;
import com.cloud.utils.testcase.Log4jEnabledTestCase;
import com.google.gson.Gson;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.VirtualMachineConfigSpec;

public class TestVmwareMO extends Log4jEnabledTestCase {
    private static final Logger s_logger = Logger.getLogger(TestVmwareMO.class);

    public void test() {
    }
}

