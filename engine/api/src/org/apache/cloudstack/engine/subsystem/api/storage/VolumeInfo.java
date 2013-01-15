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
package org.apache.cloudstack.engine.subsystem.api.storage;


import java.util.Date;

import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;

import com.cloud.storage.Volume;

public interface VolumeInfo {
	public long getSize();
	public String getUuid();
	public String getPath();
	public PrimaryDataStoreInfo getDataStore() ;
	public String getTemplateUuid();
	public String getTemplatePath();
	public VolumeType getType();
	public VolumeDiskType getDiskType();
	public long getId();
	public Volume.State getCurrentState();
	public Volume.State getDesiredState();
	public Date getCreatedDate();
	public Date getUpdatedDate();
	public String getOwner();
	public String getName();
	public boolean isAttachedVM();
}
