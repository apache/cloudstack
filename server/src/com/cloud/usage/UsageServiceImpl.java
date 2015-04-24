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
package com.cloud.usage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.VpnUserVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.command.admin.usage.GenerateUsageRecordsCmd;
import org.apache.cloudstack.api.command.admin.usage.GetUsageRecordsCmd;
import org.apache.cloudstack.api.response.UsageTypeResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.usage.Usage;
import org.apache.cloudstack.usage.UsageService;
import org.apache.cloudstack.usage.UsageTypes;

import com.cloud.configuration.Config;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageJobDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = {UsageService.class})
public class UsageServiceImpl extends ManagerBase implements UsageService, Manager {
    public static final Logger s_logger = Logger.getLogger(UsageServiceImpl.class);

    //ToDo: Move implementation to ManagaerImpl

    @Inject
    private AccountDao _accountDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private UsageDao _usageDao;
    @Inject
    private UsageJobDao _usageJobDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ProjectManager _projectMgr;
    private TimeZone _usageTimezone;
    @Inject
    private AccountService _accountService;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private SnapshotDao _snapshotDao;
    @Inject
    private SecurityGroupDao _sgDao;
    @Inject
    private VpnUserDao _vpnUserDao;
    @Inject
    private PortForwardingRulesDao _pfDao;
    @Inject
    private LoadBalancerDao _lbDao;
    @Inject
    private VMTemplateDao _vmTemplateDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private IPAddressDao _ipDao;
    @Inject
    private HostDao _hostDao;

    public UsageServiceImpl() {
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        String timeZoneStr = _configDao.getValue(Config.UsageAggregationTimezone.toString());
        if (timeZoneStr == null) {
           timeZoneStr = "GMT";
        }
        _usageTimezone = TimeZone.getTimeZone(timeZoneStr);
        return true;
    }

