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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.admin.ipv6.CreateIpv6RangeCmd;
import org.apache.cloudstack.api.command.admin.ipv6.DedicateIpv6RangeCmd;
import org.apache.cloudstack.api.command.admin.ipv6.DeleteIpv6RangeCmd;
import org.apache.cloudstack.api.command.admin.ipv6.ListIpv6RangesCmd;
import org.apache.cloudstack.api.command.admin.ipv6.ReleaseIpv6RangeCmd;
import org.apache.cloudstack.api.command.admin.ipv6.UpdateIpv6RangeCmd;
import org.apache.cloudstack.api.command.user.ipv6.CreateIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.command.user.ipv6.DeleteIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.command.user.ipv6.ListIpv6FirewallRulesCmd;
import org.apache.cloudstack.api.command.user.ipv6.UpdateIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.response.Ipv6RangeResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterIpv6AddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterIpv6AddressDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Ipv6Address.IPv6Routing;
import com.cloud.network.Ipv6Address.InternetProtocol;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.vpc.VpcVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.projects.Project;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.googlecode.ipv6.IPv6Address;

public class Ipv6ServiceImpl implements Ipv6Service, PluggableService, Configurable {

    public static final Logger s_logger = Logger.getLogger(Ipv6ServiceImpl.class.getName());

    @Inject
    AccountManager accountManager;
    @Inject
    DataCenterIpv6AddressDao ipv6AddressDao;
    @Inject
    DomainDao domainDao;
    @Inject
    NetworkOfferingDetailsDao networkOfferingDetailsDao;
    @Inject
    FirewallRulesDao firewallRulesDao;
    @Inject
    FirewallService firewallservice;

    @Override
    public Ipv6Address createIpv6Range(CreateIpv6RangeCmd cmd) {
        // check: TODO
        DataCenterIpv6AddressVO range = ipv6AddressDao.addIpRange(cmd.getZoneId(), cmd.getPhysicalNetworkId(), cmd.getIp6Gateway(), cmd.getIp6Cidr(),
                cmd.getRouterIpv6(), cmd.getRouterIpv6Gateway(), cmd.getRouterIpv6Vlan());
        return range;
    }

    @Override
    public Ipv6Address updateIpv6Range(UpdateIpv6RangeCmd cmd) {
        // check: TODO
        Ipv6Address range = ipv6AddressDao.findById(cmd.getId());
        if (range != null && range.getNetworkId() != null) {
            throw new InvalidParameterValueException("Cannot update this IPv6 range as it is currently in use");
        }

        if (ipv6AddressDao.updateIpRange(cmd.getId(), cmd.getIp6Gateway(), cmd.getIp6Cidr(), cmd.getRouterIpv6(), cmd.getRouterIpv6Gateway(), cmd.getRouterIpv6Vlan())) {
            return ipv6AddressDao.findById(cmd.getId());
        }
        return null;
    }

    @Override
    public boolean deleteIpv6Range(DeleteIpv6RangeCmd cmd) {
        // check: TODO
        Ipv6Address range = ipv6AddressDao.findById(cmd.getId());
        if (range != null && range.getNetworkId() != null) {
            throw new InvalidParameterValueException("Cannot remove this IPv6 range as it is currently in use");
        }
        return ipv6AddressDao.removeIpv6Range(cmd.getId());
    }

    @Override
    public Ipv6Address dedicateIpv6Range(DedicateIpv6RangeCmd cmd) {
        // check: TODO
        Long accountId = accountManager.finalyzeAccountId(cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId(), true);
        if (ipv6AddressDao.dedicateIpv6Range(cmd.getId(), cmd.getDomainId(), accountId)) {
            return ipv6AddressDao.findById(cmd.getId());
        }
        return null;
    }

    @Override
    public boolean releaseIpv6Range(ReleaseIpv6RangeCmd cmd) {
        // check: TODO
        return ipv6AddressDao.releaseIpv6Range(cmd.getId());
    }

    @Override
    public Ipv6Address takeIpv6Range(long zoneId, boolean isRouterIpv6Null) {
        return ipv6AddressDao.takeIpv6Range(zoneId, isRouterIpv6Null);
    }

