/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore;

import java.util.List;

import org.apache.cloudstack.storage.datastore.db.DataStoreVO;
import org.apache.cloudstack.storage.volume.disktype.VolumeDiskType;

import com.cloud.hypervisor.Hypervisor.HypervisorType;

public class PrimaryDataStoreInfoImpl implements PrimaryDataStoreInfo {
	protected List<HypervisorType> supportedHypervs;
	protected List<VolumeDiskType> supportedDiskTypes;
	protected long caapcity;
	protected long avail;
	protected boolean localStorage;
	
	public PrimaryDataStoreInfoImpl(List<HypervisorType> hypers, List<VolumeDiskType> diskTypes, 
			long capacity, long avail, boolean localStorage) {
		this.avail = avail;
		this.caapcity = capacity;
		this.localStorage = localStorage;
		this.supportedDiskTypes = diskTypes;
		this.supportedHypervs = hypers;
	}
	
	@Override
	public boolean isHypervisorSupported(HypervisorType hypervisor) {
		return this.supportedHypervs.contains(hypervisor) ? true : false;
	}

	@Override
	public boolean isLocalStorageSupported() {
		return this.localStorage;
	}

	@Override
	public boolean isVolumeDiskTypeSupported(VolumeDiskType diskType) {
		return this.supportedDiskTypes.contains(diskType) ? true : false;
	}

	@Override
	public long getCapacity() {
		return this.caapcity;
	}

	@Override
	public long getAvailableCapacity() {
		return this.avail;
	}
}
