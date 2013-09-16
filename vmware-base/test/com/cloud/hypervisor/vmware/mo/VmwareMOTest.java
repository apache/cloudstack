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

import com.cloud.hypervisor.vmware.util.VmwareContext;

public class VmwareMOTest {

	public static void main(String args[]) {
        try {
/*        	
        	VmwareContext context = TestVmwareContextFactory.create("10.223.80.29", "Administrator", "Suite219");
        	
        	DatacenterMO dcMo = new DatacenterMO(context, "vsphere5");
        	HostMO hostMo = new HostMO(context, dcMo.findHost("10.223.80.27"));

        	DatastoreMO dsMo = new DatastoreMO(context, dcMo.findDatastore("Storage1"));
        	hostMo.importVmFromOVF("/tmp/ubuntu-12.04.1-desktop-i386-nest-13.02.04.ovf", "Test123", dsMo, "thin");

        	
        	VirtualMachineMO vmMo = dcMo.findVm("i-2-3-VM");
        	Thread.sleep(10*60000);
        	vmMo.removeAllSnapshots();
*/        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
}

