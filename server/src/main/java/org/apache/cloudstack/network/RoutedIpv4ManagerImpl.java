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

package org.apache.cloudstack.network;

import com.cloud.api.ApiDBUtils;
import com.cloud.bgp.BGPService;
import com.cloud.dc.DataCenter;
import com.cloud.domain.Domain;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.projects.Project;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

import org.apache.cloudstack.api.command.admin.network.CreateIpv4SubnetForZoneCmd;
import org.apache.cloudstack.api.command.admin.network.CreateIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.DedicateIpv4SubnetForZoneCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4SubnetForZoneCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.ListIpv4SubnetsForZoneCmd;
import org.apache.cloudstack.api.command.admin.network.ListIpv4SubnetsForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.ReleaseDedicatedIpv4SubnetForZoneCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateIpv4SubnetForZoneCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.ChangeBgpPeersForNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.ChangeBgpPeersForVpcCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.CreateBgpPeerCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.DedicateBgpPeerCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.DeleteBgpPeerCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.ListBgpPeersCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.ReleaseDedicatedBgpPeerCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.UpdateBgpPeerCmd;
import org.apache.cloudstack.api.command.user.network.routing.CreateRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.network.routing.DeleteRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.network.routing.ListRoutingFirewallRulesCmd;
import org.apache.cloudstack.api.command.user.network.routing.UpdateRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.response.BgpPeerResponse;
import org.apache.cloudstack.api.response.DataCenterIpv4SubnetResponse;
import org.apache.cloudstack.api.response.Ipv4SubnetForGuestNetworkResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnet;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnetVO;
import org.apache.cloudstack.datacenter.dao.DataCenterIpv4GuestSubnetDao;
import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMap.State;
import org.apache.cloudstack.network.dao.BgpPeerDao;
import org.apache.cloudstack.network.dao.BgpPeerDetailsDao;
import org.apache.cloudstack.network.dao.BgpPeerNetworkMapDao;
import org.apache.cloudstack.network.dao.Ipv4GuestSubnetNetworkMapDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class RoutedIpv4ManagerImpl extends ComponentLifecycleBase implements RoutedIpv4Manager {

    @Inject
    DataCenterIpv4GuestSubnetDao dataCenterIpv4GuestSubnetDao;
    @Inject
    Ipv4GuestSubnetNetworkMapDao ipv4GuestSubnetNetworkMapDao;
    @Inject
    FirewallService firewallService;
    @Inject
    FirewallManager firewallManager;
    @Inject
    FirewallRulesDao firewallDao;
    @Inject
    NetworkServiceMapDao networkServiceMapDao;
    @Inject
    NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Inject
    NetworkOfferingDao networkOfferingDao;
    @Inject
    NetworkModel networkModel;
    @Inject
    AccountManager accountManager;
    @Inject
    VpcOfferingDao vpcOfferingDao;
    @Inject
    VpcOfferingServiceMapDao vpcOfferingServiceMapDao;
    @Inject
    VpcDao vpcDao;
    @Inject
    BgpPeerDao bgpPeerDao;
    @Inject
    BgpPeerDetailsDao bgpPeerDetailsDao;
    @Inject
    BgpPeerNetworkMapDao bgpPeerNetworkMapDao;
    @Inject
    NetworkDao networkDao;
    @Inject
    BGPService bgpService;

    @Override
    public String getConfigComponentName() {
        return RoutedIpv4Manager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {
                RoutedNetworkIPv4MaxCidrSize, RoutedNetworkIPv4MinCidrSize, RoutedIPv4NetworkCidrAutoAllocationEnabled,
                RoutedVpcIPv4MaxCidrSize, RoutedVpcIPv4MinCidrSize, UseSystemBgpPeers
        };
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateIpv4SubnetForZoneCmd.class);
        cmdList.add(DeleteIpv4SubnetForZoneCmd.class);
        cmdList.add(ListIpv4SubnetsForZoneCmd.class);
        cmdList.add(UpdateIpv4SubnetForZoneCmd.class);
        cmdList.add(DedicateIpv4SubnetForZoneCmd.class);
        cmdList.add(ReleaseDedicatedIpv4SubnetForZoneCmd.class);
        cmdList.add(CreateIpv4SubnetForGuestNetworkCmd.class);
        cmdList.add(ListIpv4SubnetsForGuestNetworkCmd.class);
        cmdList.add(DeleteIpv4SubnetForGuestNetworkCmd.class);
        cmdList.add(CreateRoutingFirewallRuleCmd.class);
        cmdList.add(ListRoutingFirewallRulesCmd.class);
        cmdList.add(UpdateRoutingFirewallRuleCmd.class);
        cmdList.add(DeleteRoutingFirewallRuleCmd.class);
        cmdList.add(CreateBgpPeerCmd.class);
        cmdList.add(DeleteBgpPeerCmd.class);
        cmdList.add(ListBgpPeersCmd.class);
        cmdList.add(UpdateBgpPeerCmd.class);
        cmdList.add(DedicateBgpPeerCmd.class);
        cmdList.add(ReleaseDedicatedBgpPeerCmd.class);
        cmdList.add(ChangeBgpPeersForNetworkCmd.class);
        cmdList.add(ChangeBgpPeersForVpcCmd.class);
        return cmdList;
    }


    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ZONE_IP4_SUBNET_CREATE,
            eventDescription = "Creating IPv4 subnet for a zone",
            async = true, create = true)
    public DataCenterIpv4GuestSubnet createDataCenterIpv4GuestSubnet(CreateIpv4SubnetForZoneCmd cmd) {
        Long zoneId = cmd.getZoneId();
        String subnet = cmd.getSubnet();
        if (!NetUtils.isValidIp4Cidr(subnet)) {
            throw new InvalidParameterValueException("Invalid IPv4 subnet: " + subnet);
        }

        // check conflicts
        List<DataCenterIpv4GuestSubnetVO> existingSubnets = dataCenterIpv4GuestSubnetDao.listByDataCenterId(zoneId);
        checkConflicts(existingSubnets, subnet, null);

        Long domainId = cmd.getDomainId();
        final Long projectId = cmd.getProjectId();
        final String accountName = cmd.getAccountName();

        Long accountId = null;
        if (accountName != null || (projectId != null && projectId != -1L)) {
            accountId = accountManager.finalyzeAccountId(accountName, domainId, projectId, false);
        }
        if (accountId != null) {
            Account account = accountManager.getAccount(accountId);
            domainId = account.getDomainId();
        }

        DataCenterIpv4GuestSubnetVO subnetVO = new DataCenterIpv4GuestSubnetVO(zoneId, NetUtils.transformCidr(subnet));
        if (domainId != null) {
            subnetVO.setDomainId(domainId);
        }
        if (accountId != null) {
            subnetVO.setAccountId(accountId);
        }
        subnetVO = dataCenterIpv4GuestSubnetDao.persist(subnetVO);
        return subnetVO;
    }

    @Override
    public DataCenterIpv4SubnetResponse createDataCenterIpv4SubnetResponse(DataCenterIpv4GuestSubnet subnet) {
        DataCenterIpv4SubnetResponse response = new DataCenterIpv4SubnetResponse();
        response.setCreated(subnet.getCreated());
        response.setSubnet(subnet.getSubnet());
        response.setId(subnet.getUuid());

        DataCenter zone = ApiDBUtils.findZoneById(subnet.getDataCenterId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }

        if (subnet.getDomainId() != null) {
            Domain domain = ApiDBUtils.findDomainById(subnet.getDomainId());
            if (domain != null) {
                response.setDomainId(domain.getUuid());
                response.setDomainName(domain.getName());
            }
        }

        if (subnet.getAccountId() != null) {
            Account account = ApiDBUtils.findAccountById(subnet.getAccountId());
            if (account != null) {
                if (account.getType() == Account.Type.PROJECT) {
                    // find the project
                    Project project = ApiDBUtils.findProjectByProjectAccountId(account.getId());
                    response.setProjectId(project.getUuid());
                    response.setProjectName(project.getName());
                } else {
                    response.setAccountName(account.getAccountName());
                }
            }
        }

        response.setObjectName("zoneipv4subnet");
        return response;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ZONE_IP4_SUBNET_DELETE,
            eventDescription = "Deleting IPv4 subnet for a zone",
            async = true)
    public boolean deleteDataCenterIpv4GuestSubnet(DeleteIpv4SubnetForZoneCmd cmd) {
        // check if subnet is in use
        Long subnetId = cmd.getId();
        List<Ipv4GuestSubnetNetworkMapVO> usedNetworks = ipv4GuestSubnetNetworkMapDao.listUsedByParent(subnetId);
        if (CollectionUtils.isNotEmpty(usedNetworks)) {
            throw new InvalidParameterValueException(String.format("The subnet is being used by %s guest networks.", usedNetworks.size()));
        }

        // remove via dataCenterIpv4GuestSubnetDao and ipv4GuestSubnetNetworkMapDao
        ipv4GuestSubnetNetworkMapDao.deleteByParentId(subnetId);
        dataCenterIpv4GuestSubnetDao.remove(subnetId);
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ZONE_IP4_SUBNET_UPDATE,
            eventDescription = "Updating IPv4 subnet for a zone",
            async = true)
    public DataCenterIpv4GuestSubnet updateDataCenterIpv4GuestSubnet(UpdateIpv4SubnetForZoneCmd cmd) {
        Long subnetId = cmd.getId();
        String newSubnet = cmd.getSubnet();
        DataCenterIpv4GuestSubnetVO subnetVO = dataCenterIpv4GuestSubnetDao.findById(subnetId);
        if (subnetVO == null) {
            throw new InvalidParameterValueException(String.format("Invalid subnet ID: %s", subnetId));
        }

        if (!NetUtils.isValidIp4Cidr(newSubnet)) {
            throw new InvalidParameterValueException(String.format("Invalid IPv4 cidr: %s", newSubnet));
        }

        // check conflicts
        List<DataCenterIpv4GuestSubnetVO> existingSubnets = dataCenterIpv4GuestSubnetDao.listByDataCenterId(subnetVO.getDataCenterId());
        checkConflicts(existingSubnets, newSubnet, subnetId);

        // check if subnet can be updated
        List<Ipv4GuestSubnetNetworkMapVO> usedSubnets = ipv4GuestSubnetNetworkMapDao.listByParent(subnetId);
        for (Ipv4GuestSubnetNetworkMap used : usedSubnets) {
            if (!NetUtils.isNetworkAWithinNetworkB(used.getSubnet(), newSubnet)) {
                throw new InvalidParameterValueException(String.format("Used subnet for guest network %s is not within new cidr: %s", used.getSubnet(), newSubnet));
            }
        }

        // update via dataCenterIpv4GuestSubnetDao
        DataCenterIpv4GuestSubnetVO subnet = dataCenterIpv4GuestSubnetDao.findById(subnetId);
        subnet.setSubnet(NetUtils.transformCidr(newSubnet));
        dataCenterIpv4GuestSubnetDao.update(subnetId, subnet);

        return dataCenterIpv4GuestSubnetDao.findById(subnetId);
    }

    private void checkConflicts(List<DataCenterIpv4GuestSubnetVO> existingSubnets, String newSubnet, Long ignoreSubnetId) {
        for (DataCenterIpv4GuestSubnetVO existing : existingSubnets) {
            if ((ignoreSubnetId == null || existing.getId() != ignoreSubnetId) && NetUtils.isNetworksOverlap(existing.getSubnet(), newSubnet)) {
                throw new InvalidParameterValueException(String.format("Existing zone subnet %s has overlap with: %s", existing.getSubnet(), newSubnet));
            }
        }
    }

    @Override
    public List<DataCenterIpv4GuestSubnet> listDataCenterIpv4GuestSubnets(ListIpv4SubnetsForZoneCmd cmd) {
        Long id = cmd.getId();
        Long zoneId = cmd.getZoneId();
        String subnet = cmd.getSubnet();
        Long domainId = cmd.getDomainId();
        Long projectId = cmd.getProjectId();
        String accountName = cmd.getAccountName();

        SearchCriteria sc = dataCenterIpv4GuestSubnetDao.createSearchCriteria();
        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }
        if (zoneId != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }
        if (subnet != null) {
            sc.addAnd("subnet", SearchCriteria.Op.EQ, subnet);
        }
        if (domainId != null) {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        }
        if (accountName != null || (projectId != null && projectId != -1L)) {
            Long accountId= accountManager.finalyzeAccountId(accountName, domainId, projectId, false);
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        }
        // search via dataCenterIpv4GuestSubnetDao
        return dataCenterIpv4GuestSubnetDao.search(sc, null);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ZONE_IP4_SUBNET_DEDICATE,
            eventDescription = "Dedicating IPv4 subnet for a zone to a domain or an account",
            async = true)
    public DataCenterIpv4GuestSubnet dedicateDataCenterIpv4GuestSubnet(DedicateIpv4SubnetForZoneCmd cmd) {
        final Long id = cmd.getId();
        Long domainId = cmd.getDomainId();
        final Long projectId = cmd.getProjectId();
        final String accountName = cmd.getAccountName();

        DataCenterIpv4GuestSubnetVO subnetVO = dataCenterIpv4GuestSubnetDao.findById(id);
        if (subnetVO == null) {
            throw new InvalidParameterValueException(String.format("Cannot find subnet with id: ", id));
        }
        Long accountId = null;
        if (accountName != null || (projectId != null && projectId != -1L)) {
            accountId = accountManager.finalyzeAccountId(accountName, domainId, projectId, false);
        }
        if (accountId != null) {
            Account account = accountManager.getAccount(accountId);
            domainId = account.getDomainId();
        }

        // Check if the guest subnet is used by other domain or account
        if (domainId != null) {
            List<Ipv4GuestSubnetNetworkMapVO> createdSubnets = ipv4GuestSubnetNetworkMapDao.listUsedByOtherDomains(id, domainId);
            if (CollectionUtils.isNotEmpty(createdSubnets)) {
                throw new InvalidParameterValueException(String.format("The subnet is being used by %s guest networks of other domains.", createdSubnets.size()));
            }
        }
        if (accountId != null) {
            List<Ipv4GuestSubnetNetworkMapVO> createdSubnets = ipv4GuestSubnetNetworkMapDao.listUsedByOtherAccounts(id, accountId);
            if (CollectionUtils.isNotEmpty(createdSubnets)) {
                throw new InvalidParameterValueException(String.format("The subnet is being used by %s guest networks of other accounts.", createdSubnets.size()));
            }
        }

        // update domain_id or account_id via dataCenterIpv4GuestSubnetDao to Mark the subnet as dedicated
        subnetVO.setDomainId(domainId);
        subnetVO.setAccountId(accountId);
        dataCenterIpv4GuestSubnetDao.update(id, subnetVO);
        return dataCenterIpv4GuestSubnetDao.findById(id);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ZONE_IP4_SUBNET_RELEASE,
            eventDescription = "Releasing dedicated IPv4 subnet for a zone from a domain or an account",
            async = true)
    public DataCenterIpv4GuestSubnet releaseDedicatedDataCenterIpv4GuestSubnet(ReleaseDedicatedIpv4SubnetForZoneCmd cmd) {
        final Long id = cmd.getId();
        DataCenterIpv4GuestSubnetVO subnetVO = dataCenterIpv4GuestSubnetDao.findById(id);
        if (subnetVO == null) {
            throw new InvalidParameterValueException(String.format("Cannot find subnet with id: ", id));
        }

        // update domain_id and account_id to null via dataCenterIpv4GuestSubnetDao, to release the dedication
        subnetVO.setDomainId(null);
        subnetVO.setAccountId(null);
        dataCenterIpv4GuestSubnetDao.update(id, subnetVO);
        return dataCenterIpv4GuestSubnetDao.findById(id);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IP4_GUEST_SUBNET_CREATE,
            eventDescription = "Creating IPv4 subnet for guest network",
            async = true, create = true)
    public Ipv4GuestSubnetNetworkMap createIpv4SubnetForGuestNetwork(CreateIpv4SubnetForGuestNetworkCmd cmd) {
        if (ObjectUtils.allNull(cmd.getSubnet(), cmd.getCidrSize())) {
            throw new InvalidParameterValueException("One of subnet and cidrsize must be specified");
        }
        if (ObjectUtils.allNotNull(cmd.getSubnet(), cmd.getCidrSize())) {
            throw new InvalidParameterValueException("subnet and cidrsize are mutually exclusive");
        }
        DataCenterIpv4GuestSubnet parent = dataCenterIpv4GuestSubnetDao.findById(cmd.getParentId());
        if (parent == null) {
            throw new InvalidParameterValueException("the parent subnet is invalid");
        }
        if (cmd.getSubnet() != null) {
            return createIpv4SubnetFromParentSubnet(parent, cmd.getSubnet());
        } else if (cmd.getCidrSize() != null) {
            return createIpv4SubnetFromParentSubnet(parent, cmd.getCidrSize());
        }
        return null;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IP4_GUEST_SUBNET_DELETE,
            eventDescription = "Deleting IPv4 subnet for guest network",
            async = true)
    public boolean deleteIpv4SubnetForGuestNetwork(DeleteIpv4SubnetForGuestNetworkCmd cmd) {
        Long mapId = cmd.getId();
        Ipv4GuestSubnetNetworkMapVO mapVO = ipv4GuestSubnetNetworkMapDao.findById(mapId);
        if (mapVO == null) {
            return true;
        }
        // check if the subnet is not in use
        if (!State.Free.equals(mapVO.getState()) || mapVO.getNetworkId() != null) {
            throw new InvalidParameterValueException("Cannot delete the subnet which is in use");
        }
        return ipv4GuestSubnetNetworkMapDao.remove(mapId);
    }

    @Override
    public void releaseIpv4SubnetForGuestNetwork(long networkId) {
        // check if the network has corresponding subnet
        Ipv4GuestSubnetNetworkMapVO mapVO = ipv4GuestSubnetNetworkMapDao.findByNetworkId(networkId);
        if (mapVO == null) {
            return;
        }
        releaseIpv4SubnetForGuestNetworkOrVpcInternal(mapVO);
    }

    @Override
    public void releaseIpv4SubnetForVpc(long vpcId) {
        // check if the network has corresponding subnet
        Ipv4GuestSubnetNetworkMapVO mapVO = ipv4GuestSubnetNetworkMapDao.findByVpcId(vpcId);
        if (mapVO == null) {
            return;
        }
        releaseIpv4SubnetForGuestNetworkOrVpcInternal(mapVO);
    }

    private void releaseIpv4SubnetForGuestNetworkOrVpcInternal(Ipv4GuestSubnetNetworkMapVO mapVO) {
        ipv4GuestSubnetNetworkMapDao.remove(mapVO.getId());
    }

    @Override
    public List<Ipv4GuestSubnetNetworkMap> listIpv4GuestSubnetsForGuestNetwork(ListIpv4SubnetsForGuestNetworkCmd cmd) {
        Long id = cmd.getId();
        Long zoneId = cmd.getZoneId();
        Long parentId = cmd.getParentId();
        String subnet = cmd.getSubnet();
        String keyword = cmd.getKeyword();
        Long networkId = cmd.getNetworkId();
        Long vpcId = cmd.getVpcId();

        SearchCriteria sc = ipv4GuestSubnetNetworkMapDao.createSearchCriteria();
        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }
        if (zoneId != null) {
            List<DataCenterIpv4GuestSubnetVO> subnets = dataCenterIpv4GuestSubnetDao.listByDataCenterId(zoneId);
            if (CollectionUtils.isEmpty(subnets)) {
                return new ArrayList<>();
            }
            List<Long> parentIds = subnets.stream().map(DataCenterIpv4GuestSubnetVO::getId).collect(Collectors.toList());
            sc.addAnd("parentId", SearchCriteria.Op.IN, parentIds.toArray());
        }
        if (parentId != null) {
            sc.addAnd("parentId", SearchCriteria.Op.EQ, parentId);
        }
        if (subnet != null) {
            sc.addAnd("subnet", SearchCriteria.Op.EQ, subnet);
        }
        if (networkId != null) {
            sc.addAnd("networkId", SearchCriteria.Op.EQ, networkId);
        }
        if (vpcId != null) {
            sc.addAnd("vpcId", SearchCriteria.Op.EQ, vpcId);
        }
        if (keyword != null) {
            sc.addAnd("subnet", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        }

        return ipv4GuestSubnetNetworkMapDao.search(sc, null);
    }

    @Override
    public Ipv4SubnetForGuestNetworkResponse createIpv4SubnetForGuestNetworkResponse(Ipv4GuestSubnetNetworkMap subnet) {
        Ipv4SubnetForGuestNetworkResponse response = new Ipv4SubnetForGuestNetworkResponse();

        response.setCreated(subnet.getCreated());
        response.setSubnet(subnet.getSubnet());
        response.setState(subnet.getState().name());
        response.setId(subnet.getUuid());
        response.setAllocatedTime(subnet.getAllocated());
        Long zoneId = null;
        if (subnet.getNetworkId() != null) {
            Network network = ApiDBUtils.findNetworkById(subnet.getNetworkId());
            response.setNetworkId(network.getUuid());
            response.setNetworkName(network.getName());
            zoneId = network.getDataCenterId();
        }
        if (subnet.getVpcId() != null) {
            Vpc vpc = ApiDBUtils.findVpcById(subnet.getVpcId());
            response.setVpcId(vpc.getUuid());
            response.setVpcName(vpc.getName());
            zoneId = vpc.getZoneId();
        }
        if (subnet.getParentId() != null) {
            DataCenterIpv4GuestSubnet parent = dataCenterIpv4GuestSubnetDao.findById(subnet.getParentId());
            if (parent != null) {
                response.setParentId(parent.getUuid());
                response.setParentSubnet(parent.getSubnet());
                zoneId = parent.getDataCenterId();
            }
        } else if (subnet.getNetworkId() != null) {
            Network network = ApiDBUtils.findNetworkById(subnet.getNetworkId());
            if (network != null) {
                zoneId = network.getDataCenterId();
            }
        }
        if (zoneId != null) {
            DataCenter zone = ApiDBUtils.findZoneById(zoneId);
            if (zone != null) {
                response.setZoneId(zone.getUuid());
                response.setZoneName(zone.getName());
            }
        }
        response.setObjectName("ipv4subnetforguestnetwork");
        return response;
    }

    @Override
    public void getOrCreateIpv4SubnetForGuestNetwork(Network network, String networkCidr) {
        getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(networkCidr, network.getDomainId(), network.getAccountId(), network.getDataCenterId());
    }

    @Override
    public Ipv4GuestSubnetNetworkMap getOrCreateIpv4SubnetForGuestNetwork(Long domainId, Long accountId, Long zoneId, Integer networkCidrSize) {
        return getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(networkCidrSize, domainId, accountId, zoneId);
    }

    @Override
    public void getOrCreateIpv4SubnetForVpc(Vpc vpc, String networkCidr) {
        getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(networkCidr, vpc.getDomainId(), vpc.getAccountId(), vpc.getZoneId());
    }

    @Override
    public Ipv4GuestSubnetNetworkMap getOrCreateIpv4SubnetForVpc(Vpc vpc, Integer vpcCidrSize) {
        return getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(vpcCidrSize, vpc.getDomainId(), vpc.getAccountId(), vpc.getZoneId());
    }

    private Ipv4GuestSubnetNetworkMap getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(Integer cidrSize, Long ownerDomainId, Long ownerAccountId, Long zoneId) {
        validateNetworkCidrSize(ownerAccountId, cidrSize);
        List<DataCenterIpv4GuestSubnetVO> subnets = getZoneSubnetsForAccount(ownerDomainId, ownerAccountId, zoneId);
        for (DataCenterIpv4GuestSubnetVO subnet : subnets) {
            Ipv4GuestSubnetNetworkMap result = getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(cidrSize, subnet);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Ipv4GuestSubnetNetworkMap getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(Integer cidrSize, DataCenterIpv4GuestSubnetVO subnet) {
        Ipv4GuestSubnetNetworkMap map = ipv4GuestSubnetNetworkMapDao.findFirstAvailable(subnet.getId(), cidrSize);
        if (map != null) {
            return map;
        }
        try {
            return createIpv4SubnetFromParentSubnet(subnet, cidrSize);
        } catch (Exception ex) {
            logger.debug("Failed to create Ipv4 subnet from parent subnet {}: {}", subnet.getSubnet(), ex.getMessage());
        }
        return null;
    }

    private void getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(String networkCidr, Long ownerDomainId, Long ownerAccountId, Long zoneId) {
        Ipv4GuestSubnetNetworkMapVO subnetMap = ipv4GuestSubnetNetworkMapDao.findBySubnet(networkCidr);
        if (subnetMap != null) {
            // check if the subnet is in use
            if (subnetMap.getNetworkId() != null || subnetMap.getVpcId() != null) {
                throw new InvalidParameterValueException("The subnet is in use");
            }
            // check if the subnet accessible by the owner
            if (subnetMap.getParentId() != null) {
                DataCenterIpv4GuestSubnetVO parent = dataCenterIpv4GuestSubnetDao.findById(subnetMap.getParentId());
                checkIfNetworkOwnerCanAccessIpv4Subnet(parent, ownerDomainId, ownerAccountId);
            }
            return;
        }

        DataCenterIpv4GuestSubnet parent = getParentOfNetworkCidr(zoneId, networkCidr);
        if (parent != null) {
            // check if the parent subnet is accessible by the owner
            checkIfNetworkOwnerCanAccessIpv4Subnet(parent, ownerDomainId, ownerAccountId);
        }

        // Create new record without networkId
        final Long parentId = parent != null ? parent.getId() : null;
        subnetMap = new Ipv4GuestSubnetNetworkMapVO(parentId, NetUtils.transformCidr(networkCidr), null, State.Free);
        ipv4GuestSubnetNetworkMapDao.persist(subnetMap);
    }

    private void checkIfNetworkOwnerCanAccessIpv4Subnet(DataCenterIpv4GuestSubnet parent, Long ownerDomainId, Long ownerAccountId) {
        if (parent != null
                && ((parent.getDomainId() != null && !parent.getDomainId().equals(ownerDomainId))
                ||  (parent.getAccountId() != null && !parent.getAccountId().equals(ownerAccountId)))) {
            throw new InvalidParameterValueException("The owner of the network has no permission to access the subnet");
        }
    }

    private DataCenterIpv4GuestSubnet getParentOfNetworkCidr(Long zoneId, String networkCidr) {
        List<DataCenterIpv4GuestSubnetVO> existingSubnets = dataCenterIpv4GuestSubnetDao.listByDataCenterId(zoneId);
        for (DataCenterIpv4GuestSubnetVO existing : existingSubnets) {
            if (NetUtils.isNetworkAWithinNetworkB(networkCidr, existing.getSubnet())) {
                // check conflicts
                List<Ipv4GuestSubnetNetworkMapVO> subnetsForNetwork = ipv4GuestSubnetNetworkMapDao.listByParent(existing.getId());
                checkConflicts(subnetsForNetwork, networkCidr);
                return existing;
            }
            if (NetUtils.isNetworksOverlap(existing.getSubnet(), networkCidr)) {
                throw new InvalidParameterValueException(String.format("Existing zone subnet %s has overlap with: %s", existing.getSubnet(), networkCidr));
            }
        }
        // check conflicts
        List<Ipv4GuestSubnetNetworkMapVO> subnetsForNetworkNoParents = ipv4GuestSubnetNetworkMapDao.listAllNoParent();
        checkConflicts(subnetsForNetworkNoParents, networkCidr);
        return null;
    }

    private void checkConflicts(List<Ipv4GuestSubnetNetworkMapVO> subnetsForNetwork, String networkCidr) {
        for (Ipv4GuestSubnetNetworkMapVO subnetForNetwork : subnetsForNetwork) {
            if (NetUtils.isNetworksOverlap(subnetForNetwork.getSubnet(), networkCidr)) {
                throw new InvalidParameterValueException(String.format("Existing subnet %s has overlap with: %s", subnetForNetwork.getSubnet(), networkCidr));
            }
        }
    }

    private void validateNetworkCidrSize(long accountId, Integer networkCidrSize) {
        if (networkCidrSize == null) {
            throw new CloudRuntimeException("network/vpc CidrSize is null");
        }
        Boolean isAutoAllocationEnabled = RoutedIPv4NetworkCidrAutoAllocationEnabled.valueIn(accountId);
        if (!Boolean.TRUE.equals(isAutoAllocationEnabled)) {
            throw new CloudRuntimeException("CIDR auto-allocation is disabled for this account");
        }
    }

    private List<DataCenterIpv4GuestSubnetVO> getZoneSubnetsForAccount(long domainId, long accountId, long zoneId) {
        // Get dedicated guest subnets for the account
        List<DataCenterIpv4GuestSubnetVO> subnets = dataCenterIpv4GuestSubnetDao.listByDataCenterIdAndAccountId(zoneId, accountId);
        subnets.addAll(dataCenterIpv4GuestSubnetDao.listByDataCenterIdAndDomainId(zoneId, domainId));
        // Get non-dedicated zone guest subnets for the account
        subnets.addAll(dataCenterIpv4GuestSubnetDao.listNonDedicatedByDataCenterId(zoneId));
        return subnets;
    }

    private Ipv4GuestSubnetNetworkMap createIpv4SubnetFromParentSubnet(DataCenterIpv4GuestSubnet parent, Integer networkCidrSize) {
        DataCenterIpv4GuestSubnetVO subnetVO = dataCenterIpv4GuestSubnetDao.findById(parent.getId());
        if (subnetVO == null) {
            throw new InvalidParameterValueException(String.format("Invalid subnet ID: %s", parent.getId()));
        }
        // Order subnets by start IP
        List<Ipv4GuestSubnetNetworkMapVO> existingSubnets = ipv4GuestSubnetNetworkMapDao.listByParent(parent.getId());
        Collections.sort(existingSubnets, (subnet1, subnet2) -> {
            Long ip1 = NetUtils.ip2Long(subnet1.getSubnet().split("/")[0]);
            Long ip2 = NetUtils.ip2Long(subnet2.getSubnet().split("/")[0]);
            return ip1.compareTo(ip2);
        });
        // get all free IP ranges
        final List<Pair<Long, Long>> freeIpranges = new ArrayList<>();
        final long[] parentSubnetIpRange = NetUtils.getIpRangeStartIpAndEndIpFromCidr(parent.getSubnet());
        long startIp = parentSubnetIpRange[0];
        for (Ipv4GuestSubnetNetworkMapVO subnet : existingSubnets) {
            long[] subnetIpRange = NetUtils.getIpRangeStartIpAndEndIpFromCidr(subnet.getSubnet());
            if (startIp < subnetIpRange[0]) {
                freeIpranges.add(new Pair<>(startIp, subnetIpRange[0] -1));
            }
            startIp = subnetIpRange[1] + 1;
        }
        if (startIp <= parentSubnetIpRange[1]) {
            freeIpranges.add(new Pair<>(startIp, parentSubnetIpRange[1]));
        }
        // split the IP ranges into list of subnet
        final List<Pair<Long, Integer>> subnetsInFreeIpRanges = new ArrayList<>();
        for (Pair<Long, Long> freeIpRange : freeIpranges) {
            subnetsInFreeIpRanges.addAll(NetUtils.splitIpRangeIntoSubnets(freeIpRange.first(), freeIpRange.second()));
        }

        // Allocate a subnet automatically
        String networkCidr = getFreeNetworkCidr(subnetsInFreeIpRanges, networkCidrSize);
        if (networkCidr == null) {
            throw new CloudRuntimeException("Failed to automatically allocate a subnet with specified cidrsize");
        }
        // create DB record
        Ipv4GuestSubnetNetworkMapVO subnetMap = new Ipv4GuestSubnetNetworkMapVO(parent.getId(), NetUtils.transformCidr(networkCidr), null, State.Free);
        return ipv4GuestSubnetNetworkMapDao.persist(subnetMap);
    }

    private String getFreeNetworkCidr(List<Pair<Long, Integer>> subnetsInFreeIpRanges, int networkCidrSize) {
        for (int cidrSize = networkCidrSize; cidrSize >= 1; cidrSize--) {
            for (Pair<Long, Integer> freeSubnet : subnetsInFreeIpRanges) {
                if (freeSubnet.second().equals(cidrSize)) {
                    String networkCidr = String.format("%s/%s", NetUtils.long2Ip(freeSubnet.first()), networkCidrSize);
                    if (ipv4GuestSubnetNetworkMapDao.findBySubnet(networkCidr) == null) {
                        return networkCidr;
                    }
                }
            }
        }
        return null;
    }

    private Ipv4GuestSubnetNetworkMap createIpv4SubnetFromParentSubnet(DataCenterIpv4GuestSubnet parent, String networkCidr) {
        // Validate the network cidr
        if (!NetUtils.isNetworkAWithinNetworkB(networkCidr, parent.getSubnet())) {
            throw new InvalidParameterValueException(String.format("networkCidr %s is not within parent cidr: %s", networkCidr, parent.getSubnet()));
        }
        // check conflicts
        List<Ipv4GuestSubnetNetworkMapVO> existingSubnets = ipv4GuestSubnetNetworkMapDao.listByParent(parent.getId());
        checkConflicts(existingSubnets, networkCidr);

        // create DB record
        Ipv4GuestSubnetNetworkMapVO subnetMap = new Ipv4GuestSubnetNetworkMapVO(parent.getId(), NetUtils.transformCidr(networkCidr), null, State.Free);
        return ipv4GuestSubnetNetworkMapDao.persist(subnetMap);
    }

    @Override
    public void assignIpv4SubnetToNetwork(Network network) {
        if (network == null || network.getCidr() == null) {
            return;
        }
        Ipv4GuestSubnetNetworkMapVO subnetMap = ipv4GuestSubnetNetworkMapDao.findBySubnet(network.getCidr());
        if (subnetMap != null) {
            if (network.getId() > 0L) {
                subnetMap.setNetworkId(network.getId());
            }
            subnetMap.setState(State.Allocated);
            subnetMap.setAllocated(new Date());
            ipv4GuestSubnetNetworkMapDao.update(subnetMap.getId(), subnetMap);
        }
    }

    @Override
    public void assignIpv4SubnetToVpc(Vpc vpc) {
        if (vpc == null || vpc.getCidr() == null) {
            return;
        }
        Ipv4GuestSubnetNetworkMapVO subnetMap = ipv4GuestSubnetNetworkMapDao.findBySubnet(vpc.getCidr());
        if (subnetMap != null) {
            if (vpc != null && vpc.getId() > 0L) {
                subnetMap.setVpcId(vpc.getId());
            }
            subnetMap.setState(State.Allocated);
            subnetMap.setAllocated(new Date());
            ipv4GuestSubnetNetworkMapDao.update(subnetMap.getId(), subnetMap);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROUTING_IPV4_FIREWALL_RULE_CREATE,
            eventDescription = "Creating routing firewall rule", async = true)
    public FirewallRule createRoutingFirewallRule(CreateRoutingFirewallRuleCmd createRoutingFirewallRuleCmd) throws NetworkRuleConflictException {
        final Account caller = CallContext.current().getCallingAccount();
        final long networkId = createRoutingFirewallRuleCmd.getNetworkId();
        final Integer portStart = createRoutingFirewallRuleCmd.getSourcePortStart();
        final Integer portEnd = createRoutingFirewallRuleCmd.getSourcePortEnd();
        final FirewallRule.TrafficType trafficType = createRoutingFirewallRuleCmd.getTrafficType();
        final String protocol = createRoutingFirewallRuleCmd.getProtocol();
        final Integer icmpCode = createRoutingFirewallRuleCmd.getIcmpCode();
        final Integer icmpType = createRoutingFirewallRuleCmd.getIcmpType();
        final boolean forDisplay = createRoutingFirewallRuleCmd.isDisplay();
        final FirewallRule.FirewallRuleType type = FirewallRule.FirewallRuleType.User;
        final List<String> sourceCidrList = createRoutingFirewallRuleCmd.getSourceCidrList();
        final List<String> destinationCidrList = createRoutingFirewallRuleCmd.getDestinationCidrList();

        for (String cidr : sourceCidrList) {
            if (!NetUtils.isValidIp4Cidr(cidr)) {
                throw new InvalidParameterValueException(String.format("Invalid source IPv4 CIDR: %s", cidr));
            }
        }
        for (String cidr : destinationCidrList) {
            if (!NetUtils.isValidIp4Cidr(cidr)) {
                throw new InvalidParameterValueException(String.format("Invalid destination IPv4 CIDR: %s", cidr));
            }
        }
        if (portStart != null && !NetUtils.isValidPort(portStart)) {
            throw new InvalidParameterValueException("publicPort is an invalid value: " + portStart);
        }
        if (portEnd != null && !NetUtils.isValidPort(portEnd)) {
            throw new InvalidParameterValueException("Public port range is an invalid value: " + portEnd);
        }
        if (ObjectUtils.allNotNull(portStart, portEnd) && portStart > portEnd) {
            throw new InvalidParameterValueException("Start port can't be bigger than end port");
        }

        Network network = networkModel.getNetwork(networkId);
        assert network != null : "Can't create rule as network is null?";

        final long accountId = network.getAccountId();
        final long domainId = network.getDomainId();

        accountManager.checkAccess(caller, null, true, network);

        // Verify that the network guru supports the protocol specified
        Map<Network.Capability, String> caps = networkModel.getNetworkServiceCapabilities(network.getId(), Network.Service.Firewall);

        if (caps != null) {
            String supportedProtocols;
            String supportedTrafficTypes = null;
            supportedTrafficTypes = caps.get(Network.Capability.SupportedTrafficDirection).toLowerCase();

            if (trafficType == FirewallRule.TrafficType.Egress) {
                supportedProtocols = caps.get(Network.Capability.SupportedEgressProtocols).toLowerCase();
            } else {
                supportedProtocols = caps.get(Network.Capability.SupportedProtocols).toLowerCase();
            }

            if (!supportedProtocols.contains(protocol.toLowerCase())) {
                throw new InvalidParameterValueException(String.format("Protocol %s is not supported in zone", protocol));
            } else if (!supportedTrafficTypes.contains(trafficType.toString().toLowerCase())) {
                throw new InvalidParameterValueException("Traffic Type " + trafficType + " is currently supported by Firewall in network " + networkId);
            }
        }

        // icmp code and icmp type can't be passed in for any other protocol rather than icmp
        if (!protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (icmpCode != null || icmpType != null)) {
            throw new InvalidParameterValueException("Can specify icmpCode and icmpType for ICMP protocol only");
        }

        if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (portStart != null || portEnd != null)) {
            throw new InvalidParameterValueException("Can't specify start/end port when protocol is ICMP");
        }

        return Transaction.execute(new TransactionCallbackWithException<FirewallRuleVO, NetworkRuleConflictException>() {
            @Override
            public FirewallRuleVO doInTransaction(TransactionStatus status) throws NetworkRuleConflictException {
                FirewallRuleVO newRule =
                        new FirewallRuleVO(null, null, portStart, portEnd, protocol.toLowerCase(), networkId, accountId, domainId, FirewallRule.Purpose.Firewall,
                                sourceCidrList, destinationCidrList, icmpCode, icmpType, null, trafficType);
                newRule.setType(type);
                newRule.setDisplay(forDisplay);
                newRule = firewallDao.persist(newRule);

                if (FirewallRule.FirewallRuleType.User.equals(type)) {
                    firewallManager.detectRulesConflict(newRule);
                }

                if (!firewallDao.setStateToAdd(newRule)) {
                    throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
                }
                CallContext.current().setEventDetails("Rule Id: " + newRule.getId());

                return newRule;
            }
        });
    }

    @Override
    public Pair<List<? extends FirewallRule>, Integer> listRoutingFirewallRules(ListRoutingFirewallRulesCmd listRoutingFirewallRulesCmd) {
        return firewallService.listFirewallRules(listRoutingFirewallRulesCmd);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROUTING_IPV4_FIREWALL_RULE_UPDATE,
            eventDescription = "Updating routing firewall rule", async = true)
    public FirewallRule updateRoutingFirewallRule(UpdateRoutingFirewallRuleCmd updateRoutingFirewallRuleCmd) {
        final long id = updateRoutingFirewallRuleCmd.getId();
        final boolean forDisplay = updateRoutingFirewallRuleCmd.isDisplay();
        FirewallRuleVO rule = firewallDao.findById(id);
        if (rule == null) {
            throw new InvalidParameterValueException(String.format("Unable to find routing firewall rule with id %d", id));
        }
        if (FirewallRule.TrafficType.Ingress.equals(rule.getTrafficType())) {
            return firewallManager.updateIngressFirewallRule(rule.getId(), null, forDisplay);
        }
        return firewallManager.updateEgressFirewallRule(rule.getId(), null, forDisplay);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROUTING_IPV4_FIREWALL_RULE_DELETE,
            eventDescription = "revoking routing firewall rule", async = true)
    public boolean revokeRoutingFirewallRule(Long id) {
        FirewallRuleVO rule = firewallDao.findById(id);
        if (rule == null) {
            throw new InvalidParameterValueException(String.format("Unable to find routing firewall rule with id %d", id));
        }
        if (FirewallRule.TrafficType.Ingress.equals(rule.getTrafficType())) {
            return firewallManager.revokeIngressFirewallRule(rule.getId(), true);
        }
        return firewallManager.revokeEgressFirewallRule(rule.getId(), true);
    }

    @Override
    public boolean applyRoutingFirewallRule(long id) {
        FirewallRuleVO rule = firewallDao.findById(id);
        if (rule == null) {
            logger.error(String.format("Unable to find routing firewall rule with ID: %d", id));
            return false;
        }
        if (!FirewallRule.Purpose.Firewall.equals(rule.getPurpose())) {
            logger.error(String.format("Cannot apply routing firewall rule with ID: %d as purpose %s is not %s", id, rule.getPurpose(), FirewallRule.Purpose.Firewall));
        }
        logger.debug(String.format("Applying routing firewall rules for rule with ID: %s", rule.getUuid()));
        List<FirewallRuleVO> rules = firewallDao.listByNetworkPurposeTrafficType(rule.getNetworkId(), rule.getPurpose(), FirewallRule.TrafficType.Egress);
        rules.addAll(firewallDao.listByNetworkPurposeTrafficType(rule.getNetworkId(), rule.getPurpose(), FirewallRule.TrafficType.Ingress));
        return firewallManager.applyFirewallRules(rules, false, CallContext.current().getCallingAccount());
    }

    @Override
    public boolean isVirtualRouterGateway(Network network) {
        return isRoutedNetwork(network)
                && (networkServiceMapDao.canProviderSupportServiceInNetwork(network.getId(), Service.Gateway, Provider.VirtualRouter))
                || networkServiceMapDao.canProviderSupportServiceInNetwork(network.getId(), Service.Gateway, Provider.VPCVirtualRouter);
    }

    @Override
    public boolean isVirtualRouterGateway(NetworkOffering networkOffering) {
        return NetworkOffering.NetworkMode.ROUTED.equals(networkOffering.getNetworkMode())
                && networkOfferingServiceMapDao.canProviderSupportServiceInNetworkOffering(networkOffering.getId(), Service.Gateway, Provider.VirtualRouter)
                || networkOfferingServiceMapDao.canProviderSupportServiceInNetworkOffering(networkOffering.getId(), Service.Gateway, Provider.VPCVirtualRouter);
    }

    @Override
    public boolean isRoutedNetwork(Network network) {
        return NetworkOffering.NetworkMode.ROUTED.equals(networkOfferingDao.findById(network.getNetworkOfferingId()).getNetworkMode());
    }

    @Override
    public boolean isDynamicRoutedNetwork(Network network) {
        NetworkOffering networkOffering = networkOfferingDao.findById(network.getNetworkOfferingId());
        return isDynamicRoutedNetwork(networkOffering);
    }

    @Override
    public boolean isDynamicRoutedNetwork(NetworkOffering networkOffering) {
        return NetworkOffering.NetworkMode.ROUTED.equals(networkOffering.getNetworkMode())
                && NetworkOffering.RoutingMode.Dynamic.equals(networkOffering.getRoutingMode());
    }

    @Override
    public boolean isRoutedVpc(Vpc vpc) {
        return NetworkOffering.NetworkMode.ROUTED.equals(vpcOfferingDao.findById(vpc.getVpcOfferingId()).getNetworkMode());
    }

    @Override
    public boolean isDynamicRoutedVpc(Vpc vpc) {
        VpcOffering vpcOffering = vpcOfferingDao.findById(vpc.getVpcOfferingId());
        return isDynamicRoutedVpc(vpcOffering);
    }

    @Override
    public boolean isDynamicRoutedVpc(VpcOffering vpcOffering) {
        return NetworkOffering.NetworkMode.ROUTED.equals(vpcOffering.getNetworkMode())
                && NetworkOffering.RoutingMode.Dynamic.equals(vpcOffering.getRoutingMode());
    }

    @Override
    public boolean isVpcVirtualRouterGateway(VpcOffering vpcOffering) {
        return NetworkOffering.NetworkMode.ROUTED.equals(vpcOffering.getNetworkMode())
                && vpcOfferingServiceMapDao.findByServiceProviderAndOfferingId(Service.Gateway.getName(), Provider.VPCVirtualRouter.getName(), vpcOffering.getId()) != null;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BGP_PEER_CREATE,
            eventDescription = "Creating BGP Peer",
            async = true, create = true)
    public BgpPeer createBgpPeer(CreateBgpPeerCmd createBgpPeerCmd) {
        Long zoneId = createBgpPeerCmd.getZoneId();
        Long asNumber = createBgpPeerCmd.getAsNumber();
        String ip4Address = createBgpPeerCmd.getIp4Address();
        String ip6Address = createBgpPeerCmd.getIp6Address();
        String password = createBgpPeerCmd.getPassword();
        Map<String, String> detailsStr = createBgpPeerCmd.getDetails();

        if (ObjectUtils.allNull(ip4Address, ip6Address)) {
            throw new InvalidParameterValueException("At least one of IPv4 and IPv6 address must be specified.");
        }

        if (ip4Address != null) {
            if (!NetUtils.isValidIp4(ip4Address)) {
                throw new InvalidParameterValueException("IPv4 address is not valid.");
            }
            if (bgpPeerDao.findByZoneAndAsNumberAndAddress(zoneId, asNumber, ip4Address, null) != null) {
                throw new InvalidParameterValueException("There is already a BGP peer with same IPv4 address and AS number in the zone.");
            }
        }

        if (ip6Address != null) {
            if (!NetUtils.isValidIp6(ip6Address)) {
                throw new InvalidParameterValueException("IPv6 address is not valid.");
            }
            if (bgpPeerDao.findByZoneAndAsNumberAndAddress(zoneId, asNumber, null, ip6Address) != null) {
                throw new InvalidParameterValueException("There is already a BGP peer with same IPv6 address and AS number in the zone.");
            }
        }

        final Map<BgpPeer.Detail, String> details = new HashMap<>();
        if (detailsStr != null) {
            for (final String detailStr : detailsStr.keySet()) {
                BgpPeer.Detail bgpPeerDetail = EnumUtils.getEnumIgnoreCase(BgpPeer.Detail.class, detailStr);
                if (bgpPeerDetail == null) {
                    throw new InvalidParameterValueException("Unsupported BGP peer detail " + detailStr);
                }
                details.put(bgpPeerDetail, detailsStr.get(detailStr));
            }
        }

        Long domainId = createBgpPeerCmd.getDomainId();
        final Long projectId = createBgpPeerCmd.getProjectId();
        final String accountName = createBgpPeerCmd.getAccountName();

        Long accountId = null;
        if (accountName != null || (projectId != null && projectId != -1L)) {
            accountId = accountManager.finalyzeAccountId(accountName, domainId, projectId, false);
        }
        if (accountId != null) {
            Account account = accountManager.getAccount(accountId);
            domainId = account.getDomainId();
        }

        BgpPeerVO bgpPeerVO = new BgpPeerVO(zoneId, ip4Address, ip6Address, asNumber, password);
        if (domainId != null) {
            bgpPeerVO.setDomainId(domainId);
        }
        if (accountId != null) {
            bgpPeerVO.setAccountId(accountId);
        }
        bgpPeerVO = bgpPeerDao.persist(bgpPeerVO, details);
        return bgpPeerVO;
    }

    @Override
    public BgpPeerResponse createBgpPeerResponse(BgpPeer bgpPeer) {
        BgpPeerResponse response = new BgpPeerResponse();
        response.setCreated(bgpPeer.getCreated());
        response.setAsNumber(bgpPeer.getAsNumber());
        response.setId(bgpPeer.getUuid());
        response.setIp4Address(bgpPeer.getIp4Address());
        response.setIp6Address(bgpPeer.getIp6Address());

        DataCenter zone = ApiDBUtils.findZoneById(bgpPeer.getDataCenterId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }

        if (bgpPeer.getDomainId() != null) {
            Domain domain = ApiDBUtils.findDomainById(bgpPeer.getDomainId());
            if (domain != null) {
                response.setDomainId(domain.getUuid());
                response.setDomainName(domain.getName());
            }
        }

        if (bgpPeer.getAccountId() != null) {
            Account account = ApiDBUtils.findAccountById(bgpPeer.getAccountId());
            if (account != null) {
                if (account.getType() == Account.Type.PROJECT) {
                    // find the project
                    Project project = ApiDBUtils.findProjectByProjectAccountId(account.getId());
                    response.setProjectId(project.getUuid());
                    response.setProjectName(project.getName());
                } else {
                    response.setAccountName(account.getAccountName());
                }
            }
        }

        Map<BgpPeer.Detail, String> detailsMap = bgpPeerDetailsDao.getBgpPeerDetails(bgpPeer.getId());
        if (MapUtils.isNotEmpty(detailsMap)) {
            response.setDetails(detailsMap);
        }

        response.setObjectName("bgppeer");
        return response;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BGP_PEER_DELETE,
            eventDescription = "Deleting BGP Peer",
            async = true)
    public boolean deleteBgpPeer(DeleteBgpPeerCmd deleteBgpPeerCmd) {
        // check if BGP peer is in use
        Long bgpPeerId = deleteBgpPeerCmd.getId();
        List<BgpPeerNetworkMapVO> usedBgpPeers = bgpPeerNetworkMapDao.listByBgpPeerId(bgpPeerId);
        if (CollectionUtils.isNotEmpty(usedBgpPeers)) {
            throw new InvalidParameterValueException(String.format("The BGP peer is being used by %s guest networks.", usedBgpPeers.size()));
        }

        bgpPeerDao.remove(bgpPeerId);
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BGP_PEER_UPDATE,
            eventDescription = "Updating a BGP Peer",
            async = true)
    public BgpPeer updateBgpPeer(UpdateBgpPeerCmd updateBgpPeerCmd) {
        Long bgpPeerId = updateBgpPeerCmd.getId();
        Long newAsNumber = updateBgpPeerCmd.getAsNumber();
        String newIp4Address = updateBgpPeerCmd.getIp4Address();
        String newIp6Address = updateBgpPeerCmd.getIp6Address();
        String password = updateBgpPeerCmd.getPassword();
        Map<String, String> detailsStr = updateBgpPeerCmd.getDetails();

        BgpPeerVO bgpPeerVO = bgpPeerDao.findById(bgpPeerId);
        if (bgpPeerVO == null) {
            throw new InvalidParameterValueException(String.format("Invalid BGP peer ID: %s", bgpPeerId));
        }

        Long zoneId = bgpPeerVO.getDataCenterId();

        boolean isAsNumberChanged = (newAsNumber != null) && !newAsNumber.equals(bgpPeerVO.getAsNumber());
        boolean isIp4AddressChanged = StringUtils.isNotBlank(newIp4Address) && !newIp4Address.equals(bgpPeerVO.getIp4Address());
        boolean isIp6AddressChanged = StringUtils.isNotBlank(newIp6Address) && !newIp6Address.equals(bgpPeerVO.getIp6Address());

        if (newAsNumber == null) {
            newAsNumber = bgpPeerVO.getAsNumber();
        }
        if (newIp4Address == null) {
            newIp4Address = bgpPeerVO.getIp4Address();
        } else if (StringUtils.isBlank(newIp4Address)) {
            newIp4Address = null;
        } else if (!NetUtils.isValidIp4(newIp4Address)) {
            throw new InvalidParameterValueException("new IPv4 address is not valid.");
        }

        if (newIp6Address == null) {
            newIp6Address = bgpPeerVO.getIp6Address();
        } else if (StringUtils.isBlank(newIp6Address)) {
            newIp6Address = null;
        } else if (!NetUtils.isValidIp6(newIp6Address)) {
            throw new InvalidParameterValueException("new IPv6 address is not valid.");
        }

        if (ObjectUtils.allNull(newIp4Address, newIp6Address)) {
            throw new InvalidParameterValueException("At least one of IPv4 and IPv6 address must be specified.");
        }

        if ((isAsNumberChanged || isIp4AddressChanged) && newIp4Address != null) {
            if (bgpPeerDao.findByZoneAndAsNumberAndAddress(zoneId, newAsNumber, newIp4Address, null) != null) {
                throw new InvalidParameterValueException("There is already a BGP peer with same IPv4 address and AS number in the zone.");
            }
        }
        if ((isAsNumberChanged || isIp6AddressChanged) && newIp6Address != null) {
            if (bgpPeerDao.findByZoneAndAsNumberAndAddress(zoneId, newAsNumber, null, newIp6Address) != null) {
                throw new InvalidParameterValueException("There is already a BGP peer with same IPv6 address and AS number in the zone.");
            }
        }

        final Map<BgpPeer.Detail, String> details = new HashMap<>();
        if (detailsStr != null) {
            for (final String detailStr : detailsStr.keySet()) {
                BgpPeer.Detail bgpPeerDetail = EnumUtils.getEnumIgnoreCase(BgpPeer.Detail.class, detailStr);
                if (bgpPeerDetail == null) {
                    throw new InvalidParameterValueException("Unsupported BGP peer detail " + detailStr);
                }
                details.put(bgpPeerDetail, detailsStr.get(detailStr));
            }
        }

        // update via bgpPeerDao
        bgpPeerVO.setAsNumber(newAsNumber);
        bgpPeerVO.setIp4Address(newIp4Address);
        bgpPeerVO.setIp6Address(newIp6Address);
        if (password != null) {
            bgpPeerVO.setPassword(password);
        }
        bgpPeerDao.update(bgpPeerId, bgpPeerVO);

        boolean cleanupDetails = updateBgpPeerCmd.isCleanupDetails();
        if (cleanupDetails){
            bgpPeerDetailsDao.removeByBgpPeerId(bgpPeerId);
        } else if (MapUtils.isNotEmpty(details)) {
            bgpPeerDetailsDao.removeByBgpPeerId(bgpPeerId);
            List<BgpPeerDetailsVO> bgpPeerDetails = new ArrayList<>();
            for (BgpPeer.Detail key : details.keySet()) {
                BgpPeerDetailsVO detail = new BgpPeerDetailsVO(bgpPeerVO.getId(), key, details.get(key), true);
                bgpPeerDetails.add(detail);
            }
            bgpPeerDetailsDao.saveDetails(bgpPeerDetails);
        }

        return bgpPeerDao.findById(bgpPeerId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BGP_PEER_DEDICATE,
            eventDescription = "Dedicating BGP Peer to a domain or an account",
            async = true)
    public BgpPeer dedicateBgpPeer(DedicateBgpPeerCmd dedicateBgpPeerCmd) {
        final Long id = dedicateBgpPeerCmd.getId();
        Long domainId = dedicateBgpPeerCmd.getDomainId();
        final Long projectId = dedicateBgpPeerCmd.getProjectId();
        final String accountName = dedicateBgpPeerCmd.getAccountName();

        BgpPeerVO bgpPeerVO = bgpPeerDao.findById(id);
        if (bgpPeerVO == null) {
            throw new InvalidParameterValueException(String.format("Cannot find BGP peer with id: ", id));
        }
        Long accountId = null;
        if (accountName != null || (projectId != null && projectId != -1L)) {
            accountId = accountManager.finalyzeAccountId(accountName, domainId, projectId, false);
        }
        if (accountId != null) {
            Account account = accountManager.getAccount(accountId);
            domainId = account.getDomainId();
        }

        // Check if the BGP peer is used by other domain or account
        if (domainId != null) {
            List<BgpPeerNetworkMapVO> usedBgpPeers = bgpPeerNetworkMapDao.listUsedNetworksByOtherDomains(id, domainId);
            if (CollectionUtils.isNotEmpty(usedBgpPeers)) {
                throw new InvalidParameterValueException(String.format("The subnet is being used by %s guest networks of other domains.", usedBgpPeers.size()));
            }
            usedBgpPeers = bgpPeerNetworkMapDao.listUsedVpcsByOtherDomains(id, domainId);
            if (CollectionUtils.isNotEmpty(usedBgpPeers)) {
                throw new InvalidParameterValueException(String.format("The subnet is being used by %s vpcs of other domains.", usedBgpPeers.size()));
            }
        }
        if (accountId != null) {
            List<BgpPeerNetworkMapVO> usedBgpPeers = bgpPeerNetworkMapDao.listUsedNetworksByOtherAccounts(id, accountId);
            if (CollectionUtils.isNotEmpty(usedBgpPeers)) {
                throw new InvalidParameterValueException(String.format("The subnet is being used by %s guest networks of other accounts.", usedBgpPeers.size()));
            }
            usedBgpPeers = bgpPeerNetworkMapDao.listUsedVpcsByOtherAccounts(id, accountId);
            if (CollectionUtils.isNotEmpty(usedBgpPeers)) {
                throw new InvalidParameterValueException(String.format("The subnet is being used by %s guest networks of other accounts.", usedBgpPeers.size()));
            }
        }

        // update domain_id or account_id via dataCenterIpv4GuestSubnetDao to Mark the subnet as dedicated
        bgpPeerVO.setDomainId(domainId);
        bgpPeerVO.setAccountId(accountId);
        bgpPeerDao.update(id, bgpPeerVO);
        return bgpPeerDao.findById(id);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_BGP_PEER_RELEASE,
            eventDescription = "Releasing a dedicated BGP Peer from a domain or an account",
            async = true)
    public BgpPeer releaseDedicatedBgpPeer(ReleaseDedicatedBgpPeerCmd releaseDedicatedBgpPeerCmd) {
        final Long id = releaseDedicatedBgpPeerCmd.getId();
        BgpPeerVO bgpPeerVO = bgpPeerDao.findById(id);
        if (bgpPeerVO == null) {
            throw new InvalidParameterValueException(String.format("Cannot find BGP peer with id: ", id));
        }

        // update domain_id and account_id to null via bgpPeerDao, to release the dedication
        bgpPeerVO.setDomainId(null);
        bgpPeerVO.setAccountId(null);
        bgpPeerDao.update(id, bgpPeerVO);
        return bgpPeerDao.findById(id);
    }

    @Override
    public List<BgpPeer> listBgpPeers(ListBgpPeersCmd listBgpPeersCmd) {
        Long id = listBgpPeersCmd.getId();
        Long zoneId = listBgpPeersCmd.getZoneId();
        Long asNumber = listBgpPeersCmd.getAsNumber();
        Long domainId = listBgpPeersCmd.getDomainId();
        Long projectId = listBgpPeersCmd.getProjectId();
        String accountName = listBgpPeersCmd.getAccountName();
        Boolean isDedicated = listBgpPeersCmd.getDedicated();
        String keyword = listBgpPeersCmd.getKeyword();

        Long accountId = null;
        if (accountName != null || (projectId != null && projectId != -1L)) {
            accountId = accountManager.finalyzeAccountId(accountName, domainId, projectId, false);
        }
        if (isDedicated != null) {
            SearchCriteria sc1 = createSearchCriteriaForListBgpPeersCmd(id, zoneId, asNumber, keyword);
            if (Boolean.TRUE.equals(isDedicated)) {
                sc1.addAnd("domainId", SearchCriteria.Op.NNULL);
            } else {
                sc1.addAnd("domainId", SearchCriteria.Op.NULL);
            }
            if (domainId != null) {
                sc1.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            }
            if (accountId != null) {
                sc1.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
            }
            // search via bgpPeerDao
            return bgpPeerDao.search(sc1, null);
        } else if (accountId != null) {
            if (zoneId == null) {
                throw new InvalidParameterValueException("zoneId is required when list BGP peers for an account.");
            }
            Account account = accountManager.getAccount(accountId);
            List<Long> bgpPeerIds = getBgpPeerIdsForAccount(account, zoneId);
            if (CollectionUtils.isEmpty(bgpPeerIds)) {
                return new ArrayList<>();
            }
            SearchCriteria sc2 = createSearchCriteriaForListBgpPeersCmd(id, zoneId, asNumber, keyword);
            sc2.addAnd("id", SearchCriteria.Op.IN, bgpPeerIds.toArray());
            return bgpPeerDao.search(sc2, null);
        } else {
            SearchCriteria sc3 = createSearchCriteriaForListBgpPeersCmd(id, zoneId, asNumber, keyword);
            if (domainId != null) {
                sc3.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            }
            return bgpPeerDao.search(sc3, null);
        }
    }

    private SearchCriteria createSearchCriteriaForListBgpPeersCmd(Long id, Long zoneId, Long asNumber, String keyword) {
        SearchCriteria sc = bgpPeerDao.createSearchCriteria();
        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }
        if (zoneId != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }
        if (asNumber != null) {
            sc.addAnd("asNumber", SearchCriteria.Op.EQ, asNumber);
        }
        if (StringUtils.isNotBlank(keyword)) {
            SearchCriteria ssc = bgpPeerDao.createSearchCriteria();
            ssc.addOr("asNumber", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("ip4Address", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("ip6Address", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("ip4Address", SearchCriteria.Op.SC, ssc);
        }
        return sc;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_BGP_PEER_UPDATE,
            eventDescription = "Changing BGP peers for network", async = true)
    public Network changeBgpPeersForNetwork(ChangeBgpPeersForNetworkCmd changeBgpPeersForNetworkCmd) {
        Long networkId = changeBgpPeersForNetworkCmd.getNetworkId();
        List<Long> bgpPeerIds = changeBgpPeersForNetworkCmd.getBgpPeerIds();

        Network network = networkDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException(String.format("Invalid network ID: %s", networkId));
        }
        if (network.getVpcId() != null) {
            throw new InvalidParameterValueException("The BGP peers of VPC tiers will inherit from the VPC, do not add separately.");
        }

        Account owner = accountManager.getAccount(network.getAccountId());
        NetworkOffering networkOffering = networkOfferingDao.findById(network.getNetworkOfferingId());
        if (CollectionUtils.isNotEmpty(bgpPeerIds) && !isDynamicRoutedNetwork(networkOffering)) {
            throw new InvalidParameterValueException("The network does not support Dynamic routing");
        }
        validateBgpPeers(owner, network.getDataCenterId(), bgpPeerIds);

        return changeBgpPeersForNetworkInternal(network, bgpPeerIds);
    }

    @Override
    public Network removeBgpPeersFromNetwork(Network network) {
        return changeBgpPeersForNetworkInternal(network, null);
    }

    private Network changeBgpPeersForNetworkInternal(Network network, List<Long> bgpPeerIds) {
        final List<Long> bgpPeerIdsToBeAdded;
        if (CollectionUtils.isNotEmpty(bgpPeerIds)) {
            bgpPeerIdsToBeAdded = new ArrayList<>(bgpPeerIds);
        } else {
            bgpPeerIdsToBeAdded = new ArrayList<>();
        }
        List<BgpPeerNetworkMapVO> bgpPeerNetworkMapVOS = bgpPeerNetworkMapDao.listByNetworkId(network.getId());
        for (BgpPeerNetworkMapVO bgpPeerNetworkMapVO : bgpPeerNetworkMapVOS) {
            Long bgpPeerId = bgpPeerNetworkMapVO.getBgpPeerId();
            if (bgpPeerIdsToBeAdded.contains(bgpPeerId)) {
                bgpPeerIdsToBeAdded.remove(bgpPeerId);
            } else {
                bgpPeerNetworkMapVO.setState(BgpPeer.State.Revoke);
                bgpPeerNetworkMapDao.update(bgpPeerNetworkMapVO.getId(), bgpPeerNetworkMapVO);
            }
        }

        for (Long bgpPeedId : bgpPeerIdsToBeAdded) {
            bgpPeerNetworkMapDao.persist(new BgpPeerNetworkMapVO(bgpPeedId, network.getId(), null, BgpPeer.State.Add));
        }

        boolean result = true;
        try {
            result = bgpService.applyBgpPeers(network, false);
        } catch (ResourceUnavailableException ex) {
            logger.error("Unable to apply BGP peers due to : " + ex.getMessage());
            result = false;
        }
        if (result) {
            logger.info("Succeed to apply BGP peers, updating state");
            bgpPeerNetworkMapVOS = bgpPeerNetworkMapDao.listByNetworkId(network.getId());
            for (BgpPeerNetworkMapVO bgpPeerNetworkMapVO : bgpPeerNetworkMapVOS) {
                if (BgpPeer.State.Add.equals(bgpPeerNetworkMapVO.getState())) {
                    bgpPeerNetworkMapVO.setState(BgpPeer.State.Active);
                    bgpPeerNetworkMapDao.update(bgpPeerNetworkMapVO.getId(), bgpPeerNetworkMapVO);
                } else if (BgpPeer.State.Revoke.equals(bgpPeerNetworkMapVO.getState())) {
                    bgpPeerNetworkMapDao.remove(bgpPeerNetworkMapVO.getId());
                }
            }
        } else {
            logger.info("Failed to apply BGP peers, rolling back to original state");
            bgpPeerNetworkMapVOS = bgpPeerNetworkMapDao.listByNetworkId(network.getId());
            for (BgpPeerNetworkMapVO bgpPeerNetworkMapVO : bgpPeerNetworkMapVOS) {
                if (BgpPeer.State.Add.equals(bgpPeerNetworkMapVO.getState())) {
                    bgpPeerNetworkMapDao.remove(bgpPeerNetworkMapVO.getId());
                } else if (BgpPeer.State.Revoke.equals(bgpPeerNetworkMapVO.getState())) {
                    bgpPeerNetworkMapVO.setState(BgpPeer.State.Add);
                    bgpPeerNetworkMapDao.update(bgpPeerNetworkMapVO.getId(), bgpPeerNetworkMapVO);
                }
            }
            try {
                bgpService.applyBgpPeers(network, false);
            } catch (ResourceUnavailableException ex) {
                logger.error("Unable to apply BGP peers after rollback due to : " + ex.getMessage());
            }
            return null;
        }

        return networkDao.findById(network.getId());
    }

    @Override
    public void validateBgpPeers(Account owner, Long zoneId, List<Long> bgpPeerIds) {
        if (CollectionUtils.isEmpty(bgpPeerIds)) {
            return;
        }
        for (Long bgpPeerId : bgpPeerIds) {
            BgpPeerVO bgpPeerVO = bgpPeerDao.findById(bgpPeerId);
            if (bgpPeerVO == null) {
                throw new InvalidParameterValueException(String.format("Invalid BGP peer ID: %s", bgpPeerId));
            }
            if (bgpPeerVO.getDataCenterId() != zoneId) {
                throw new InvalidParameterValueException(String.format("BGP peer (ID: %s) belongs to a different zone", bgpPeerVO.getUuid()));
            }
            if (bgpPeerVO.getDomainId() != null && !bgpPeerVO.getDomainId().equals(owner.getDomainId())) {
                throw new InvalidParameterValueException(String.format("BGP peer (ID: %s) belongs to a different domain", bgpPeerVO.getUuid()));
            }
            if (bgpPeerVO.getAccountId() != null && !bgpPeerVO.getAccountId().equals(owner.getAccountId())) {
                throw new InvalidParameterValueException(String.format("BGP peer (ID: %s) belongs to a different account", bgpPeerVO.getUuid()));
            }
        }
    }

    @Override
    public void persistBgpPeersForGuestNetwork(long networkId, List<Long> bgpPeerIds) {
        bgpPeerNetworkMapDao.persistForNetwork(networkId, bgpPeerIds);
    }

    @Override
    public void releaseBgpPeersForGuestNetwork(long networkId) {
        bgpPeerNetworkMapDao.removeByNetworkId(networkId);
    }

    @Override
    public void persistBgpPeersForVpc(long vpcId, List<Long> bgpPeerIds) {
        bgpPeerNetworkMapDao.persistForVpc(vpcId, bgpPeerIds);
    }

    @Override
    public void releaseBgpPeersForVpc(long vpcId) {
        bgpPeerNetworkMapDao.removeByVpcId(vpcId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VPC_BGP_PEER_UPDATE,
            eventDescription = "Changing BGP peers for VPC", async = true)
    public Vpc changeBgpPeersForVpc(ChangeBgpPeersForVpcCmd changeBgpPeersForVpcCmd) {
        Long vpcId = changeBgpPeersForVpcCmd.getVpcId();
        List<Long> bgpPeerIds = changeBgpPeersForVpcCmd.getBgpPeerIds();

        Vpc vpc = vpcDao.findById(vpcId);
        if (vpc == null) {
            throw new InvalidParameterValueException(String.format("Invalid VPC ID: %s", vpcId));
        }

        Account owner = accountManager.getAccount(vpc.getAccountId());
        VpcOffering vpcOffering = vpcOfferingDao.findById(vpc.getVpcOfferingId());
        if (CollectionUtils.isNotEmpty(bgpPeerIds) && !isDynamicRoutedVpc(vpcOffering)) {
            throw new InvalidParameterValueException("The VPC does not support Dynamic routing");
        }
        validateBgpPeers(owner, vpc.getZoneId(), bgpPeerIds);

        return changeBgpPeersForVpcInternal(vpc, bgpPeerIds);
    }

    @Override
    public List<Long> getBgpPeerIdsForAccount(Account owner, long zoneId) {
        return bgpPeerDao.listAvailableBgpPeerIdsForAccount(zoneId, owner.getDomainId(), owner.getId(), UseSystemBgpPeers.valueIn(owner.getId()));
    }

    private Vpc changeBgpPeersForVpcInternal(Vpc vpc, List<Long> bgpPeerIds) {
        final List<Long> bgpPeerIdsToBeAdded;
        if (CollectionUtils.isNotEmpty(bgpPeerIds)) {
            bgpPeerIdsToBeAdded = new ArrayList<>(bgpPeerIds);
        } else {
            bgpPeerIdsToBeAdded = new ArrayList<>();
        }
        List<BgpPeerNetworkMapVO> bgpPeerNetworkMapVOS = bgpPeerNetworkMapDao.listByVpcId(vpc.getId());
        for (BgpPeerNetworkMapVO bgpPeerNetworkMapVO : bgpPeerNetworkMapVOS) {
            Long bgpPeerId = bgpPeerNetworkMapVO.getBgpPeerId();
            if (bgpPeerIdsToBeAdded.contains(bgpPeerId)) {
                bgpPeerIdsToBeAdded.remove(bgpPeerId);
            } else {
                bgpPeerNetworkMapVO.setState(BgpPeer.State.Revoke);
                bgpPeerNetworkMapDao.update(bgpPeerNetworkMapVO.getId(), bgpPeerNetworkMapVO);
            }
        }

        for (Long bgpPeedId : bgpPeerIdsToBeAdded) {
            bgpPeerNetworkMapDao.persist(new BgpPeerNetworkMapVO(bgpPeedId, null, vpc.getId(), BgpPeer.State.Add));
        }

        boolean result = true;
        try {
            result = bgpService.applyBgpPeers(vpc, false);
        } catch (ResourceUnavailableException ex) {
            logger.error("Unable to apply BGP peers due to : " + ex.getMessage());
            result = false;
        }
        if (result) {
            logger.info("Succeed to apply BGP peers, updating state");
            bgpPeerNetworkMapVOS = bgpPeerNetworkMapDao.listByVpcId(vpc.getId());
            for (BgpPeerNetworkMapVO bgpPeerNetworkMapVO : bgpPeerNetworkMapVOS) {
                if (BgpPeer.State.Add.equals(bgpPeerNetworkMapVO.getState())) {
                    bgpPeerNetworkMapVO.setState(BgpPeer.State.Active);
                    bgpPeerNetworkMapDao.update(bgpPeerNetworkMapVO.getId(), bgpPeerNetworkMapVO);
                } else if (BgpPeer.State.Revoke.equals(bgpPeerNetworkMapVO.getState())) {
                    bgpPeerNetworkMapDao.remove(bgpPeerNetworkMapVO.getId());
                }
            }
        } else {
            logger.info("Failed to apply BGP peers, rolling back to original state");
            bgpPeerNetworkMapVOS = bgpPeerNetworkMapDao.listByVpcId(vpc.getId());
            for (BgpPeerNetworkMapVO bgpPeerNetworkMapVO : bgpPeerNetworkMapVOS) {
                if (BgpPeer.State.Add.equals(bgpPeerNetworkMapVO.getState())) {
                    bgpPeerNetworkMapDao.remove(bgpPeerNetworkMapVO.getId());
                } else if (BgpPeer.State.Revoke.equals(bgpPeerNetworkMapVO.getState())) {
                    bgpPeerNetworkMapVO.setState(BgpPeer.State.Add);
                    bgpPeerNetworkMapDao.update(bgpPeerNetworkMapVO.getId(), bgpPeerNetworkMapVO);
                }
            }
            try {
                bgpService.applyBgpPeers(vpc, false);
            } catch (ResourceUnavailableException ex) {
                logger.error("Unable to apply BGP peers after rollback due to : " + ex.getMessage());
            }
            return null;
        }

        return vpcDao.findById(vpc.getId());
    }

    @Override
    public void removeIpv4SubnetsForZoneByAccountId(long accountId) {
        List<DataCenterIpv4GuestSubnetVO> existingSubnets = dataCenterIpv4GuestSubnetDao.listByAccountId(accountId);
        for (DataCenterIpv4GuestSubnetVO subnet : existingSubnets) {
            ipv4GuestSubnetNetworkMapDao.deleteByParentId(subnet.getId());
            dataCenterIpv4GuestSubnetDao.remove(subnet.getId());
        }
    }

    @Override
    public void removeIpv4SubnetsForZoneByDomainId(long domainId) {
        List<DataCenterIpv4GuestSubnetVO> existingSubnets = dataCenterIpv4GuestSubnetDao.listByDomainId(domainId);
        for (DataCenterIpv4GuestSubnetVO subnet : existingSubnets) {
            ipv4GuestSubnetNetworkMapDao.deleteByParentId(subnet.getId());
            dataCenterIpv4GuestSubnetDao.remove(subnet.getId());
        }
    }

    @Override
    public void removeBgpPeersByAccountId(long accountId) {
        bgpPeerDao.removeByAccountId(accountId);
    }

    @Override
    public void removeBgpPeersByDomainId(long domainId) {
        bgpPeerDao.removeByDomainId(domainId);
    }
}
