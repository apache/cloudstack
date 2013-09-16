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

import com.cloud.vm.dao.NicDao;
import com.cloud.network.vpc.NetworkACLItemDao;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.context.CallContext;

import com.cloud.api.query.dao.ResourceTagJoinDao;
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
import com.cloud.network.vpc.dao.StaticRouteDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.server.TaggedResourceService;
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
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.uuididentity.dao.IdentityDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;


@Component
@Local(value = { TaggedResourceService.class})
public class TaggedResourceManagerImpl extends ManagerBase implements TaggedResourceService {
    public static final Logger s_logger = Logger.getLogger(TaggedResourceManagerImpl.class);
    
    private static Map<TaggedResourceType, GenericDao<?, Long>> _daoMap= 
            new HashMap<TaggedResourceType, GenericDao<?, Long>>();
    
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


    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _daoMap.put(TaggedResourceType.UserVm, _userVmDao);
        _daoMap.put(TaggedResourceType.Volume, _volumeDao);
        _daoMap.put(TaggedResourceType.Template, _templateDao);
        _daoMap.put(TaggedResourceType.ISO, _templateDao);
        _daoMap.put(TaggedResourceType.Snapshot, _snapshotDao);
        _daoMap.put(TaggedResourceType.Network, _networkDao);
        _daoMap.put(TaggedResourceType.LoadBalancer, _lbDao);
        _daoMap.put(TaggedResourceType.PortForwardingRule, _pfDao);
        _daoMap.put(TaggedResourceType.FirewallRule, _firewallDao);
        _daoMap.put(TaggedResourceType.SecurityGroup, _securityGroupDao);
        _daoMap.put(TaggedResourceType.PublicIpAddress, _publicIpDao);
        _daoMap.put(TaggedResourceType.Project, _projectDao);
        _daoMap.put(TaggedResourceType.Vpc, _vpcDao);
        _daoMap.put(TaggedResourceType.Nic, _nicDao);
        _daoMap.put(TaggedResourceType.NetworkACL, _networkACLItemDao);
        _daoMap.put(TaggedResourceType.StaticRoute, _staticRouteDao);
        _daoMap.put(TaggedResourceType.VMSnapshot, _vmSnapshotDao);
        _daoMap.put(TaggedResourceType.RemoteAccessVpn, _vpnDao);

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
    public Long getResourceId(String resourceId, TaggedResourceType resourceType) {
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

    protected String getTableName(TaggedResourceType resourceType) {
        GenericDao<?, Long> dao = _daoMap.get(resourceType);
        Class<?> claz = DbUtil.getEntityBeanType(dao);
        return DbUtil.getTableName(claz);
    }
    
    private Pair<Long, Long> getAccountDomain(long resourceId, TaggedResourceType resourceType) {
       
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
    public TaggedResourceType getResourceType(String resourceTypeStr) {
        
        for (TaggedResourceType type : ResourceTag.TaggedResourceType.values()) {
            if (type.toString().equalsIgnoreCase(resourceTypeStr)) {
                return type;
            }
        }
        throw new InvalidParameterValueException("Invalid resource type " + resourceTypeStr);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_TAGS_CREATE, eventDescription = "creating resource tags")
    public List<ResourceTag> createTags(List<String> resourceIds, TaggedResourceType resourceType, 
            Map<String, String> tags, String customer) {
        Account caller = CallContext.current().getCallingAccount();
        
        List<ResourceTag> resourceTags = new ArrayList<ResourceTag>(tags.size());
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        for (String key : tags.keySet()) {
            for (String resourceId : resourceIds) {
                Long id = getResourceId(resourceId, resourceType);
                String resourceUuid = getUuid(resourceId, resourceType);
                
                //check if object exists
                if (_daoMap.get(resourceType).findById(id) == null) {
                    throw new InvalidParameterValueException("Unable to find resource by id " + resourceId + 
                            " and type " + resourceType);
                }
                
                Pair<Long, Long> accountDomainPair = getAccountDomain(id, resourceType);
                Long domainId = accountDomainPair.second();
                Long accountId = accountDomainPair.first();
                if (accountId != null) {
                    _accountMgr.checkAccess(caller, null, false, _accountMgr.getAccount(accountId));
                } else if (domainId != null && caller.getType() != Account.ACCOUNT_TYPE_NORMAL) {
                    //check permissions;
                    _accountMgr.checkAccess(caller, _domainMgr.getDomain(domainId));
                } else {
                    throw new PermissionDeniedException("Account " + caller + " doesn't have permissions to create tags" +
                    		" for resource " + key);
                }
                
                String value = tags.get(key);
                
                if (value == null || value.isEmpty()) {
                    throw new InvalidParameterValueException("Value for the key " + key + " is either null or empty");
                }
               
                ResourceTagVO resourceTag = new ResourceTagVO(key, value, accountDomainPair.first(),
                        accountDomainPair.second(), 
                        id, resourceType, customer, resourceUuid);
                resourceTag = _resourceTagDao.persist(resourceTag);
                resourceTags.add(resourceTag);
            }
        }
        
        txn.commit();
        
        return resourceTags;
    }

    @Override
    public String getUuid(String resourceId, TaggedResourceType resourceType) {
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
    public boolean deleteTags(List<String> resourceIds, TaggedResourceType resourceType, Map<String, String> tags) {
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
        
        List<? extends ResourceTag> resourceTags = _resourceTagDao.search(sc, null);;
        List<ResourceTag> tagsToRemove = new ArrayList<ResourceTag>();
        
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
        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (ResourceTag tagToRemove : tagsToRemove) {
            _resourceTagDao.remove(tagToRemove.getId());
            s_logger.debug("Removed the tag " + tagToRemove);
        }
        txn.commit();

        return true;
    }


    @Override
    public List<? extends ResourceTag> listByResourceTypeAndId(TaggedResourceType type, long resourceId) {
        return _resourceTagDao.listBy(resourceId, type);
    }
}
