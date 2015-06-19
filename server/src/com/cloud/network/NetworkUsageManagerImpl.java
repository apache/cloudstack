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
package com.cloud.network;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.command.admin.usage.AddTrafficMonitorCmd;
import org.apache.cloudstack.api.command.admin.usage.DeleteTrafficMonitorCmd;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficMonitorsCmd;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.DirectNetworkUsageAnswer;
import com.cloud.agent.api.DirectNetworkUsageCommand;
import com.cloud.agent.api.RecurringNetworkUsageCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupTrafficMonitorCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.gpu.dao.HostGpuGroupsDao;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.resource.TrafficSentinelResource;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.usage.UsageIPAddressVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.MacAddress;

@Component
@Local(value = {NetworkUsageManager.class})
public class NetworkUsageManagerImpl extends ManagerBase implements NetworkUsageService, NetworkUsageManager, ResourceStateAdapter {
    public enum NetworkUsageResourceName {
        TrafficSentinel;
    }

    private static final org.apache.log4j.Logger s_logger = Logger.getLogger(NetworkUsageManagerImpl.class);
    @Inject
    HostDao _hostDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    UserStatisticsDao _statsDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    UsageEventDao _eventDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    HostDetailsDao _detailsDao;
    @Inject
    HostGpuGroupsDao _hostGpuGroupsDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkDao _networksDao = null;
    @Inject
    ResourceManager _resourceMgr;
    ScheduledExecutorService _executor;
    int _networkStatsInterval;
    String _TSinclZones;
    String _TSexclZones;
    protected SearchBuilder<IPAddressVO> AllocatedIpSearch;

