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
package org.apache.cloudstack.storage.volume;

import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;

public class VolumeInfo {
	private long size;
	private String uuid;
	private String path;
	private PrimaryDataStoreInfo dataStoreInfo;
	private String baseTemplateUuid;
	private String baseTemplatePath;
	private VolumeType type;
	private VolumeDiskType diskType;
	
	public VolumeInfo(VolumeEntity volume) {
		this.size = volume.getSize();
		this.uuid = volume.getUuid();
		this.baseTemplatePath = volume.getTemplatePath();
		this.baseTemplateUuid = volume.getTemplateUuid();
		//this.dataStoreInfo = volume.getDataStoreInfo();
		this.diskType = volume.getDiskType();
		this.type = volume.getType();
	}
	
	public long getSize() {
		return this.size;
	}
	
	public String getUuid() {
		return this.uuid;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public PrimaryDataStoreInfo getDataStore() {
		return this.dataStoreInfo;
	}
	
	public String getTemplateUuid() {
		return this.baseTemplateUuid;
	}
	
	public String getTemplatePath() {
		return this.baseTemplatePath;
	}
	
	public VolumeType getType() {
		return this.type;
	}
	
	public VolumeDiskType getDiskType() {
		return this.diskType;
	}
}
