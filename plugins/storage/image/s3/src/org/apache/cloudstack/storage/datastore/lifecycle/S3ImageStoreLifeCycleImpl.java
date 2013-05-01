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
package org.apache.cloudstack.storage.datastore.lifecycle;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreHelper;
import org.apache.cloudstack.storage.image.datastore.ImageStoreProviderManager;
import org.apache.cloudstack.storage.image.store.lifecycle.ImageStoreLifeCycle;
import org.apache.log4j.Logger;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.kvm.discoverer.KvmDummyResourceBase;
import com.cloud.resource.Discoverer;
import com.cloud.resource.ResourceListener;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ServerResource;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.s3.S3Manager;
import com.cloud.utils.UriUtils;

public class S3ImageStoreLifeCycleImpl implements ImageStoreLifeCycle {

    private static final Logger s_logger = Logger
            .getLogger(S3ImageStoreLifeCycleImpl.class);
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
	protected ImageStoreDao imageStoreDao;
	@Inject
	ImageStoreHelper imageStoreHelper;
	@Inject
	ImageStoreProviderManager imageStoreMgr;
    @Inject
    S3Manager                      _s3Mgr;

    protected List<? extends Discoverer> _discoverers;
    public List<? extends Discoverer> getDiscoverers() {
        return _discoverers;
    }
    public void setDiscoverers(List<? extends Discoverer> _discoverers) {
        this._discoverers = _discoverers;
    }

	public S3ImageStoreLifeCycleImpl() {
	}


    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {

        Long dcId = (Long) dsInfos.get("zoneId");
        String url = (String) dsInfos.get("url");
        String providerName = (String)dsInfos.get("providerName");
        ScopeType scope = (ScopeType)dsInfos.get("scope");
        DataStoreRole role =(DataStoreRole) dsInfos.get("role");
        Map<String, String> details = (Map<String, String>)dsInfos.get("details");

        s_logger.info("Trying to add a S3 store in data center " + dcId);

        try{
        // verify S3 parameters
        _s3Mgr.verifyS3Fields(details);
        }
        catch (DiscoveryException ex){
            throw new InvalidParameterValueException("failed to verify S3 parameters!");
        }

        Map<String, Object> imageStoreParameters = new HashMap<String, Object>();
        imageStoreParameters.put("name", url);
        imageStoreParameters.put("zoneId", dcId);
        imageStoreParameters.put("url", url);
        String protocol = "http";
        String useHttps = details.get(ApiConstants.S3_HTTPS_FLAG);
        if (useHttps != null && Boolean.parseBoolean(useHttps)){
            protocol = "https";
        }
        imageStoreParameters.put("protocol", protocol);
        if (scope != null) {
            imageStoreParameters.put("scope", scope);
        } else {
            imageStoreParameters.put("scope", ScopeType.REGION);
        }
        imageStoreParameters.put("providerName", providerName);
        imageStoreParameters.put("role", role);

        ImageStoreVO ids = imageStoreHelper.createImageStore(imageStoreParameters, details);
        return imageStoreMgr.getImageStore(ids.getId());
    }


    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean attachHost(DataStore store, HostScope scope,
            StoragePoolInfo existingInfo) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean dettach() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean unmanaged() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean maintain(DataStore store) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean cancelMaintain(DataStore store) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean deleteDataStore(DataStore store) {
        // TODO Auto-generated method stub
        return false;
    }
}
