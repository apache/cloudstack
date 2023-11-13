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
package org.apache.cloudstack.storage.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.cloud.dc.DataCenter;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.VNF;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.RulesService;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.SecurityGroupService;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.security.SecurityRule;
import com.cloud.storage.VnfTemplateDetailVO;
import com.cloud.storage.VnfTemplateNicVO;
import com.cloud.storage.dao.VnfTemplateDetailsDao;
import com.cloud.storage.dao.VnfTemplateNicDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.NicDao;
import org.apache.cloudstack.api.command.admin.template.ListVnfTemplatesCmdByAdmin;
import org.apache.cloudstack.api.command.admin.template.RegisterVnfTemplateCmdByAdmin;
import org.apache.cloudstack.api.command.admin.template.UpdateVnfTemplateCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.DeployVnfApplianceCmdByAdmin;
import org.apache.cloudstack.api.command.user.template.DeleteVnfTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ListVnfTemplatesCmd;
import org.apache.cloudstack.api.command.user.template.RegisterVnfTemplateCmd;
import org.apache.cloudstack.api.command.user.template.UpdateVnfTemplateCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVnfApplianceCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;


public class VnfTemplateManagerImpl extends ManagerBase implements VnfTemplateManager, PluggableService, Configurable {

    static final Logger LOGGER = Logger.getLogger(VnfTemplateManagerImpl.class);

    public static final String VNF_SECURITY_GROUP_NAME = "VNF_SecurityGroup_";
    public static final String ACCESS_METHOD_SEPARATOR = ",";
    public static final Integer ACCESS_DEFAULT_SSH_PORT = 22;
    public static final Integer ACCESS_DEFAULT_HTTP_PORT = 80;
    public static final Integer ACCESS_DEFAULT_HTTPS_PORT = 443;

