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
package com.cloud.resourcelimit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.alert.AlertManager;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount.Role;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDaoImpl.SumCount;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

@Component
public class ResourceLimitManagerImpl extends ManagerBase implements ResourceLimitService, Configurable {
    public static final Logger s_logger = Logger.getLogger(ResourceLimitManagerImpl.class);

    @Inject
    private DomainDao _domainDao;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private AlertManager _alertMgr;
    @Inject
    private ResourceCountDao _resourceCountDao;
    @Inject
    private ResourceLimitDao _resourceLimitDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    protected SnapshotDao _snapshotDao;
    @Inject
    protected VMTemplateDao _vmTemplateDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private IPAddressDao _ipAddressDao;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private EntityManager _entityMgr;
    @Inject
    private ProjectDao _projectDao;
    @Inject
    private ProjectAccountDao _projectAccountDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private VpcDao _vpcDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    private TemplateDataStoreDao _vmTemplateStoreDao;
    @Inject
    private VlanDao _vlanDao;
    @Inject
    private SnapshotDataStoreDao _snapshotDataStoreDao;
    @Inject
    private UserVmJoinDao _userVmJoinDao;

    protected GenericSearchBuilder<TemplateDataStoreVO, SumCount> templateSizeSearch;
    protected GenericSearchBuilder<SnapshotDataStoreVO, SumCount> snapshotSizeSearch;

    protected SearchBuilder<ResourceCountVO> ResourceCountSearch;
    ScheduledExecutorService _rcExecutor;
    long _resourceCountCheckInterval = 0;
    Map<ResourceType, Long> accountResourceLimitMap = new EnumMap<ResourceType, Long>(ResourceType.class);
    Map<ResourceType, Long> domainResourceLimitMap = new EnumMap<ResourceType, Long>(ResourceType.class);
    Map<ResourceType, Long> projectResourceLimitMap = new EnumMap<ResourceType, Long>(ResourceType.class);

