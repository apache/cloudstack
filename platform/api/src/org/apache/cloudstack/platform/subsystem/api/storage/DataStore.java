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
package org.apache.cloudstack.platform.subsystem.api.storage;

import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate;

public interface DataStore {
	public class DataStoreRef {
		
	}
	
	public class DataStoreDriverRef {
		
	}
	
	public enum StoreType {
		Primary,
		Backup;
	}
	public class StoreScope {
		public long zoneId;
		private long clusterId;
		private long hostId;
	}
	
	String getURI();
	String getUUID();
	long getCluterId();
	long getPodId();
	long getZoneId();
	String getPath();
	StoreType getType();
	StoragePoolType getPoolType();
	StoreScope getScope();
	boolean isSharedStorage();
	Long getId();
	DataStoreDriver getDataStoreDriver();
	DataStoreEndPointSelector getEndPointSelector();
	FileSystem getFileSystem();
	VolumeStrategy getVolumeStrategy();
	SnapshotStrategy getSnapshotStrategy();
	BackupStrategy getBackupStrategy();
	TemplateStrategy getTemplateStrategy();
	DataStoreLifeCycle getLifeCycle();
	
	VolumeProfile prepareVolume(Volume volume, DataStore destStore);
	SnapshotProfile prepareSnapshot(Snapshot snapshot, DataStore destStore);
	TemplateProfile prepareTemplate(VirtualMachineTemplate template, DataStore destStore);
	boolean contains(Volume volume);
	boolean contains(Snapshot snapshot);
	boolean contains(TemplateProfile template);
	TemplateProfile get(TemplateProfile template);
	StorageFilerTO getTO();
}