    @Override
    public Host addTrafficMonitor(AddTrafficMonitorCmd cmd) {

        long zoneId = cmd.getZoneId();

        DataCenterVO zone = _dcDao.findById(zoneId);
        String zoneName;
        if (zone == null) {
            throw new InvalidParameterValueException("Could not find zone with ID: " + zoneId);
        } else {
            zoneName = zone.getName();
        }

        List<HostVO> trafficMonitorsInZone = _resourceMgr.listAllHostsInOneZoneByType(Host.Type.TrafficMonitor, zoneId);
        if (trafficMonitorsInZone.size() != 0) {
            throw new InvalidParameterValueException("Already added an traffic monitor in zone: " + zoneName);
        }

        URI uri;
        try {
            uri = new URI(cmd.getUrl());
        } catch (Exception e) {
            s_logger.debug(e);
            throw new InvalidParameterValueException(e.getMessage());
        }

        String ipAddress = uri.getHost();
        //String numRetries = params.get("numretries");
        //String timeout = params.get("timeout");

        TrafficSentinelResource resource = new TrafficSentinelResource();
        String guid = getTrafficMonitorGuid(zoneId, NetworkUsageResourceName.TrafficSentinel, ipAddress);

        Map<String, Object> hostParams = new HashMap<String, Object>();
        hostParams.put("zone", String.valueOf(zoneId));
        hostParams.put("ipaddress", ipAddress);
        hostParams.put("url", cmd.getUrl());
        hostParams.put("inclZones", (cmd.getInclZones() != null) ? cmd.getInclZones() : _TSinclZones);
        hostParams.put("exclZones", (cmd.getExclZones() != null) ? cmd.getExclZones() : _TSexclZones);
        hostParams.put("guid", guid);
        hostParams.put("name", guid);

        try {
            resource.configure(guid, hostParams);
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        Map<String, String> hostDetails = new HashMap<String, String>();
        hostDetails.put("url", cmd.getUrl());
        hostDetails.put("last_collection", "" + System.currentTimeMillis());
        if (cmd.getInclZones() != null) {
            hostDetails.put("inclZones", cmd.getInclZones());
        }
        if (cmd.getExclZones() != null) {
            hostDetails.put("exclZones", cmd.getExclZones());
        }

        Host trafficMonitor = _resourceMgr.addHost(zoneId, resource, Host.Type.TrafficMonitor, hostDetails);
        return trafficMonitor;
    }

    public String getTrafficMonitorGuid(long zoneId, NetworkUsageResourceName name, String ip) {
        return zoneId + "-" + name + "-" + ip;
    }

    @Override
    public boolean deleteTrafficMonitor(DeleteTrafficMonitorCmd cmd) {
        long hostId = cmd.getId();
        HostVO trafficMonitor = _hostDao.findById(hostId);
        if (trafficMonitor == null) {
            throw new InvalidParameterValueException("Could not find an traffic monitor with ID: " + hostId);
        }

        if (_resourceMgr.deleteHost(hostId, false, false)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<HostVO> listTrafficMonitors(ListTrafficMonitorsCmd cmd) {
        long zoneId = cmd.getZoneId();
        return _resourceMgr.listAllHostsInOneZoneByType(Host.Type.TrafficMonitor, zoneId);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        AllocatedIpSearch = _ipAddressDao.createSearchBuilder();
        AllocatedIpSearch.and("allocated", AllocatedIpSearch.entity().getAllocatedTime(), Op.NNULL);
        AllocatedIpSearch.and("dc", AllocatedIpSearch.entity().getDataCenterId(), Op.EQ);
        SearchBuilder<NetworkVO> networkJoin = _networksDao.createSearchBuilder();
        networkJoin.and("guestType", networkJoin.entity().getGuestType(), Op.EQ);
        AllocatedIpSearch.join("network", networkJoin, AllocatedIpSearch.entity().getSourceNetworkId(), networkJoin.entity().getId(), JoinBuilder.JoinType.INNER);
        AllocatedIpSearch.done();

        _networkStatsInterval = NumbersUtil.parseInt(_configDao.getValue(Config.DirectNetworkStatsInterval.key()), 86400);
        _TSinclZones = _configDao.getValue(Config.TrafficSentinelIncludeZones.key());
        _TSexclZones = _configDao.getValue(Config.TrafficSentinelExcludeZones.key());
        _agentMgr.registerForHostEvents(new DirectNetworkStatsListener(_networkStatsInterval), true, false, false);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        _resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
        return true;
    }

    @Override
    public List<IPAddressVO> listAllocatedDirectIps(long zoneId) {
        SearchCriteria<IPAddressVO> sc = AllocatedIpSearch.create();
        sc.setParameters("dc", zoneId);
        sc.setJoinParameters("network", "guestType", Network.GuestType.Shared);
        return _ipAddressDao.search(sc, null);
    }

    protected class DirectNetworkStatsListener implements Listener {

        private int _interval;

        private final long mgmtSrvrId = MacAddress.getMacAddress().toLong();

        protected DirectNetworkStatsListener(int interval) {
            _interval = interval;
        }

        @Override
        public boolean isRecurring() {
            return true;
        }

        @Override
        @DB
        public boolean processAnswers(long agentId, long seq, Answer[] answers) {
            /*
             * Do not collect Direct Network usage stats if the Traffic Monitor is not owned by this mgmt server
             */
            HostVO host = _hostDao.findById(agentId);
            if (host != null) {
                if ((host.getManagementServerId() == null) || (mgmtSrvrId != host.getManagementServerId())) {
                    s_logger.warn("Not the owner. Not collecting Direct Network usage from  TrafficMonitor : " + agentId);
                    return false;
                }
            } else {
                s_logger.warn("Agent not found. Not collecting Direct Network usage from  TrafficMonitor : " + agentId);
                return false;
            }

            GlobalLock scanLock = GlobalLock.getInternLock("direct.network.usage.collect" + host.getDataCenterId());
            try {
                if (scanLock.lock(10)) {
                    try {
                        return collectDirectNetworkUsage(host);
                    } finally {
                        scanLock.unlock();
                    }
                }
            } finally {
                scanLock.releaseRef();
            }
            return false;
        }

        private boolean collectDirectNetworkUsage(final HostVO host) {
            s_logger.debug("Direct Network Usage stats collector is running...");

            final long zoneId = host.getDataCenterId();
            final DetailVO lastCollectDetail = _detailsDao.findDetail(host.getId(), "last_collection");
            if (lastCollectDetail == null) {
                s_logger.warn("Last collection time not available. Skipping direct usage collection for Traffic Monitor: " + host.getId());
                return false;
            }
            Date lastCollection = new Date(Long.parseLong(lastCollectDetail.getValue()));

            //Get list of IPs currently allocated
            List<IPAddressVO> allocatedIps = listAllocatedDirectIps(zoneId);
            Calendar rightNow = Calendar.getInstance();

            // Allow 2 hours for traffic sentinel to populate historical traffic
            // This coule be made configurable

            rightNow.add(Calendar.HOUR_OF_DAY, -2);
            final Date now = rightNow.getTime();

            if (lastCollection.after(now)) {
                s_logger.debug("Current time is less than 2 hours after last collection time : " + lastCollection.toString() +
                    ". Skipping direct network usage collection");
                return false;
            }

            //Get IP Assign/Release events from lastCollection time till now
            List<UsageEventVO> IpEvents = _eventDao.listDirectIpEvents(lastCollection, now, zoneId);

            Map<String, Date> ipAssigment = new HashMap<String, Date>();
            List<UsageIPAddressVO> IpPartialUsage = new ArrayList<UsageIPAddressVO>(); //Ips which were allocated only for the part of collection duration
            List<UsageIPAddressVO> fullDurationIpUsage = new ArrayList<UsageIPAddressVO>(); //Ips which were allocated only for the entire collection duration

            // Use UsageEvents to track the IP assignment
            // Add them to IpUsage list with account_id , ip_address, alloc_date, release_date

            for (UsageEventVO IpEvent : IpEvents) {
                String address = IpEvent.getResourceName();
                if (EventTypes.EVENT_NET_IP_ASSIGN.equals(IpEvent.getType())) {
                    ipAssigment.put(address, IpEvent.getCreateDate());
                } else if (EventTypes.EVENT_NET_IP_RELEASE.equals(IpEvent.getType())) {
                    if (ipAssigment.containsKey(address)) {
                        Date assigned = ipAssigment.get(address);
                        ipAssigment.remove(address);
                        IpPartialUsage.add(new UsageIPAddressVO(IpEvent.getAccountId(), address, assigned, IpEvent.getCreateDate()));
                    } else {
                        // Ip was assigned prior to lastCollection Date
                        IpPartialUsage.add(new UsageIPAddressVO(IpEvent.getAccountId(), address, lastCollection, IpEvent.getCreateDate()));
                    }
                }
            }

            List<String> IpList = new ArrayList<String>();
            for (IPAddressVO ip : allocatedIps) {
                if (ip.getAllocatedToAccountId() == Account.ACCOUNT_ID_SYSTEM) {
                    //Ignore usage for system account
                    continue;
                }
                String address = (ip.getAddress()).toString();
                if (ipAssigment.containsKey(address)) {
                    // Ip was assigned during the current period but not release till Date now
                    IpPartialUsage.add(new UsageIPAddressVO(ip.getAllocatedToAccountId(), address, ipAssigment.get(address), now));
                } else {
                    // Ip was not assigned or released during current period. Consider entire duration for usage calculation (lastCollection to now)
                    fullDurationIpUsage.add(new UsageIPAddressVO(ip.getAllocatedToAccountId(), address, lastCollection, now));
                    //Store just the Ips to send the list as part of DirectNetworkUsageCommand
                    IpList.add(address);
                }

            }

            final List<UserStatisticsVO> collectedStats = new ArrayList<UserStatisticsVO>();

            //Get usage for Ips which were assigned for the entire duration
            if (fullDurationIpUsage.size() > 0) {
                DirectNetworkUsageCommand cmd = new DirectNetworkUsageCommand(IpList, lastCollection, now, _TSinclZones, _TSexclZones);
                DirectNetworkUsageAnswer answer = (DirectNetworkUsageAnswer)_agentMgr.easySend(host.getId(), cmd);
                if (answer == null || !answer.getResult()) {
                    String details = (answer != null) ? answer.getDetails() : "details unavailable";
                    String msg = "Unable to get network usage stats from " + host.getId() + " due to: " + details + ".";
                    s_logger.error(msg);
                    return false;
                } else {
                    for (UsageIPAddressVO usageIp : fullDurationIpUsage) {
                        String publicIp = usageIp.getAddress();
                        long[] bytesSentRcvd = answer.get(publicIp);
                        Long bytesSent = bytesSentRcvd[0];
                        Long bytesRcvd = bytesSentRcvd[1];
                        if (bytesSent == null || bytesRcvd == null) {
                            s_logger.debug("Incorrect bytes for IP: " + publicIp);
                            continue;
                        }
                        if (bytesSent == 0L && bytesRcvd == 0L) {
                            s_logger.trace("Ignore zero bytes for IP: " + publicIp);
                            continue;
                        }
                        UserStatisticsVO stats = new UserStatisticsVO(usageIp.getAccountId(), zoneId, null, null, null, null);
                        stats.setCurrentBytesSent(bytesSent);
                        stats.setCurrentBytesReceived(bytesRcvd);
                        collectedStats.add(stats);
                    }
                }
            }

            //Get usage for Ips which were assigned for part of the duration period
            for (UsageIPAddressVO usageIp : IpPartialUsage) {
                IpList = new ArrayList<String>();
                IpList.add(usageIp.getAddress());
                DirectNetworkUsageCommand cmd = new DirectNetworkUsageCommand(IpList, usageIp.getAssigned(), usageIp.getReleased(), _TSinclZones, _TSexclZones);
                DirectNetworkUsageAnswer answer = (DirectNetworkUsageAnswer)_agentMgr.easySend(host.getId(), cmd);
                if (answer == null || !answer.getResult()) {
                    String details = (answer != null) ? answer.getDetails() : "details unavailable";
                    String msg = "Unable to get network usage stats from " + host.getId() + " due to: " + details + ".";
                    s_logger.error(msg);
                    return false;
                } else {
                    String publicIp = usageIp.getAddress();
                    long[] bytesSentRcvd = answer.get(publicIp);
                    Long bytesSent = bytesSentRcvd[0];
                    Long bytesRcvd = bytesSentRcvd[1];
                    if (bytesSent == null || bytesRcvd == null) {
                        s_logger.debug("Incorrect bytes for IP: " + publicIp);
                        continue;
                    }
                    if (bytesSent == 0L && bytesRcvd == 0L) {
                        s_logger.trace("Ignore zero bytes for IP: " + publicIp);
                        continue;
                    }
                    UserStatisticsVO stats = new UserStatisticsVO(usageIp.getAccountId(), zoneId, null, null, null, null);
                    stats.setCurrentBytesSent(bytesSent);
                    stats.setCurrentBytesReceived(bytesRcvd);
                    collectedStats.add(stats);

                }
            }

            if (collectedStats.size() == 0) {
                s_logger.debug("No new direct network stats. No need to persist");
                return false;
            }
            //Persist all the stats and last_collection time in a single transaction
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    for (UserStatisticsVO stat : collectedStats) {
                        UserStatisticsVO stats = _statsDao.lock(stat.getAccountId(), stat.getDataCenterId(), 0L, null, host.getId(), "DirectNetwork");
                        if (stats == null) {
                            stats = new UserStatisticsVO(stat.getAccountId(), zoneId, null, host.getId(), "DirectNetwork", 0L);
                            stats.setCurrentBytesSent(stat.getCurrentBytesSent());
                            stats.setCurrentBytesReceived(stat.getCurrentBytesReceived());
                            _statsDao.persist(stats);
                        } else {
                            stats.setCurrentBytesSent(stats.getCurrentBytesSent() + stat.getCurrentBytesSent());
                            stats.setCurrentBytesReceived(stats.getCurrentBytesReceived() + stat.getCurrentBytesReceived());
                            _statsDao.update(stats.getId(), stats);
                        }
                    }
                    lastCollectDetail.setValue("" + now.getTime());
                    _detailsDao.update(lastCollectDetail.getId(), lastCollectDetail);
                }
            });

            return true;
        }

        @Override
        public boolean processCommands(long agentId, long seq, Command[] commands) {
            return false;
        }

        @Override
        public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
            return null;
        }