    @Inject
    VnfTemplateDetailsDao vnfTemplateDetailsDao;
    @Inject
    VnfTemplateNicDao vnfTemplateNicDao;
    @Inject
    SecurityGroupManager securityGroupManager;
    @Inject
    SecurityGroupService securityGroupService;
    @Inject
    NetworkModel networkModel;
    @Inject
    IpAddressManager ipAddressManager;
    @Inject
    NicDao nicDao;
    @Inject
    NetworkDao networkDao;
    @Inject
    NetworkService networkService;
    @Inject
    RulesService rulesService;
    @Inject
    FirewallRulesDao firewallRulesDao;
    @Inject
    FirewallService firewallService;

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        if (!VnfTemplateAndApplianceEnabled.value()) {
            return cmdList;
        }
        cmdList.add(RegisterVnfTemplateCmd.class);
        cmdList.add(RegisterVnfTemplateCmdByAdmin.class);
        cmdList.add(ListVnfTemplatesCmd.class);
        cmdList.add(ListVnfTemplatesCmdByAdmin.class);
        cmdList.add(UpdateVnfTemplateCmd.class);
        cmdList.add(UpdateVnfTemplateCmdByAdmin.class);
        cmdList.add(DeleteVnfTemplateCmd.class);
        cmdList.add(DeployVnfApplianceCmd.class);
        cmdList.add(DeployVnfApplianceCmdByAdmin.class);
        return cmdList;
    }

    @Override
    public void persistVnfTemplate(long templateId, RegisterVnfTemplateCmd cmd) {
        persistVnfTemplateNics(templateId, cmd.getVnfNics());
        persistVnfTemplateDetails(templateId, cmd);
    }

    private void persistVnfTemplateNics(long templateId, List<VNF.VnfNic> nics) {
        for (VNF.VnfNic nic : nics) {
            VnfTemplateNicVO vnfTemplateNicVO = new VnfTemplateNicVO(templateId, nic.getDeviceId(), nic.getName(), nic.isRequired(), nic.isManagement(), nic.getDescription());
            vnfTemplateNicDao.persist(vnfTemplateNicVO);
        }
    }

    private void persistVnfTemplateDetails(long templateId, RegisterVnfTemplateCmd cmd) {
        persistVnfTemplateDetails(templateId, cmd.getVnfDetails());
    }

    private void persistVnfTemplateDetails(long templateId, Map<String, String> vnfDetails) {
        for (Map.Entry<String, String> entry:  vnfDetails.entrySet()) {
            String value = entry.getValue();
            if (VNF.AccessDetail.ACCESS_METHODS.name().equalsIgnoreCase(entry.getKey())) {
                value = Arrays.stream(value.split(ACCESS_METHOD_SEPARATOR)).sorted().collect(Collectors.joining(ACCESS_METHOD_SEPARATOR));
            }
            vnfTemplateDetailsDao.addDetail(templateId, entry.getKey().toLowerCase(), value, true);
        }
    }

    @Override
    public void updateVnfTemplate(long templateId, UpdateVnfTemplateCmd cmd) {
        updateVnfTemplateDetails(templateId, cmd);
        updateVnfTemplateNics(templateId, cmd);
    }

    private void updateVnfTemplateDetails(long templateId, UpdateVnfTemplateCmd cmd) {
        boolean cleanupVnfDetails = cmd.isCleanupVnfDetails();
        if (cleanupVnfDetails) {
            vnfTemplateDetailsDao.removeDetails(templateId);
        } else if (MapUtils.isNotEmpty(cmd.getVnfDetails())) {
            vnfTemplateDetailsDao.removeDetails(templateId);
            persistVnfTemplateDetails(templateId, cmd.getVnfDetails());
        }
    }

    private void updateVnfTemplateNics(long templateId, UpdateVnfTemplateCmd cmd) {
        boolean cleanupVnfNics = cmd.isCleanupVnfNics();
        if (cleanupVnfNics) {
            vnfTemplateNicDao.deleteByTemplateId(templateId);
        } else if (CollectionUtils.isNotEmpty(cmd.getVnfNics())) {
            vnfTemplateNicDao.deleteByTemplateId(templateId);
            persistVnfTemplateNics(templateId, cmd.getVnfNics());
        }
    }

    @Override
    public String getConfigComponentName() {
        return VnfTemplateManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] { VnfTemplateAndApplianceEnabled };
    }

    @Override
    public void validateVnfApplianceNics(VirtualMachineTemplate template, List<Long> networkIds) {
        if (CollectionUtils.isEmpty(networkIds)) {
            throw new InvalidParameterValueException("VNF nics list is empty");
        }
        List<VnfTemplateNicVO> vnfNics = vnfTemplateNicDao.listByTemplateId(template.getId());
        for (VnfTemplateNicVO vnfNic : vnfNics) {
            if (vnfNic.isRequired() && networkIds.size() <= vnfNic.getDeviceId()) {
                throw new InvalidParameterValueException("VNF nic is required but not found: " + vnfNic);
            }
        }
    }

    protected Set<Integer> getOpenPortsForVnfAppliance(VirtualMachineTemplate template) {
        Set<Integer> ports = new HashSet<>();
        VnfTemplateDetailVO accessMethodsDetail = vnfTemplateDetailsDao.findDetail(template.getId(), VNF.AccessDetail.ACCESS_METHODS.name().toLowerCase());
        if (accessMethodsDetail == null || accessMethodsDetail.getValue() == null) {
            return ports;
        }
        String[] accessMethods = accessMethodsDetail.getValue().split(ACCESS_METHOD_SEPARATOR);
        for (String accessMethod : accessMethods) {
            if (VNF.AccessMethod.SSH_WITH_KEY.toString().equalsIgnoreCase(accessMethod)
                    || VNF.AccessMethod.SSH_WITH_PASSWORD.toString().equalsIgnoreCase(accessMethod)) {
                VnfTemplateDetailVO accessDetail = vnfTemplateDetailsDao.findDetail(template.getId(), VNF.AccessDetail.SSH_PORT.name().toLowerCase());
                if (accessDetail == null) {
                    ports.add(ACCESS_DEFAULT_SSH_PORT);
                } else {
                    ports.add(NumbersUtil.parseInt(accessDetail.getValue(), ACCESS_DEFAULT_SSH_PORT));
                }
            } else if (VNF.AccessMethod.HTTP.toString().equalsIgnoreCase(accessMethod)) {
                VnfTemplateDetailVO accessDetail = vnfTemplateDetailsDao.findDetail(template.getId(), VNF.AccessDetail.HTTP_PORT.name().toLowerCase());
                if (accessDetail == null) {
                    ports.add(ACCESS_DEFAULT_HTTP_PORT);
                } else {
                    ports.add(NumbersUtil.parseInt(accessDetail.getValue(), ACCESS_DEFAULT_HTTP_PORT));
                }
            } else if (VNF.AccessMethod.HTTPS.toString().equalsIgnoreCase(accessMethod)) {
                VnfTemplateDetailVO accessDetail = vnfTemplateDetailsDao.findDetail(template.getId(), VNF.AccessDetail.HTTPS_PORT.name().toLowerCase());
                if (accessDetail == null) {
                    ports.add(ACCESS_DEFAULT_HTTPS_PORT);
                } else {
                    ports.add(NumbersUtil.parseInt(accessDetail.getValue(), ACCESS_DEFAULT_HTTPS_PORT));
                }
            }
        }
        return ports;
    }

    private Set<Long> getDeviceIdsOfVnfManagementNics(VirtualMachineTemplate template) {
        Set<Long> deviceIds = new HashSet<>();
        for (VnfTemplateNicVO nic : vnfTemplateNicDao.listByTemplateId(template.getId())) {
            if (nic.isManagement()) {
                deviceIds.add(nic.getDeviceId());
            }
        }
        return deviceIds;
    }

    protected Map<Network, String> getManagementNetworkAndIp(VirtualMachineTemplate template, UserVm vm) {
        Map<Network, String> networkAndIpMap = new HashMap<>();
        Set<Long> managementDeviceIds = getDeviceIdsOfVnfManagementNics(template);
        for (NicVO nic : nicDao.listByVmId(vm.getId())) {
            if (managementDeviceIds.contains((long) nic.getDeviceId()) && nic.getIPv4Address() != null) {
                Network network = networkDao.findById(nic.getNetworkId());
                if (network == null || !Network.GuestType.Isolated.equals(network.getGuestType())) {
                    continue;
                }
                if (!networkModel.areServicesSupportedInNetwork(network.getId(), Network.Service.StaticNat)) {
                    LOGGER.info(String.format("Network ID: %s does not support static nat, " +
                            "skipping this network configuration for VNF appliance", network.getUuid()));
                    continue;
                }
                if (network.getVpcId() != null) {
                    LOGGER.info(String.format("Network ID: %s is a VPC tier, " +
                            "skipping this network configuration for VNF appliance", network.getUuid()));
                    continue;
                }
                if (!networkModel.areServicesSupportedInNetwork(network.getId(), Network.Service.Firewall)) {
                    LOGGER.info(String.format("Network ID: %s does not support firewall, " +
                            "skipping this network configuration for VNF appliance", network.getUuid()));
                    continue;
                }
                networkAndIpMap.put(network, nic.getIPv4Address());
            }
        }
        return networkAndIpMap;
    }

    @Override
    public SecurityGroup createSecurityGroupForVnfAppliance(DataCenter zone, VirtualMachineTemplate template, Account owner,
                                                            DeployVnfApplianceCmd cmd) {
        if (zone == null || !zone.isSecurityGroupEnabled()) {
            return null;
        }
        if (!cmd.getVnfConfigureManagement()) {
            return null;
        }
        LOGGER.debug("Creating security group and rules for VNF appliance");
        Set<Integer> ports = getOpenPortsForVnfAppliance(template);
        if (ports.size() == 0) {
            LOGGER.debug("No need to create security group and rules for VNF appliance as there is no ports to be open");
            return null;
        }
        String securityGroupName = VNF_SECURITY_GROUP_NAME.concat(Long.toHexString(System.currentTimeMillis()));
        SecurityGroupVO securityGroupVO = securityGroupManager.createSecurityGroup(securityGroupName,
                "Security group for VNF appliance", owner.getDomainId(), owner.getId(), owner.getAccountName());
        if (securityGroupVO == null) {
            throw new CloudRuntimeException(String.format("Failed to create security group: %s", securityGroupName));
        }
        List<String> cidrList = cmd.getVnfCidrlist();
        for (Integer port : ports) {
            securityGroupService.authorizeSecurityGroupRule(securityGroupVO.getId(), NetUtils.TCP_PROTO, port, port,
                    null, null, cidrList, null, SecurityRule.SecurityRuleType.IngressRule);
        }
        return securityGroupVO;
    }

    @Override
    public void createIsolatedNetworkRulesForVnfAppliance(DataCenter zone, VirtualMachineTemplate template, Account owner,
                                                          UserVm vm, DeployVnfApplianceCmd cmd)
            throws InsufficientAddressCapacityException, ResourceAllocationException, ResourceUnavailableException {

        Map<Network, String> networkAndIpMap = getManagementNetworkAndIp(template, vm);
        Set<Integer> ports = getOpenPortsForVnfAppliance(template);
        for (Map.Entry<Network, String> entry : networkAndIpMap.entrySet()) {
            Network network = entry.getKey();
            LOGGER.debug("Creating network rules for VNF appliance on isolated network " + network.getUuid());
            String ip = entry.getValue();
            IpAddress publicIp = networkService.allocateIP(owner, zone.getId(), network.getId(), null, null);
            if (publicIp == null) {
                continue;
            }
            publicIp = ipAddressManager.associateIPToGuestNetwork(publicIp.getId(), network.getId(), false);
            if (publicIp.isSourceNat()) {
                // If isolated network is not implemented, the first acquired Public IP will be Source NAT IP
                publicIp = networkService.allocateIP(owner, zone.getId(), network.getId(), null, null);
                if (publicIp == null) {
                    continue;
                }
                publicIp = ipAddressManager.associateIPToGuestNetwork(publicIp.getId(), network.getId(), false);
            }
            final IpAddress publicIpFinal = publicIp;
            final List<String> cidrList = cmd.getVnfCidrlist();
            try {
                boolean result = rulesService.enableStaticNat(publicIp.getId(), vm.getId(), network.getId(), ip);
                if (!result) {
                    throw new CloudRuntimeException(String.format("Failed to create static nat for vm: %s", vm.getUuid()));
                }
            } catch (NetworkRuleConflictException e) {
                throw new CloudRuntimeException(String.format("Failed to create static nat for vm %s due to %s", vm.getUuid(), e.getMessage()));
            }
            if (network.getVpcId() == null) {
                Transaction.execute(new TransactionCallbackWithExceptionNoReturn<>() {
                    @Override
                    public void doInTransactionWithoutResult(final TransactionStatus status) throws CloudRuntimeException {
                        for (Integer port : ports) {
                            FirewallRuleVO newFirewallRule = new FirewallRuleVO(null, publicIpFinal.getId(), port, port, NetUtils.TCP_PROTO,
                                    network.getId(), owner.getAccountId(), owner.getDomainId(), FirewallRule.Purpose.Firewall,
                                    cidrList, null, null, null, FirewallRule.TrafficType.Ingress);
                            newFirewallRule.setDisplay(true);
                            newFirewallRule.setState(FirewallRule.State.Add);
                            firewallRulesDao.persist(newFirewallRule);
                        }
                    }
                });
                firewallService.applyIngressFwRules(publicIp.getId(), owner);
            }
            LOGGER.debug("Created network rules for VNF appliance on isolated network " + network.getUuid());
        }
    }
}
