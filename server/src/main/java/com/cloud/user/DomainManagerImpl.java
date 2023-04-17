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
package com.cloud.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.cloud.configuration.Resource;
import com.cloud.domain.dao.DomainDetailsDao;
import com.cloud.exception.ResourceAllocationException;
import org.apache.cloudstack.affinity.dao.AffinityGroupDomainMapDao;
import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.domain.ListDomainChildrenCmd;
import org.apache.cloudstack.api.command.admin.domain.ListDomainsCmd;
import org.apache.cloudstack.api.command.admin.domain.MoveDomainCmd;
import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.region.RegionManager;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.query.dao.DiskOfferingJoinDao;
import com.cloud.api.query.dao.ServiceOfferingJoinDao;
import com.cloud.api.query.vo.DiskOfferingJoinVO;
import com.cloud.api.query.vo.ServiceOfferingJoinVO;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.ResourceLimit;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import org.apache.commons.lang3.StringUtils;

@Component
public class DomainManagerImpl extends ManagerBase implements DomainManager, DomainService {
    public static final Logger s_logger = Logger.getLogger(DomainManagerImpl.class);

    @Inject
    private DomainDao _domainDao;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private ResourceCountDao _resourceCountDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private DiskOfferingJoinDao diskOfferingJoinDao;
    @Inject
    private DiskOfferingDao diskOfferingDao;
    @Inject
    private DiskOfferingDetailsDao diskOfferingDetailsDao;
    @Inject
    private ServiceOfferingJoinDao serviceOfferingJoinDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    private ServiceOfferingDetailsDao serviceOfferingDetailsDao;
    @Inject
    private ProjectDao _projectDao;
    @Inject
    private ProjectManager _projectMgr;
    @Inject
    private RegionManager _regionMgr;
    @Inject
    private ResourceLimitDao _resourceLimitDao;
    @Inject
    private DedicatedResourceDao _dedicatedDao;
    @Inject
    private NetworkOrchestrationService _networkMgr;
    @Inject
    private NetworkDomainDao _networkDomainDao;
    @Inject
    private ConfigurationManager _configMgr;
    @Inject
    private DomainDetailsDao _domainDetailsDao;
    @Inject
    private AnnotationDao annotationDao;
    @Inject
    private ResourceLimitService resourceLimitService;
    @Inject
    private AffinityGroupDomainMapDao affinityGroupDomainMapDao;

    @Inject
    MessageBus _messageBus;

    protected GlobalLock getGlobalLock(String name) {
        return GlobalLock.getInternLock(name);
    }

    protected Account getCaller() {
        return CallContext.current().getCallingAccount();
    }

    @Override
    public Domain getDomain(long domainId) {
        return _domainDao.findById(domainId);
    }

    @Override
    public Domain getDomain(String domainUuid) {
        return _domainDao.findByUuid(domainUuid);
    }

    @Override
    public Domain getDomainByName(String name, long parentId) {
        SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        sc.addAnd("parent", SearchCriteria.Op.EQ, parentId);
        Domain domain = _domainDao.findOneBy(sc);
        return domain;
    }

    @Override
    public Set<Long> getDomainChildrenIds(String parentDomainPath) {
        Set<Long> childDomains = new HashSet<Long>();
        SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
        sc.addAnd("path", SearchCriteria.Op.LIKE, parentDomainPath + "%");

        List<DomainVO> domains = _domainDao.search(sc, null);

        for (DomainVO domain : domains) {
            childDomains.add(domain.getId());
        }

        return childDomains;
    }

