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
package org.apache.cloudstack.storage;

import java.util.Date;

import org.apache.cloudstack.platform.subsystem.api.storage.DataObjectBackupStorageOperationState;

import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;

public interface VolumeBackupRef {
	public DataObjectBackupStorageOperationState getOperationState();
	
	public String getInstallPath();

	public long getHostId();

	public long getVolumeId();

	public long getZoneId();

	public int getDownloadPercent();

	public  long getVolumeSize();

	public  Storage.ImageFormat getFormat();

	public  String getDownloadUrl();

	public  boolean getDestroyed();

	public  long getPhysicalSize();

	public  long getSize();

	public  String getLocalDownloadPath();

	public  String getChecksum();

	public  Status getDownloadState();

	public  Date getLastUpdated();

	public  Date getCreated();

	public  long getId();
}
