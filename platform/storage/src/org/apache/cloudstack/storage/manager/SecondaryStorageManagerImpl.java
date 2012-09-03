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
package org.apache.cloudstack.storage.manager;

import java.util.List;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;

import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.utils.component.Inject;

public class SecondaryStorageManagerImpl implements SecondaryStorageManager {
	public DataStore getStore(Volume volume) {
		// TODO Auto-generated method stub
		return null;
	}

	public DataStore getImageStore(DataStore destStore) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<DataStore> getImageStores() {
		
		return null;
	}

	public DataStore getStore(Snapshot snapshot) {
		// TODO Auto-generated method stub
		return null;
	}

}