    @Override
    public boolean isChildDomain(Long parentId, Long childId) {
        return _domainDao.isChildDomain(parentId, childId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DOMAIN_CREATE, eventDescription = "creating Domain")
    public Domain createDomain(String name, Long parentId, String networkDomain, String domainUUID) {
        Account caller = getCaller();

        if (parentId == null) {
            parentId = Long.valueOf(Domain.ROOT_DOMAIN);
        }

        DomainVO parentDomain = _domainDao.findById(parentId);
        if (parentDomain == null) {
            throw new InvalidParameterValueException("Unable to create domain " + name + ", parent domain " + parentId + " not found.");
        }

        if (parentDomain.getState().equals(Domain.State.Inactive)) {
            throw new CloudRuntimeException("The domain cannot be created as the parent domain " + parentDomain.getName() + " is being deleted");
        }

        _accountMgr.checkAccess(caller, parentDomain);

        return createDomain(name, parentId, caller.getId(), networkDomain, domainUUID);

    }

    @Override
    @DB
    public Domain createDomain(final String name, final Long parentId, final Long ownerId, final String networkDomain, String domainUuid) {
        validateDomainNameAndNetworkDomain(name, parentId, networkDomain);

        DomainVO domainVO = createDomainVo(name, parentId, ownerId, networkDomain, domainUuid);

        DomainVO domain = Transaction.execute(new TransactionCallback<DomainVO>() {
            @Override
            public DomainVO doInTransaction(TransactionStatus status) {
                DomainVO domain = _domainDao.create(domainVO);
                _resourceCountDao.createResourceCounts(domain.getId(), ResourceLimit.ResourceOwnerType.Domain);

                CallContext.current().putContextParameter(Domain.class, domain.getUuid());
                return domain;
            }
        });
        if (domain != null) {
            _messageBus.publish(_name, MESSAGE_ADD_DOMAIN_EVENT, PublishScope.LOCAL, domain.getId());
            _messageBus.publish(_name, MESSAGE_CREATE_TUNGSTEN_DOMAIN_EVENT, PublishScope.LOCAL, domain);
        }
        return domain;
    }

    protected DomainVO createDomainVo(String name, Long parentId, Long ownerId, String networkDomain, String domainUuid) {
        if (StringUtils.isBlank(domainUuid)) {
            domainUuid = UUID.randomUUID().toString();
            s_logger.info(String.format("Domain UUID [%s] generated for domain name [%s].", domainUuid, name));
        }

        DomainVO domainVO = new DomainVO(name, ownerId, parentId, networkDomain, domainUuid);
        return domainVO;
    }

    protected void validateDomainNameAndNetworkDomain(String name, Long parentId, String networkDomain) {
        validateNetworkDomain(networkDomain);
        validateUniqueDomainName(name, parentId);
    }

    protected void validateUniqueDomainName(String name, Long parentId) {
        SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
        sc.addAnd("parent", SearchCriteria.Op.EQ, parentId);
        List<DomainVO> domains = _domainDao.search(sc, null);

        if (!domains.isEmpty()) {
            throw new InvalidParameterValueException(String.format("Domain with name [%s] already exists for the parent with ID [%s].", name, parentId));
        }
    }

    protected void validateNetworkDomain(String networkDomain) {
        if (networkDomain != null && !NetUtils.verifyDomainName(networkDomain)) {
            throw new InvalidParameterValueException(
                    "Invalid network domain. Total length should not exceed 190 chars. Each domain label must be between 1 and 63 characters long." +
                            " It can contain ASCII letters 'a' through 'z', the digits '0' through '9', and the hyphen ('-'); it cannot start or end with \"-\"."
            );
        }
    }

    @Override
    public DomainVO findDomainByPath(String domainPath) {
        return _domainDao.findDomainByPath(domainPath);
    }

    @Override
    public Domain findDomainByIdOrPath(final Long id, final String domainPath) {
        Long domainId = id;
        if (domainId == null || domainId < 1L) {
            if (StringUtils.isBlank(domainPath)) {
                domainId = Domain.ROOT_DOMAIN;
            } else {
                final Domain domainVO = findDomainByPath(domainPath.trim());
                if (domainVO != null) {
                    return domainVO;
                }
            }
        }
        if (domainId != null && domainId > 0L) {
            return _domainDao.findById(domainId);
        }
        return null;
    }

    @Override
    public Set<Long> getDomainParentIds(long domainId) {
        return _domainDao.getDomainParentIds(domainId);
    }

    @Override
    public boolean removeDomain(long domainId) {
        return _domainDao.remove(domainId);
    }

    @Override
    public List<? extends Domain> findInactiveDomains() {
        return _domainDao.findInactiveDomains();
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DOMAIN_DELETE, eventDescription = "deleting Domain", async = true)
    public boolean deleteDomain(long domainId, Boolean cleanup) {
        Account caller = getCaller();

        DomainVO domain = _domainDao.findById(domainId);

        if (domain == null) {
            throw new InvalidParameterValueException("Failed to delete domain " + domainId + ", domain not found");
        } else if (domainId == Domain.ROOT_DOMAIN) {
            throw new PermissionDeniedException("Can't delete ROOT domain");
        }

        _accountMgr.checkAccess(caller, domain);

        return deleteDomain(domain, cleanup);
    }

    @Override
    public boolean deleteDomain(DomainVO domain, Boolean cleanup) {
        GlobalLock lock = getGlobalLock("AccountCleanup");
        if (lock == null) {
            s_logger.debug("Couldn't get the global lock");
            return false;
        }

        if (!lock.lock(30)) {
            s_logger.debug("Couldn't lock the db");
            return false;
        }

        try {
            // mark domain as inactive
            s_logger.debug("Marking domain id=" + domain.getId() + " as " + Domain.State.Inactive + " before actually deleting it");
            domain.setState(Domain.State.Inactive);
            _domainDao.update(domain.getId(), domain);

            try {
                long ownerId = domain.getAccountId();
                if (BooleanUtils.toBoolean(cleanup)) {
                    tryCleanupDomain(domain, ownerId);
                } else {
                    removeDomainWithNoAccountsForCleanupNetworksOrDedicatedResources(domain);
                }

                if (!_configMgr.releaseDomainSpecificVirtualRanges(domain.getId())) {
                    CloudRuntimeException e = new CloudRuntimeException("Can't delete the domain yet because failed to release domain specific virtual ip ranges");
                    e.addProxyObject(domain.getUuid(), "domainId");
                    throw e;
                } else {
                    s_logger.debug("Domain specific Virtual IP ranges " + " are successfully released as a part of domain id=" + domain.getId() + " cleanup.");
                }

                cleanupDomainDetails(domain.getId());
                cleanupDomainOfferings(domain.getId());
                annotationDao.removeByEntityType(AnnotationService.EntityType.DOMAIN.name(), domain.getUuid());
                CallContext.current().putContextParameter(Domain.class, domain.getUuid());
                return true;
            } catch (Exception ex) {
                s_logger.error("Exception deleting domain with id " + domain.getId(), ex);
                if (ex instanceof CloudRuntimeException) {
                    rollbackDomainState(domain);
                    throw (CloudRuntimeException)ex;
                }
                else
                    return false;
            }
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Roll back domain state to Active
     * @param domain domain
     */
    protected void rollbackDomainState(DomainVO domain) {
        s_logger.debug("Changing domain id=" + domain.getId() + " state back to " + Domain.State.Active +
                " because it can't be removed due to resources referencing to it");
        domain.setState(Domain.State.Active);
        _domainDao.update(domain.getId(), domain);
    }

    /**
     * Try cleaning up domain. If it couldn't throws CloudRuntimeException
     * @param domain domain
     * @param ownerId owner id
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     * @throws CloudRuntimeException when cleanupDomain
     */
    protected void tryCleanupDomain(DomainVO domain, long ownerId) throws ConcurrentOperationException, ResourceUnavailableException, CloudRuntimeException {
        if (!cleanupDomain(domain.getId(), ownerId)) {
            CloudRuntimeException e =
                new CloudRuntimeException("Failed to clean up domain resources and sub domains, delete failed on domain " + domain.getName() + " (id: " +
                    domain.getId() + ").");
            e.addProxyObject(domain.getUuid(), "domainId");
            throw e;
        }
    }

    /**
     * First check domain resources before removing domain. There are 2 cases:
     * <ol>
     * <li>Domain doesn't have accounts for cleanup, non-removed networks, or dedicated resources</li>
     * <ul><li>Delete domain</li></ul>
     * <li>Domain has one of the following: accounts set for cleanup, non-removed networks, dedicated resources</li>
     * <ul><li>Dont' delete domain</li><li>Fail operation</li></ul>
     * </ol>
     * @param domain domain to remove
     * @throws CloudRuntimeException when case 2 or when domain cannot be deleted on case 1
     */
    protected void removeDomainWithNoAccountsForCleanupNetworksOrDedicatedResources(DomainVO domain) {
        boolean hasDedicatedResources = false;
        List<Long> networkIds = _networkDomainDao.listNetworkIdsByDomain(domain.getId());
        List<AccountVO> accountsForCleanup = _accountDao.findCleanupsForRemovedAccounts(domain.getId());
        List<DedicatedResourceVO> dedicatedResources = _dedicatedDao.listByDomainId(domain.getId());
        if (CollectionUtils.isNotEmpty(dedicatedResources)) {
            s_logger.error("There are dedicated resources for the domain " + domain.getId());
            hasDedicatedResources = true;
        }
        if (accountsForCleanup.isEmpty() && networkIds.isEmpty() && !hasDedicatedResources) {
            publishRemoveEventsAndRemoveDomain(domain);
        } else {
            failRemoveOperation(domain, accountsForCleanup, networkIds, hasDedicatedResources);
        }
    }

    /**
     * Fail domain remove operation including proper message
     * @param domain domain
     * @param accountsForCleanup domain accounts for cleanup
     * @param networkIds domain network ids
     * @param hasDedicatedResources indicates if domain has dedicated resources
     * @throws CloudRuntimeException including descriptive message indicating the reason for failure
     */
    protected void failRemoveOperation(DomainVO domain, List<AccountVO> accountsForCleanup, List<Long> networkIds, boolean hasDedicatedResources) {
        String msg = null;
        if (!accountsForCleanup.isEmpty()) {
            msg = accountsForCleanup.size() + " accounts to cleanup";
        } else if (!networkIds.isEmpty()) {
            msg = networkIds.size() + " non-removed networks";
        } else if (hasDedicatedResources) {
            msg = "dedicated resources.";
        }

        CloudRuntimeException e = new CloudRuntimeException("Can't delete the domain yet because it has " + msg);
        e.addProxyObject(domain.getUuid(), "domainId");
        throw e;
    }

    /**
     * Publish pre-remove and remove domain events and remove domain
     * @param domain domain to remove
     * @throws CloudRuntimeException when domain cannot be removed
     */
    protected void publishRemoveEventsAndRemoveDomain(DomainVO domain) {
        _messageBus.publish(_name, MESSAGE_PRE_REMOVE_DOMAIN_EVENT, PublishScope.LOCAL, domain);
        if (!_domainDao.remove(domain.getId())) {
            CloudRuntimeException e =
                new CloudRuntimeException("Delete failed on domain " + domain.getName() + " (id: " + domain.getId() +
                    "); Please make sure all users and sub domains have been removed from the domain before deleting");
            e.addProxyObject(domain.getUuid(), "domainId");
            throw e;
        }
        _messageBus.publish(_name, MESSAGE_REMOVE_DOMAIN_EVENT, PublishScope.LOCAL, domain);
    }

    protected void cleanupDomainDetails(Long domainId) {
        _domainDetailsDao.deleteDetails(domainId);
    }

    protected void cleanupDomainOfferings(Long domainId) {
        if (domainId == null) {
            return;
        }

        String domainIdString = String.valueOf(domainId);
        List<Long> diskOfferingsDetailsToRemove = new ArrayList<>();
        List<Long> serviceOfferingsDetailsToRemove = new ArrayList<>();

        // delete the service and disk offerings associated with this domain
        List<DiskOfferingJoinVO> diskOfferingsForThisDomain = diskOfferingJoinDao.findByDomainId(domainId);
        for (DiskOfferingJoinVO diskOffering : diskOfferingsForThisDomain) {
            if (domainIdString.equals(diskOffering.getDomainId())) {
                diskOfferingDao.remove(diskOffering.getId());
            } else {
                diskOfferingsDetailsToRemove.add(diskOffering.getId());
            }
        }
        List<ServiceOfferingJoinVO> serviceOfferingsForThisDomain = serviceOfferingJoinDao.findByDomainId(domainId);
        for (ServiceOfferingJoinVO serviceOffering : serviceOfferingsForThisDomain) {
            if (domainIdString.equals(serviceOffering.getDomainId())) {
                serviceOfferingDao.remove(serviceOffering.getId());
            } else {
                serviceOfferingsDetailsToRemove.add(serviceOffering.getId());
            }
        }

        // Remove domain IDs for offerings which may be multi-domain
        for (final Long diskOfferingId : diskOfferingsDetailsToRemove) {
            diskOfferingDetailsDao.removeDetail(diskOfferingId, ApiConstants.DOMAIN_ID, domainIdString);
        }
        for (final Long serviceOfferingId : serviceOfferingsDetailsToRemove) {
            serviceOfferingDetailsDao.removeDetail(serviceOfferingId, ApiConstants.DOMAIN_ID, domainIdString);
        }
    }

    protected boolean cleanupDomain(Long domainId, Long ownerId) throws ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Cleaning up domain id=" + domainId);
        boolean success = true;
        DomainVO domainHandle = _domainDao.findById(domainId);
        {
            domainHandle.setState(Domain.State.Inactive);
            _domainDao.update(domainId, domainHandle);

            SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
            sc.addAnd("parent", SearchCriteria.Op.EQ, domainId);
            List<DomainVO> domains = _domainDao.search(sc, null);

            SearchCriteria<DomainVO> sc1 = _domainDao.createSearchCriteria();
            sc1.addAnd("path", SearchCriteria.Op.LIKE, "%" + "replace(" + domainHandle.getPath() + ", '%', '[%]')" + "%");
            List<DomainVO> domainsToBeInactivated = _domainDao.search(sc1, null);

            // update all subdomains to inactive so no accounts/users can be created
            for (DomainVO domain : domainsToBeInactivated) {
                domain.setState(Domain.State.Inactive);
                _domainDao.update(domain.getId(), domain);
            }

            // cleanup sub-domains first
            for (DomainVO domain : domains) {
                success = (success && cleanupDomain(domain.getId(), domain.getAccountId()));
                if (!success) {
                    s_logger.warn("Failed to cleanup domain id=" + domain.getId());
                }
            }
        }

        // delete users which will also delete accounts and release resources for those accounts
        SearchCriteria<AccountVO> sc = _accountDao.createSearchCriteria();
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        List<AccountVO> accounts = _accountDao.search(sc, null);
        for (AccountVO account : accounts) {
            if (account.getType() != Account.Type.PROJECT) {
                s_logger.debug("Deleting account " + account + " as a part of domain id=" + domainId + " cleanup");
                boolean deleteAccount = _accountMgr.deleteAccount(account, CallContext.current().getCallingUserId(), getCaller());
                if (!deleteAccount) {
                    s_logger.warn("Failed to cleanup account id=" + account.getId() + " as a part of domain cleanup");
                }
                success = (success && deleteAccount);
            } else {
                ProjectVO project = _projectDao.findByProjectAccountId(account.getId());
                s_logger.debug("Deleting project " + project + " as a part of domain id=" + domainId + " cleanup");
                boolean deleteProject = _projectMgr.deleteProject(getCaller(), CallContext.current().getCallingUserId(), project);
                if (!deleteProject) {
                    s_logger.warn("Failed to cleanup project " + project + " as a part of domain cleanup");
                }
                success = (success && deleteProject);
            }
        }

        //delete the domain shared networks
        boolean networksDeleted = true;
        s_logger.debug("Deleting networks for domain id=" + domainId);
        List<Long> networkIds = _networkDomainDao.listNetworkIdsByDomain(domainId);
        CallContext ctx = CallContext.current();
        ReservationContext context = new ReservationContextImpl(null, null, _accountMgr.getActiveUser(ctx.getCallingUserId()), ctx.getCallingAccount());
        for (Long networkId : networkIds) {
            s_logger.debug("Deleting network id=" + networkId + " as a part of domain id=" + domainId + " cleanup");
            if (!_networkMgr.destroyNetwork(networkId, context, false)) {
                s_logger.warn("Unable to destroy network id=" + networkId + " as a part of domain id=" + domainId + " cleanup.");
                networksDeleted = false;
            } else {
                s_logger.debug("Network " + networkId + " successfully deleted as a part of domain id=" + domainId + " cleanup.");
            }
        }

        //don't proceed if networks failed to cleanup. The cleanup will be performed for inactive domain once again
        if (!networksDeleted) {
            s_logger.debug("Failed to delete the shared networks as a part of domain id=" + domainId + " clenaup");
            return false;
        }

        // don't remove the domain if there are accounts required cleanup
        boolean deleteDomainSuccess = true;
        List<AccountVO> accountsForCleanup = _accountDao.findCleanupsForRemovedAccounts(domainId);
        if (accountsForCleanup.isEmpty()) {
            //release dedication if any, before deleting the domain
            List<DedicatedResourceVO> dedicatedResources = _dedicatedDao.listByDomainId(domainId);
            if (dedicatedResources != null && !dedicatedResources.isEmpty()) {
                s_logger.debug("Releasing dedicated resources for domain" + domainId);
                for (DedicatedResourceVO dr : dedicatedResources) {
                    if (!_dedicatedDao.remove(dr.getId())) {
                        s_logger.warn("Fail to release dedicated resources for domain " + domainId);
                        return false;
                    }
                }
            }
            //delete domain
            _messageBus.publish(_name, MESSAGE_PRE_REMOVE_DOMAIN_EVENT, PublishScope.LOCAL, domainHandle);
            deleteDomainSuccess = _domainDao.remove(domainId);
            _messageBus.publish(_name, MESSAGE_REMOVE_DOMAIN_EVENT, PublishScope.LOCAL, domainHandle);

            // Delete resource count and resource limits entries set for this domain (if there are any).
            _resourceCountDao.removeEntriesByOwner(domainId, ResourceOwnerType.Domain);
            _resourceLimitDao.removeEntriesByOwner(domainId, ResourceOwnerType.Domain);
        } else {
            s_logger.debug("Can't delete the domain yet because it has " + accountsForCleanup.size() + "accounts that need a cleanup");
            return false;
        }

        return success && deleteDomainSuccess;
    }

    @Override
    public Pair<List<? extends Domain>, Integer> searchForDomains(ListDomainsCmd cmd) {
        Account caller = getCaller();
        Long domainId = cmd.getId();
        boolean listAll = cmd.listAll();
        boolean isRecursive = false;

        if (domainId != null) {
            Domain domain = getDomain(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Domain id=" + domainId + " doesn't exist");
            }
            _accountMgr.checkAccess(caller, domain);
        } else {
            if (!_accountMgr.isRootAdmin(caller.getId())) {
            domainId = caller.getDomainId();
            }
            if (listAll) {
                isRecursive = true;
            }
        }

        Filter searchFilter = new Filter(DomainVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        String domainName = cmd.getDomainName();
        Integer level = cmd.getLevel();
        Object keyword = cmd.getKeyword();

        SearchBuilder<DomainVO> sb = _domainDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("level", sb.entity().getLevel(), SearchCriteria.Op.EQ);
        sb.and("path", sb.entity().getPath(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);

        SearchCriteria<DomainVO> sc = sb.create();

        if (keyword != null) {
            SearchCriteria<DomainVO> ssc = _domainDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (domainName != null) {
            sc.setParameters("name", domainName);
        }

        if (level != null) {
            sc.setParameters("level", level);
        }

        if (domainId != null) {
            if (isRecursive) {
                sc.setParameters("path", getDomain(domainId).getPath() + "%");
            } else {
                sc.setParameters("id", domainId);
            }
        }

        // return only Active domains to the API
        sc.setParameters("state", Domain.State.Active);

        Pair<List<DomainVO>, Integer> result = _domainDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Domain>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends Domain>, Integer> searchForDomainChildren(ListDomainChildrenCmd cmd) throws PermissionDeniedException {
        Long domainId = cmd.getId();
        String domainName = cmd.getDomainName();
        Boolean isRecursive = cmd.isRecursive();
        Object keyword = cmd.getKeyword();
        boolean listAll = cmd.listAll();
        String path = null;

        Account caller = getCaller();
        if (domainId != null) {
            _accountMgr.checkAccess(caller, getDomain(domainId));
        } else {
            domainId = caller.getDomainId();
        }

        DomainVO domain = _domainDao.findById(domainId);
        if (domain != null && isRecursive && !listAll) {
            path = domain.getPath();
            domainId = null;
        }

        Filter searchFilter = new Filter(DomainVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Pair<List<DomainVO>, Integer> result = searchForDomainChildren(searchFilter, domainId, domainName, keyword, path, true);

        return new Pair<List<? extends Domain>, Integer>(result.first(), result.second());
    }

    private Pair<List<DomainVO>, Integer> searchForDomainChildren(Filter searchFilter, Long domainId, String domainName, Object keyword, String path,
        boolean listActiveOnly) {
        SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();

        if (keyword != null) {
            SearchCriteria<DomainVO> ssc = _domainDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (domainId != null) {
            sc.addAnd("parent", SearchCriteria.Op.EQ, domainId);
        }

        if (domainName != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + domainName + "%");
        }

        if (path != null) {
            sc.addAnd("path", SearchCriteria.Op.NEQ, path);
            sc.addAnd("path", SearchCriteria.Op.LIKE, path + "%");
        }

        if (listActiveOnly) {
            sc.addAnd("state", SearchCriteria.Op.EQ, Domain.State.Active);
        }

        return _domainDao.searchAndCount(sc, searchFilter);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DOMAIN_UPDATE, eventDescription = "updating Domain")
    @DB
    public DomainVO updateDomain(UpdateDomainCmd cmd) {
        final Long domainId = cmd.getId();
        final String domainName = cmd.getDomainName();
        final String networkDomain = cmd.getNetworkDomain();

        // check if domain exists in the system
        final DomainVO domain = _domainDao.findById(domainId);
        if (domain == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find domain with specified domain id");
            ex.addProxyObject(domainId.toString(), "domainId");
            throw ex;
        } else if (domain.getParent() == null && domainName != null) {
            // check if domain is ROOT domain - and deny to edit it with the new name
            throw new InvalidParameterValueException("ROOT domain can not be edited with a new name");
        }

        // check permissions
        Account caller = getCaller();
        _accountMgr.checkAccess(caller, domain);

        // domain name is unique in the cloud
        if (domainName != null) {
            SearchCriteria<DomainVO> sc = _domainDao.createSearchCriteria();
            sc.addAnd("name", SearchCriteria.Op.EQ, domainName);
            sc.addAnd("parent", SearchCriteria.Op.EQ, domain.getParent());
            List<DomainVO> domains = _domainDao.search(sc, null);

            boolean sameDomain = (domains.size() == 1 && domains.get(0).getId() == domainId);

            if (!domains.isEmpty() && !sameDomain) {
                InvalidParameterValueException ex =
                    new InvalidParameterValueException("Failed to update specified domain id with name '" + domainName + "' since it already exists in the system");
                ex.addProxyObject(domain.getUuid(), "domainId");
                throw ex;
            }
        }

        // validate network domain
        if (networkDomain != null && !networkDomain.isEmpty()) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
        if (domainName != null) {
            String updatedDomainPath = getUpdatedDomainPath(domain.getPath(), domainName);
            updateDomainChildren(domain, updatedDomainPath);
            domain.setName(domainName);
            domain.setPath(updatedDomainPath);
        }

        if (networkDomain != null) {
            if (networkDomain.isEmpty()) {
                domain.setNetworkDomain(null);
            } else {
                domain.setNetworkDomain(networkDomain);
            }
        }
        _domainDao.update(domainId, domain);
        CallContext.current().putContextParameter(Domain.class, domain.getUuid());
            }
        });

        return _domainDao.findById(domainId);

    }

    private String getUpdatedDomainPath(String oldPath, String newName) {
        String[] tokenizedPath = oldPath.split("/");
        tokenizedPath[tokenizedPath.length - 1] = newName;
        StringBuilder finalPath = new StringBuilder();
        for (String token : tokenizedPath) {
            finalPath.append(token);
            finalPath.append("/");
        }
        return finalPath.toString();
    }

    private void updateDomainChildren(DomainVO domain, String updatedDomainPrefix) {
        List<DomainVO> domainChildren = _domainDao.findAllChildren(domain.getPath(), domain.getId());
        // for each child, update the path
        for (DomainVO dom : domainChildren) {
            dom.setPath(dom.getPath().replaceFirst(domain.getPath(), updatedDomainPrefix));
            _domainDao.update(dom.getId(), dom);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DOMAIN_MOVE, eventDescription = "moving Domain")
    @DB
    public Domain moveDomainAndChildrenToNewParentDomain(MoveDomainCmd cmd) throws ResourceAllocationException {
        Long idOfDomainToBeMoved = cmd.getDomainId();
        Long idOfNewParentDomain = cmd.getParentDomainId();

        if (idOfDomainToBeMoved == Domain.ROOT_DOMAIN) {
            throw new InvalidParameterValueException("The domain to be moved cannot be the ROOT domain.");
        }

        if (idOfDomainToBeMoved.equals(idOfNewParentDomain)) {
            throw new InvalidParameterValueException("The domain to be moved and the new parent domain cannot be the same.");
        }

        DomainVO domainToBeMoved = returnDomainIfExistsAndIsActive(idOfDomainToBeMoved);
        s_logger.debug(String.format("Found the domain [%s] as the domain to be moved.", domainToBeMoved));

        DomainVO newParentDomain = returnDomainIfExistsAndIsActive(idOfNewParentDomain);
        s_logger.debug(String.format("Found the domain [%s] as the new parent domain of the domain to be moved [%s].", newParentDomain, domainToBeMoved));

        Long idOfCurrentParentOfDomainToBeMoved = domainToBeMoved.getParent();
        if (idOfCurrentParentOfDomainToBeMoved.equals(idOfNewParentDomain)) {
            throw new InvalidParameterValueException(String.format("The current parent domain of the domain to be moved is equal to the new parent domain [%s].", newParentDomain));
        }

        if (newParentDomain.getPath().startsWith(domainToBeMoved.getPath())) {
            throw new InvalidParameterValueException("The new parent domain of the domain cannot be one of its child.");
        }

        validateUniqueDomainName(domainToBeMoved.getName(), idOfNewParentDomain);

        String currentPathOfDomainToBeMoved = domainToBeMoved.getPath();
        String domainToBeMovedName = domainToBeMoved.getName().concat("/");
        String newPathOfDomainToBeMoved = newParentDomain.getPath().concat(domainToBeMovedName);

        validateNewParentDomainCanAccessAllDomainToBeMovedResources(domainToBeMoved, newParentDomain, currentPathOfDomainToBeMoved, newPathOfDomainToBeMoved);

        DomainVO parentOfDomainToBeMoved = _domainDao.findById(idOfCurrentParentOfDomainToBeMoved);
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                s_logger.debug(String.format("Setting the new parent of the domain to be moved [%s] as [%s].", domainToBeMoved, newParentDomain));
                domainToBeMoved.setParent(idOfNewParentDomain);

                updateDomainAndChildrenPathAndLevel(domainToBeMoved, newParentDomain, currentPathOfDomainToBeMoved, newPathOfDomainToBeMoved);

                for (Resource.ResourceType resourceType : Resource.ResourceType.values()) {
                    s_logger.debug(String.format("Recalculating the resource limit of the domain to be moved [%s] for the type [%s].", domainToBeMoved, resourceType));
                    resourceLimitService.findCorrectResourceLimitForDomain(domainToBeMoved, resourceType);
                }

                updateResourceCounts(idOfCurrentParentOfDomainToBeMoved, idOfNewParentDomain);

                updateChildCounts(parentOfDomainToBeMoved, newParentDomain);
            }
        });

        return domainToBeMoved;
    }

    protected void validateNewParentDomainCanAccessAllDomainToBeMovedResources(DomainVO domainToBeMoved, DomainVO newParentDomain, String currentPathOfDomainToBeMoved,
                                                                               String newPathOfDomainToBeMoved) {
        Map<Long, List<String>> idsOfDomainsWithNetworksUsedByDomainToBeMoved = _networkDomainDao.listDomainsOfSharedNetworksUsedByDomainPath(currentPathOfDomainToBeMoved);
        validateNewParentDomainCanAccessResourcesOfDomainToBeMoved(newPathOfDomainToBeMoved, domainToBeMoved, newParentDomain, idsOfDomainsWithNetworksUsedByDomainToBeMoved);

        Map<Long, List<String>> idsOfDomainsOfAffinityGroupsUsedByDomainToBeMoved =
                affinityGroupDomainMapDao.listDomainsOfAffinityGroupsUsedByDomainPath(currentPathOfDomainToBeMoved);
        validateNewParentDomainCanAccessResourcesOfDomainToBeMoved(newPathOfDomainToBeMoved, domainToBeMoved, newParentDomain, idsOfDomainsOfAffinityGroupsUsedByDomainToBeMoved);
    }

    protected void validateNewParentDomainCanAccessResourcesOfDomainToBeMoved(String newPathOfDomainToBeMoved, DomainVO domainToBeMoved, DomainVO newParentDomain,
                                                                              Map<Long, List<String>> idsOfDomainsWithResourcesUsedByDomainToBeMoved) {
        Map<DomainVO, List<String>> domainsOfResourcesInaccessibleToNewParentDomain = new HashMap<>();
        for (Map.Entry<Long, List<String>> entry : idsOfDomainsWithResourcesUsedByDomainToBeMoved.entrySet()) {
            DomainVO domainWithResourceUsedByDomainToBeMoved = _domainDao.findById(entry.getKey());

            Pattern pattern = Pattern.compile(domainWithResourceUsedByDomainToBeMoved.getPath().replace("/", "\\/").concat(".*"));
            Matcher matcher = pattern.matcher(newPathOfDomainToBeMoved);
            if (!matcher.matches()) {
                domainsOfResourcesInaccessibleToNewParentDomain.put(domainWithResourceUsedByDomainToBeMoved, entry.getValue());
            }
        }

        if (!domainsOfResourcesInaccessibleToNewParentDomain.isEmpty()) {
            s_logger.error(String.format("The new parent domain [%s] does not have access to the domains [%s] used by [%s] in the domain to be moved [%s].",
                    newParentDomain, domainsOfResourcesInaccessibleToNewParentDomain.keySet(), domainsOfResourcesInaccessibleToNewParentDomain.values(), domainToBeMoved));
            throw new InvalidParameterValueException(String.format("The domain [%s] could not be moved to domain [%s].", domainToBeMoved, newParentDomain));
        }
    }

    protected DomainVO returnDomainIfExistsAndIsActive(Long idOfDomain) {
        s_logger.debug(String.format("Checking if domain with ID [%s] exists and is active.", idOfDomain));
        DomainVO domain = _domainDao.findById(idOfDomain);

        if (domain == null) {
            throw new InvalidParameterValueException(String.format("Unable to find a domain with the specified ID [%s].", idOfDomain));
        } else if (domain.getState().equals(Domain.State.Inactive)) {
            throw new InvalidParameterValueException(String.format("Unable to use the domain [%s] as it is in state [%s].", domain, Domain.State.Inactive));
        }

        return domain;
    }

    protected void updateDomainAndChildrenPathAndLevel(DomainVO domainToBeMoved, DomainVO newParentDomain, String oldPath, String newPath) {
        Integer oldRootLevel = domainToBeMoved.getLevel();
        Integer newLevel = newParentDomain.getLevel() + 1;

        updateDomainPathAndLevel(domainToBeMoved, oldPath, newPath, oldRootLevel, newLevel);

        List<DomainVO> childrenDomain = _domainDao.findAllChildren(oldPath, domainToBeMoved.getId());
        for (DomainVO childDomain : childrenDomain) {
            updateDomainPathAndLevel(childDomain, oldPath, newPath, oldRootLevel, newLevel);
        }
    }

    protected void updateDomainPathAndLevel(DomainVO domain, String oldPath, String newPath, Integer oldRootLevel, Integer newLevel) {
        String finalPath = StringUtils.replaceOnce(domain.getPath(), oldPath, newPath);
        domain.setPath(finalPath);

        Integer currentLevel = domain.getLevel();
        int finalLevel = newLevel + currentLevel - oldRootLevel;
        domain.setLevel(finalLevel);

        s_logger.debug(String.format("Updating the path to [%s] and the level to [%s] of the domain [%s].", finalPath, finalLevel, domain));
        _domainDao.update(domain.getId(), domain);
    }

    protected void updateResourceCounts(Long idOfOldParentDomain, Long idOfNewParentDomain) {
        s_logger.debug(String.format("Updating the resource counts of the old parent domain [%s] and of the new parent domain [%s].", idOfOldParentDomain, idOfNewParentDomain));
        resourceLimitService.recalculateResourceCount(null, idOfOldParentDomain, null);
        resourceLimitService.recalculateResourceCount(null, idOfNewParentDomain, null);
    }

    protected void updateChildCounts(DomainVO oldParentDomain, DomainVO newParentDomain) {
        int finalOldParentChildCount = oldParentDomain.getChildCount() - 1;

        oldParentDomain.setChildCount(finalOldParentChildCount);
        oldParentDomain.setNextChildSeq(finalOldParentChildCount + 1);

        s_logger.debug(String.format("Updating the child count of the old parent domain [%s] to [%s].", oldParentDomain, finalOldParentChildCount));
        _domainDao.update(oldParentDomain.getId(), oldParentDomain);

        int finalNewParentChildCount = newParentDomain.getChildCount() + 1;

        newParentDomain.setChildCount(finalNewParentChildCount);
        newParentDomain.setNextChildSeq(finalNewParentChildCount + 1);

        s_logger.debug(String.format("Updating the child count of the new parent domain [%s] to [%s].", newParentDomain, finalNewParentChildCount));
        _domainDao.update(newParentDomain.getId(), newParentDomain);
    }
}
