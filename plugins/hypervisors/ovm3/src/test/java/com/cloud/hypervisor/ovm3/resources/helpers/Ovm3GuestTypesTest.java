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

package com.cloud.hypervisor.ovm3.resources.helpers;

import org.junit.Test;

import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;

public class Ovm3GuestTypesTest {
    XmlTestResultTest results = new XmlTestResultTest();
    String ora = "Oracle Enterprise Linux 6.0 (64-bit)";
    Ovm3VmGuestTypes ovm3gt = new Ovm3VmGuestTypes();

    @Test
    public void testGetPvByOs() {
        results.basicStringTest(ovm3gt.getOvm3GuestType(ora), "xen_pvm");
    }
}
