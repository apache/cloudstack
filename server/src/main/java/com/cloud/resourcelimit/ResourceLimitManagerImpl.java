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

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.utils.Ternary;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ResourceLimitAndCountResponse;
import org.apache.cloudstack.api.response.TaggedResourceLimitAndCountResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKeyScheduledExecutionWrapper;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.reservation.ReservationVO;
import org.apache.cloudstack.reservation.dao.ReservationDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloud.alert.AlertManager;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
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
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount.Role;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDaoImpl.SumCount;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.GlobalLock;
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
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class ResourceLimitManagerImpl extends ManagerBase implements ResourceLimitService, Configurable {

    @Inject
    private AccountManager _accountMgr;
    @Inject
    private AlertManager _alertMgr;
    @Inject
    AccountDao _accountDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private EntityManager _entityMgr;
    @Inject
    private IPAddressDao _ipAddressDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private ProjectDao _projectDao;
    @Inject
    private ProjectAccountDao _projectAccountDao;
    @Inject
    private ResourceCountDao _resourceCountDao;
    @Inject
    private ResourceLimitDao _resourceLimitDao;
    @Inject
    private ResourceLimitService resourceLimitService;
    @Inject
    private ReservationDao reservationDao;
    @Inject
    protected SnapshotDao _snapshotDao;
    @Inject
    private SnapshotDataStoreDao _snapshotDataStoreDao;
    @Inject
    private TemplateDataStoreDao _vmTemplateStoreDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private UserVmJoinDao _userVmJoinDao;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    protected VMTemplateDao _vmTemplateDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private VpcDao _vpcDao;
    @Inject
    private VlanDao _vlanDao;
    @Inject
    private ManagementServerHostDao managementServerHostDao;
    @Inject
    ServiceOfferingDao serviceOfferingDao;
    @Inject
    DiskOfferingDao diskOfferingDao;

    protected GenericSearchBuilder<TemplateDataStoreVO, SumCount> templateSizeSearch;
    protected GenericSearchBuilder<SnapshotDataStoreVO, SumCount> snapshotSizeSearch;

    protected SearchBuilder<ResourceCountVO> ResourceCountSearch;
    ScheduledExecutorService _rcExecutor;
    Map<String, Long> accountResourceLimitMap = new HashMap<>();
    Map<String, Long> domainResourceLimitMap = new HashMap<>();
    Map<String, Long> projectResourceLimitMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    protected void removeResourceReservationIfNeededAndIncrementResourceCount(final long accountId, final ResourceType type, String tag, final long numToIncrement) {
        Object obj = CallContext.current().getContextParameter(CheckedReservation.getResourceReservationContextParameterKey(type));
        List<Long> reservationIds = (List<Long>)obj; // This complains an unchecked casting warning
        Transaction.execute(new TransactionCallbackWithExceptionNoReturn<CloudRuntimeException>() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) throws CloudRuntimeException {
                reservationDao.removeByIds(reservationIds);
                if (!updateResourceCountForAccount(accountId, type, tag, true, numToIncrement)) {
                    // we should fail the operation (resource creation) when failed to update the resource count
                    throw new CloudRuntimeException("Failed to increment resource count of type " + type + " for account id=" + accountId);
                }
            }
        });
    }

    private void cleanupResourceReservationsForMs() {
        int reservationsRemoved = reservationDao.removeByMsId(ManagementServerNode.getManagementServerId());
        if (reservationsRemoved > 0) {
            logger.warn("Removed {} resource reservations for management server id {}", reservationsRemoved, ManagementServerNode.getManagementServerId());
        }
    }

    @Override
    public boolean start() {
        cleanupResourceReservationsForMs();
        if (ResourceCountCheckInterval.value() >= 0) {
            ConfigKeyScheduledExecutionWrapper runner = new ConfigKeyScheduledExecutionWrapper(_rcExecutor, new ResourceCountCheckTask(), ResourceCountCheckInterval, TimeUnit.SECONDS);
            runner.start();
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (_rcExecutor != null) {
            _rcExecutor.shutdown();
        }
        cleanupResourceReservationsForMs();
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

        if (ResourceCountCheckInterval.value() >= 0) {
            _rcExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ResourceCountChecker"));
        }

        try {
            projectResourceLimitMap.put(Resource.ResourceType.public_ip.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectPublicIPs.key())));
            projectResourceLimitMap.put(Resource.ResourceType.snapshot.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectSnapshots.key())));
            projectResourceLimitMap.put(Resource.ResourceType.template.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectTemplates.key())));
            projectResourceLimitMap.put(Resource.ResourceType.user_vm.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectUserVms.key())));
            projectResourceLimitMap.put(Resource.ResourceType.volume.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectVolumes.key())));
            projectResourceLimitMap.put(Resource.ResourceType.network.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectNetworks.key())));
            projectResourceLimitMap.put(Resource.ResourceType.vpc.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectVpcs.key())));
            projectResourceLimitMap.put(Resource.ResourceType.cpu.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectCpus.key())));
            projectResourceLimitMap.put(Resource.ResourceType.memory.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectMemory.key())));
            projectResourceLimitMap.put(Resource.ResourceType.primary_storage.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxProjectPrimaryStorage.key())));
            projectResourceLimitMap.put(Resource.ResourceType.secondary_storage.name(), MaxProjectSecondaryStorage.value());

            accountResourceLimitMap.put(Resource.ResourceType.public_ip.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountPublicIPs.key())));
            accountResourceLimitMap.put(Resource.ResourceType.snapshot.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountSnapshots.key())));
            accountResourceLimitMap.put(Resource.ResourceType.template.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountTemplates.key())));
            accountResourceLimitMap.put(Resource.ResourceType.user_vm.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountUserVms.key())));
            accountResourceLimitMap.put(Resource.ResourceType.volume.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountVolumes.key())));
            accountResourceLimitMap.put(Resource.ResourceType.network.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountNetworks.key())));
            accountResourceLimitMap.put(Resource.ResourceType.vpc.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountVpcs.key())));
            accountResourceLimitMap.put(Resource.ResourceType.cpu.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountCpus.key())));
            accountResourceLimitMap.put(Resource.ResourceType.memory.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountMemory.key())));
            accountResourceLimitMap.put(Resource.ResourceType.primary_storage.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxAccountPrimaryStorage.key())));
            accountResourceLimitMap.put(Resource.ResourceType.secondary_storage.name(), MaxAccountSecondaryStorage.value());
            accountResourceLimitMap.put(Resource.ResourceType.project.name(), DefaultMaxAccountProjects.value());

            domainResourceLimitMap.put(Resource.ResourceType.public_ip.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainPublicIPs.key())));
            domainResourceLimitMap.put(Resource.ResourceType.snapshot.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainSnapshots.key())));
            domainResourceLimitMap.put(Resource.ResourceType.template.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainTemplates.key())));
            domainResourceLimitMap.put(Resource.ResourceType.user_vm.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainUserVms.key())));
            domainResourceLimitMap.put(Resource.ResourceType.volume.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainVolumes.key())));
            domainResourceLimitMap.put(Resource.ResourceType.network.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainNetworks.key())));
            domainResourceLimitMap.put(Resource.ResourceType.vpc.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainVpcs.key())));
            domainResourceLimitMap.put(Resource.ResourceType.cpu.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainCpus.key())));
            domainResourceLimitMap.put(Resource.ResourceType.memory.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainMemory.key())));
            domainResourceLimitMap.put(Resource.ResourceType.primary_storage.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainPrimaryStorage.key())));
            domainResourceLimitMap.put(Resource.ResourceType.secondary_storage.name(), Long.parseLong(_configDao.getValue(Config.DefaultMaxDomainSecondaryStorage.key())));
            domainResourceLimitMap.put(Resource.ResourceType.project.name(), DefaultMaxDomainProjects.value());
        } catch (NumberFormatException e) {
            logger.error("NumberFormatException during configuration", e);
            throw new ConfigurationException("Configuration failed due to NumberFormatException, see log for the stacktrace");
        }

        return true;
    }

    @Override
    public void incrementResourceCountWithTag(long accountId, ResourceType type, String tag, Long... delta) {
        // don't upgrade resource count for system account
        if (accountId == Account.ACCOUNT_ID_SYSTEM) {
            logger.trace("Not incrementing resource count for system accounts, returning");
            return;
        }

        final long numToIncrement = (delta.length == 0) ? 1 : delta[0].longValue();
        removeResourceReservationIfNeededAndIncrementResourceCount(accountId, type, tag, numToIncrement);
    }

    @Override
    public void incrementResourceCount(long accountId, ResourceType type, Long... delta) {
        incrementResourceCountWithTag(accountId, type, null, delta);
    }

    @Override
    public void decrementResourceCountWithTag(long accountId, ResourceType type, String tag, Long... delta) {
        // don't upgrade resource count for system account
        if (accountId == Account.ACCOUNT_ID_SYSTEM) {
            logger.trace("Not decrementing resource count for system accounts, returning");
            return;
        }
        long numToDecrement = (delta.length == 0) ? 1 : delta[0].longValue();

        if (!updateResourceCountForAccount(accountId, type, tag, false, numToDecrement)) {
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_UPDATE_RESOURCE_COUNT, 0L, 0L, "Failed to decrement resource count of type " + type + " for account id=" + accountId,
                    "Failed to decrement resource count of type " + type + " for account id=" + accountId + "; use updateResourceCount API to recalculate/fix the problem");
        }
    }

    @Override
    public void decrementResourceCount(long accountId, ResourceType type, Long... delta) {
        decrementResourceCountWithTag(accountId, type, null, delta);
    }

    @Override
    public long findCorrectResourceLimitForAccount(Account account, ResourceType type, String tag) {

        long max = Resource.RESOURCE_UNLIMITED; // if resource limit is not found, then we treat it as unlimited

        // No limits for Root Admin accounts
        if (_accountMgr.isRootAdmin(account.getId())) {
            return max;
        }

        ResourceLimitVO limit = _resourceLimitDao.findByOwnerIdAndTypeAndTag(account.getId(), ResourceOwnerType.Account, type, tag);

        // Check if limit is configured for account
        if (limit != null) {
            max = limit.getMax().longValue();
        } else {
            String resourceTypeName = type.name();
            // If the account has an no limit set, then return global default account limits
            Long value = null;
            if (account.getType() == Account.Type.PROJECT) {
                value = projectResourceLimitMap.get(resourceTypeName);
            } else {
                if (StringUtils.isNotEmpty(tag)) {
                    return findCorrectResourceLimitForAccount(account, type, null);
                }
                value = accountResourceLimitMap.get(resourceTypeName);
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
            if (account.getType() == Account.Type.PROJECT) {
                value = projectResourceLimitMap.get(type.getName());
            } else {
                value = accountResourceLimitMap.get(type.getName());
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
    public long findCorrectResourceLimitForDomain(Domain domain, ResourceType type, String tag) {
        long max = Resource.RESOURCE_UNLIMITED;

        // no limits on ROOT domain
        if (domain.getId() == Domain.ROOT_DOMAIN) {
            return Resource.RESOURCE_UNLIMITED;
        }
        // Check account
        ResourceLimitVO limit = _resourceLimitDao.findByOwnerIdAndTypeAndTag(domain.getId(), ResourceOwnerType.Domain, type, tag);

        if (limit != null) {
            max = limit.getMax().longValue();
        } else {
            // check domain hierarchy
            Long domainId = domain.getParent();
            while ((domainId != null) && (limit == null)) {
                if (domainId == Domain.ROOT_DOMAIN) {
                    break;
                }
                limit = _resourceLimitDao.findByOwnerIdAndTypeAndTag(domainId, ResourceOwnerType.Domain, type, tag);
                DomainVO tmpDomain = _domainDao.findById(domainId);
                domainId = tmpDomain.getParent();
            }

            if (limit != null) {
                max = limit.getMax().longValue();
            } else {
                if (StringUtils.isNotEmpty(tag)) {
                    return findCorrectResourceLimitForDomain(domain, type, null);
                }
                Long value = null;
                value = domainResourceLimitMap.get(type.name());
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

    protected void checkDomainResourceLimit(final Account account, final Project project, final ResourceType type, String tag, long numResources) throws ResourceAllocationException {
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
                long domainResourceLimit = findCorrectResourceLimitForDomain(domain, type, tag);
                long currentDomainResourceCount = _resourceCountDao.getResourceCount(domainId, ResourceOwnerType.Domain, type, tag);
                long currentResourceReservation = reservationDao.getDomainReservation(domainId, type, tag);
                long requestedDomainResourceCount = currentDomainResourceCount + currentResourceReservation + numResources;

                String convDomainResourceLimit = String.valueOf(domainResourceLimit);
                String convCurrentDomainResourceCount = String.valueOf(currentDomainResourceCount);
                String convCurrentResourceReservation = String.valueOf(currentResourceReservation);
                String convNumResources = String.valueOf(numResources);

                if (type == ResourceType.secondary_storage || type == ResourceType.primary_storage){
                    convDomainResourceLimit = toHumanReadableSize(domainResourceLimit);
                    convCurrentDomainResourceCount = toHumanReadableSize(currentDomainResourceCount);
                    convCurrentResourceReservation = toHumanReadableSize(currentResourceReservation);
                    convNumResources = toHumanReadableSize(numResources);
                }

                String typeString = type.getName();
                if (StringUtils.isNotEmpty(tag)) {
                    typeString = String.format("%s (tag: %s)", typeString, tag);
                }
                String messageSuffix = String.format(
                        " domain resource limits of Type '%s' for Domain Id = %s is exceeded: Domain Resource Limit = %s, " +
                        "Current Domain Resource Amount = %s, Current Resource Reservation = %s, Requested Resource Amount = %s.",
                        typeString, domain.getUuid(), convDomainResourceLimit,
                        convCurrentDomainResourceCount, convCurrentResourceReservation, convNumResources
                );

                if (logger.isDebugEnabled()) {
                    logger.debug("Checking if" + messageSuffix);
                }

                if (domainResourceLimit != Resource.RESOURCE_UNLIMITED && requestedDomainResourceCount > domainResourceLimit) {
                    String message = "Maximum" + messageSuffix;
                    ResourceAllocationException e = new ResourceAllocationException(message, type);
                    logger.error(message, e);
                    throw e;
                }
            }
            domainId = domain.getParent();
        }
    }

    protected void checkAccountResourceLimit(final Account account, final Project project, final ResourceType type, String tag, long numResources) throws ResourceAllocationException {
        // Check account limits
        long accountResourceLimit = findCorrectResourceLimitForAccount(account, type, tag);
        long currentResourceCount = _resourceCountDao.getResourceCount(account.getId(), ResourceOwnerType.Account, type, tag);
        long currentResourceReservation = reservationDao.getAccountReservation(account.getId(), type, tag);
        long requestedResourceCount = currentResourceCount + currentResourceReservation + numResources;

        String convertedAccountResourceLimit = String.valueOf(accountResourceLimit);
        String convertedCurrentResourceCount = String.valueOf(currentResourceCount);
        String convertedCurrentResourceReservation = String.valueOf(currentResourceReservation);
        String convertedNumResources = String.valueOf(numResources);

        if (type == ResourceType.secondary_storage || type == ResourceType.primary_storage){
            convertedAccountResourceLimit = toHumanReadableSize(accountResourceLimit);
            convertedCurrentResourceCount = toHumanReadableSize(currentResourceCount);
            convertedCurrentResourceReservation = toHumanReadableSize(currentResourceReservation);
            convertedNumResources = toHumanReadableSize(numResources);
        }

        String messageSuffix = String.format(
                " amount of resources of Type = '%s', tag = '%s' for %s in Domain Id = %s is exceeded: " +
                "Account Resource Limit = %s, Current Account Resource Amount = %s, Current Account Resource Reservation = %s, Requested Resource Amount = %s.",
                type, tag, (project == null ? "Account Name = " + account.getAccountName() : "Project Name = " + project.getName()), account.getDomainId(),
                convertedAccountResourceLimit, convertedCurrentResourceCount, convertedCurrentResourceReservation, convertedNumResources
        );

        if (logger.isDebugEnabled()) {
            logger.debug("Checking if" + messageSuffix);
        }

        if (accountResourceLimit != Resource.RESOURCE_UNLIMITED && requestedResourceCount > accountResourceLimit) {
            String message = "Maximum" + messageSuffix;
            ResourceAllocationException e = new ResourceAllocationException(message, type);
            logger.error(message, e);
            throw e;
        }
    }

    protected List<ResourceCountVO> lockAccountAndOwnerDomainRows(long accountId, final ResourceType type, String tag) {
        Set<Long> rowIdsToLock = _resourceCountDao.listAllRowsToUpdate(accountId, ResourceOwnerType.Account, type, tag);
        SearchCriteria<ResourceCountVO> sc = ResourceCountSearch.create();
        sc.setParameters("id", rowIdsToLock.toArray());
        return _resourceCountDao.lockRows(sc, null, true);
    }

    @Override
    public long findDefaultResourceLimitForDomain(ResourceType resourceType) {
        Long resourceLimit = null;
        resourceLimit = domainResourceLimitMap.get(resourceType.getName());
        if (resourceLimit != null && (resourceType == ResourceType.primary_storage || resourceType == ResourceType.secondary_storage)) {
            if (! Long.valueOf(Resource.RESOURCE_UNLIMITED).equals(resourceLimit)) {
                resourceLimit = resourceLimit * ResourceType.bytesToGiB;
            }
        } else {
            resourceLimit = Long.valueOf(Resource.RESOURCE_UNLIMITED);
        }
        return resourceLimit;
    }

    @Override
    public long findCorrectResourceLimitForAccountAndDomain(Account account, Domain domain, ResourceType type, String tag) {
        long maxSecondaryStorageForAccount = findCorrectResourceLimitForAccount(account, type, tag);
        long maxSecondaryStorageForDomain = findCorrectResourceLimitForDomain(domain, type, tag);

        if (maxSecondaryStorageForDomain == Resource.RESOURCE_UNLIMITED || maxSecondaryStorageForAccount == Resource.RESOURCE_UNLIMITED) {
            return Math.max(maxSecondaryStorageForDomain, maxSecondaryStorageForAccount);
        }

        return Math.min(maxSecondaryStorageForDomain, maxSecondaryStorageForAccount);
    }

    @Override
    public void checkResourceLimit(final Account account, final ResourceType type, long... count) throws ResourceAllocationException {
        checkResourceLimitWithTag(account, type, null, count);
    }

    @Override
    public void checkResourceLimitWithTag(final Account account, final ResourceType type, String tag, long... count) throws ResourceAllocationException {
        final long numResources = ((count.length == 0) ? 1 : count[0]);
        Project project = null;

        // Don't place any limits on system or root admin accounts
        if (_accountMgr.isRootAdmin(account.getId())) {
            return;
        }

        if (account.getType() == Account.Type.PROJECT) {
            project = _projectDao.findByProjectAccountId(account.getId());
        }

        final Project projectFinal = project;
        Transaction.execute(new TransactionCallbackWithExceptionNoReturn<ResourceAllocationException>() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) throws ResourceAllocationException {
                // Lock all rows first so nobody else can read it
                lockAccountAndOwnerDomainRows(account.getId(), type, tag);
                // Check account limits
                checkAccountResourceLimit(account, projectFinal, type, tag, numResources);
                // check all domains in the account's domain hierarchy
                checkDomainResourceLimit(account, projectFinal, type, tag, numResources);
            }
        });
    }

    /**
     * To retrieve host and storage limit tags lists with or without a given tag string
     * while searching for limits for an account or domain
     * @param tag - tag string to filter list of host and storage limit tags
     * @return a pair of host tags list and storage tags list
     */
    protected Pair<List<String>, List<String>> getResourceLimitTagsForLimitSearch(String tag) {
        List<String> hostTags = getResourceLimitHostTags();
        List<String> storageTags = getResourceLimitStorageTags();
        if (tag == null) {
            return new Pair<>(hostTags, storageTags);
        }
        if (hostTags.contains(tag)) {
            hostTags = List.of(tag);
        } else {
            hostTags = new ArrayList<>();
        }
        if (storageTags.contains(tag)) {
            storageTags = List.of(tag);
        } else  {
            storageTags = new ArrayList<>();
        }
        return new Pair<>(hostTags, storageTags);
    }

    @Override
    public List<ResourceLimitVO> searchForLimits(Long id, Long accountId, Long domainId, ResourceType resourceType, String tag, Long startIndex, Long pageSizeVal) {
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
        sb.and("tag", sb.entity().getTag(), SearchCriteria.Op.EQ);

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

        if (tag != null) {
            sc.setParameters("tag", tag);
        }

        List<ResourceLimitVO> foundLimits = _resourceLimitDao.search(sc, filter);

        Pair<List<String>, List<String>> tagsPair = getResourceLimitTagsForLimitSearch(tag);
        List<String> hostTags = tagsPair.first();
        List<String> storageTags = tagsPair.second();

        if (resourceType != null) {
            if (foundLimits.isEmpty()) {
                ResourceOwnerType ownerType = ResourceOwnerType.Domain;
                Long ownerId = domainId;
                long max = 0;
                if (isAccount) {
                    ownerType = ResourceOwnerType.Account;
                    ownerId = accountId;
                    max = findCorrectResourceLimitForAccount(_accountMgr.getAccount(accountId), resourceType, tag);
                } else {
                    max = findCorrectResourceLimitForDomain(_domainDao.findById(domainId), resourceType, tag);
                }
                limits.add(new ResourceLimitVO(resourceType, max, ownerId, ownerType));
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
                            if (!accountLimitStr.contains(rt.toString())) {
                                limits.add(new ResourceLimitVO(rt, findCorrectResourceLimitForAccount(_accountMgr.getAccount(accountId), rt, null), accountId, ResourceOwnerType.Account));
                            }
                        }
                    }
                } else {
                    if (domainLimitStr.size() < resourceTypes.length) {
                        for (ResourceType rt : resourceTypes) {
                            if (!domainLimitStr.contains(rt.toString())) {
                                limits.add(new ResourceLimitVO(rt, findCorrectResourceLimitForDomain(_domainDao.findById(domainId), rt, null), domainId, ResourceOwnerType.Domain));
                            }
                        }
                    }
                }
            }
        }
        addTaggedResourceLimits(limits, resourceType, isAccount ? ResourceOwnerType.Account : ResourceOwnerType.Domain, isAccount ? accountId : domainId, hostTags, storageTags);
        return limits;
    }

    protected void addTaggedResourceLimits(List<ResourceLimitVO> limits, List<ResourceType> types, List<String> tags, ResourceOwnerType ownerType, long ownerId) {
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        if (CollectionUtils.isEmpty(types)) {
            return;
        }
        for (String tag : tags) {
            for (ResourceType type : types) {
                if (limits.stream().noneMatch(l -> type.equals(l.getType()) && tag.equals(l.getTag()))) {
                    limits.add(new ResourceLimitVO(type, ResourceOwnerType.Domain.equals(ownerType) ?
                            findCorrectResourceLimitForDomain(_domainDao.findById(ownerId), type, tag) :
                            findCorrectResourceLimitForAccount(_accountDao.findById(ownerId), type, tag),
                            ownerId, ownerType, tag));
                }
            }
        }
    }

    protected void removeUndesiredTaggedLimits(List<ResourceLimitVO> limits, List<String> hostTags, List<String> storageTags) {
        Iterator<ResourceLimitVO> itr = limits.iterator();
        while (itr.hasNext()) {
            ResourceLimitVO limit = itr.next();
            if (StringUtils.isEmpty(limit.getTag())) {
                continue;
            }
            if (HostTagsSupportingTypes.contains(limit.getType()) &&
                    (CollectionUtils.isEmpty(hostTags) || !hostTags.contains(limit.getTag()))) {
                itr.remove();
            }
            if (StorageTagsSupportingTypes.contains(limit.getType()) &&
                    (CollectionUtils.isEmpty(storageTags) || !storageTags.contains(limit.getTag()))) {
                itr.remove();
            }
        }
    }

    protected void addTaggedResourceLimits(List<ResourceLimitVO> limits, ResourceType resourceType, ResourceOwnerType ownerType, long ownerId, List<String> hostTags, List<String> storageTags) {
        removeUndesiredTaggedLimits(limits, hostTags, storageTags);
        if (CollectionUtils.isEmpty(hostTags) && CollectionUtils.isEmpty(storageTags)) {
            return;
        }
        List<ResourceType> types = resourceType != null ? HostTagsSupportingTypes.contains(resourceType) ? List.of(resourceType) : null : HostTagsSupportingTypes;
        addTaggedResourceLimits(limits, types, hostTags, ownerType, ownerId);
        types = resourceType != null ? StorageTagsSupportingTypes.contains(resourceType) ? List.of(resourceType) : null : StorageTagsSupportingTypes;
        addTaggedResourceLimits(limits, types, storageTags, ownerType, ownerId);
        limits.sort((o1, o2) -> {
            Integer type1 = o1.getType().getOrdinal();
            Integer type2 = o2.getType().getOrdinal();
            if (type1.equals(type2)) {
                return StringUtils.defaultString(o1.getTag(), "").compareTo(StringUtils.defaultString(o2.getTag(), ""));
            }
            return type1.compareTo(type2);
        });
    }

    @Override
    public ResourceLimitVO updateResourceLimit(Long accountId, Long domainId, Integer typeId, Long max, String tag) {
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

        if (StringUtils.isNotEmpty(tag) &&
                !(HostTagsSupportingTypes.contains(resourceType) ||
                        StorageTagsSupportingTypes.contains(resourceType))) {
            throw new InvalidParameterValueException(String.format("Resource limit with a tag is not supported for resource type %d", typeId));
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

            if ((caller.getAccountId() == accountId.longValue()) && (_accountMgr.isDomainAdmin(caller.getId()) || caller.getType() == Account.Type.RESOURCE_DOMAIN_ADMIN)) {
                // If the admin is trying to update their own account, disallow.
                throw new PermissionDeniedException("Unable to update resource limit for their own account " + accountId + ", permission denied");
            }

            if (account.getType() == Account.Type.PROJECT) {
                _accountMgr.checkAccess(caller, AccessType.ModifyProject, true, account);
            } else {
                _accountMgr.checkAccess(caller, null, true, account);
            }

            ownerType = ResourceOwnerType.Account;
            ownerId = accountId;
            if (StringUtils.isNotEmpty(tag)) {
                long untaggedLimit = findCorrectResourceLimitForAccount(account, resourceType, null);
                if (untaggedLimit > 0 && max > untaggedLimit) {
                    throw new InvalidParameterValueException(String.format("Maximum untagged resource limit for account %s for resource type %s is %d, please specify a value less than or equal to that",
                            account.getAccountName(), resourceType, untaggedLimit));
                }
            }
        } else if (domainId != null) {
            Domain domain = _entityMgr.findById(Domain.class, domainId);

            _accountMgr.checkAccess(caller, domain);

            if (Domain.ROOT_DOMAIN == domainId.longValue()) {
                // no one can add limits on ROOT domain, disallow...
                throw new PermissionDeniedException("Cannot update resource limit for ROOT domain " + domainId + ", permission denied");
            }

            if ((caller.getDomainId() == domainId.longValue()) && caller.getType() == Account.Type.DOMAIN_ADMIN || caller.getType() == Account.Type.RESOURCE_DOMAIN_ADMIN) {
                // if the admin is trying to update their own domain, disallow...
                throw new PermissionDeniedException("Unable to update resource limit for domain " + domainId + ", permission denied");
            }
            if (StringUtils.isNotEmpty(tag)) {
                long untaggedLimit = findCorrectResourceLimitForDomain(domain, resourceType, null);
                if (untaggedLimit > 0 && max > untaggedLimit) {
                    throw new InvalidParameterValueException(String.format("Maximum untagged resource limit for domain %s for resource type %s is %d, please specify a value less than or equal to that",
                            domain.getName(), resourceType, untaggedLimit));
                }
            }
            Long parentDomainId = domain.getParent();
            if (parentDomainId != null) {
                DomainVO parentDomain = _domainDao.findById(parentDomainId);
                long parentMaximum = findCorrectResourceLimitForDomain(parentDomain, resourceType, tag);
                if ((parentMaximum >= 0) && (max.longValue() > parentMaximum)) {
                    throw new InvalidParameterValueException("Domain " + domain.getName() + "(id: " + parentDomain.getId() + ") has maximum allowed resource limit " + parentMaximum + " for "
                            + resourceType + ", please specify a value less than or equal to " + parentMaximum);
                }
            }
            ownerType = ResourceOwnerType.Domain;
            ownerId = domainId;
        }

        if (ownerId == null) {
            throw new InvalidParameterValueException("AccountId or domainId have to be specified in order to update resource limit");
        }

        ResourceLimitVO limit = _resourceLimitDao.findByOwnerIdAndTypeAndTag(ownerId, ownerType, resourceType, tag);
        if (limit != null) {
            // Update the existing limit
            _resourceLimitDao.update(limit.getId(), max);
            return _resourceLimitDao.findById(limit.getId());
        } else {
            return _resourceLimitDao.persist(new ResourceLimitVO(resourceType, max, ownerId, ownerType, tag));
        }
    }

    protected boolean isTaggedResourceCountRecalculationNotNeeded(ResourceType type, List<String> hostTags, List <String> storageTags) {
        if (!HostTagsSupportingTypes.contains(type) && !StorageTagsSupportingTypes.contains(type)) {
            return true;
        }
        return CollectionUtils.isEmpty(hostTags) && CollectionUtils.isEmpty(storageTags);
    }

    protected void removeResourceLimitAndCountForNonMatchingTags(Long ownerId, ResourceOwnerType ownerType,
                                                                 List<String> hostTags, List<String> storageTags) {
        if (logger.isDebugEnabled()) {
            String msg = String.format("Clearing tagged resource limits and counts which do not match " +
                            "host tags: %s, storage tags: %s",
                    StringUtils.join(hostTags), StringUtils.join(storageTags));
            if (ObjectUtils.allNotNull(ownerId, ownerType)) {
                msg = String.format("%s for %s ID: %d", msg, ownerType.getName().toLowerCase(), ownerId);
            }
            logger.debug(msg);
        }
        _resourceLimitDao.removeResourceLimitsForNonMatchingTags(ownerId, ownerType, HostTagsSupportingTypes, hostTags);
        _resourceLimitDao.removeResourceLimitsForNonMatchingTags(ownerId, ownerType, StorageTagsSupportingTypes, storageTags);
        _resourceCountDao.removeResourceCountsForNonMatchingTags(ownerId, ownerType, HostTagsSupportingTypes, hostTags);
        _resourceCountDao.removeResourceCountsForNonMatchingTags(ownerId, ownerType, StorageTagsSupportingTypes, storageTags);
    }

    protected List<ResourceCountVO> recalculateAccountTaggedResourceCount(long accountId, ResourceType type, final List<String> hostTags, final List<String> storageTags) {
        List<ResourceCountVO> result = new ArrayList<>();
        if (isTaggedResourceCountRecalculationNotNeeded(type, hostTags, storageTags)) {
            return result;
        }
        if (HostTagsSupportingTypes.contains(type) && CollectionUtils.isNotEmpty(hostTags)) {
            for (String tag : hostTags) {
                long count = recalculateAccountResourceCount(accountId, type, tag);
                result.add(new ResourceCountVO(type, count, accountId, ResourceOwnerType.Account, tag));
            }
        }
        if (StorageTagsSupportingTypes.contains(type) && CollectionUtils.isNotEmpty(storageTags)) {
            for (String tag : storageTags) {
                long count = recalculateAccountResourceCount(accountId, type, tag);
                result.add(new ResourceCountVO(type, count, accountId, ResourceOwnerType.Account, tag));
            }
        }
        return result;
    }

    protected List<ResourceCountVO> recalculateDomainTaggedResourceCount(long domainId, ResourceType type, final List<String> hostTags, final List<String> storageTags) {
        List<ResourceCountVO> result = new ArrayList<>();
        if (isTaggedResourceCountRecalculationNotNeeded(type, hostTags, storageTags)) {
            return result;
        }
        if (HostTagsSupportingTypes.contains(type) && CollectionUtils.isNotEmpty(hostTags)) {
            for (String tag : hostTags) {
                long count = recalculateDomainResourceCount(domainId, type, tag);
                result.add(new ResourceCountVO(type, count, domainId, ResourceOwnerType.Domain, tag));
            }
        }
        if (StorageTagsSupportingTypes.contains(type) && CollectionUtils.isNotEmpty(storageTags)) {
            for (String tag : storageTags) {
                long count = recalculateDomainResourceCount(domainId, type, tag);
                result.add(new ResourceCountVO(type, count, domainId, ResourceOwnerType.Domain, tag));
            }
        }
        return result;
    }

    @Override
    public List<? extends ResourceCount> recalculateResourceCount(Long accountId, Long domainId, Integer typeId, String tag) throws CloudRuntimeException {
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
            if (StringUtils.isNotEmpty(tag) &&
                    !(HostTagsSupportingTypes.contains(resourceType) ||
                            StorageTagsSupportingTypes.contains(resourceType))) {
                throw new InvalidParameterValueException(String.format("Resource count with a tag is not supported for resource type %d", typeId));
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

        List<String> hostTags = getResourceLimitHostTags();
        List<String> storageTags = getResourceLimitStorageTags();
        removeResourceLimitAndCountForNonMatchingTags(accountId != null ? accountId : domainId,
                accountId != null ? ResourceOwnerType.Account : ResourceOwnerType.Domain, hostTags, storageTags);
        for (ResourceType type : resourceTypes) {
            if (accountId != null) {
                count = recalculateAccountResourceCount(accountId, type, tag);
                counts.add(new ResourceCountVO(type, count, accountId, ResourceOwnerType.Account));
                if (StringUtils.isEmpty(tag)) {
                    counts.addAll(recalculateAccountTaggedResourceCount(accountId, type, hostTags, storageTags));
                }
            } else {
                count = recalculateDomainResourceCount(domainId, type, tag);
                counts.add(new ResourceCountVO(type, count, domainId, ResourceOwnerType.Domain));
                if (StringUtils.isEmpty(tag)) {
                    counts.addAll(recalculateDomainTaggedResourceCount(domainId, type, hostTags, storageTags));
                }
            }
        }

        return counts;
    }

    @Override
    public List<? extends ResourceCount> recalculateResourceCount(Long accountId, Long domainId, Integer typeId) throws CloudRuntimeException {
        return recalculateResourceCount(accountId, domainId, typeId, null);
    }

    protected boolean updateResourceCountForAccount(final long accountId, final ResourceType type, String tag, final boolean increment, final long delta) {
        if (delta == 0) {
            return true;
        } else if (delta < 0) {
            logger.warn("Resource count delta is negative, delta = {} for Account = {} Type = {} tag = {}",
                    delta, accountId, type, tag);
            return true;
        }
        if (logger.isDebugEnabled()) {
            String convertedDelta = String.valueOf(delta);
            if (type == ResourceType.secondary_storage || type == ResourceType.primary_storage){
                convertedDelta = toHumanReadableSize(delta);
            }
            String typeStr = StringUtils.isNotEmpty(tag) ? String.format("%s (tag: %s)", type, tag) : type.getName();
            logger.debug("Updating resource Type = " + typeStr + " count for Account = " + accountId + " Operation = " + (increment ? "increasing" : "decreasing") + " Amount = " + convertedDelta);
        }
        Set<Long> rowIdsToUpdate = _resourceCountDao.listAllRowsToUpdate(accountId, ResourceOwnerType.Account, type, tag);
        return _resourceCountDao.updateCountByDeltaForIds(new ArrayList<>(rowIdsToUpdate), increment, delta);
    }

    /**
     * This will take care of re-calculation of resource counts for root and sub-domains
     * and accounts of the sub-domains also. so just loop through immediate children of root domain
     *
     * @param domainId the domain level to start at
     * @param type the resource type to do the recalculation for
     * @return the resulting new resource count
     */
    protected long recalculateDomainResourceCount(final long domainId, final ResourceType type, String tag) {
        List<AccountVO> accounts = _accountDao.findActiveAccountsForDomain(domainId);
        List<DomainVO> childDomains = _domainDao.findImmediateChildrenForParent(domainId);

        if (CollectionUtils.isNotEmpty(childDomains)) {
            for (DomainVO childDomain : childDomains) {
                recalculateDomainResourceCount(childDomain.getId(), type, tag);
            }
        }
        if (CollectionUtils.isNotEmpty(accounts)) {
            for (AccountVO account : accounts) {
                recalculateAccountResourceCount(account.getId(), type, tag);
            }
        }

        return Transaction.execute((TransactionCallback<Long>) status -> {
            long newResourceCount = 0L;
            List<Long> domainIdList = childDomains.stream().map(DomainVO::getId).collect(Collectors.toList());
            domainIdList.add(domainId);
            List<Long> accountIdList = accounts.stream().map(AccountVO::getId).collect(Collectors.toList());
            List<ResourceCountVO> domainRCList = _resourceCountDao.findByOwnersAndTypeAndTag(domainIdList, ResourceOwnerType.Domain, type, tag);
            List<ResourceCountVO> accountRCList = _resourceCountDao.findByOwnersAndTypeAndTag(accountIdList, ResourceOwnerType.Account, type, tag);

            Set<Long> rowIdsToLock = new HashSet<>();
            if (domainRCList != null) {
                rowIdsToLock.addAll(domainRCList.stream().map(ResourceCountVO::getId).collect(Collectors.toList()));
            }
            if (accountRCList != null) {
                rowIdsToLock.addAll(accountRCList.stream().map(ResourceCountVO::getId).collect(Collectors.toList()));
            }
            // lock the resource count rows for current domain, immediate child domain & accounts
            List<ResourceCountVO> resourceCounts = _resourceCountDao.lockRows(rowIdsToLock);

            long oldResourceCount = 0L;
            ResourceCountVO domainRC = null;

            // calculate project count here
            if (type == ResourceType.project) {
                newResourceCount += _projectDao.countProjectsForDomain(domainId);
            }

            for (ResourceCountVO resourceCount : resourceCounts) {
                if (resourceCount.getResourceOwnerType() == ResourceOwnerType.Domain && resourceCount.getDomainId() == domainId) {
                    oldResourceCount = resourceCount.getCount();
                    domainRC = resourceCount;
                } else {
                    newResourceCount += resourceCount.getCount();
                }
            }

            if (oldResourceCount != newResourceCount) {
                domainRC.setCount(newResourceCount);
                _resourceCountDao.update(domainRC.getId(), domainRC);
                logger.warn("Discrepency in the resource count has been detected " + "(original count = " + oldResourceCount + " correct count = " + newResourceCount + ") for Type = " + type
                        + " for Domain ID = " + domainId + " is fixed during resource count recalculation.");
            }
            return newResourceCount;
        });
    }

    protected void cleanupStaleResourceReservations(final long accountId, final ResourceType type, String tag) {
        Long delay = ResourceReservationCleanupDelay.value();
        if (delay == null || delay <= 0) {
            return;
        }
        Date cleanupBefore = new Date(System.currentTimeMillis() - delay * 1000);
        int rowsRemoved = reservationDao.removeStaleReservations(accountId, type, tag, cleanupBefore);
        if (rowsRemoved > 0) {
            logger.warn("Removed {} stale resource reservations for account {} of type {} and tag {}",
                    rowsRemoved, accountId, type, tag);
        }
    }

    @DB
    protected long recalculateAccountResourceCount(final long accountId, final ResourceType type, String tag) {
        cleanupStaleResourceReservations(accountId, type, tag);
        final Long newCount;
        if (type == Resource.ResourceType.user_vm) {
            newCount = calculateVmCountForAccount(accountId, tag);
        } else if (type == Resource.ResourceType.volume) {
            newCount = calculateVolumeCountForAccount(accountId, tag);
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
            newCount = calculateVmCpuCountForAccount(accountId, tag);
        } else if (type == Resource.ResourceType.memory) {
            newCount = calculateVmMemoryCountForAccount(accountId, tag);
        } else if (type == Resource.ResourceType.primary_storage) {
            newCount = calculatePrimaryStorageForAccount(accountId, tag);
        } else if (type == Resource.ResourceType.secondary_storage) {
            newCount = calculateSecondaryStorageForAccount(accountId);
        } else {
            throw new InvalidParameterValueException("Unsupported resource type " + type);
        }

        long oldCount = 0;
        final ResourceCountVO accountRC = _resourceCountDao.findByOwnerAndTypeAndTag(accountId, ResourceOwnerType.Account, type, tag);
        if (accountRC != null) {
            oldCount = accountRC.getCount();
            if (newCount == null || !newCount.equals(oldCount)) {
                accountRC.setCount((newCount == null) ? 0 : newCount);
                _resourceCountDao.update(accountRC.getId(), accountRC);
            }
        } else if (newCount != null) {
            _resourceCountDao.persist(new ResourceCountVO(type, newCount, accountId, ResourceOwnerType.Account, tag));
        }

        // No need to log message for primary and secondary storage because both are recalculating the
        // resource count which will not lead to any discrepancy.
        if (newCount != null && !newCount.equals(oldCount) &&
                type != Resource.ResourceType.primary_storage && type != Resource.ResourceType.secondary_storage) {
            logger.warn("Discrepancy in the resource count " + "(original count=" + oldCount + " correct count = " + newCount + ") for type " + type +
                    " for account ID " + accountId + " is fixed during resource count recalculation.");
        }

        return (newCount == null) ? 0 : newCount;
    }

    protected List<UserVmJoinVO> getVmsWithAccountAndTag(long accountId, String tag) {
        List<VirtualMachine.State> states = new ArrayList<>(Arrays.asList(State.Destroyed, State.Error, State.Expunging));
        if (VirtualMachineManager.ResourceCountRunningVMsonly.value()) {
            states.add(State.Stopped);
        }
        if (StringUtils.isEmpty(tag)) {
            return _userVmJoinDao.listByAccountServiceOfferingTemplateAndNotInState(accountId, states, null, null);
        }
        List<ServiceOfferingVO> offerings = serviceOfferingDao.listByHostTag(tag);
        List<VMTemplateVO> templates = _vmTemplateDao.listByTemplateTag(tag);
        if (CollectionUtils.isEmpty(offerings) && CollectionUtils.isEmpty(templates)) {
            return new ArrayList<>();
        }

        return  _userVmJoinDao.listByAccountServiceOfferingTemplateAndNotInState(accountId, states,
                offerings.stream().map(ServiceOfferingVO::getId).collect(Collectors.toList()),
                templates.stream().map(VMTemplateVO::getId).collect(Collectors.toList())
        );
    }

    protected List<UserVmJoinVO> getVmsWithAccount(long accountId) {
        return getVmsWithAccountAndTag(accountId, null);
    }

    protected List<VolumeVO> getVolumesWithAccountAndTag(long accountId, String tag) {
        List<DiskOfferingVO> offerings = diskOfferingDao.listByStorageTag(tag);
        if (CollectionUtils.isEmpty(offerings)) {
            return new ArrayList<>();
        }
        List<Long> vrIds = _vmDao.findIdsOfAllocatedVirtualRoutersForAccount(accountId);
        return _volumeDao.listAllocatedVolumesForAccountDiskOfferingIdsAndNotForVms(accountId,
                offerings.stream().map(DiskOfferingVO::getId).collect(Collectors.toList()),
                vrIds);
    }

    private long calculateReservedResources(List<UserVmJoinVO> vms, long accountId, ResourceType type, String tag) {
        Set<Long> vmIds = vms.stream().map(UserVmJoinVO::getId).collect(Collectors.toSet());
        List<ReservationVO> reservations = reservationDao.getReservationsForAccount(accountId, type, tag);
        long reserved = 0;
        for (ReservationVO reservation : reservations) {
            if (vmIds.contains(reservation.getResourceId()) ? reservation.getReservedAmount() > 0 : reservation.getReservedAmount() < 0) {
                reserved += reservation.getReservedAmount();
            }
        }
        return reserved;
    }

    protected long calculateVmCountForAccount(long accountId, String tag) {
        if (StringUtils.isEmpty(tag)) {
            return _userVmDao.countAllocatedVMsForAccount(accountId, VirtualMachineManager.ResourceCountRunningVMsonly.value());
        }

        List<UserVmJoinVO> vms = getVmsWithAccountAndTag(accountId, tag);
        long reservedVMs = calculateReservedResources(vms, accountId, ResourceType.user_vm, tag);
        return vms.size() - reservedVMs;
    }

    protected long calculateVolumeCountForAccount(long accountId, String tag) {
        if (StringUtils.isEmpty(tag)) {
            long virtualRouterCount = _vmDao.findIdsOfAllocatedVirtualRoutersForAccount(accountId).size();
            return _volumeDao.countAllocatedVolumesForAccount(accountId) - virtualRouterCount; // don't count the volumes of virtual router
        }
        List<VolumeVO> volumes = getVolumesWithAccountAndTag(accountId, tag);
        return volumes.size();
    }

    protected long calculateVmCpuCountForAccount(long accountId, String tag) {
        if (StringUtils.isEmpty(tag)) {
            return countCpusForAccount(accountId);
        }
        long cputotal = 0;
        List<UserVmJoinVO> vms = getVmsWithAccountAndTag(accountId, tag);

        for (UserVmJoinVO vm : vms) {
            cputotal += vm.getCpu();
        }
        long reservedCpus = calculateReservedResources(vms, accountId, ResourceType.cpu, tag);
        return cputotal - reservedCpus;
    }

    protected long calculateVmMemoryCountForAccount(long accountId, String tag) {
        if (StringUtils.isEmpty(tag)) {
            return calculateMemoryForAccount(accountId);
        }
        long memory = 0;
        List<UserVmJoinVO> vms = getVmsWithAccountAndTag(accountId, tag);

        for (UserVmJoinVO vm : vms) {
            memory += vm.getRamSize();
        }
        long reservedMemory = calculateReservedResources(vms, accountId, ResourceType.memory, tag);
        return memory - reservedMemory;
    }

    public long countCpusForAccount(long accountId) {
        long cputotal = 0;
        List<UserVmJoinVO> userVms = getVmsWithAccount(accountId);
        for (UserVmJoinVO vm : userVms) {
            cputotal += vm.getCpu();
        }
        long reservedCpuTotal = calculateReservedResources(userVms, accountId, ResourceType.cpu, null);
        return cputotal - reservedCpuTotal;
    }

    public long calculateMemoryForAccount(long accountId) {
        long ramtotal = 0;
        List<UserVmJoinVO> userVms = getVmsWithAccount(accountId);
        for (UserVmJoinVO vm : userVms) {
            ramtotal += vm.getRamSize();
        }
        long reservedRamTotal = calculateReservedResources(userVms, accountId, ResourceType.memory, null);
        return ramtotal - reservedRamTotal;
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

    protected long calculatePrimaryStorageForAccount(long accountId, String tag) {
        if (StringUtils.isEmpty(tag)) {
            List<Long> virtualRouters = _vmDao.findIdsOfAllocatedVirtualRoutersForAccount(accountId);
            return _volumeDao.primaryStorageUsedForAccount(accountId, virtualRouters);
        }
        long storage = 0;
        List<VolumeVO> volumes = getVolumesWithAccountAndTag(accountId, tag);
        for (VolumeVO volume : volumes) {
            storage += volume.getSize() == null ? 0L : volume.getSize();
        }
        return storage;
    }

    @Override
    public long getResourceCount(Account account, ResourceType type, String tag) {
        return _resourceCountDao.getResourceCount(account.getId(), ResourceOwnerType.Account, type, tag);
    }

    private boolean isDisplayFlagOn(Boolean displayResource) {

        // 1. If its null assume displayResource = 1
        // 2. If its not null then send true if displayResource = 1
        return ! Boolean.FALSE.equals(displayResource);
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
    public List<String> getResourceLimitHostTags() {
        if (StringUtils.isEmpty(ResourceLimitService.ResourceLimitHostTags.value())) {
            return new ArrayList<>();
        }
        return Stream.of(ResourceLimitService.ResourceLimitHostTags.value().split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getResourceLimitStorageTags() {
        if (StringUtils.isEmpty(ResourceLimitService.ResourceLimitStorageTags.value())) {
            return new ArrayList<>();
        }
        return Arrays.asList(ResourceLimitService.ResourceLimitStorageTags.value().split(","));
    }

    protected TaggedResourceLimitAndCountResponse getTaggedResourceLimitAndCountResponse(Account account,
         Domain domain, ResourceOwnerType ownerType, ResourceType type, String tag) {
        Long limit = ResourceOwnerType.Account.equals(ownerType) ?
                findCorrectResourceLimitForAccount(account, type, tag) :
                findCorrectResourceLimitForDomain(domain, type, tag);
        Long count = 0L;
        ResourceCountVO countVO = _resourceCountDao.findByOwnerAndTypeAndTag(
                ResourceOwnerType.Account.equals(ownerType) ? account.getId() : domain.getId(), ownerType, type, tag);
        if (countVO != null) {
            count = countVO.getCount();
        }
        TaggedResourceLimitAndCountResponse taggedResourceLimitAndCountResponse = new TaggedResourceLimitAndCountResponse();
        taggedResourceLimitAndCountResponse.setResourceType(type);
        taggedResourceLimitAndCountResponse.setTag(tag);
        taggedResourceLimitAndCountResponse.setLimit(limit);
        taggedResourceLimitAndCountResponse.setTotal(count);
        taggedResourceLimitAndCountResponse.setAvailable(limit == Resource.RESOURCE_UNLIMITED ? Resource.RESOURCE_UNLIMITED : (limit - count));
        return taggedResourceLimitAndCountResponse;
    }

    protected void updateTaggedResourceLimitsAndCounts(String uuid, ResourceOwnerType ownerType, List<String> hostTags,
           List<String> storageTags, ResourceLimitAndCountResponse response) {
        Account account = null;
        if (ResourceOwnerType.Account.equals(ownerType)) {
            account = _accountDao.findByUuid(uuid);
        }
        Domain domain = null;
        if (ResourceOwnerType.Domain.equals(ownerType)) {
            domain = _domainDao.findByUuid(uuid);
        }
        List<TaggedResourceLimitAndCountResponse> taggedResponses = new ArrayList<>();
        for (String tag : hostTags) {
            for (ResourceType type : HostTagsSupportingTypes) {
                taggedResponses.add(getTaggedResourceLimitAndCountResponse(account, domain, ownerType, type, tag));
            }
        }
        for (String tag : storageTags) {
            for (ResourceType type : StorageTagsSupportingTypes) {
                taggedResponses.add(getTaggedResourceLimitAndCountResponse(account, domain, ownerType, type, tag));
            }
        }
        response.setTaggedResourceLimitsAndCounts(taggedResponses);
    }

    protected void updateTaggedResourceLimitsAndCountsForAccountsOrDomains(List<AccountResponse> accountResponses, List<DomainResponse> domainResponses, String tag) {
        List<String> hostTags = new ArrayList<>(getResourceLimitHostTags());
        List<String> storageTags = new ArrayList<>(getResourceLimitStorageTags());
        if (StringUtils.isNotEmpty(tag)) {
            hostTags.retainAll(List.of(tag));
            storageTags.retainAll(List.of(tag));
        }
        if (CollectionUtils.isEmpty(hostTags) && CollectionUtils.isEmpty(storageTags)) {
            return;
        }
        if (CollectionUtils.isNotEmpty(accountResponses)) {
            for (AccountResponse response : accountResponses) {
                updateTaggedResourceLimitsAndCounts(response.getObjectId(), ResourceOwnerType.Account, hostTags, storageTags, response);
            }
        }
        if (CollectionUtils.isNotEmpty(domainResponses)) {
            for (DomainResponse response : domainResponses) {
                updateTaggedResourceLimitsAndCounts(response.getId(), ResourceOwnerType.Domain, hostTags, storageTags, response);
            }
        }
    }

    @Override
    public void updateTaggedResourceLimitsAndCountsForAccounts(List<AccountResponse> responses, String tag) {
        updateTaggedResourceLimitsAndCountsForAccountsOrDomains(responses, null, tag);
    }

    @Override
    public void updateTaggedResourceLimitsAndCountsForDomains(List<DomainResponse> responses, String tag) {
        updateTaggedResourceLimitsAndCountsForAccountsOrDomains(null, responses, tag);
    }

    @Override
    public List<String> getResourceLimitHostTags(ServiceOffering serviceOffering, VirtualMachineTemplate template) {
        if (StringUtils.isEmpty(serviceOffering.getHostTag()) && StringUtils.isEmpty(template.getTemplateTag())) {
            return new ArrayList<>();
        }
        List<String> resourceLimitTagsFromConfig = getResourceLimitHostTags();
        if (CollectionUtils.isEmpty(resourceLimitTagsFromConfig)) {
            return new ArrayList<>();
        }
        List<String> tags = new ArrayList<>();
        if (StringUtils.isNotEmpty(serviceOffering.getHostTag())) {
            List<String> offeringTags = com.cloud.utils.StringUtils.csvTagsToList(serviceOffering.getHostTag());
            for (String tag : offeringTags) {
                if (StringUtils.isNotEmpty(tag) && resourceLimitTagsFromConfig.contains(tag)) {
                    tags.add(tag);
                }
            }
        }
        if (StringUtils.isNotEmpty(template.getTemplateTag())
                && resourceLimitTagsFromConfig.contains(template.getTemplateTag())
                && !tags.contains(template.getTemplateTag())) {
            tags.add(template.getTemplateTag());
        }
        return tags;
    }

    @Override
    public List<String> getResourceLimitStorageTags(DiskOffering diskOffering) {
        if (diskOffering == null || StringUtils.isEmpty(diskOffering.getTags())) {
            return new ArrayList<>();
        }
        List<String> resourceLimitTagsFromConfig = getResourceLimitStorageTags();
        if (CollectionUtils.isEmpty(resourceLimitTagsFromConfig)) {
            return new ArrayList<>();
        }
        String[] offeringTags = diskOffering.getTagsArray();
        List<String> tags = new ArrayList<>();
        for (String tag : offeringTags) {
            if (StringUtils.isNotEmpty(tag) && resourceLimitTagsFromConfig.contains(tag)) {
                tags.add(tag);
            }
        }
        return tags;
    }

    protected List<String> getResourceLimitStorageTagsForResourceCountOperation(Boolean display, DiskOffering diskOffering) {
        if (Boolean.FALSE.equals(display)) {
            return new ArrayList<>();
        }
        List<String> tags = getResourceLimitStorageTags(diskOffering);
        if (tags.isEmpty()) {
            tags.add(null);
        } else {
            tags.add(0, null);
        }
        return tags;
    }

    @Override
    public void checkVolumeResourceLimit(Account owner, Boolean display, Long size, DiskOffering diskOffering) throws ResourceAllocationException {
        List<String> tags = getResourceLimitStorageTagsForResourceCountOperation(display, diskOffering);
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        for (String tag : tags) {
            checkResourceLimitWithTag(owner, ResourceType.volume, tag);
            if (size != null) {
                checkResourceLimitWithTag(owner, ResourceType.primary_storage, tag, size);
            }
        }
    }

    @Override
    public void checkPrimaryStorageResourceLimit(Account owner, Boolean display, Long size, DiskOffering diskOffering) throws ResourceAllocationException {
        List<String> tags = getResourceLimitStorageTagsForResourceCountOperation(display, diskOffering);
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        if (size != null) {
            for (String tag : tags) {
                checkResourceLimitWithTag(owner, ResourceType.primary_storage, tag, size);
            }
        }
    }

    @Override
    public void checkVolumeResourceLimitForDiskOfferingChange(Account owner, Boolean display, Long currentSize, Long newSize,
            DiskOffering currentOffering, DiskOffering newOffering
    ) throws ResourceAllocationException {
        Ternary<Set<String>, Set<String>, Set<String>> updatedResourceLimitStorageTags = getResourceLimitStorageTagsForDiskOfferingChange(display, currentOffering, newOffering);
        if (updatedResourceLimitStorageTags == null) {
            return;
        }

        Set<String> sameTags = updatedResourceLimitStorageTags.first();
        Set<String> newTags = updatedResourceLimitStorageTags.second();

        if (newSize > currentSize) {
            for (String tag : sameTags) {
                checkResourceLimitWithTag(owner, ResourceType.primary_storage, tag, newSize - currentSize);
            }
        }

        for (String tag : newTags) {
            checkResourceLimitWithTag(owner, ResourceType.volume, tag, 1L);
            checkResourceLimitWithTag(owner, ResourceType.primary_storage, tag, newSize);
        }
    }

    @DB
    @Override
    public void incrementVolumeResourceCount(long accountId, Boolean display, Long size, DiskOffering diskOffering) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                List<String> tags = getResourceLimitStorageTagsForResourceCountOperation(display, diskOffering);
                if (CollectionUtils.isEmpty(tags)) {
                    return;
                }
                for (String tag : tags) {
                    incrementResourceCountWithTag(accountId, ResourceType.volume, tag);
                    if (size != null) {
                        incrementResourceCountWithTag(accountId, ResourceType.primary_storage, tag, size);
                    }
                }
            }
        });
    }

    @DB
    @Override
    public void decrementVolumeResourceCount(long accountId, Boolean display, Long size, DiskOffering diskOffering) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                List<String> tags = getResourceLimitStorageTagsForResourceCountOperation(display, diskOffering);
                if (CollectionUtils.isEmpty(tags)) {
                    return;
                }
                for (String tag : tags) {
                    decrementResourceCountWithTag(accountId, ResourceType.volume, tag);
                    if (size != null) {
                        decrementResourceCountWithTag(accountId, ResourceType.primary_storage, tag, size);
                    }
                }
            }
        });
    }

    @Override
    public void updateVmResourceCountForTemplateChange(long accountId, Boolean display, ServiceOffering offering,
            VirtualMachineTemplate currentTemplate, VirtualMachineTemplate newTemplate
    ) {
        updateVmResourceCountForServiceOfferingAndTemplateChange(accountId, display, null, null, null, null,
                offering, offering, currentTemplate, newTemplate);
    }

    @Override
    public void updateVmResourceCountForServiceOfferingChange(long accountId, Boolean display, Long currentCpu, Long newCpu,Long currentMemory, Long newMemory,
            ServiceOffering currentOffering, ServiceOffering newOffering, VirtualMachineTemplate template
    ) {
        updateVmResourceCountForServiceOfferingAndTemplateChange(accountId, display, currentCpu, newCpu, currentMemory, newMemory, currentOffering,
                newOffering != null ? newOffering : currentOffering, template, template);
    }

    private Ternary<Set<String>, Set<String>, Set<String>> getResourceLimitHostTagsForVmServiceOfferingAndTemplateChange(
            Boolean display, ServiceOffering currentOffering, ServiceOffering newOffering,
            VirtualMachineTemplate currentTemplate, VirtualMachineTemplate newTemplate
    ) {
        Set<String> currentOfferingTags = new HashSet<>(getResourceLimitHostTagsForResourceCountOperation(display, currentOffering, currentTemplate));
        if (currentOffering.getId() == newOffering.getId() && currentTemplate.getId() == newTemplate.getId()) {
            return new Ternary<>(currentOfferingTags, new HashSet<>(), new HashSet<>());
        }
        Set<String> newOfferingTags = new HashSet<>(getResourceLimitHostTagsForResourceCountOperation(display, newOffering, newTemplate));

        if (currentOfferingTags.isEmpty() && newOfferingTags.isEmpty()) {
            return null;
        }
        Set<String> sameTags = currentOfferingTags.stream().filter(newOfferingTags::contains).collect(Collectors.toSet());;
        Set<String> newTags = newOfferingTags.stream().filter(tag -> !currentOfferingTags.contains(tag)).collect(Collectors.toSet());
        Set<String> removedTags = currentOfferingTags.stream().filter(tag -> !newOfferingTags.contains(tag)).collect(Collectors.toSet());
        return new Ternary<>(sameTags, newTags, removedTags);
    }

    private void updateVmResourceCountForServiceOfferingAndTemplateChange(long accountId, Boolean display, Long currentCpu,
            Long newCpu, Long currentMemory, Long newMemory, ServiceOffering currentOffering, ServiceOffering newOffering,
            VirtualMachineTemplate currentTemplate, VirtualMachineTemplate newTemplate
    ) {
        Ternary<Set<String>, Set<String>, Set<String>> updatedResourceLimitHostTags = getResourceLimitHostTagsForVmServiceOfferingAndTemplateChange(display, currentOffering, newOffering, currentTemplate, newTemplate);
        if (updatedResourceLimitHostTags == null) {
            return;
        }
        if (currentCpu == null) {
            currentCpu = currentOffering.getCpu() != null ? Long.valueOf(currentOffering.getCpu()) : 0L;
        }
        if (newCpu == null) {
            newCpu = newOffering.getCpu() != null ? Long.valueOf(newOffering.getCpu()) : 0L;
        }
        if (currentMemory == null) {
            currentMemory = currentOffering.getRamSize() != null ? Long.valueOf(currentOffering.getRamSize()) : 0L;
        }
        if (newMemory == null) {
            newMemory = newOffering.getRamSize() != null ? Long.valueOf(newOffering.getRamSize()) : 0L;
        }

        Set<String> sameTags = updatedResourceLimitHostTags.first();
        Set<String> newTags = updatedResourceLimitHostTags.second();
        Set<String> removedTags = updatedResourceLimitHostTags.third();

        if (!newCpu.equals(currentCpu) || !newMemory.equals(currentMemory)) {
            for (String tag : sameTags) {
                if (newCpu - currentCpu > 0) {
                    incrementResourceCountWithTag(accountId, ResourceType.cpu, tag, newCpu - currentCpu);
                } else if (newCpu - currentCpu < 0) {
                    decrementResourceCountWithTag(accountId, ResourceType.cpu, tag, currentCpu - newCpu);
                }

                if (newMemory - currentMemory > 0) {
                    incrementResourceCountWithTag(accountId, ResourceType.memory, tag, newMemory - currentMemory);
                } else if (newMemory - currentMemory < 0) {
                    decrementResourceCountWithTag(accountId, ResourceType.memory, tag, currentMemory - newMemory);
                }
            }
        }

        for (String tag : removedTags) {
            decrementResourceCountWithTag(accountId, ResourceType.user_vm, tag, 1L);
            decrementResourceCountWithTag(accountId, ResourceType.cpu, tag, currentCpu);
            decrementResourceCountWithTag(accountId, ResourceType.memory, tag, currentMemory);
        }

        for (String tag : newTags) {
            incrementResourceCountWithTag(accountId, ResourceType.user_vm, tag, 1L);
            incrementResourceCountWithTag(accountId, ResourceType.cpu, tag, newCpu);
            incrementResourceCountWithTag(accountId, ResourceType.memory, tag, newMemory);
        }
    }

    private Ternary<Set<String>, Set<String>, Set<String>> getResourceLimitStorageTagsForDiskOfferingChange(
            Boolean display, DiskOffering currentOffering, DiskOffering newOffering
    ) {
        Set<String> currentOfferingTags = new HashSet<>(getResourceLimitStorageTagsForResourceCountOperation(display, currentOffering));
        if (newOffering == null || currentOffering.getId() == newOffering.getId()) {
            return new Ternary<>(currentOfferingTags, new HashSet<>(), new HashSet<>());
        }
        Set<String> newOfferingTags = new HashSet<>(getResourceLimitStorageTagsForResourceCountOperation(display, newOffering));
        if (currentOfferingTags.isEmpty() && newOfferingTags.isEmpty()) {
            return null;
        }
        Set<String> sameTags = currentOfferingTags.stream().filter(newOfferingTags::contains).collect(Collectors.toSet());;
        Set<String> newTags = newOfferingTags.stream().filter(tag -> !currentOfferingTags.contains(tag)).collect(Collectors.toSet());
        Set<String> removedTags = currentOfferingTags.stream().filter(tag -> !newOfferingTags.contains(tag)).collect(Collectors.toSet());
        return new Ternary<>(sameTags, newTags, removedTags);
    }

    @Override
    public void updateVolumeResourceCountForDiskOfferingChange(long accountId, Boolean display, Long currentSize, Long newSize,
            DiskOffering currentOffering, DiskOffering newOffering
    ) {
        Ternary<Set<String>, Set<String>, Set<String>> updatedResourceLimitStorageTags = getResourceLimitStorageTagsForDiskOfferingChange(display, currentOffering, newOffering);
        if (updatedResourceLimitStorageTags == null) {
            return;
        }
        Set<String> sameTags = updatedResourceLimitStorageTags.first();
        Set<String> newTags = updatedResourceLimitStorageTags.second();
        Set<String> removedTags = updatedResourceLimitStorageTags.third();

        if (!newSize.equals(currentSize)) {
            for (String tag : sameTags) {
                if (newSize - currentSize > 0) {
                    incrementResourceCountWithTag(accountId, ResourceType.primary_storage, tag, newSize - currentSize);
                } else if (newSize - currentSize < 0) {
                    decrementResourceCountWithTag(accountId, ResourceType.primary_storage, tag, currentSize - newSize);
                }
            }
        }
        for (String tag : removedTags) {
            decrementResourceCountWithTag(accountId, ResourceType.volume, tag, 1L);
            decrementResourceCountWithTag(accountId, ResourceType.primary_storage, tag, currentSize);
        }

        for (String tag : newTags) {
            incrementResourceCountWithTag(accountId, ResourceType.volume, tag, 1L);
            incrementResourceCountWithTag(accountId, ResourceType.primary_storage, tag, newSize);
        }
    }

    @Override
    public void incrementVolumePrimaryStorageResourceCount(long accountId, Boolean display, Long size, DiskOffering diskOffering) {
        if (size == null) {
            return;
        }
        List<String> tags = getResourceLimitStorageTagsForResourceCountOperation(display, diskOffering);
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        for (String tag : tags) {
            incrementResourceCountWithTag(accountId, ResourceType.primary_storage, tag, size);
        }
    }

    @Override
    public void decrementVolumePrimaryStorageResourceCount(long accountId, Boolean display, Long size, DiskOffering diskOffering) {
        if (size == null) {
            return;
        }
        List<String> tags = getResourceLimitStorageTagsForResourceCountOperation(display, diskOffering);
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        for (String tag : tags) {
            decrementResourceCountWithTag(accountId, ResourceType.primary_storage, tag, size);
        }
    }

    protected List<String> getResourceLimitHostTagsForResourceCountOperation(Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template) {
        if (Boolean.FALSE.equals(display)) {
            return new ArrayList<>();
        }
        List<String> tags = getResourceLimitHostTags(serviceOffering, template);
        if (tags.isEmpty()) {
            tags.add(null);
        } else {
            tags.add(0, null);
        }
        return tags;
    }

    @Override
    public void checkVmResourceLimit(Account owner, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template) throws ResourceAllocationException {
        List<String> tags = getResourceLimitHostTagsForResourceCountOperation(display, serviceOffering, template);
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        Long cpu = serviceOffering.getCpu() != null ? Long.valueOf(serviceOffering.getCpu()) : 0L;
        Long ram = serviceOffering.getRamSize() != null ? Long.valueOf(serviceOffering.getRamSize()) : 0L;
        for (String tag : tags) {
            checkResourceLimitWithTag(owner, ResourceType.user_vm, tag);
            checkResourceLimitWithTag(owner, ResourceType.cpu, tag, cpu);
            checkResourceLimitWithTag(owner, ResourceType.memory, tag, ram);
        }
    }

    @Override
    public void incrementVmResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                List<String> tags = getResourceLimitHostTagsForResourceCountOperation(display, serviceOffering, template);
                if (CollectionUtils.isEmpty(tags)) {
                    return;
                }
                Long cpu = serviceOffering.getCpu() != null ? Long.valueOf(serviceOffering.getCpu()) : 0L;
                Long ram = serviceOffering.getRamSize() != null ? Long.valueOf(serviceOffering.getRamSize()) : 0L;
                for (String tag : tags) {
                    incrementResourceCountWithTag(accountId, ResourceType.user_vm, tag);
                    incrementResourceCountWithTag(accountId, ResourceType.cpu, tag, cpu);
                    incrementResourceCountWithTag(accountId, ResourceType.memory, tag, ram);
                }
            }
        });
    }

    @Override
    public void decrementVmResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering,
            VirtualMachineTemplate template) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                List<String> tags = getResourceLimitHostTagsForResourceCountOperation(display, serviceOffering, template);
                if (CollectionUtils.isEmpty(tags)) {
                    return;
                }
                Long cpu = serviceOffering.getCpu() != null ? Long.valueOf(serviceOffering.getCpu()) : 0L;
                Long ram = serviceOffering.getRamSize() != null ? Long.valueOf(serviceOffering.getRamSize()) : 0L;
                for (String tag : tags) {
                    decrementResourceCountWithTag(accountId, ResourceType.user_vm, tag);
                    decrementResourceCountWithTag(accountId, ResourceType.cpu, tag, cpu);
                    decrementResourceCountWithTag(accountId, ResourceType.memory, tag, ram);
                }
            }
        });
    }

    @Override
    public void checkVmResourceLimitsForTemplateChange(Account owner, Boolean display, ServiceOffering offering,
            VirtualMachineTemplate currentTemplate, VirtualMachineTemplate newTemplate) throws ResourceAllocationException {
        checkVmResourceLimitsForServiceOfferingAndTemplateChange(owner, display, null, null,
                null, null, offering, offering, currentTemplate, newTemplate);
    }

    @Override
    public void checkVmResourceLimitsForServiceOfferingChange(Account owner, Boolean display, Long currentCpu, Long newCpu,
            Long currentMemory, Long newMemory,
            ServiceOffering currentOffering, ServiceOffering newOffering, VirtualMachineTemplate template
    ) throws ResourceAllocationException {
        checkVmResourceLimitsForServiceOfferingAndTemplateChange(owner, display, currentCpu, newCpu, currentMemory, newMemory, currentOffering,
                newOffering != null ? newOffering : currentOffering, template, template);
    }

    private void checkVmResourceLimitsForServiceOfferingAndTemplateChange(Account owner, Boolean display, Long currentCpu, Long newCpu,
            Long currentMemory, Long newMemory, ServiceOffering currentOffering, ServiceOffering newOffering,
            VirtualMachineTemplate currentTemplate, VirtualMachineTemplate newTemplate
    ) throws ResourceAllocationException {
        Ternary<Set<String>, Set<String>, Set<String>> updatedResourceLimitHostTags = getResourceLimitHostTagsForVmServiceOfferingAndTemplateChange(display, currentOffering, newOffering, currentTemplate, newTemplate);
        if (updatedResourceLimitHostTags == null) {
            return;
        }

        if (currentCpu == null) {
            currentCpu = currentOffering.getCpu() != null ? Long.valueOf(currentOffering.getCpu()) : 0L;
        }
        if (newCpu == null) {
            newCpu = newOffering.getCpu() != null ? Long.valueOf(newOffering.getCpu()) : 0L;
        }
        if (currentMemory == null) {
            currentMemory = currentOffering.getRamSize() != null ? Long.valueOf(currentOffering.getRamSize()) : 0L;
        }
        if (newMemory == null) {
            newMemory = newOffering.getRamSize() != null ? Long.valueOf(newOffering.getRamSize()) : 0L;
        }

        Set<String> sameTags = updatedResourceLimitHostTags.first();
        Set<String> newTags = updatedResourceLimitHostTags.second();

        if (newCpu - currentCpu > 0 || newMemory - currentMemory > 0) {
            for (String tag : sameTags) {
                if (newCpu - currentCpu > 0) {
                    checkResourceLimitWithTag(owner, ResourceType.cpu, tag, newCpu - currentCpu);
                }

                if (newMemory - currentMemory > 0) {
                    checkResourceLimitWithTag(owner, ResourceType.memory, tag, newMemory - currentMemory);
                }
            }
        }

        for (String tag : newTags) {
            checkResourceLimitWithTag(owner, ResourceType.user_vm, tag, 1L);
            checkResourceLimitWithTag(owner, ResourceType.cpu, tag, newCpu);
            checkResourceLimitWithTag(owner, ResourceType.memory, tag, newMemory);
        }
    }

    @Override
    public void checkVmCpuResourceLimit(Account owner, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long cpu) throws ResourceAllocationException {
        List<String> tags = getResourceLimitHostTagsForResourceCountOperation(display, serviceOffering, template);
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        if (cpu == null) {
            cpu = serviceOffering.getCpu() != null ? Long.valueOf(serviceOffering.getCpu()) : 0L;
        }
        for (String tag : tags) {
            checkResourceLimitWithTag(owner, ResourceType.cpu, tag, cpu);
        }
    }

    @Override
    public void incrementVmCpuResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long cpu) {
        List<String> tags = getResourceLimitHostTagsForResourceCountOperation(display, serviceOffering, template);
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        if (cpu == null) {
            cpu = serviceOffering.getCpu() != null ? Long.valueOf(serviceOffering.getCpu()) : 0L;
        }
        for (String tag : tags) {
            incrementResourceCountWithTag(accountId, ResourceType.cpu, tag, cpu);
        }
    }

    @Override
    public void decrementVmCpuResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long cpu) {
        List<String> tags = getResourceLimitHostTagsForResourceCountOperation(display, serviceOffering, template);
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        if (cpu == null) {
            cpu = serviceOffering.getCpu() != null ? Long.valueOf(serviceOffering.getCpu()) : 0L;
        }
        for (String tag : tags) {
            decrementResourceCountWithTag(accountId, ResourceType.cpu, tag, cpu);
        }
    }

    @Override
    public void checkVmMemoryResourceLimit(Account owner, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long memory) throws ResourceAllocationException {
        List<String> tags = getResourceLimitHostTagsForResourceCountOperation(display, serviceOffering, template);
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        if (memory == null) {
            memory = serviceOffering.getRamSize() != null ? Long.valueOf(serviceOffering.getRamSize()) : 0L;
        }
        for (String tag : tags) {
            checkResourceLimitWithTag(owner, ResourceType.memory, tag, memory);
        }
    }

    @Override
    public void incrementVmMemoryResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long memory) {
        List<String> tags = getResourceLimitHostTagsForResourceCountOperation(display, serviceOffering, template);
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        if (memory == null) {
            memory = serviceOffering.getRamSize() != null ? Long.valueOf(serviceOffering.getRamSize()) : 0L;
        }
        for (String tag : tags) {
            incrementResourceCountWithTag(accountId, ResourceType.memory, tag, memory);
        }
    }

    @Override
    public void decrementVmMemoryResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long memory) {
        List<String> tags = getResourceLimitHostTagsForResourceCountOperation(display, serviceOffering, template);
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        if (memory == null) {
            memory = serviceOffering.getRamSize() != null ? Long.valueOf(serviceOffering.getRamSize()) : 0L;
        }
        for (String tag : tags) {
            decrementResourceCountWithTag(accountId, ResourceType.memory, tag, memory);
        }
    }

    @Override
    public String getConfigComponentName() {
        return ResourceLimitManagerImpl.class.getName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                ResourceCountCheckInterval,
                ResourceReservationCleanupDelay,
                MaxAccountSecondaryStorage,
                MaxProjectSecondaryStorage,
                ResourceLimitHostTags,
                ResourceLimitStorageTags,
                DefaultMaxAccountProjects,
                DefaultMaxDomainProjects
        };
    }

    protected class ResourceCountCheckTask extends ManagedContextRunnable {
        public ResourceCountCheckTask() {

        }

        @Override
        protected void runInContext() {
            GlobalLock lock = GlobalLock.getInternLock("ResourceCheckTask");
            try {
                if (lock.lock(30)) {
                    try {
                        ManagementServerHostVO msHost = managementServerHostDao.findOneByLongestRuntime();
                        if (msHost == null || (msHost.getMsid() != ManagementServerNode.getManagementServerId())) {
                            logger.trace("Skipping the resource counters recalculation task on this management server");
                            return;
                        }
                        runResourceCheckTaskInternal();
                    } finally {
                        lock.unlock();
                    }
                }
            } finally {
                lock.releaseRef();
            }
        }

        private void runResourceCheckTaskInternal() {
            logger.info("Started resource counters recalculation periodic task.");
            List<DomainVO> domains;
            List<AccountVO> accounts;
            // try/catch task, otherwise it won't be rescheduled in case of exception
            try {
                domains = _domainDao.findImmediateChildrenForParent(Domain.ROOT_DOMAIN);
            } catch (Exception e) {
                logger.warn("Resource counters recalculation periodic task failed, unable to fetch immediate children for the domain " + Domain.ROOT_DOMAIN, e);
                // initialize domains as empty list to do best effort recalculation
                domains = new ArrayList<>();
            }
            // try/catch task, otherwise it won't be rescheduled in case of exception
            try {
                accounts = _accountDao.findActiveAccountsForDomain(Domain.ROOT_DOMAIN);
            } catch (Exception e) {
                logger.warn("Resource counters recalculation periodic task failed, unable to fetch active accounts for domain " + Domain.ROOT_DOMAIN, e);
                // initialize accounts as empty list to do best effort recalculation
                accounts = new ArrayList<>();
            }
            // try/catch task, otherwise it won't be rescheduled in case of exception
            try {
                removeResourceLimitAndCountForNonMatchingTags(null, null, getResourceLimitHostTags(), getResourceLimitStorageTags());
            } catch (Exception e) {
                logger.warn("Failure in resource counters recalculation periodic task, unable to clear undesired tagged limits and counts", e);
            }

            for (ResourceType type : ResourceType.values()) {
                if (CollectionUtils.isEmpty(domains)) {
                    recalculateDomainResourceCount(Domain.ROOT_DOMAIN, type, null);
                    recalculateDomainTaggedResourceCount(Domain.ROOT_DOMAIN, type, getResourceLimitHostTags(), getResourceLimitStorageTags());
                } else {
                    for (Domain domain : domains) {
                        recalculateDomainResourceCount(domain.getId(), type, null);
                        recalculateDomainTaggedResourceCount(domain.getId(), type, getResourceLimitHostTags(), getResourceLimitStorageTags());
                    }
                }
                // run through the accounts in the root domain
                for (AccountVO account : accounts) {
                    recalculateAccountResourceCount(account.getId(), type, null);
                    recalculateAccountTaggedResourceCount(account.getId(), type, getResourceLimitHostTags(), getResourceLimitStorageTags());
                }
            }
            logger.info("Finished resource counters recalculation periodic task.");
        }
    }
}
