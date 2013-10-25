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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ResourceDetail;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenterDetailVO;
import com.cloud.dc.dao.DataCenterDetailsDao;
import com.cloud.dc.dao.ResourceDetailDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.server.ResourceMetaDataService;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.server.TaggedResourceService;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.VMTemplateDetailVO;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.vm.NicDetailVO;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.dao.NicDetailDao;
import com.cloud.vm.dao.UserVmDetailsDao;


@Component
@Local(value = { ResourceMetaDataService.class, ResourceMetaDataManager.class })
public class ResourceMetaDataManagerImpl extends ManagerBase implements ResourceMetaDataService, ResourceMetaDataManager {
    public static final Logger s_logger = Logger.getLogger(ResourceMetaDataManagerImpl.class);
    @Inject
    VolumeDetailsDao _volumeDetailDao;
    @Inject
    NicDetailDao _nicDetailDao;
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
    
    private static Map<ResourceObjectType, ResourceDetailDao<? extends ResourceDetail>> _daoMap= 
            new HashMap<ResourceObjectType, ResourceDetailDao<? extends ResourceDetail>>();
    

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _daoMap.put(ResourceObjectType.UserVm, _userVmDetailDao);
        _daoMap.put(ResourceObjectType.Volume, _volumeDetailDao);
        _daoMap.put(ResourceObjectType.Template, _templateDetailsDao);
        _daoMap.put(ResourceObjectType.Network, _networkDetailsDao);
        _daoMap.put(ResourceObjectType.Nic, _nicDetailDao);
        _daoMap.put(ResourceObjectType.ServiceOffering, _serviceOfferingDetailsDao);
        _daoMap.put(ResourceObjectType.Zone, _dcDetailsDao);
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
                    long id = _taggedResourceMgr.getResourceId(resourceId, resourceType);
                    String value = details.get(key);

                    if (value == null || value.isEmpty()) {
                        throw new InvalidParameterValueException("Value for the key " + key + " is either null or empty");
                    }

                    DetailDaoHelper newDetailDaoHelper = new DetailDaoHelper(resourceType);
                    ResourceDetail detail = null;
                    
                    // TODO - Have a better design here for getting the DAO.
                    if(resourceType == ResourceObjectType.Volume){
                        detail = new VolumeDetailVO(id, key, value);
                    } else if (resourceType == ResourceObjectType.Nic){
                        detail = new NicDetailVO(id, key, value);
                    } else if (resourceType == ResourceObjectType.Zone){
                        detail = new DataCenterDetailVO(id, key, value);
                    } else if (resourceType == ResourceObjectType.Network){
                        detail = new NetworkDetailVO(id, key, value);
                    } else if (resourceType == ResourceObjectType.UserVm) {
                        detail = new UserVmDetailVO(id, key, value);
                    } else if (resourceType == ResourceObjectType.Template) {
                        detail = new VMTemplateDetailVO(id, key, value);
                    } else if (resourceType == ResourceObjectType.ServiceOffering) {
                        detail = new ServiceOfferingDetailsVO(id, key, value);
                    }
                    newDetailDaoHelper.addDetail(detail);
                        
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
        
        private DetailDaoHelper(ResourceObjectType resourceType) {
            if (!resourceType.resourceMetadataSupport()) {
                throw new UnsupportedOperationException("ResourceType " + resourceType + " doesn't support metadata");
            }
            this.resourceType = resourceType;
        }
        
        private void addDetail(ResourceDetail detail) {
            ResourceDetailDao<ResourceDetail> dao = (ResourceDetailDao<ResourceDetail>)_daoMap.get(resourceType);
            dao.addDetail(detail);   
        }
        
        private void removeDetail(long resourceId, String key) {
            ResourceDetailDao<? extends ResourceDetail> dao = _daoMap.get(resourceType);
            dao.removeDetail(resourceId, key);
        }
        
        private List<? extends ResourceDetail> getDetails(long resourceId) {
            ResourceDetailDao<? extends ResourceDetail> dao = _daoMap.get(resourceType);
            List<? extends ResourceDetail> detailList = new ArrayList<ResourceDetail>();        
            detailList = dao.findDetailsList(resourceId);
            return detailList;
        }
        
        private ResourceDetail getDetail(long resourceId, String key) {
            ResourceDetailDao<? extends ResourceDetail> dao = _daoMap.get(resourceType);
            return dao.findDetail(resourceId, key);
        }
    }
    
    @Override
    public List<? extends ResourceDetail> getDetails(long resourceId, ResourceObjectType resourceType) {
        DetailDaoHelper newDetailDaoHelper = new DetailDaoHelper(resourceType);
        return newDetailDaoHelper.getDetails(resourceId);  
    }
    
    @Override
    public ResourceDetail getDetail(long resourceId, ResourceObjectType resourceType, String key) {
        DetailDaoHelper newDetailDaoHelper = new DetailDaoHelper(resourceType);
        return newDetailDaoHelper.getDetail(resourceId, key);  
    }
}
