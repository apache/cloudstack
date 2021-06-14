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

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.server.ResourceManagerUtil;
import org.apache.cloudstack.api.ResourceDetail;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.AutoScaleVmGroupDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.AutoScaleVmProfileDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.FirewallRuleDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.NetworkACLItemDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.NetworkACLListDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.RemoteAccessVpnDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.Site2SiteCustomerGatewayDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.Site2SiteVpnConnectionDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.Site2SiteVpnGatewayDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.SnapshotPolicyDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.UserDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.UserIpAddressDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.VpcGatewayDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.LBStickinessPolicyDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.LBHealthCheckPolicyDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.GuestOsDetailsDao;
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
    @Inject
    UserIpAddressDetailsDao _userIpAddressDetailsDao;
    @Inject
    RemoteAccessVpnDetailsDao _vpnDetailsDao;
    @Inject
    VpcDetailsDao _vpcDetailsDao;
    @Inject
    VpcGatewayDetailsDao _vpcGatewayDetailsDao;
    @Inject
    NetworkACLListDetailsDao _networkACLListDetailsDao;
    @Inject
    NetworkACLItemDetailsDao _networkACLDetailsDao;
    @Inject
    Site2SiteVpnGatewayDetailsDao _vpnGatewayDetailsDao;
    @Inject
    Site2SiteCustomerGatewayDetailsDao _customerGatewayDetailsDao;
    @Inject
    Site2SiteVpnConnectionDetailsDao _vpnConnectionDetailsDao;
    @Inject
    DiskOfferingDetailsDao _diskOfferingDetailsDao;
    @Inject
    UserDetailsDao _userDetailsDao;
    @Inject
    AutoScaleVmProfileDetailsDao _autoScaleVmProfileDetailsDao;
    @Inject
    AutoScaleVmGroupDetailsDao _autoScaleVmGroupDetailsDao;
    @Inject
    LBStickinessPolicyDetailsDao _stickinessPolicyDetailsDao;
    @Inject
    LBHealthCheckPolicyDetailsDao _healthcheckPolicyDetailsDao;
    @Inject
    SnapshotPolicyDetailsDao _snapshotPolicyDetailsDao;
    @Inject
    GuestOsDetailsDao _guestOsDetailsDao;
    @Inject
    NetworkOfferingDetailsDao _networkOfferingDetailsDao;
    @Inject
    ResourceManagerUtil resourceManagerUtil;

    private static Map<ResourceObjectType, ResourceDetailsDao<? extends ResourceDetail>> s_daoMap = new HashMap<ResourceObjectType, ResourceDetailsDao<? extends ResourceDetail>>();

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        s_daoMap.put(ResourceObjectType.UserVm, _userVmDetailDao);
        s_daoMap.put(ResourceObjectType.Volume, _volumeDetailDao);
        s_daoMap.put(ResourceObjectType.Template, _templateDetailsDao);
        s_daoMap.put(ResourceObjectType.Network, _networkDetailsDao);
        s_daoMap.put(ResourceObjectType.Nic, _nicDetailDao);
        s_daoMap.put(ResourceObjectType.ServiceOffering, _serviceOfferingDetailsDao);
        s_daoMap.put(ResourceObjectType.Zone, _dcDetailsDao);
        s_daoMap.put(ResourceObjectType.Storage, _storageDetailsDao);
        s_daoMap.put(ResourceObjectType.FirewallRule, _firewallRuleDetailsDao);
        s_daoMap.put(ResourceObjectType.PublicIpAddress, _userIpAddressDetailsDao);
        s_daoMap.put(ResourceObjectType.PortForwardingRule, _firewallRuleDetailsDao);
        s_daoMap.put(ResourceObjectType.LoadBalancer, _firewallRuleDetailsDao);
        s_daoMap.put(ResourceObjectType.RemoteAccessVpn, _vpnDetailsDao);
        s_daoMap.put(ResourceObjectType.Vpc, _vpcDetailsDao);
        s_daoMap.put(ResourceObjectType.PrivateGateway, _vpcGatewayDetailsDao);
        s_daoMap.put(ResourceObjectType.NetworkACLList, _networkACLListDetailsDao);
        s_daoMap.put(ResourceObjectType.NetworkACL, _networkACLDetailsDao);
        s_daoMap.put(ResourceObjectType.VpnGateway, _vpnGatewayDetailsDao);
        s_daoMap.put(ResourceObjectType.CustomerGateway, _customerGatewayDetailsDao);
        s_daoMap.put(ResourceObjectType.VpnConnection, _vpnConnectionDetailsDao);
        s_daoMap.put(ResourceObjectType.DiskOffering, _diskOfferingDetailsDao);
        s_daoMap.put(ResourceObjectType.User, _userDetailsDao);
        s_daoMap.put(ResourceObjectType.AutoScaleVmProfile, _autoScaleVmProfileDetailsDao);
        s_daoMap.put(ResourceObjectType.AutoScaleVmGroup, _autoScaleVmGroupDetailsDao);
        s_daoMap.put(ResourceObjectType.LBStickinessPolicy, _stickinessPolicyDetailsDao);
        s_daoMap.put(ResourceObjectType.LBHealthCheckPolicy, _healthcheckPolicyDetailsDao);
        s_daoMap.put(ResourceObjectType.SnapshotPolicy, _snapshotPolicyDetailsDao);
        s_daoMap.put(ResourceObjectType.GuestOs, _guestOsDetailsDao);
        s_daoMap.put(ResourceObjectType.NetworkOffering, _networkOfferingDetailsDao);
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
    public boolean addResourceMetaData(final String resourceId, final ResourceObjectType resourceType, final Map<String, String> details, final boolean forDisplay) {
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                for (String key : details.keySet()) {
                    String value = details.get(key);

                    if (value == null || value.isEmpty()) {
                        throw new InvalidParameterValueException("Value for the key " + key + " is either null or empty");
                    }

                    DetailDaoHelper newDetailDaoHelper = new DetailDaoHelper(resourceType);
                    newDetailDaoHelper.addDetail(resourceManagerUtil.getResourceId(resourceId, resourceType), key, value, forDisplay);
                }

                return true;
            }
        });
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_RESOURCE_DETAILS_DELETE, eventDescription = "deleting resource meta data")
    public boolean deleteResourceMetaData(String resourceId, ResourceObjectType resourceType, String key) {
        long id = resourceManagerUtil.getResourceId(resourceId, resourceType);

        DetailDaoHelper newDetailDaoHelper = new DetailDaoHelper(resourceType);
        if (key != null) {
            newDetailDaoHelper.removeDetail(id, key);
        } else {
            newDetailDaoHelper.removeDetails(id);
        }

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
            ResourceDetailsDao<?> dao = s_daoMap.get(resourceType);
            if (dao == null) {
                throw new UnsupportedOperationException("ResourceType " + resourceType + " doesn't support metadata");
            }
            this.dao = (ResourceDetailsDao)s_daoMap.get(resourceType);
        }

        private void removeDetail(long resourceId, String key) {
            dao.removeDetail(resourceId, key);
        }

        private void removeDetails(long resourceId) {
            dao.removeDetails(resourceId);
        }

        private ResourceDetail getDetail(long resourceId, String key) {
            return dao.findDetail(resourceId, key);
        }

        private List<? extends ResourceDetail> getDetails(String key, String value, Boolean forDisplay) {
            return dao.findDetails(key, value, forDisplay);
        }

        private void addDetail(long resourceId, String key, String value, boolean forDisplay) {
            dao.addDetail(resourceId, key, value, forDisplay);
        }

        private Map<String, String> getDetailsMap(long resourceId, Boolean forDisplay) {
            if (forDisplay == null) {
                return dao.listDetailsKeyPairs(resourceId);
            } else {
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
    public List<? extends ResourceDetail> getDetails(ResourceObjectType resourceType, String key, String value, Boolean forDisplay){
        DetailDaoHelper newDetailDaoHelper = new DetailDaoHelper(resourceType);
        return newDetailDaoHelper.getDetails(key, value, forDisplay);
    }

    @Override
    public Map<String, String> getDetailsMap(long resourceId, ResourceObjectType resourceType, Boolean forDisplay) {
        DetailDaoHelper newDetailDaoHelper = new DetailDaoHelper(resourceType);
        return newDetailDaoHelper.getDetailsMap(resourceId, forDisplay);
    }
}
