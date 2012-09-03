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
package org.apache.cloudstack.storage.driver;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPoint;
import org.apache.cloudstack.platform.subsystem.api.storage.VolumeProfile;

import org.apache.cloudstack.platform.subsystem.api.storage.TemplateStrategy;
import com.cloud.agent.api.storage.DownloadProgressCommand;
import com.cloud.agent.api.storage.DownloadProgressCommand.RequestType;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DownloadCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.storage.TemplateProfile;
import com.cloud.vm.DiskProfile;



public abstract class AbstractStorageDriver implements DataStoreDriver {
	protected DataStore _ds;
	protected TemplateStrategy _ts;
	
	public AbstractStorageDriver(DataStore ds) {
		_ds = ds;
		_ts = ds.getTemplateStrategy();
	}
	
	public TemplateProfile install(TemplateProfile tp, DataStoreEndPoint ep) {
		PrimaryStorageDownloadCommand dcmd = new PrimaryStorageDownloadCommand(tp.getName(), tp.getUrl(), tp.getFormat(), 
				0, _ds.getId(), _ds.getUUID(), _ts.getDownloadWait());
		dcmd.setSecondaryStorageUrl(tp.getImageStorageUri());
		dcmd.setPrimaryStorageUrl(_ds.getURI());
		PrimaryStorageDownloadAnswer asw = (PrimaryStorageDownloadAnswer)ep.sendCommand(dcmd);

		tp.setLocalPath(asw.getInstallPath());
		return tp;
	}
	
	public TemplateProfile register(TemplateProfile tp, DataStoreEndPoint ep, boolean freshDownload) {
		
		DownloadCommand dcmd =
				new DownloadCommand(_ds.getURI(), tp.getTemplate(), _ts.getMaxTemplateSizeInBytes());
		dcmd.setProxy(_ts.getHttpProxy());
		if (!freshDownload) {
			dcmd = new DownloadProgressCommand(dcmd, tp.getJobId(), RequestType.GET_OR_RESTART);
		}
		
		ep.sendCommand(dcmd);
		return tp;
	}

	public DiskProfile createVolumeFromTemplate(DiskProfile volProfile, TemplateProfile tp, DataStoreEndPoint ep) {
		CreateCommand cmd = new CreateCommand(volProfile, tp.getLocalPath(), _ds.getTO());
		CreateAnswer ans = (CreateAnswer)ep.sendCommand(cmd);
		VolumeTO created = ans.getVolume();
		DiskProfile diskProfile = new DiskProfile(volProfile);
		diskProfile.setPath(created.getPath());
		diskProfile.setSize(created.getSize());
		return diskProfile;
	}
}