    @Override
    public boolean generateUsageRecords(GenerateUsageRecordsCmd cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            UsageJobVO immediateJob = _usageJobDao.getNextImmediateJob();
            if (immediateJob == null) {
                UsageJobVO job = _usageJobDao.getLastJob();

                String host = null;
                int pid = 0;
                if (job != null) {
                    host = job.getHost();
                    pid = ((job.getPid() == null) ? 0 : job.getPid().intValue());
                }
                _usageJobDao.createNewJob(host, pid, UsageJobVO.JOB_TYPE_SINGLE);
            }
        } finally {
            txn.close();

            // switch back to VMOPS_DB
            TransactionLegacy swap = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            swap.close();
        }
        return true;
    }

    @Override
    public Pair<List<? extends Usage>, Integer> getUsageRecords(GetUsageRecordsCmd cmd) {
        Long accountId = cmd.getAccountId();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Account userAccount = null;
        Account caller = CallContext.current().getCallingAccount();
        Long usageType = cmd.getUsageType();
        Long projectId = cmd.getProjectId();
        String usageId = cmd.getUsageId();

        if (projectId != null) {
            if (accountId != null) {
                throw new InvalidParameterValueException("Projectid and accountId can't be specified together");
            }
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }
            accountId = project.getProjectAccountId();
        }

        //if accountId is not specified, use accountName and domainId
        if ((accountId == null) && (accountName != null) && (domainId != null)) {
            if (_domainDao.isChildDomain(caller.getDomainId(), domainId)) {
                Filter filter = new Filter(AccountVO.class, "id", Boolean.FALSE, null, null);
                List<AccountVO> accounts = _accountDao.listAccounts(accountName, domainId, filter);
                if (accounts.size() > 0) {
                    userAccount = accounts.get(0);
                }
                if (userAccount != null) {
                    accountId = userAccount.getId();
                } else {
                    throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                }
            } else {
                throw new PermissionDeniedException("Invalid Domain Id or Account");
            }
        }

        boolean isAdmin = false;
        boolean isDomainAdmin = false;

        //If accountId couldn't be found using accountName and domainId, get it from userContext
        if (accountId == null) {
            accountId = caller.getId();
            //List records for all the accounts if the caller account is of type admin.
            //If account_id or account_name is explicitly mentioned, list records for the specified account only even if the caller is of type admin
            if (_accountService.isRootAdmin(caller.getId())) {
                isAdmin = true;
            } else if (_accountService.isDomainAdmin(caller.getId())) {
                isDomainAdmin = true;
            }
            s_logger.debug("Account details not available. Using userContext accountId: " + accountId);
        }

        Date startDate = cmd.getStartDate();
        Date endDate = cmd.getEndDate();
        if (startDate.after(endDate)) {
            throw new InvalidParameterValueException("Incorrect Date Range. Start date: " + startDate + " is after end date:" + endDate);
        }
        TimeZone usageTZ = getUsageTimezone();
        Date adjustedStartDate = computeAdjustedTime(startDate, usageTZ);
        Date adjustedEndDate = computeAdjustedTime(endDate, usageTZ);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("getting usage records for account: " + accountId + ", domainId: " + domainId + ", between " + adjustedStartDate + " and " + adjustedEndDate +
                ", using pageSize: " + cmd.getPageSizeVal() + " and startIndex: " + cmd.getStartIndex());
        }

        Filter usageFilter = new Filter(UsageVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        SearchCriteria<UsageVO> sc = _usageDao.createSearchCriteria();

        if (accountId != -1 && accountId != Account.ACCOUNT_ID_SYSTEM && !isAdmin && !isDomainAdmin) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        }

        if (isDomainAdmin) {
            SearchCriteria<DomainVO> sdc = _domainDao.createSearchCriteria();
            sdc.addOr("path", SearchCriteria.Op.LIKE, _domainDao.findById(caller.getDomainId()).getPath() + "%");
            List<DomainVO> domains = _domainDao.search(sdc, null);
            List<Long> domainIds = new ArrayList<Long>();
            for (DomainVO domain : domains)
                domainIds.add(domain.getId());
            sc.addAnd("domainId", SearchCriteria.Op.IN, domainIds.toArray());
        }

        if (domainId != null) {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        }

        if (usageType != null) {
            sc.addAnd("usageType", SearchCriteria.Op.EQ, usageType);
        }

        if (usageId != null) {
            if (usageType == null) {
                throw new InvalidParameterValueException("Usageid must be specified together with usageType");
            }

            Long usageDbId = null;

            switch (usageType.intValue()) {
                case UsageTypes.NETWORK_BYTES_RECEIVED:
                case UsageTypes.NETWORK_BYTES_SENT:
                case UsageTypes.RUNNING_VM:
                case UsageTypes.ALLOCATED_VM:
                case UsageTypes.VM_SNAPSHOT:
                    VMInstanceVO vm = _vmDao.findByUuidIncludingRemoved(usageId);
                    if (vm != null) {
                        usageDbId = vm.getId();
                    }

                    if (vm == null && (usageType == UsageTypes.NETWORK_BYTES_RECEIVED || usageType == UsageTypes.NETWORK_BYTES_SENT)) {
                        HostVO host = _hostDao.findByUuidIncludingRemoved(usageId);
                        if (host != null) {
                            usageDbId = host.getId();
                        }
                    }
                    break;
                case UsageTypes.SNAPSHOT:
                    SnapshotVO snap = _snapshotDao.findByUuidIncludingRemoved(usageId);
                    if (snap != null) {
                        usageDbId = snap.getId();
                    }
                    break;
                case UsageTypes.TEMPLATE:
                case UsageTypes.ISO:
                    VMTemplateVO tmpl = _vmTemplateDao.findByUuidIncludingRemoved(usageId);
                    if (tmpl != null) {
                        usageDbId = tmpl.getId();
                    }
                    break;
                case UsageTypes.LOAD_BALANCER_POLICY:
                    LoadBalancerVO lb = _lbDao.findByUuidIncludingRemoved(usageId);
                    if (lb != null) {
                        usageDbId = lb.getId();
                    }
                    break;
                case UsageTypes.PORT_FORWARDING_RULE:
                    PortForwardingRuleVO pf = _pfDao.findByUuidIncludingRemoved(usageId);
                    if (pf != null) {
                        usageDbId = pf.getId();
                    }
                    break;
                case UsageTypes.VOLUME:
                case UsageTypes.VM_DISK_IO_READ:
                case UsageTypes.VM_DISK_IO_WRITE:
                case UsageTypes.VM_DISK_BYTES_READ:
                case UsageTypes.VM_DISK_BYTES_WRITE:
                    VolumeVO volume = _volumeDao.findByUuidIncludingRemoved(usageId);
                    if (volume != null) {
                        usageDbId = volume.getId();
                    }
                    break;
                case UsageTypes.VPN_USERS:
                    VpnUserVO vpnUser = _vpnUserDao.findByUuidIncludingRemoved(usageId);
                    if (vpnUser != null) {
                        usageDbId = vpnUser.getId();
                    }
                    break;
                case UsageTypes.SECURITY_GROUP:
                    SecurityGroupVO sg = _sgDao.findByUuidIncludingRemoved(usageId);
                    if (sg != null) {
                        usageDbId = sg.getId();
                    }
                    break;
                case UsageTypes.IP_ADDRESS:
                    IPAddressVO ip = _ipDao.findByUuidIncludingRemoved(usageId);
                    if (ip != null) {
                        usageDbId = ip.getId();
                    }
                    break;
                default:
                    break;
            }

            if (usageDbId != null) {
                sc.addAnd("usageId", SearchCriteria.Op.EQ, usageDbId);
            } else {
                // return an empty list if usageId was not found
                return new Pair<List<? extends Usage>, Integer>(new ArrayList<Usage>(), new Integer(0));
            }
        }

        if ((adjustedStartDate != null) && (adjustedEndDate != null) && adjustedStartDate.before(adjustedEndDate)) {
            sc.addAnd("startDate", SearchCriteria.Op.BETWEEN, adjustedStartDate, adjustedEndDate);
            sc.addAnd("endDate", SearchCriteria.Op.BETWEEN, adjustedStartDate, adjustedEndDate);
        } else {
            return new Pair<List<? extends Usage>, Integer>(new ArrayList<Usage>(), new Integer(0)); // return an empty list if we fail to validate the dates
        }

        Pair<List<UsageVO>, Integer> usageRecords = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            usageRecords = _usageDao.searchAndCountAllRecords(sc, usageFilter);
        } finally {
            txn.close();

            // switch back to VMOPS_DB
            TransactionLegacy swap = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            swap.close();
        }

        return new Pair<List<? extends Usage>, Integer>(usageRecords.first(), usageRecords.second());
    }

    @Override
    public TimeZone getUsageTimezone() {
        return _usageTimezone;
    }

    private Date computeAdjustedTime(Date initialDate, TimeZone targetTZ) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(initialDate);
        TimeZone localTZ = cal.getTimeZone();
        int timezoneOffset = cal.get(Calendar.ZONE_OFFSET);
        if (localTZ.inDaylightTime(initialDate)) {
            timezoneOffset += (60 * 60 * 1000);
        }
        cal.add(Calendar.MILLISECOND, timezoneOffset);

        Date newTime = cal.getTime();

        Calendar calTS = Calendar.getInstance(targetTZ);
        calTS.setTime(newTime);
        timezoneOffset = calTS.get(Calendar.ZONE_OFFSET);
        if (targetTZ.inDaylightTime(initialDate)) {
            timezoneOffset += (60 * 60 * 1000);
        }

        calTS.add(Calendar.MILLISECOND, -1 * timezoneOffset);

        return calTS.getTime();
    }

    @Override
    public List<UsageTypeResponse> listUsageTypes() {
        return UsageTypes.listUsageTypes();
    }

}
