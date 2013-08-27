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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualMachineDiskInfoBuilder {
	Map<String, List<String>> disks;
	
	public VirtualMachineDiskInfoBuilder() {
		disks = new HashMap<String, List<String>>(); 
	}
	
	public void addDisk(String diskDeviceBusName, String diskBackingFilePath) {
		List<String> chain = getDiskChainContainer(diskDeviceBusName);
		chain.add(diskBackingFilePath);
	}
	
	public int getDiskCount() {
		return disks.keySet().size();
	}
	
	public List<VirtualMachineDiskInfo> getAllDiskInfo() {
		List<VirtualMachineDiskInfo> infoList = new ArrayList<VirtualMachineDiskInfo>();
		for(Map.Entry<String, List<String>> entry : disks.entrySet()) {
			VirtualMachineDiskInfo diskInfo = new VirtualMachineDiskInfo();
			diskInfo.setDiskDeviceBusName(entry.getKey());
			diskInfo.setDiskChain(entry.getValue().toArray(new String[1]));
			infoList.add(diskInfo);
		}
		return infoList;
	}
	
	public VirtualMachineDiskInfo getDiskInfoByDeviceBusName(String diskDeviceBusName) {
		List<String> chain = disks.get(diskDeviceBusName);
		if(chain != null && chain.size() > 0) {
			VirtualMachineDiskInfo diskInfo = new VirtualMachineDiskInfo();
			diskInfo.setDiskDeviceBusName(diskDeviceBusName);
			diskInfo.setDiskChain(chain.toArray(new String[1]));
			return diskInfo;
		}
		
		return null;
	}
	
	public VirtualMachineDiskInfo getDiskInfoByBackingFileBaseName(String diskBackingFileBaseName) {
		for(Map.Entry<String, List<String>> entry : disks.entrySet()) {
			if(chainContains(entry.getValue(), diskBackingFileBaseName)) {
				VirtualMachineDiskInfo diskInfo = new VirtualMachineDiskInfo();
				diskInfo.setDiskDeviceBusName(entry.getKey());
				diskInfo.setDiskChain(entry.getValue().toArray(new String[1]));
				return diskInfo;
			}
		}
		
		return null;
	}
	
	private List<String> getDiskChainContainer(String diskDeviceBusName) {
		assert(diskDeviceBusName != null);
		List<String> chain = disks.get(diskDeviceBusName);
		if(chain == null) {
			chain = new ArrayList<String>();
			disks.put(diskDeviceBusName, chain);
		}
		return chain;
	}
	
	private static boolean chainContains(List<String> chain, String diskBackingFileBaseName) {
		for(String backing : chain) {
			DatastoreFile file = new DatastoreFile(backing);
			
			if(file.getFileBaseName().contains(diskBackingFileBaseName))
				return true;
		}
		
		return false;
	}
}
