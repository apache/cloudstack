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
package com.cloud.tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.query.dao.ResourceTagJoinDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.StaticRouteDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.server.TaggedResourceService;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.DbUtil;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.uuididentity.dao.IdentityDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@Component
@Local(value = {TaggedResourceService.class})
public class TaggedResourceManagerImpl extends ManagerBase implements TaggedResourceService {
    public static final Logger s_logger = Logger.getLogger(TaggedResourceManagerImpl.class);

    private static Map<ResourceObjectType, GenericDao<?, Long>> _daoMap = new HashMap<ResourceObjectType, GenericDao<?, Long>>();

    @Inject
    AccountManager _accountMgr;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    ResourceTagJoinDao _resourceTagJoinDao;
    @Inject
    IdentityDao _identityDao;
    @Inject
    DomainManager _domainMgr;
    @Inject
    UserVmDao _userVmDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    VMTemplateDao _templateDao;
    @Inject
    SnapshotDao _snapshotDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    LoadBalancerDao _lbDao;
    @Inject
    PortForwardingRulesDao _pfDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    SecurityGroupDao _securityGroupDao;
    @Inject
    RemoteAccessVpnDao _vpnDao;
    @Inject
    IPAddressDao _publicIpDao;
    @Inject
    ProjectDao _projectDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    StaticRouteDao _staticRouteDao;
    @Inject
    VMSnapshotDao _vmSnapshotDao;
    @Inject
    NicDao _nicDao;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    @Inject
    DataCenterDao _dataCenterDao;
    @Inject
    ServiceOfferingDao _serviceOffDao;
    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    VpcGatewayDao _vpcGatewayDao;
    @Inject
    NetworkACLDao _networkACLListDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _daoMap.put(ResourceObjectType.UserVm, _userVmDao);
        _daoMap.put(ResourceObjectType.Volume, _volumeDao);
        _daoMap.put(ResourceObjectType.Template, _templateDao);
        _daoMap.put(ResourceObjectType.ISO, _templateDao);
        _daoMap.put(ResourceObjectType.Snapshot, _snapshotDao);
        _daoMap.put(ResourceObjectType.Network, _networkDao);
        _daoMap.put(ResourceObjectType.LoadBalancer, _lbDao);
        _daoMap.put(ResourceObjectType.PortForwardingRule, _pfDao);
        _daoMap.put(ResourceObjectType.FirewallRule, _firewallDao);
        _daoMap.put(ResourceObjectType.SecurityGroup, _securityGroupDao);
        _daoMap.put(ResourceObjectType.PublicIpAddress, _publicIpDao);
        _daoMap.put(ResourceObjectType.Project, _projectDao);
        _daoMap.put(ResourceObjectType.Vpc, _vpcDao);
        _daoMap.put(ResourceObjectType.Nic, _nicDao);
        _daoMap.put(ResourceObjectType.NetworkACL, _networkACLItemDao);
        _daoMap.put(ResourceObjectType.StaticRoute, _staticRouteDao);
        _daoMap.put(ResourceObjectType.VMSnapshot, _vmSnapshotDao);
        _daoMap.put(ResourceObjectType.RemoteAccessVpn, _vpnDao);
        _daoMap.put(ResourceObjectType.Zone, _dataCenterDao);
        _daoMap.put(ResourceObjectType.ServiceOffering, _serviceOffDao);
        _daoMap.put(ResourceObjectType.Storage, _storagePoolDao);
        _daoMap.put(ResourceObjectType.PrivateGateway, _vpcGatewayDao);
        _daoMap.put(ResourceObjectType.NetworkACLList, _networkACLListDao);

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
    public long getResourceId(String resourceId, ResourceObjectType resourceType) {
        GenericDao<?, Long> dao = _daoMap.get(resourceType);
        if (dao == null) {
            throw new CloudRuntimeException("Dao is not loaded for the resource type " + resourceType);
        }
        Class<?> claz = DbUtil.getEntityBeanType(dao);

        Long identityId = null;

        while (claz != null && claz != Object.class) {
            try {
                String tableName = DbUtil.getTableName(claz);
                if (tableName == null) {
                    throw new InvalidParameterValueException("Unable to find resource of type " + resourceType + " in the database");
                }
                identityId = _identityDao.getIdentityId(tableName, resourceId);
                if (identityId != null) {
                    break;
                }
            } catch (Exception ex) {
                //do nothing here, it might mean uuid field is missing and we have to search further
            }
            claz = claz.getSuperclass();
        }

        if (identityId == null) {
            throw new InvalidParameterValueException("Unable to find resource by id " + resourceId + " and type " + resourceType);
        }
        return identityId;
    }