        @Override
        public boolean processDisconnect(long agentId, Status state) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Disconnected called on " + agentId + " with status " + state.toString());
            }
            return true;
        }

        @Override
        public void processConnect(Host agent, StartupCommand cmd, boolean forRebalance) {
            if (cmd instanceof StartupTrafficMonitorCommand) {
                long agentId = agent.getId();
                s_logger.debug("Sending RecurringNetworkUsageCommand to " + agentId);
                RecurringNetworkUsageCommand watch = new RecurringNetworkUsageCommand(_interval);
                try {
                    _agentMgr.send(agentId, new Commands(watch), this);
                } catch (AgentUnavailableException e) {
                    s_logger.debug("Can not process connect for host " + agentId, e);
                }
            }
            return;
        }

        @Override
        public boolean processTimeout(long agentId, long seq) {
            return true;
        }

        @Override
        public int getTimeout() {
            return -1;
        }

        protected DirectNetworkStatsListener() {
        }

    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupTrafficMonitorCommand)) {
            return null;
        }

        host.setType(Host.Type.TrafficMonitor);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (host.getType() != Host.Type.TrafficMonitor) {
            return null;
        }

        long hostId = host.getId();
        _agentMgr.disconnectWithoutInvestigation(hostId, Status.Event.Remove);
        _detailsDao.deleteDetails(hostId);
        _hostGpuGroupsDao.deleteGpuEntries(hostId);
        host.setGuid(null);
        _hostDao.update(hostId, host);
        _hostDao.remove(hostId);
        return new DeleteHostAnswer(false);

    }

}
