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

import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenterDetailVO;
import com.cloud.dc.dao.DataCenterDetailsDao;
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
    UserVmDetailsDao _userVmDetailsDao;
    @Inject
    ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
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
                        
                        if (!resourceType.resourceMetadataSupport())  {
                            throw new InvalidParameterValueException("The resource type " + resourceType + " doesn't support metadata (resource details)");
                        }

                        long id = _taggedResourceMgr.getResourceId(resourceId, resourceType);
                        // TODO - Have a better design here for getting the DAO.
                        if(resourceType == ResourceObjectType.Volume){
                            VolumeDetailVO detail = new VolumeDetailVO(id, key, value);
                            _volumeDetailDao.addDetail(detail);
                        } else if (resourceType == ResourceObjectType.Nic){
                            NicDetailVO detail = new NicDetailVO(id, key, value);
                            _nicDetailDao.addDetail(detail);
                        } else if (resourceType == ResourceObjectType.Zone){
                             DataCenterDetailVO dataCenterDetail = new DataCenterDetailVO(id, key, value);
                             _dcDetailsDao.addDetail(dataCenterDetail);
                        } else if (resourceType == ResourceObjectType.Network){
                            NetworkDetailVO detail = new NetworkDetailVO(id, key, value);
                            _networkDetailsDao.addDetail(detail);
                        } else if (resourceType == ResourceObjectType.UserVm) {
                            UserVmDetailVO detail = new UserVmDetailVO(id, key, value);
                            _userVmDetailsDao.addDetail(detail);
                        } else if (resourceType == ResourceObjectType.Template) {
                             VMTemplateDetailVO detail = new VMTemplateDetailVO(id, key, value);
                            _templateDetailsDao.addDetail(detail);
                        } else if (resourceType == ResourceObjectType.ServiceOffering) {
                            ServiceOfferingDetailsVO detail = new ServiceOfferingDetailsVO(id, key, value);
                            _serviceOfferingDetailsDao.addDetail(detail);
                        }
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
        
        if (!resourceType.resourceMetadataSupport()) {
            throw new InvalidParameterValueException("The resource type " + resourceType + " is not supported by the API yet");
        }
        
        // TODO - Have a better design here for getting the DAO.
        if (resourceType == ResourceObjectType.Volume){
           _volumeDetailDao.removeDetail(id, key);
        } else if (resourceType == ResourceObjectType.Nic){
            _nicDetailDao.removeDetail(id, key);
        } else if (resourceType == ResourceObjectType.UserVm) {
            _userVmDetailsDao.removeDetail(id, key); 
        } else if (resourceType == ResourceObjectType.Template) {
            _templateDetailsDao.removeDetail(id, key);
        } else if (resourceType == ResourceObjectType.Zone){
            _dcDetailsDao.removeDetail(id, key);
        } else if (resourceType == ResourceObjectType.ServiceOffering) {
            _serviceOfferingDetailsDao.removeDetail(id, key);
        } else if (resourceType == ResourceObjectType.Network) {
            _networkDetailsDao.removeDetail(id, key);
        }

        return true;
    }


}