    private Pair<Long, Long> getAccountDomain(long resourceId, ResourceObjectType resourceType) {

        Pair<Long, Long> pair = null;
        GenericDao<?, Long> dao = _daoMap.get(resourceType);
        Class<?> claz = DbUtil.getEntityBeanType(dao);
        while (claz != null && claz != Object.class) {
            try {
                String tableName = DbUtil.getTableName(claz);
                if (tableName == null) {
                    throw new InvalidParameterValueException("Unable to find resource of type " + resourceType + " in the database");
                }
                pair = _identityDao.getAccountDomainInfo(tableName, resourceId, resourceType);
                if (pair.first() != null || pair.second() != null) {
                    break;
                }
            } catch (Exception ex) {
                //do nothing here, it might mean uuid field is missing and we have to search further
            }
            claz = claz.getSuperclass();
        }

        Long accountId = pair.first();
        Long domainId = pair.second();

        if (accountId == null) {
            accountId = Account.ACCOUNT_ID_SYSTEM;
        }

        if (domainId == null) {
            domainId = Domain.ROOT_DOMAIN;
        }

        return new Pair<Long, Long>(accountId, domainId);
    }

    @Override
    public ResourceObjectType getResourceType(String resourceTypeStr) {

        for (ResourceObjectType type : ResourceTag.ResourceObjectType.values()) {
            if (type.toString().equalsIgnoreCase(resourceTypeStr)) {
                return type;
            }
        }
        throw new InvalidParameterValueException("Invalid resource type " + resourceTypeStr);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_TAGS_CREATE, eventDescription = "creating resource tags")
    public List<ResourceTag> createTags(final List<String> resourceIds, final ResourceObjectType resourceType, final Map<String, String> tags, final String customer) {
        final Account caller = CallContext.current().getCallingAccount();

        final List<ResourceTag> resourceTags = new ArrayList<ResourceTag>(tags.size());

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (String key : tags.keySet()) {
                    for (String resourceId : resourceIds) {
                        if (!resourceType.resourceTagsSupport()) {
                            throw new InvalidParameterValueException("The resource type " + resourceType + " doesn't support resource tags");
                        }

                        long id = getResourceId(resourceId, resourceType);
                        String resourceUuid = getUuid(resourceId, resourceType);

                        Pair<Long, Long> accountDomainPair = getAccountDomain(id, resourceType);
                        Long domainId = accountDomainPair.second();
                        Long accountId = accountDomainPair.first();
                        if (accountId != null) {
                            _accountMgr.checkAccess(caller, null, false, _accountMgr.getAccount(accountId));
                        } else if (domainId != null && caller.getType() != Account.ACCOUNT_TYPE_NORMAL) {
                            //check permissions;
                            _accountMgr.checkAccess(caller, _domainMgr.getDomain(domainId));
                        } else {
                            throw new PermissionDeniedException("Account " + caller + " doesn't have permissions to create tags" + " for resource " + key);
                        }

                        String value = tags.get(key);

                        if (value == null || value.isEmpty()) {
                            throw new InvalidParameterValueException("Value for the key " + key + " is either null or empty");
                        }

                        ResourceTagVO resourceTag =
                            new ResourceTagVO(key, value, accountDomainPair.first(), accountDomainPair.second(), id, resourceType, customer, resourceUuid);
                        resourceTag = _resourceTagDao.persist(resourceTag);
                        resourceTags.add(resourceTag);
                    }
                }
            }
        });

        return resourceTags;
    }

    @Override
    public String getUuid(String resourceId, ResourceObjectType resourceType) {
        GenericDao<?, Long> dao = _daoMap.get(resourceType);
        Class<?> claz = DbUtil.getEntityBeanType(dao);

        String identiyUUId = null;

        while (claz != null && claz != Object.class) {
            try {
                String tableName = DbUtil.getTableName(claz);
                if (tableName == null) {
                    throw new InvalidParameterValueException("Unable to find resource of type " + resourceType + " in the database");
                }

                claz = claz.getSuperclass();
                if (claz == Object.class) {
                    identiyUUId = _identityDao.getIdentityUuid(tableName, resourceId);
                }
            } catch (Exception ex) {
                //do nothing here, it might mean uuid field is missing and we have to search further
            }
        }

        if (identiyUUId == null) {
            return resourceId;
        }

        return identiyUUId;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_TAGS_DELETE, eventDescription = "deleting resource tags")
    public boolean deleteTags(List<String> resourceIds, ResourceObjectType resourceType, Map<String, String> tags) {
        Account caller = CallContext.current().getCallingAccount();

        SearchBuilder<ResourceTagVO> sb = _resourceTagDao.createSearchBuilder();
        sb.and().op("resourceId", sb.entity().getResourceId(), SearchCriteria.Op.IN);
        sb.or("resourceUuid", sb.entity().getResourceUuid(), SearchCriteria.Op.IN);
        sb.cp();
        sb.and("resourceType", sb.entity().getResourceType(), SearchCriteria.Op.EQ);

        SearchCriteria<ResourceTagVO> sc = sb.create();
        sc.setParameters("resourceId", resourceIds.toArray());
        sc.setParameters("resourceUuid", resourceIds.toArray());
        sc.setParameters("resourceType", resourceType);

        List<? extends ResourceTag> resourceTags = _resourceTagDao.search(sc, null);
        ;
        final List<ResourceTag> tagsToRemove = new ArrayList<ResourceTag>();

        // Finalize which tags should be removed
        for (ResourceTag resourceTag : resourceTags) {
            //1) validate the permissions
            Account owner = _accountMgr.getAccount(resourceTag.getAccountId());
            _accountMgr.checkAccess(caller, null, false, owner);
            //2) Only remove tag if it matches key value pairs
            if (tags != null && !tags.isEmpty()) {
                for (String key : tags.keySet()) {
                    boolean canBeRemoved = false;
                    if (resourceTag.getKey().equalsIgnoreCase(key)) {
                        String value = tags.get(key);
                        if (value != null) {
                            if (resourceTag.getValue().equalsIgnoreCase(value)) {
                                canBeRemoved = true;
                            }
                        } else {
                            canBeRemoved = true;
                        }
                        if (canBeRemoved) {
                            tagsToRemove.add(resourceTag);
                            break;
                        }
                    }
                }
            } else {
                tagsToRemove.add(resourceTag);
            }
        }

        if (tagsToRemove.isEmpty()) {
            throw new InvalidParameterValueException("Unable to find tags by parameters specified");
        }

        //Remove the tags
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                for (ResourceTag tagToRemove : tagsToRemove) {
                    _resourceTagDao.remove(tagToRemove.getId());
                    s_logger.debug("Removed the tag " + tagToRemove);
                }
            }
        });

        return true;
    }

    @Override
    public List<? extends ResourceTag> listByResourceTypeAndId(ResourceObjectType type, long resourceId) {
        return _resourceTagDao.listBy(resourceId, type);
    }
}
