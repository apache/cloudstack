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
package org.apache.cloudstack.storage.datastore.configurator.validator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.to.NfsPrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeTO;

import com.cloud.utils.exception.CloudRuntimeException;

public class NfsProtocolTransformer implements StorageProtocolTransformer {
    private final PrimaryDataStoreDao dataStoreDao;
    
    public NfsProtocolTransformer(PrimaryDataStoreDao dao) {
        this.dataStoreDao = dao;
    }
    
    @Override
    public boolean normalizeUserInput(Map<String, String> params) {
    	String url = params.get("url");
    	
    	try {
			URI uri = new URI(url);
			if (!"nfs".equalsIgnoreCase(uri.getScheme())) {
				throw new CloudRuntimeException("invalid protocol, must starting with nfs");
			}
	        String storageHost = uri.getHost();
	        String hostPath = uri.getPath();
	        String userInfo = uri.getUserInfo();
	        int port = uri.getPort();
	        if (port == -1) {
                port = 2049;
            }
			params.put("server", storageHost);
			params.put("path", hostPath);
			params.put("user", userInfo);
			params.put("port", String.valueOf(port));
			params.put("uuid", UUID.nameUUIDFromBytes((storageHost + hostPath).getBytes()).toString());
		} catch (URISyntaxException e) {
			throw new CloudRuntimeException("invalid url: " + e.toString());
		}
        return true;
    }

    @Override
    public List<String> getInputParamNames() {
        List<String> paramNames = new ArrayList<String>();
        paramNames.add("server");
        paramNames.add("path");
        return paramNames;
    }

    @Override
    public PrimaryDataStoreTO getDataStoreTO(PrimaryDataStoreInfo dataStore) {
        NfsPrimaryDataStoreTO dataStoreTO = new NfsPrimaryDataStoreTO(dataStore);
        PrimaryDataStoreVO dataStoreVO = dataStoreDao.findById(dataStore.getId());
        dataStoreTO.setServer(dataStoreVO.getHostAddress());
        dataStoreTO.setPath(dataStoreVO.getPath());
        return dataStoreTO;
    }

    @Override
    public VolumeTO getVolumeTO(VolumeInfo volume) {
        VolumeTO vol = new VolumeTO(volume);
        vol.setDataStore(this.getDataStoreTO(volume.getDataStore()));
        return vol;
    }

}