    @Override
    public Pair<List<? extends Ipv6Address>, Integer> searchForIpv6Range(ListIpv6RangesCmd cmd) {
        final Long id = cmd.getId();
        final Long zoneId = cmd.getZoneId();
        final Long physicalNetworkId = cmd.getPhysicalNetworkId();
        final Long networkId = cmd.getNetworkId();

        final Account caller = CallContext.current().getCallingAccount();
        final List<Long> permittedAccounts = new ArrayList<>();
        final Ternary<Long, Boolean, Project.ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(cmd.getDomainId(), cmd.isRecursive(),null);
        accountManager.buildACLSearchParameters(caller, cmd.getId(), cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        final Long domainId = domainIdRecursiveListProject.first();
        final Boolean isRecursive = domainIdRecursiveListProject.second();
        final Project.ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        final SearchBuilder<DataCenterIpv6AddressVO> sb = ipv6AddressDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("physicalNetworkId", sb.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        sb.and("networkId", sb.entity().getNetworkId(), SearchCriteria.Op.EQ);
        sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        if ((permittedAccounts.isEmpty() && domainId != null && isRecursive)) {
            // if accountId isn't specified, we can do a domain match for the admin case if isRecursive is true
            SearchBuilder<DomainVO> domainSearch = domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        Filter searchFilter = new Filter(DataCenterIpv6AddressVO.class,"id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        final SearchCriteria<DataCenterIpv6AddressVO> sc = sb.create();

        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountIdIN", permittedAccounts.toArray());
        } else if (domainId != null) {
            DomainVO domain = domainDao.findById(domainId);
            if (isRecursive) {
                sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            } else {
                sc.setParameters("domainId", domainId);
            }
        }

        if (id != null) {
            sc.setParameters("id", id);
        }
        if (zoneId != null) {
            sc.setParameters("zoneId", zoneId);
        }
        if (physicalNetworkId != null) {
            sc.setParameters("physicalNetworkId", physicalNetworkId);
        }
        if (networkId != null) {
            sc.setParameters("networkId", networkId);
        }
        final Pair<List<DataCenterIpv6AddressVO>, Integer> result = ipv6AddressDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Ipv6Address>, Integer>(result.first(), result.second());
    }

    @Override
    public Ipv6RangeResponse createIpv6RangeResponse(Ipv6Address address) {
        Ipv6RangeResponse response = new Ipv6RangeResponse();

        response.setId(address.getUuid());
        response.setIp6Gateway(address.getIp6Gateway());
        response.setIp6Cidr(address.getIp6Cidr());
        response.setRouterIpv6(address.getRouterIpv6());
        response.setRouterIpv6Gateway(address.getRouterIpv6Gateway());
        response.setRouterIpv6Vlan(address.getRouterIpv6Vlan());

        DataCenterVO dc = ApiDBUtils.findZoneById(address.getDataCenterId());
        response.setZoneId(dc.getUuid());
        response.setZoneName(dc.getName());

        PhysicalNetworkVO physicalNetwork = ApiDBUtils.findPhysicalNetworkById(address.getPhysicalNetworkId());
        response.setPhysicalNetworkId(physicalNetwork.getUuid());

        if (address.getNetworkId() != null) {
            NetworkVO network = ApiDBUtils.findNetworkById(address.getNetworkId());
            if (network != null) {
                response.setNetworkId(network.getUuid());
                response.setNetworkName(network.getName());
                if (network.getVpcId() != null) {
                    VpcVO vpc = ApiDBUtils.findVpcById(network.getVpcId());
                    if (vpc != null) {
                        response.setVpcId(vpc.getUuid());
                        response.setVpcName(vpc.getName());
                    }
                }
            }
        }

        if (address.getAccountId() != null) {
            Account account = ApiDBUtils.findAccountById(address.getAccountId());
            if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                // find the project
                Project project = ApiDBUtils.findProjectByProjectAccountId(account.getId());
                response.setProjectId(project.getUuid());
                response.setProjectName(project.getName());
            } else {
                response.setAccountName(account.getAccountName());
            }
        }
        if (address.getDomainId() != null) {
            DomainVO domain = ApiDBUtils.findDomainById(address.getDomainId());
            if (domain != null) {
                response.setDomainId(domain.getUuid());
                response.setDomainName(domain.getName());
            }
        }

        response.setObjectName("ipv6range");
        return response;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateIpv6RangeCmd.class);
        cmdList.add(DeleteIpv6RangeCmd.class);
        cmdList.add(UpdateIpv6RangeCmd.class);
        cmdList.add(ListIpv6RangesCmd.class);
        cmdList.add(DeleteIpv6RangeCmd.class);
        cmdList.add(ReleaseIpv6RangeCmd.class);
        cmdList.add(CreateIpv6FirewallRuleCmd.class);
        cmdList.add(ListIpv6FirewallRulesCmd.class);
        cmdList.add(UpdateIpv6FirewallRuleCmd.class);
        cmdList.add(DeleteIpv6FirewallRuleCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return Ipv6Service.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] { };
    }

    @Override
    public InternetProtocol getNetworkOfferingInternetProtocol(Long offeringId) {
        String internetProtocolStr = networkOfferingDetailsDao.getDetail(offeringId, NetworkOffering.Detail.internetProtocol);
        InternetProtocol internetProtocol = InternetProtocol.fromValue(internetProtocolStr);
        return internetProtocol;
    }

    @Override
    public IPv6Routing getNetworkOfferingIpv6Routing(Long offeringId) {
        String ipv6RoutingStr = networkOfferingDetailsDao.getDetail(offeringId, NetworkOffering.Detail.ipv6Routing);
        IPv6Routing ipv6Routing = IPv6Routing.fromValue(ipv6RoutingStr);
        return ipv6Routing;
    }

    @Override
    public boolean isIpv6Supported(Long offeringId) {
        InternetProtocol internetProtocol = getNetworkOfferingInternetProtocol(offeringId);
        if (InternetProtocol.IPv6.equals(internetProtocol) || InternetProtocol.DualStack.equals(internetProtocol)) {
            return true;
        }
        return false;
    }

    @Override
    public Boolean isIpv6FirewallEnabled(Long offeringId) {
        String ipv6FirewallStr = networkOfferingDetailsDao.getDetail(offeringId, NetworkOffering.Detail.ipv6Firewall);
        return Boolean.parseBoolean(ipv6FirewallStr);
    }

    @Override
    public Boolean isSpecifyRouterIpv6(Long offeringId) {
        String specifyRouterIpv6 = networkOfferingDetailsDao.getDetail(offeringId, NetworkOffering.Detail.specifyRouterIpv6);
        return Boolean.parseBoolean(specifyRouterIpv6);
    }

    @Override
    public void updateNicIpv6(NicProfile nic, DataCenter dc, Network network) {
        boolean isIpv6Supported = isIpv6Supported(network.getNetworkOfferingId());
        if (nic.getIPv6Address() == null && isIpv6Supported) {
            final DataCenterIpv6AddressVO range = ipv6AddressDao.getIpv6RangeByNetwork(network.getId());
            if (range == null) {
                throw new CloudRuntimeException(String.format("Cannot find IPv6 range for network %s", network.getName()));
            }
            final String routerIpv6Gateway = range.getRouterIpv6Gateway();
            if (routerIpv6Gateway == null) {
                throw new CloudRuntimeException(String.format("Invalid routerIpv6Gateway for network %s", network.getName()));
            }
            final String routerIpv6 = ipv6AddressDao.getRouterIpv6(range);
            if (routerIpv6 == null) {
                final String routerIpv6Prefix = getRouterIpv6Prefix(routerIpv6Gateway);
                IPv6Address ipv6addr = NetUtils.EUI64Address(routerIpv6Prefix + Ipv6Service.IPV6_CIDR_SUFFIX, nic.getMacAddress());
                s_logger.info("Calculated IPv6 address " + ipv6addr + " using EUI-64 for NIC " + nic.getUuid());
                nic.setIPv6Address(ipv6addr.toString());
                nic.setIPv6Cidr(routerIpv6Prefix + Ipv6Service.IPV6_CIDR_SUFFIX);
                nic.setIPv6Gateway(routerIpv6Gateway);
                ipv6AddressDao.updateRouterIpv6ByNetwork(network.getId(), ipv6addr.toString());
            } else {
                nic.setIPv6Address(routerIpv6);
                nic.setIPv6Cidr(routerIpv6 + Ipv6Service.IPV6_CIDR_SUFFIX);
                nic.setIPv6Gateway(routerIpv6Gateway);
            }
            if (nic.getIPv4Address() != null) {
                nic.setFormat(Networks.AddressFormat.DualStack);
            } else {
                nic.setFormat(Networks.AddressFormat.Ip6);
            }
            nic.setIPv6Dns1(dc.getIp6Dns1());
            nic.setIPv6Dns2(dc.getIp6Dns2());
        }
    }

    @Override
    public FirewallRule updateIpv6FirewallRule(UpdateIpv6FirewallRuleCmd updateIpv6FirewallRuleCmd) {
        // TODO
        return firewallRulesDao.findById(updateIpv6FirewallRuleCmd.getId());
    }

    @Override
    public Pair<List<? extends FirewallRule>, Integer> listIpv6FirewallRules(ListIpv6FirewallRulesCmd listIpv6FirewallRulesCmd) {
        return firewallservice.listFirewallRules(listIpv6FirewallRulesCmd);
    }

    @Override
    public boolean revokeIpv6FirewallRule(Long id) {
        // TODO
        return true;
    }

    @Override
    public FirewallRule createIpv6FirewallRule(CreateIpv6FirewallRuleCmd createIpv6FirewallRuleCmd) {
        return null;
    }

    @Override
    public FirewallRule getIpv6FirewallRule(Long entityId) {
        return firewallRulesDao.findById(entityId);
    }

    @Override
    public boolean applyIpv6FirewallRule(long id) {
        return false;
    }

    @Override
    public String getRouterIpv6Prefix(String routerIpv6Gateway) {
        if (routerIpv6Gateway == null) {
            return null;
        }
        return routerIpv6Gateway.split("::")[0] + "::";
    }

}