    @Override
    public boolean start() {
        if (_resourceCountCheckInterval > 0) {
            _rcExecutor.scheduleAtFixedRate(new ResourceCountCheckTask(), _resourceCountCheckInterval, _resourceCountCheckInterval, TimeUnit.SECONDS);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {

        ResourceCountSearch = _resourceCountDao.createSearchBuilder();
        ResourceCountSearch.and("id", ResourceCountSearch.entity().getId(), SearchCriteria.Op.IN);
        ResourceCountSearch.and("accountId", ResourceCountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        ResourceCountSearch.and("domainId", ResourceCountSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        ResourceCountSearch.done();

        templateSizeSearch = _vmTemplateStoreDao.createSearchBuilder(SumCount.class);
        templateSizeSearch.select("sum", Func.SUM, templateSizeSearch.entity().getSize());
        templateSizeSearch.and("downloadState", templateSizeSearch.entity().getDownloadState(), Op.EQ);
        templateSizeSearch.and("destroyed", templateSizeSearch.entity().getDestroyed(), Op.EQ);
        SearchBuilder<VMTemplateVO> join1 = _vmTemplateDao.createSearchBuilder();
        join1.and("accountId", join1.entity().getAccountId(), Op.EQ);
        templateSizeSearch.join("templates", join1, templateSizeSearch.entity().getTemplateId(), join1.entity().getId(), JoinBuilder.JoinType.INNER);
        templateSizeSearch.done();

        snapshotSizeSearch = _snapshotDataStoreDao.createSearchBuilder(SumCount.class);
        snapshotSizeSearch.select("sum", Func.SUM, snapshotSizeSearch.entity().getPhysicalSize());
        snapshotSizeSearch.and("state", snapshotSizeSearch.entity().getState(), Op.EQ);
        snapshotSizeSearch.and("storeRole", snapshotSizeSearch.entity().getRole(), Op.EQ);
        SearchBuilder<SnapshotVO> join2 = _snapshotDao.createSearchBuilder();
        join2.and("accountId", join2.entity().getAccountId(), Op.EQ);
        snapshotSizeSearch.join("snapshots", join2, snapshotSizeSearch.entity().getSnapshotId(), join2.entity().getId(), JoinBuilder.JoinType.INNER);
        snapshotSizeSearch.done();

        _resourceCountCheckInterval = ResourceCountCheckInterval.value();
        if (_resourceCountCheckInterval > 0) {
            _rcExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ResourceCountChecker"));
        }

        try {
            projectResourceLimitMap.put(Resource.ResourceType.public_ip, Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectPublicIPs.key())));
            projectResourceLimitMap.put(Resource.ResourceType.snapshot, Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectSnapshots.key())));
            projectResourceLimitMap.put(Resource.ResourceType.template, Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectTemplates.key())));
            projectResourceLimitMap.put(Resource.ResourceType.user_vm, Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectUserVms.key())));
            projectResourceLimitMap.put(Resource.ResourceType.volume, Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectVolumes.key())));
            projectResourceLimitMap.put(Resource.ResourceType.network, Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectNetworks.key())));
            projectResourceLimitMap.put(Resource.ResourceType.vpc, Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectVpcs.key())));
            projectResourceLimitMap.put(Resource.ResourceType.cpu, Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectCpus.key())));
            projectResourceLimitMap.put(Resource.ResourceType.memory, Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectMemory.key())));
            projectResourceLimitMap.put(Resource.ResourceType.primary_storage, Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectPrimaryStorage.key())));
            projectResourceLimitMap.put(Resource.ResourceType.secondary_storage, Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectSecondaryStorage.key())));

            accountResourceLimitMap.put(Resource.ResourceType.public_ip, Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountPublicIPs.key())));
            accountResourceLimitMap.put(Resource.ResourceType.snapshot, Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountSnapshots.key())));
            accountResourceLimitMap.put(Resource.ResourceType.template, Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountTemplates.key())));
            accountResourceLimitMap.put(Resource.ResourceType.user_vm, Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountUserVms.key())));
            accountResourceLimitMap.put(Resource.ResourceType.volume, Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountVolumes.key())));
            accountResourceLimitMap.put(Resource.ResourceType.network, Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountNetworks.key())));
            accountResourceLimitMap.put(Resource.ResourceType.vpc, Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountVpcs.key())));
            accountResourceLimitMap.put(Resource.ResourceType.cpu, Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountCpus.key())));
            accountResourceLimitMap.put(Resource.ResourceType.memory, Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountMemory.key())));
            accountResourceLimitMap.put(Resource.ResourceType.primary_storage, Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountPrimaryStorage.key())));
            accountResourceLimitMap.put(Resource.ResourceType.secondary_storage, Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountSecondaryStorage.key())));

            domainResourceLimitMap.put(Resource.ResourceType.public_ip, Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainPublicIPs.key())));
            domainResourceLimitMap.put(Resource.ResourceType.snapshot, Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainSnapshots.key())));
            domainResourceLimitMap.put(Resource.ResourceType.template, Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainTemplates.key())));
            domainResourceLimitMap.put(Resource.ResourceType.user_vm, Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainUserVms.key())));
            domainResourceLimitMap.put(Resource.ResourceType.volume, Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainVolumes.key())));
            domainResourceLimitMap.put(Resource.ResourceType.network, Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainNetworks.key())));
            domainResourceLimitMap.put(Resource.ResourceType.vpc, Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainVpcs.key())));
            domainResourceLimitMap.put(Resource.ResourceType.cpu, Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainCpus.key())));
            domainResourceLimitMap.put(Resource.ResourceType.memory, Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainMemory.key())));
            domainResourceLimitMap.put(Resource.ResourceType.primary_storage, Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainPrimaryStorage.key())));
            domainResourceLimitMap.put(Resource.ResourceType.secondary_storage, Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainSecondaryStorage.key())));
        } catch (NumberFormatException e) {
            s_logger.error("NumberFormatException during configuration", e);
            throw new ConfigurationException("Configuration failed due to NumberFormatException, see log for the stacktrace");
        }

        return true;
    }

    @Override
    public void incrementResourceCount(long accountId, ResourceType type, Long... delta) {
        // don't upgrade resource count for system account
        if (accountId == Account.ACCOUNT_ID_SYSTEM) {
            s_logger.trace("Not incrementing resource count for system accounts, returning");
            return;
        }

        long numToIncrement = (delta.length == 0) ? 1 : delta[0].longValue();

        if (!updateResourceCountForAccount(accountId, type, true, numToIncrement)) {
            // we should fail the operation (resource creation) when failed to update the resource count
            throw new CloudRuntimeException("Failed to increment resource count of type " + type + " for account id=" + accountId);
        }
    }

    @Override
    public void decrementResourceCount(long accountId, ResourceType type, Long... delta) {
        // don't upgrade resource count for system account
        if (accountId == Account.ACCOUNT_ID_SYSTEM) {
            s_logger.trace("Not decrementing resource count for system accounts, returning");
            return;
        }
        long numToDecrement = (delta.length == 0) ? 1 : delta[0].longValue();

        if (!updateResourceCountForAccount(accountId, type, false, numToDecrement)) {
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_UPDATE_RESOURCE_COUNT, 0L, 0L, "Failed to decrement resource count of type " + type + " for account id=" + accountId,
                    "Failed to decrement resource count of type " + type + " for account id=" + accountId + "; use updateResourceCount API to recalculate/fix the problem");
        }
    }

    @Override
    public long findCorrectResourceLimitForAccount(Account account, ResourceType type) {

        long max = Resource.RESOURCE_UNLIMITED; // if resource limit is not found, then we treat it as unlimited

        // No limits for Root Admin accounts
        if (_accountMgr.isRootAdmin(account.getId())) {
            return max;
        }

        ResourceLimitVO limit = _resourceLimitDao.findByOwnerIdAndType(account.getId(), ResourceOwnerType.Account, type);

        // Check if limit is configured for account
        if (limit != null) {
            max = limit.getMax().longValue();
        } else {
            // If the account has an no limit set, then return global default account limits
            Long value = null;
            if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                value = projectResourceLimitMap.get(type);
            } else {
                value = accountResourceLimitMap.get(type);
            }
            if (value != null) {
                if (value < 0) { // return unlimit if value is set to negative
                    return max;
                }
                // convert the value from GiB to bytes in case of primary or secondary storage.
                if (type == ResourceType.primary_storage || type == ResourceType.secondary_storage) {
                    value = value * ResourceType.bytesToGiB;
                }
                return value;
            }
        }

        return max;
    }

    @Override
    public long findCorrectResourceLimitForAccount(long accountId, Long limit, ResourceType type) {

        long max = Resource.RESOURCE_UNLIMITED; // if resource limit is not found, then we treat it as unlimited

        // No limits for Root Admin accounts
        if (_accountMgr.isRootAdmin(accountId)) {
            return max;
        }

        Account account = _accountDao.findById(accountId);
        if (account == null) {
            return max;
        }

        // Check if limit is configured for account
        if (limit != null) {
            max = limit.longValue();
        } else {
            // If the account has an no limit set, then return global default account limits
            Long value = null;
            if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                value = projectResourceLimitMap.get(type);
            } else {
                value = accountResourceLimitMap.get(type);
            }
            if (value != null) {
                if (value < 0) { // return unlimit if value is set to negative
                    return max;
                }
                if (type == ResourceType.primary_storage || type == ResourceType.secondary_storage) {
                    value = value * ResourceType.bytesToGiB;
                }
                return value;
            }
        }

        return max;
    }

    @Override
    public long findCorrectResourceLimitForDomain(Domain domain, ResourceType type) {
        long max = Resource.RESOURCE_UNLIMITED;

        // no limits on ROOT domain
        if (domain.getId() == Domain.ROOT_DOMAIN) {
            return Resource.RESOURCE_UNLIMITED;
        }
        // Check account
        ResourceLimitVO limit = _resourceLimitDao.findByOwnerIdAndType(domain.getId(), ResourceOwnerType.Domain, type);

        if (limit != null) {
            max = limit.getMax().longValue();
        } else {
            // check domain hierarchy
            Long domainId = domain.getParent();
            while ((domainId != null) && (limit == null)) {
                if (domainId == Domain.ROOT_DOMAIN) {
                    break;
                }
                limit = _resourceLimitDao.findByOwnerIdAndType(domainId, ResourceOwnerType.Domain, type);
                DomainVO tmpDomain = _domainDao.findById(domainId);
                domainId = tmpDomain.getParent();
            }

            if (limit != null) {
                max = limit.getMax().longValue();
            } else {
                Long value = null;
                value = domainResourceLimitMap.get(type);
                if (value != null) {
                    if (value < 0) { // return unlimit if value is set to negative
                        return max;
                    }
                    if (type == ResourceType.primary_storage || type == ResourceType.secondary_storage) {
                        value = value * ResourceType.bytesToGiB;
                    }
                    return value;
                }
            }
        }

        return max;
    }

    private void checkDomainResourceLimit(final Account account, final Project project, final ResourceType type, long numResources) throws ResourceAllocationException {
        // check all domains in the account's domain hierarchy
        Long domainId = null;
        if (project != null) {
            domainId = project.getDomainId();
        } else {
            domainId = account.getDomainId();
        }

        while (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            // no limit check if it is ROOT domain
            if (domainId != Domain.ROOT_DOMAIN) {
                long domainResourceLimit = findCorrectResourceLimitForDomain(domain, type);
                long currentDomainResourceCount = _resourceCountDao.getResourceCount(domainId, ResourceOwnerType.Domain, type);
                long requestedDomainResourceCount = currentDomainResourceCount + numResources;
                String messageSuffix = " domain resource limits of Type '" + type + "'" + " for Domain Id = " + domainId + " is exceeded: Domain Resource Limit = " + toHumanReadableSize(domainResourceLimit)
                        + ", Current Domain Resource Amount = " + toHumanReadableSize(currentDomainResourceCount) + ", Requested Resource Amount = " + toHumanReadableSize(numResources) + ".";

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Checking if" + messageSuffix);
                }

                if (domainResourceLimit != Resource.RESOURCE_UNLIMITED && requestedDomainResourceCount > domainResourceLimit) {
                    String message = "Maximum" + messageSuffix;
                    ResourceAllocationException e = new ResourceAllocationException(message, type);
                    s_logger.error(message, e);
                    throw e;
                }
            }
            domainId = domain.getParent();
        }
    }

    private void checkAccountResourceLimit(final Account account, final Project project, final ResourceType type, long numResources) throws ResourceAllocationException {
        // Check account limits
        long accountResourceLimit = findCorrectResourceLimitForAccount(account, type);
        long currentResourceCount = _resourceCountDao.getResourceCount(account.getId(), ResourceOwnerType.Account, type);
        long requestedResourceCount = currentResourceCount + numResources;

        String convertedAccountResourceLimit = String.valueOf(accountResourceLimit);
        String convertedCurrentResourceCount = String.valueOf(currentResourceCount);
        String convertedNumResources = String.valueOf(numResources);

        if (type == ResourceType.secondary_storage || type == ResourceType.primary_storage){
            convertedAccountResourceLimit = toHumanReadableSize(accountResourceLimit);
            convertedCurrentResourceCount = toHumanReadableSize(currentResourceCount);
            convertedNumResources = toHumanReadableSize(numResources);
        }

        String messageSuffix = " amount of resources of Type = '" + type + "' for " + (project == null ? "Account Name = " + account.getAccountName() : "Project Name = " + project.getName())
                + " in Domain Id = " + account.getDomainId() + " is exceeded: Account Resource Limit = " + convertedAccountResourceLimit + ", Current Account Resource Amount = " + convertedCurrentResourceCount
                + ", Requested Resource Amount = " + convertedNumResources + ".";

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking if" + messageSuffix);
        }

        if (accountResourceLimit != Resource.RESOURCE_UNLIMITED && requestedResourceCount > accountResourceLimit) {
            String message = "Maximum" + messageSuffix;
            ResourceAllocationException e = new ResourceAllocationException(message, type);
            s_logger.error(message, e);
            throw e;
        }
    }

    private List<ResourceCountVO> lockAccountAndOwnerDomainRows(long accountId, final ResourceType type) {
        Set<Long> rowIdsToLock = _resourceCountDao.listAllRowsToUpdate(accountId, ResourceOwnerType.Account, type);
        SearchCriteria<ResourceCountVO> sc = ResourceCountSearch.create();
        sc.setParameters("id", rowIdsToLock.toArray());
        return _resourceCountDao.lockRows(sc, null, true);
    }

    private List<ResourceCountVO> lockDomainRows(long domainId, final ResourceType type) {
        Set<Long> rowIdsToLock = _resourceCountDao.listAllRowsToUpdate(domainId, ResourceOwnerType.Domain, type);
        SearchCriteria<ResourceCountVO> sc = ResourceCountSearch.create();
        sc.setParameters("id", rowIdsToLock.toArray());
        return _resourceCountDao.lockRows(sc, null, true);
    }

    @Override
    public long findDefaultResourceLimitForDomain(ResourceType resourceType) {
        Long resourceLimit = null;
        resourceLimit = domainResourceLimitMap.get(resourceType);
        if (resourceLimit != null && (resourceType == ResourceType.primary_storage || resourceType == ResourceType.secondary_storage)) {
            resourceLimit = resourceLimit * ResourceType.bytesToGiB;
        } else {
            resourceLimit = Long.valueOf(Resource.RESOURCE_UNLIMITED);
        }
        return resourceLimit;
    }

    @Override
    @DB
    public void checkResourceLimit(final Account account, final ResourceType type, long... count) throws ResourceAllocationException {
        final long numResources = ((count.length == 0) ? 1 : count[0]);
        Project project = null;

        // Don't place any limits on system or root admin accounts
        if (_accountMgr.isRootAdmin(account.getId())) {
            return;
        }

        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            project = _projectDao.findByProjectAccountId(account.getId());
        }

        final Project projectFinal = project;
        Transaction.execute(new TransactionCallbackWithExceptionNoReturn<ResourceAllocationException>() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) throws ResourceAllocationException {
                // Lock all rows first so nobody else can read it
                lockAccountAndOwnerDomainRows(account.getId(), type);
                // Check account limits
                checkAccountResourceLimit(account, projectFinal, type, numResources);
                // check all domains in the account's domain hierarchy
                checkDomainResourceLimit(account, projectFinal, type, numResources);
            }
        });
    }

    @Override
    public List<ResourceLimitVO> searchForLimits(Long id, Long accountId, Long domainId, ResourceType resourceType, Long startIndex, Long pageSizeVal) {
        Account caller = CallContext.current().getCallingAccount();
        List<ResourceLimitVO> limits = new ArrayList<ResourceLimitVO>();
        boolean isAccount = true;

        if (!_accountMgr.isAdmin(caller.getId())) {
            accountId = caller.getId();
            domainId = null;
        } else {
            if (domainId != null) {
                // verify domain information and permissions
                Domain domain = _domainDao.findById(domainId);
                if (domain == null) {
                    // return empty set
                    return limits;
                }

                _accountMgr.checkAccess(caller, domain);

                if (accountId != null) {
                    // Verify account information and permissions
                    Account account = _accountDao.findById(accountId);
                    if (account == null) {
                        // return empty set
                        return limits;
                    }

                    _accountMgr.checkAccess(caller, null, true, account);
                    domainId = null;
                }
            }
        }

        // If id is passed in, get the record and return it if permission check has passed
        if (id != null) {
            ResourceLimitVO vo = _resourceLimitDao.findById(id);
            if (vo.getAccountId() != null) {
                _accountMgr.checkAccess(caller, null, true, _accountDao.findById(vo.getAccountId()));
                limits.add(vo);
            } else if (vo.getDomainId() != null) {
                _accountMgr.checkAccess(caller, _domainDao.findById(vo.getDomainId()));
                limits.add(vo);
            }

            return limits;
        }

        // If account is not specified, default it to caller account
        if (accountId == null) {
            if (domainId == null) {
                accountId = caller.getId();
                isAccount = true;
            } else {
                isAccount = false;
            }
        } else {
            isAccount = true;
        }

        SearchBuilder<ResourceLimitVO> sb = _resourceLimitDao.createSearchBuilder();
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);

        SearchCriteria<ResourceLimitVO> sc = sb.create();
        Filter filter = new Filter(ResourceLimitVO.class, "id", true, startIndex, pageSizeVal);

        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        }

        if (domainId != null) {
            sc.setParameters("domainId", domainId);
            sc.setParameters("accountId", (Object[])null);
        }

        if (resourceType != null) {
            sc.setParameters("type", resourceType);
        }

        List<ResourceLimitVO> foundLimits = _resourceLimitDao.search(sc, filter);

        if (resourceType != null) {
            if (foundLimits.isEmpty()) {
                if (isAccount) {
                    limits.add(new ResourceLimitVO(resourceType, findCorrectResourceLimitForAccount(_accountMgr.getAccount(accountId), resourceType), accountId, ResourceOwnerType.Account));
                } else {
                    limits.add(new ResourceLimitVO(resourceType, findCorrectResourceLimitForDomain(_domainDao.findById(domainId), resourceType), domainId, ResourceOwnerType.Domain));
                }
            } else {
                limits.addAll(foundLimits);
            }
        } else {
            limits.addAll(foundLimits);

            // see if any limits are missing from the table, and if yes - get it from the config table and add
            ResourceType[] resourceTypes = ResourceCount.ResourceType.values();
            if (foundLimits.size() != resourceTypes.length) {
                List<String> accountLimitStr = new ArrayList<String>();
                List<String> domainLimitStr = new ArrayList<String>();
                for (ResourceLimitVO foundLimit : foundLimits) {
                    if (foundLimit.getAccountId() != null) {
                        accountLimitStr.add(foundLimit.getType().toString());
                    } else {
                        domainLimitStr.add(foundLimit.getType().toString());
                    }
                }

                // get default from config values
                if (isAccount) {
                    if (accountLimitStr.size() < resourceTypes.length) {
                        for (ResourceType rt : resourceTypes) {
                            if (!accountLimitStr.contains(rt.toString()) && rt.supportsOwner(ResourceOwnerType.Account)) {
                                limits.add(new ResourceLimitVO(rt, findCorrectResourceLimitForAccount(_accountMgr.getAccount(accountId), rt), accountId, ResourceOwnerType.Account));
                            }
                        }
                    }

                } else {
                    if (domainLimitStr.size() < resourceTypes.length) {
                        for (ResourceType rt : resourceTypes) {
                            if (!domainLimitStr.contains(rt.toString()) && rt.supportsOwner(ResourceOwnerType.Domain)) {
                                limits.add(new ResourceLimitVO(rt, findCorrectResourceLimitForDomain(_domainDao.findById(domainId), rt), domainId, ResourceOwnerType.Domain));
                            }
                        }
                    }
                }
            }
        }

        return limits;
    }

    @Override
    public ResourceLimitVO updateResourceLimit(Long accountId, Long domainId, Integer typeId, Long max) {
        Account caller = CallContext.current().getCallingAccount();

        if (max == null) {
            max = new Long(Resource.RESOURCE_UNLIMITED);
        } else if (max.longValue() < Resource.RESOURCE_UNLIMITED) {
            throw new InvalidParameterValueException("Please specify either '-1' for an infinite limit, or a limit that is at least '0'.");
        }

        // Map resource type
        ResourceType resourceType = null;
        if (typeId != null) {
            for (ResourceType type : Resource.ResourceType.values()) {
                if (type.getOrdinal() == typeId.intValue()) {
                    resourceType = type;
                }
            }
            if (resourceType == null) {
                throw new InvalidParameterValueException("Please specify valid resource type");
            }
        }

        //Convert max storage size from GiB to bytes
        if ((resourceType == ResourceType.primary_storage || resourceType == ResourceType.secondary_storage) && max >= 0) {
            max *= ResourceType.bytesToGiB;
        }

        ResourceOwnerType ownerType = null;
        Long ownerId = null;

        if (accountId != null) {
            Account account = _entityMgr.findById(Account.class, accountId);
            if (account == null) {
                throw new InvalidParameterValueException("Unable to find account " + accountId);
            }
            if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
                throw new InvalidParameterValueException("Can't update system account");
            }

            //only Unlimited value is accepted if account is  Root Admin
            if (_accountMgr.isRootAdmin(account.getId()) && max.shortValue() != Resource.RESOURCE_UNLIMITED) {
                throw new InvalidParameterValueException("Only " + Resource.RESOURCE_UNLIMITED + " limit is supported for Root Admin accounts");
            }

            if ((caller.getAccountId() == accountId.longValue()) && (_accountMgr.isDomainAdmin(caller.getId()) || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN)) {
                // If the admin is trying to update their own account, disallow.
                throw new PermissionDeniedException("Unable to update resource limit for their own account " + accountId + ", permission denied");
            }

            if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                _accountMgr.checkAccess(caller, AccessType.ModifyProject, true, account);
            } else {
                _accountMgr.checkAccess(caller, null, true, account);
            }

            ownerType = ResourceOwnerType.Account;
            ownerId = accountId;
        } else if (domainId != null) {
            Domain domain = _entityMgr.findById(Domain.class, domainId);

            _accountMgr.checkAccess(caller, domain);

            if (Domain.ROOT_DOMAIN == domainId.longValue()) {
                // no one can add limits on ROOT domain, disallow...
                throw new PermissionDeniedException("Cannot update resource limit for ROOT domain " + domainId + ", permission denied");
            }

            if ((caller.getDomainId() == domainId.longValue()) && caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
                // if the admin is trying to update their own domain, disallow...
                throw new PermissionDeniedException("Unable to update resource limit for domain " + domainId + ", permission denied");
            }
            Long parentDomainId = domain.getParent();
            if (parentDomainId != null) {
                DomainVO parentDomain = _domainDao.findById(parentDomainId);
                long parentMaximum = findCorrectResourceLimitForDomain(parentDomain, resourceType);
                if ((parentMaximum >= 0) && (max.longValue() > parentMaximum)) {
                    throw new InvalidParameterValueException("Domain " + domain.getName() + "(id: " + parentDomain.getId() + ") has maximum allowed resource limit " + parentMaximum + " for "
                            + resourceType + ", please specify a value less that or equal to " + parentMaximum);
                }
            }
            ownerType = ResourceOwnerType.Domain;
            ownerId = domainId;
        }

        if (ownerId == null) {
            throw new InvalidParameterValueException("AccountId or domainId have to be specified in order to update resource limit");
        }

        ResourceLimitVO limit = _resourceLimitDao.findByOwnerIdAndType(ownerId, ownerType, resourceType);
        if (limit != null) {
            // Update the existing limit
            _resourceLimitDao.update(limit.getId(), max);
            return _resourceLimitDao.findById(limit.getId());
        } else {
            return _resourceLimitDao.persist(new ResourceLimitVO(resourceType, max, ownerId, ownerType));
        }
    }

    @Override
    public List<ResourceCountVO> recalculateResourceCount(Long accountId, Long domainId, Integer typeId) throws InvalidParameterValueException, CloudRuntimeException, PermissionDeniedException {
        Account callerAccount = CallContext.current().getCallingAccount();
        long count = 0;
        List<ResourceCountVO> counts = new ArrayList<ResourceCountVO>();
        List<ResourceType> resourceTypes = new ArrayList<ResourceType>();

        ResourceType resourceType = null;

        if (typeId != null) {
            for (ResourceType type : Resource.ResourceType.values()) {
                if (type.getOrdinal() == typeId.intValue()) {
                    resourceType = type;
                }
            }
            if (resourceType == null) {
                throw new InvalidParameterValueException("Please specify valid resource type");
            }
        }

        DomainVO domain = _domainDao.findById(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("Please specify a valid domain ID.");
        }
        _accountMgr.checkAccess(callerAccount, domain);

        if (resourceType != null) {
            resourceTypes.add(resourceType);
        } else {
            resourceTypes = Arrays.asList(Resource.ResourceType.values());
        }

        for (ResourceType type : resourceTypes) {
            if (accountId != null) {
                if (type.supportsOwner(ResourceOwnerType.Account)) {
                    count = recalculateAccountResourceCount(accountId, type);
                    counts.add(new ResourceCountVO(type, count, accountId, ResourceOwnerType.Account));
                }

            } else {
                if (type.supportsOwner(ResourceOwnerType.Domain)) {
                    count = recalculateDomainResourceCount(domainId, type);
                    counts.add(new ResourceCountVO(type, count, domainId, ResourceOwnerType.Domain));
                }
            }
        }

        return counts;
    }

    @DB
    protected boolean updateResourceCountForAccount(final long accountId, final ResourceType type, final boolean increment, final long delta) {
        if (s_logger.isDebugEnabled()) {
            String convertedDelta = String.valueOf(delta);
            if (type == ResourceType.secondary_storage || type == ResourceType.primary_storage){
                convertedDelta = toHumanReadableSize(delta);
            }
            s_logger.debug("Updating resource Type = " + type + " count for Account = " + accountId + " Operation = " + (increment ? "increasing" : "decreasing") + " Amount = " + convertedDelta);
        }
        try {
            return Transaction.execute(new TransactionCallback<Boolean>() {
                @Override
                public Boolean doInTransaction(TransactionStatus status) {
                    boolean result = true;
                    List<ResourceCountVO> rowsToUpdate = lockAccountAndOwnerDomainRows(accountId, type);
                    for (ResourceCountVO rowToUpdate : rowsToUpdate) {
                        if (!_resourceCountDao.updateById(rowToUpdate.getId(), increment, delta)) {
                            s_logger.trace("Unable to update resource count for the row " + rowToUpdate);
                            result = false;
                        }
                    }
                    return result;
                }
            });
        } catch (Exception ex) {
            s_logger.error("Failed to update resource count for account id=" + accountId);
            return false;
        }
    }

    @DB
    protected long recalculateDomainResourceCount(final long domainId, final ResourceType type) {
        return Transaction.execute(new TransactionCallback<Long>() {
            @Override
            public Long doInTransaction(TransactionStatus status) {
                long newResourceCount = 0;
                lockDomainRows(domainId, type);
                ResourceCountVO domainRC = _resourceCountDao.findByOwnerAndType(domainId, ResourceOwnerType.Domain, type);
                long oldResourceCount = domainRC.getCount();

                List<DomainVO> domainChildren = _domainDao.findImmediateChildrenForParent(domainId);
                // for each child domain update the resource count
                if (type.supportsOwner(ResourceOwnerType.Domain)) {

                    // calculate project count here
                    if (type == ResourceType.project) {
                        newResourceCount += _projectDao.countProjectsForDomain(domainId);
                    }

                    for (DomainVO childDomain : domainChildren) {
                        long childDomainResourceCount = recalculateDomainResourceCount(childDomain.getId(), type);
                        newResourceCount += childDomainResourceCount; // add the child domain count to parent domain count
                    }
                }

                if (type.supportsOwner(ResourceOwnerType.Account)) {
                    List<AccountVO> accounts = _accountDao.findActiveAccountsForDomain(domainId);
                    for (AccountVO account : accounts) {
                        long accountResourceCount = recalculateAccountResourceCount(account.getId(), type);
                        newResourceCount += accountResourceCount; // add account's resource count to parent domain count
                    }
                }
                _resourceCountDao.setResourceCount(domainId, ResourceOwnerType.Domain, type, newResourceCount);

                if (oldResourceCount != newResourceCount) {
                    s_logger.warn("Discrepency in the resource count has been detected " + "(original count = " + oldResourceCount + " correct count = " + newResourceCount + ") for Type = " + type
                            + " for Domain ID = " + domainId + " is fixed during resource count recalculation.");
                }

                return newResourceCount;
            }
        });
    }

    @DB
    protected long recalculateAccountResourceCount(final long accountId, final ResourceType type) {
        final Long newCount;
        if (type == Resource.ResourceType.user_vm) {
            newCount = _userVmDao.countAllocatedVMsForAccount(accountId, VirtualMachineManager.ResoureCountRunningVMsonly.value());
        } else if (type == Resource.ResourceType.volume) {
            long virtualRouterCount = _vmDao.findIdsOfAllocatedVirtualRoutersForAccount(accountId).size();
            newCount = _volumeDao.countAllocatedVolumesForAccount(accountId) - virtualRouterCount; // don't count the volumes of virtual router
        } else if (type == Resource.ResourceType.snapshot) {
            newCount = _snapshotDao.countSnapshotsForAccount(accountId);
        } else if (type == Resource.ResourceType.public_ip) {
            newCount = calculatePublicIpForAccount(accountId);
        } else if (type == Resource.ResourceType.template) {
            newCount = _vmTemplateDao.countTemplatesForAccount(accountId);
        } else if (type == Resource.ResourceType.project) {
            newCount = _projectAccountDao.countByAccountIdAndRole(accountId, Role.Admin);
        } else if (type == Resource.ResourceType.network) {
            newCount = _networkDao.countNetworksUserCanCreate(accountId);
        } else if (type == Resource.ResourceType.vpc) {
            newCount = _vpcDao.countByAccountId(accountId);
        } else if (type == Resource.ResourceType.cpu) {
            newCount = countCpusForAccount(accountId);
        } else if (type == Resource.ResourceType.memory) {
            newCount = calculateMemoryForAccount(accountId);
        } else if (type == Resource.ResourceType.primary_storage) {
            List<Long> virtualRouters = _vmDao.findIdsOfAllocatedVirtualRoutersForAccount(accountId);
            newCount = _volumeDao.primaryStorageUsedForAccount(accountId, virtualRouters);
        } else if (type == Resource.ResourceType.secondary_storage) {
            newCount = calculateSecondaryStorageForAccount(accountId);
        } else {
            throw new InvalidParameterValueException("Unsupported resource type " + type);
        }

        long oldCount = 0;
        final ResourceCountVO accountRC = _resourceCountDao.findByOwnerAndType(accountId, ResourceOwnerType.Account, type);
        if (accountRC != null) {
            oldCount = accountRC.getCount();
        }

        if (newCount == null || !newCount.equals(oldCount)) {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    lockAccountAndOwnerDomainRows(accountId, type);
                    _resourceCountDao.setResourceCount(accountId, ResourceOwnerType.Account, type, (newCount == null) ? 0 : newCount);
                }
            });
        }

        // No need to log message for primary and secondary storage because both are recalculating the
        // resource count which will not lead to any discrepancy.
        if (newCount != null && !newCount.equals(oldCount) &&
                type != Resource.ResourceType.primary_storage && type != Resource.ResourceType.secondary_storage) {
            s_logger.warn("Discrepancy in the resource count " + "(original count=" + oldCount + " correct count = " + newCount + ") for type " + type +
                    " for account ID " + accountId + " is fixed during resource count recalculation.");
        }

        return (newCount == null) ? 0 : newCount;
    }

    public long countCpusForAccount(long accountId) {
        long cputotal = 0;
        // user vms
        SearchBuilder<UserVmJoinVO> userVmSearch = _userVmJoinDao.createSearchBuilder();
        userVmSearch.and("accountId", userVmSearch.entity().getAccountId(), Op.EQ);
        userVmSearch.and("state", userVmSearch.entity().getState(), SearchCriteria.Op.NIN);
        userVmSearch.and("displayVm", userVmSearch.entity().isDisplayVm(), Op.EQ);
        userVmSearch.groupBy(userVmSearch.entity().getId()); // select distinct
        userVmSearch.done();

        SearchCriteria<UserVmJoinVO> sc1 = userVmSearch.create();
        sc1.setParameters("accountId", accountId);
        if (VirtualMachineManager.ResoureCountRunningVMsonly.value())
            sc1.setParameters("state", new Object[] {State.Destroyed, State.Error, State.Expunging, State.Stopped});
        else
            sc1.setParameters("state", new Object[] {State.Destroyed, State.Error, State.Expunging});
        sc1.setParameters("displayVm", 1);
        List<UserVmJoinVO> userVms = _userVmJoinDao.search(sc1,null);
        for (UserVmJoinVO vm : userVms) {
            cputotal += Long.valueOf(vm.getCpu());
        }
        return cputotal;
    }

    public long calculateMemoryForAccount(long accountId) {
        long ramtotal = 0;
        // user vms
        SearchBuilder<UserVmJoinVO> userVmSearch = _userVmJoinDao.createSearchBuilder();
        userVmSearch.and("accountId", userVmSearch.entity().getAccountId(), Op.EQ);
        userVmSearch.and("state", userVmSearch.entity().getState(), SearchCriteria.Op.NIN);
        userVmSearch.and("displayVm", userVmSearch.entity().isDisplayVm(), Op.EQ);
        userVmSearch.groupBy(userVmSearch.entity().getId()); // select distinct
        userVmSearch.done();

        SearchCriteria<UserVmJoinVO> sc1 = userVmSearch.create();
        sc1.setParameters("accountId", accountId);
        if (VirtualMachineManager.ResoureCountRunningVMsonly.value())
            sc1.setParameters("state", new Object[] {State.Destroyed, State.Error, State.Expunging, State.Stopped});
        else
            sc1.setParameters("state", new Object[] {State.Destroyed, State.Error, State.Expunging});
        sc1.setParameters("displayVm", 1);
        List<UserVmJoinVO> userVms = _userVmJoinDao.search(sc1,null);
        for (UserVmJoinVO vm : userVms) {
            ramtotal += Long.valueOf(vm.getRamSize());
        }
        return ramtotal;
    }

    public long calculateSecondaryStorageForAccount(long accountId) {
        long totalVolumesSize = _volumeDao.secondaryStorageUsedForAccount(accountId);
        long totalSnapshotsSize = 0;
        long totalTemplatesSize = 0;

        SearchCriteria<SumCount> sc = templateSizeSearch.create();
        sc.setParameters("downloadState", Status.DOWNLOADED);
        sc.setParameters("destroyed", false);
        sc.setJoinParameters("templates", "accountId", accountId);
        List<SumCount> templates = _vmTemplateStoreDao.customSearch(sc, null);
        if (templates != null) {
            totalTemplatesSize = templates.get(0).sum;
        }

        SearchCriteria<SumCount> sc2 = snapshotSizeSearch.create();
        sc2.setParameters("state", ObjectInDataStoreStateMachine.State.Ready);
        sc2.setParameters("storeRole", DataStoreRole.Image);
        sc2.setJoinParameters("snapshots", "accountId", accountId);
        List<SumCount> snapshots = _snapshotDataStoreDao.customSearch(sc2, null);
        if (snapshots != null) {
            totalSnapshotsSize = snapshots.get(0).sum;
        }
        return totalVolumesSize + totalSnapshotsSize + totalTemplatesSize;
    }

    private long calculatePublicIpForAccount(long accountId) {
        Long dedicatedCount = 0L;
        Long allocatedCount = 0L;

        List<VlanVO> dedicatedVlans = _vlanDao.listDedicatedVlans(accountId);
        for (VlanVO dedicatedVlan : dedicatedVlans) {
            List<IPAddressVO> ips = _ipAddressDao.listByVlanId(dedicatedVlan.getId());
            dedicatedCount += new Long(ips.size());
        }
        allocatedCount = _ipAddressDao.countAllocatedIPsForAccount(accountId);
        if (dedicatedCount > allocatedCount) {
            return dedicatedCount;
        } else {
            return allocatedCount;
        }
    }

    @Override
    public long getResourceCount(Account account, ResourceType type) {
        return _resourceCountDao.getResourceCount(account.getId(), ResourceOwnerType.Account, type);
    }

    private boolean isDisplayFlagOn(Boolean displayResource) {

        // 1. If its null assume displayResource = 1
        // 2. If its not null then send true if displayResource = 1
        return (displayResource == null) || (displayResource != null && displayResource);
    }

    @Override
    public void checkResourceLimit(Account account, ResourceType type, Boolean displayResource, long... count) throws ResourceAllocationException {

        if (isDisplayFlagOn(displayResource)) {
            checkResourceLimit(account, type, count);
        }
    }

    @Override
    public void incrementResourceCount(long accountId, ResourceType type, Boolean displayResource, Long... delta) {

        if (isDisplayFlagOn(displayResource)) {
            incrementResourceCount(accountId, type, delta);
        }
    }

    @Override
    public void decrementResourceCount(long accountId, ResourceType type, Boolean displayResource, Long... delta) {

        if (isDisplayFlagOn(displayResource)) {
            decrementResourceCount(accountId, type, delta);
        }
    }

    @Override
    public void changeResourceCount(long accountId, ResourceType type, Boolean displayResource, Long... delta) {

        // meaning that the display flag is not changed so neither increment or decrement
        if (displayResource == null) {
            return;
        }

        // Increment because the display is turned on.
        if (displayResource) {
            incrementResourceCount(accountId, type, delta);
        } else {
            decrementResourceCount(accountId, type, delta);
        }
    }

    @Override
    public String getConfigComponentName() {
        return ResourceLimitManagerImpl.class.getName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {ResourceCountCheckInterval};
    }

    protected class ResourceCountCheckTask extends ManagedContextRunnable {
        public ResourceCountCheckTask() {

        }

        @Override
        protected void runInContext() {
            s_logger.info("Started resource counters recalculation periodic task.");
            List<DomainVO> domains = _domainDao.findImmediateChildrenForParent(Domain.ROOT_DOMAIN);

            // recalculateDomainResourceCount will take care of re-calculation of resource counts for sub-domains
            // and accounts of the sub-domains also. so just loop through immediate children of root domain
            for (Domain domain : domains) {
                for (ResourceType type : ResourceCount.ResourceType.values()) {
                    if (type.supportsOwner(ResourceOwnerType.Domain)) {
                        recalculateDomainResourceCount(domain.getId(), type);
                    }
                }
            }

            // run through the accounts in the root domain
            List<AccountVO> accounts = _accountDao.findActiveAccountsForDomain(Domain.ROOT_DOMAIN);
            for (AccountVO account : accounts) {
                for (ResourceType type : ResourceCount.ResourceType.values()) {
                    if (type.supportsOwner(ResourceOwnerType.Account)) {
                        recalculateAccountResourceCount(account.getId(), type);
                    }
                }
            }
        }
    }
}
