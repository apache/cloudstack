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
import com.cloud.dc.DataCenter;
import com.cloud.domain.Domain;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcOffering;
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

import org.apache.cloudstack.api.command.admin.network.CreateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.CreateIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.DedicateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.ListIpv4GuestSubnetsCmd;
import org.apache.cloudstack.api.command.admin.network.ListIpv4SubnetsForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.ReleaseDedicatedIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.user.network.routing.CreateRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.network.routing.DeleteRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.network.routing.ListRoutingFirewallRulesCmd;
import org.apache.cloudstack.api.command.user.network.routing.UpdateRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.response.DataCenterIpv4SubnetResponse;
import org.apache.cloudstack.api.response.Ipv4SubnetForGuestNetworkResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnet;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnetVO;
import org.apache.cloudstack.datacenter.dao.DataCenterIpv4GuestSubnetDao;
import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMap.State;
import org.apache.cloudstack.network.dao.Ipv4GuestSubnetNetworkMapDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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

    @Override
    public String getConfigComponentName() {
        return RoutedIpv4Manager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {
                RoutedNetworkIPv4MaxCidrSize, RoutedNetworkIPv4MinCidrSize, RoutedIPv4NetworkCidrAutoAllocationEnabled,
                RoutedVpcIPv4MaxCidrSize, RoutedVpcIPv4MinCidrSize
        };
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateIpv4GuestSubnetCmd.class);
        cmdList.add(DeleteIpv4GuestSubnetCmd.class);
        cmdList.add(ListIpv4GuestSubnetsCmd.class);
        cmdList.add(UpdateIpv4GuestSubnetCmd.class);
        cmdList.add(DedicateIpv4GuestSubnetCmd.class);
        cmdList.add(ReleaseDedicatedIpv4GuestSubnetCmd.class);
        cmdList.add(CreateIpv4SubnetForGuestNetworkCmd.class);
        cmdList.add(ListIpv4SubnetsForGuestNetworkCmd.class);
        cmdList.add(DeleteIpv4SubnetForGuestNetworkCmd.class);
        cmdList.add(CreateRoutingFirewallRuleCmd.class);
        cmdList.add(ListRoutingFirewallRulesCmd.class);
        cmdList.add(UpdateRoutingFirewallRuleCmd.class);
        cmdList.add(DeleteRoutingFirewallRuleCmd.class);
        return cmdList;
    }


    @Override
    public DataCenterIpv4GuestSubnet createDataCenterIpv4GuestSubnet(CreateIpv4GuestSubnetCmd cmd) {
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
        if (accountName != null || projectId != null) {
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
    public boolean deleteDataCenterIpv4GuestSubnet(DeleteIpv4GuestSubnetCmd cmd) {
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
    public DataCenterIpv4GuestSubnet updateDataCenterIpv4GuestSubnet(UpdateIpv4GuestSubnetCmd cmd) {
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
    public List<? extends DataCenterIpv4GuestSubnet> listDataCenterIpv4GuestSubnets(ListIpv4GuestSubnetsCmd cmd) {
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
        if (accountName != null || projectId != null) {
            Long accountId= accountManager.finalyzeAccountId(accountName, domainId, projectId, false);
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        }
        // search via dataCenterIpv4GuestSubnetDao
        return dataCenterIpv4GuestSubnetDao.search(sc, null);
    }

    @Override
    public DataCenterIpv4GuestSubnet dedicateDataCenterIpv4GuestSubnet(DedicateIpv4GuestSubnetCmd cmd) {
        final Long id = cmd.getId();
        Long domainId = cmd.getDomainId();
        final Long projectId = cmd.getProjectId();
        final String accountName = cmd.getAccountName();

        DataCenterIpv4GuestSubnetVO subnetVO = dataCenterIpv4GuestSubnetDao.findById(id);
        if (subnetVO == null) {
            throw new InvalidParameterValueException(String.format("Cannot find subnet with id: ", id));
        }
        Long accountId = null;
        if (accountName != null || projectId != null) {
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
    public DataCenterIpv4GuestSubnet releaseDedicatedDataCenterIpv4GuestSubnet(ReleaseDedicatedIpv4GuestSubnetCmd cmd) {
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
        if (mapVO.getParentId() == null) {
            // if parent_id is NULL, remove it
            ipv4GuestSubnetNetworkMapDao.remove(mapVO.getId());
        } else {
            // otherwise, release it
            mapVO.setAllocated(null);
            mapVO.setVpcId(null);
            mapVO.setNetworkId(null);
            mapVO.setState(State.Free);
            ipv4GuestSubnetNetworkMapDao.update(mapVO.getId(), mapVO);
        }
    }

    @Override
    public List<? extends Ipv4GuestSubnetNetworkMap> listIpv4GuestSubnetsForGuestNetwork(ListIpv4SubnetsForGuestNetworkCmd cmd) {
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
            sc.addAnd("subnet", SearchCriteria.Op.LIKE, keyword);
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
    public Ipv4GuestSubnetNetworkMap getOrCreateIpv4SubnetForGuestNetwork(Network network, Integer networkCidrSize) {
        Ipv4GuestSubnetNetworkMap subnet = getIpv4SubnetForAccount(network.getDomainId(), network.getAccountId(), network.getDataCenterId(), networkCidrSize);
        if (subnet != null) {
            return subnet;
        }
        return createIpv4SubnetForAccount(network.getDomainId(), network.getAccountId(), network.getDataCenterId(), networkCidrSize);
    }

    @Override
    public void getOrCreateIpv4SubnetForVpc(Vpc vpc, String networkCidr) {
        getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(networkCidr, vpc.getDomainId(), vpc.getAccountId(), vpc.getZoneId());
    }

    @Override
    public Ipv4GuestSubnetNetworkMap getOrCreateIpv4SubnetForVpc(Vpc vpc, Integer vpcCidrSize) {
        Ipv4GuestSubnetNetworkMap subnet = getIpv4SubnetForAccount(vpc.getDomainId(), vpc.getAccountId(), vpc.getZoneId(), vpcCidrSize);
        if (subnet != null) {
            return subnet;
        }
        return createIpv4SubnetForAccount(vpc.getDomainId(), vpc.getAccountId(), vpc.getZoneId(), vpcCidrSize);
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
            throw new CloudRuntimeException("networkCidrSize is null");
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
        // Get zone guest subnets for the account
        subnets.addAll(dataCenterIpv4GuestSubnetDao.listByDataCenterId(zoneId));
        return subnets;
    }

    private Ipv4GuestSubnetNetworkMap getIpv4SubnetForAccount(long domainId, long accountId, long zoneId, Integer networkCidrSize) {
        List<DataCenterIpv4GuestSubnetVO> subnets = getZoneSubnetsForAccount(domainId, accountId, zoneId);
        // find an allocated subnet
        for (DataCenterIpv4GuestSubnetVO subnet : subnets) {
            Ipv4GuestSubnetNetworkMap map = ipv4GuestSubnetNetworkMapDao.findFirstAvailable(subnet.getId(), networkCidrSize);
            if (map != null) {
                return map;
            }
        }
        return null;
    }

    private Ipv4GuestSubnetNetworkMap createIpv4SubnetForAccount(long domainId, long accountId, long zoneId, Integer networkCidrSize) {
        List<DataCenterIpv4GuestSubnetVO> subnets = getZoneSubnetsForAccount(domainId, accountId, zoneId);
        for (DataCenterIpv4GuestSubnetVO subnet : subnets) {
            try {
                return createIpv4SubnetFromParentSubnet(subnet, networkCidrSize);
            } catch (Exception ex) {
                logger.debug("Failed to create Ipv4 subnet from parent subnet {}: {}", subnet.getSubnet(), ex.getMessage());
            }
        }
        return null;
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
    public void assignIpv4SubnetToNetwork(String cidr, long networkId) {
        Ipv4GuestSubnetNetworkMapVO subnetMap = ipv4GuestSubnetNetworkMapDao.findBySubnet(cidr);
        if (subnetMap != null) {
            if (networkId > 0L) {
                subnetMap.setNetworkId(networkId);
            }
            subnetMap.setState(State.Allocated);
            subnetMap.setAllocated(new Date());
            ipv4GuestSubnetNetworkMapDao.update(subnetMap.getId(), subnetMap);
        }
    }

    @Override
    public void assignIpv4SubnetToVpc(String cidr, long vpcId) {
        Ipv4GuestSubnetNetworkMapVO subnetMap = ipv4GuestSubnetNetworkMapDao.findBySubnet(cidr);
        if (subnetMap != null) {
            if (vpcId > 0L) {
                subnetMap.setVpcId(vpcId);
            }
            subnetMap.setState(State.Allocated);
            subnetMap.setAllocated(new Date());
            ipv4GuestSubnetNetworkMapDao.update(subnetMap.getId(), subnetMap);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROUTING_IPV4_FIREWALL_RULE_CREATE,
            eventDescription = "creating routing firewall rule", async = true)
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
            eventDescription = "updating routing firewall rule", async = true)
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
    public boolean isRoutedVpc(Vpc vpc) {
        return NetworkOffering.NetworkMode.ROUTED.equals(vpcOfferingDao.findById(vpc.getVpcOfferingId()).getNetworkMode());
    }

    @Override
    public boolean isVpcVirtualRouterGateway(VpcOffering vpcOffering) {
        return NetworkOffering.NetworkMode.ROUTED.equals(vpcOffering.getNetworkMode())
                && vpcOfferingServiceMapDao.findByServiceProviderAndOfferingId(Service.Gateway.getName(), Provider.VPCVirtualRouter.getName(), vpcOffering.getId()) != null;
    }
}
