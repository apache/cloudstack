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
package com.cloud.metadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ResourceDetail;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.FirewallRuleDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.dao.DataCenterDetailsDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.server.ResourceMetaDataService;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.server.TaggedResourceService;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.vm.dao.NicDetailsDao;
import com.cloud.vm.dao.UserVmDetailsDao;


@Component
@Local(value = { ResourceMetaDataService.class, ResourceMetaDataManager.class })
public class ResourceMetaDataManagerImpl extends ManagerBase implements ResourceMetaDataService, ResourceMetaDataManager {
    public static final Logger s_logger = Logger.getLogger(ResourceMetaDataManagerImpl.class);
    @Inject
    VolumeDetailsDao _volumeDetailDao;
    @Inject
    NicDetailsDao _nicDetailDao;
    @Inject
    UserVmDetailsDao _userVmDetailDao;
    @Inject
    DataCenterDetailsDao _dcDetailsDao;
    @Inject
    NetworkDetailsDao _networkDetailsDao;
    @Inject
    TaggedResourceService _taggedResourceMgr;
    @Inject
    VMTemplateDetailsDao _templateDetailsDao;
    @Inject
    ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    @Inject
    StoragePoolDetailsDao _storageDetailsDao;
    @Inject
    FirewallRuleDetailsDao _firewallRuleDetailsDao;
    
    private static Map<ResourceObjectType, ResourceDetailsDao<? extends ResourceDetail>> _daoMap= 
            new HashMap<ResourceObjectType, ResourceDetailsDao<? extends ResourceDetail>>();
    

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _daoMap.put(ResourceObjectType.UserVm, _userVmDetailDao);
        _daoMap.put(ResourceObjectType.Volume, _volumeDetailDao);
        _daoMap.put(ResourceObjectType.Template, _templateDetailsDao);
        _daoMap.put(ResourceObjectType.Network, _networkDetailsDao);
        _daoMap.put(ResourceObjectType.Nic, _nicDetailDao);
        _daoMap.put(ResourceObjectType.ServiceOffering, _serviceOfferingDetailsDao);
        _daoMap.put(ResourceObjectType.Zone, _dcDetailsDao);
        _daoMap.put(ResourceObjectType.Storage, _storageDetailsDao);
        _daoMap.put(ResourceObjectType.FirewallRule, _firewallRuleDetailsDao);
        
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
    

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_RESOURCE_DETAILS_CREATE, eventDescription = "creating resource meta data")
    public boolean addResourceMetaData(final String resourceId, final ResourceObjectType resourceType, final Map<String, String> details){
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                for (String key : details.keySet()) {
                    String value = details.get(key);

                    if (value == null || value.isEmpty()) {
                        throw new InvalidParameterValueException("Value for the key " + key + " is either null or empty");
                    }

                    DetailDaoHelper newDetailDaoHelper = new DetailDaoHelper(resourceType);
                    newDetailDaoHelper.addDetail( _taggedResourceMgr.getResourceId(resourceId, resourceType), key, value);                    
                }
                
                return true;
            }
        });
    }


    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_RESOURCE_DETAILS_DELETE, eventDescription = "deleting resource meta data")
    public boolean deleteResourceMetaData(String resourceId, ResourceObjectType resourceType, String key){
        long id = _taggedResourceMgr.getResourceId(resourceId, resourceType);
        
        DetailDaoHelper newDetailDaoHelper = new DetailDaoHelper(resourceType);
        newDetailDaoHelper.removeDetail(id, key);

        return true;
    }

    private class DetailDaoHelper {
        private ResourceObjectType resourceType;
        private ResourceDetailsDao<? super ResourceDetail> dao;
        
        private DetailDaoHelper(ResourceObjectType resourceType) {
            if (!resourceType.resourceMetadataSupport()) {
                throw new UnsupportedOperationException("ResourceType " + resourceType + " doesn't support metadata");
            }
            this.resourceType = resourceType;
            ResourceDetailsDao<?> dao = _daoMap.get(resourceType);
            if (dao == null) {
                throw new UnsupportedOperationException("ResourceType " + resourceType + " doesn't support metadata");
            }
            this.dao = (ResourceDetailsDao)_daoMap.get(resourceType);
        }
        
        private void removeDetail(long resourceId, String key) {
            dao.removeDetail(resourceId, key);
        }
        
        private ResourceDetail getDetail(long resourceId, String key) {
            return dao.findDetail(resourceId, key);
        }
        
        private void addDetail(long resourceId, String key, String value) {
            dao.addDetail(resourceId, key, value);
        }
        
        private Map<String, String> getDetailsMap(long resourceId, Boolean forDisplay) {            
            if (forDisplay == null) {
                return dao.listDetailsKeyPairs(resourceId);
            } else  {
                return dao.listDetailsKeyPairs(resourceId, forDisplay);
            }
        }
        
        private List<? extends ResourceDetail> getDetailsList(long resourceId, Boolean forDisplay) {
            if (forDisplay == null) {
                return dao.listDetails(resourceId);
            } else {
                return dao.listDetails(resourceId, forDisplay);
            }
        }
    }
    
    @Override
    public List<? extends ResourceDetail> getDetailsList(long resourceId, ResourceObjectType resourceType, Boolean forDisplay) {
        DetailDaoHelper newDetailDaoHelper = new DetailDaoHelper(resourceType);
        return newDetailDaoHelper.getDetailsList(resourceId, forDisplay);  
    }
    
    @Override
    public ResourceDetail getDetail(long resourceId, ResourceObjectType resourceType, String key) {
        DetailDaoHelper newDetailDaoHelper = new DetailDaoHelper(resourceType);
        return newDetailDaoHelper.getDetail(resourceId, key);
    }
    
    @Override 
    public Map<String, String> getDetailsMap(long resourceId, ResourceObjectType resourceType, Boolean forDisplay) {
        DetailDaoHelper newDetailDaoHelper = new DetailDaoHelper(resourceType);
        return newDetailDaoHelper.getDetailsMap(resourceId, forDisplay); 
    }
}
