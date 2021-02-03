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
package org.apache.cloudstack.api.command.user.vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.cloud.utils.StringUtils;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.LogLevel;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.IpAddresses;
import com.cloud.offering.DiskOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.uservm.UserVm;
import com.cloud.utils.net.Dhcp;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDetailConstants;
import com.google.common.base.Strings;

@APICommand(name = "deployVirtualMachine", description = "Creates and automatically starts a virtual machine based on a service offering, disk offering, and template.", responseObject = UserVmResponse.class, responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class DeployVMCmd extends BaseAsyncCreateCustomIdCmd implements SecurityGroupAction, UserCmd {
    public static final Logger s_logger = Logger.getLogger(DeployVMCmd.class.getName());

    private static final String s_name = "deployvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "availability zone for the virtual machine")
    private Long zoneId;

    @ACL
    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID, type = CommandType.UUID, entityType = ServiceOfferingResponse.class, required = true, description = "the ID of the service offering for the virtual machine")
    private Long serviceOfferingId;

    @ACL
    @Parameter(name = ApiConstants.TEMPLATE_ID, type = CommandType.UUID, entityType = TemplateResponse.class, required = true, description = "the ID of the template for the virtual machine")
    private Long templateId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "host name for the virtual machine")
    private String name;

    @Parameter(name = ApiConstants.DISPLAY_NAME, type = CommandType.STRING, description = "an optional user generated name for the virtual machine")
    private String displayName;

    //Owner information
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an optional account for the virtual machine. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "an optional domainId for the virtual machine. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    //Network information
    //@ACL(accessType = AccessType.UseEntry)
    @Parameter(name = ApiConstants.NETWORK_IDS, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = NetworkResponse.class, description = "list of network ids used by virtual machine. Can't be specified with ipToNetworkList parameter")
    private List<Long> networkIds;

    @Parameter(name = ApiConstants.BOOT_TYPE, type = CommandType.STRING, required = false, description = "Guest VM Boot option either custom[UEFI] or default boot [BIOS]. Not applicable with VMware, as we honour what is defined in the template.", since = "4.14.0.0")
    private String bootType;

    @Parameter(name = ApiConstants.BOOT_MODE, type = CommandType.STRING, required = false, description = "Boot Mode [Legacy] or [Secure] Applicable when Boot Type Selected is UEFI, otherwise Legacy only for BIOS. Not applicable with VMware, as we honour what is defined in the template.", since = "4.14.0.0")
    private String bootMode;

    @Parameter(name = ApiConstants.BOOT_INTO_SETUP, type = CommandType.BOOLEAN, required = false, description = "Boot into hardware setup or not (ignored if startVm = false, only valid for vmware)", since = "4.15.0.0")
    private Boolean bootIntoSetup;

    //DataDisk information
    @ACL
    @Parameter(name = ApiConstants.DISK_OFFERING_ID, type = CommandType.UUID, entityType = DiskOfferingResponse.class, description = "the ID of the disk offering for the virtual machine. If the template is of ISO format,"
            + " the diskOfferingId is for the root disk volume. Otherwise this parameter is used to indicate the "
            + "offering for the data disk volume. If the templateId parameter passed is from a Template object,"
            + " the diskOfferingId refers to a DATA Disk Volume created. If the templateId parameter passed is "
            + "from an ISO object, the diskOfferingId refers to a ROOT Disk Volume created.")
    private Long diskOfferingId;

    @Parameter(name = ApiConstants.SIZE, type = CommandType.LONG, description = "the arbitrary size for the DATADISK volume. Mutually exclusive with diskOfferingId")
    private Long size;

    @Parameter(name = ApiConstants.ROOT_DISK_SIZE,
            type = CommandType.LONG,
            description = "Optional field to resize root disk on deploy. Value is in GB. Only applies to template-based deployments. Analogous to details[0].rootdisksize, which takes precedence over this parameter if both are provided",
            since = "4.4")
    private Long rootdisksize;

    @Parameter(name = ApiConstants.GROUP, type = CommandType.STRING, description = "an optional group for the virtual machine")
    private String group;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, description = "the hypervisor on which to deploy the virtual machine. "
            + "The parameter is required and respected only when hypervisor info is not set on the ISO/Template passed to the call")
    private String hypervisor;

    @Parameter(name = ApiConstants.USER_DATA, type = CommandType.STRING, description = "an optional binary data that can be sent to the virtual machine upon a successful deployment. This binary data must be base64 encoded before adding it to the request. Using HTTP GET (via querystring), you can send up to 2KB of data after base64 encoding. Using HTTP POST(via POST body), you can send up to 32K of data after base64 encoding.", length = 32768)
    private String userData;

    @Parameter(name = ApiConstants.SSH_KEYPAIR, type = CommandType.STRING, description = "name of the ssh key pair used to login to the virtual machine")
    private String sshKeyPairName;

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, description = "destination Host ID to deploy the VM to - parameter available for root admin only")
    private Long hostId;

    @ACL
    @Parameter(name = ApiConstants.SECURITY_GROUP_IDS, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = SecurityGroupResponse.class, description = "comma separated list of security groups id that going to be applied to the virtual machine. "
            + "Should be passed only when vm is created from a zone with Basic Network support." + " Mutually exclusive with securitygroupnames parameter")
    private List<Long> securityGroupIdList;

    @ACL
    @Parameter(name = ApiConstants.SECURITY_GROUP_NAMES, type = CommandType.LIST, collectionType = CommandType.STRING, entityType = SecurityGroupResponse.class, description = "comma separated list of security groups names that going to be applied to the virtual machine."
            + " Should be passed only when vm is created from a zone with Basic Network support. " + "Mutually exclusive with securitygroupids parameter")
    private List<String> securityGroupNameList;

    @Parameter(name = ApiConstants.IP_NETWORK_LIST, type = CommandType.MAP, description = "ip to network mapping. Can't be specified with networkIds parameter."
            + " Example: iptonetworklist[0].ip=10.10.10.11&iptonetworklist[0].ipv6=fc00:1234:5678::abcd&iptonetworklist[0].networkid=uuid&iptonetworklist[0].mac=aa:bb:cc:dd:ee::ff - requests to use ip 10.10.10.11 in network id=uuid")
    private Map ipToNetworkList;

    @Parameter(name = ApiConstants.IP_ADDRESS, type = CommandType.STRING, description = "the ip address for default vm's network")
    private String ipAddress;

    @Parameter(name = ApiConstants.IP6_ADDRESS, type = CommandType.STRING, description = "the ipv6 address for default vm's network")
    private String ip6Address;

    @Parameter(name = ApiConstants.MAC_ADDRESS, type = CommandType.STRING, description = "the mac address for default vm's network")
    private String macAddress;

    @Parameter(name = ApiConstants.KEYBOARD, type = CommandType.STRING, description = "an optional keyboard device type for the virtual machine. valid value can be one of de,de-ch,es,fi,fr,fr-be,fr-ch,is,it,jp,nl-be,no,pt,uk,us")
    private String keyboard;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "Deploy vm for the project")
    private Long projectId;

    @Parameter(name = ApiConstants.START_VM, type = CommandType.BOOLEAN, description = "true if start vm after creating; defaulted to true if not specified")
    private Boolean startVm;

    @ACL
    @Parameter(name = ApiConstants.AFFINITY_GROUP_IDS, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = AffinityGroupResponse.class, description = "comma separated list of affinity groups id that are going to be applied to the virtual machine."
            + " Mutually exclusive with affinitygroupnames parameter")
    private List<Long> affinityGroupIdList;

    @ACL
    @Parameter(name = ApiConstants.AFFINITY_GROUP_NAMES, type = CommandType.LIST, collectionType = CommandType.STRING, entityType = AffinityGroupResponse.class, description = "comma separated list of affinity groups names that are going to be applied to the virtual machine."
            + "Mutually exclusive with affinitygroupids parameter")
    private List<String> affinityGroupNameList;

    @Parameter(name = ApiConstants.DISPLAY_VM, type = CommandType.BOOLEAN, since = "4.2", description = "an optional field, whether to the display the vm to the end user or not.", authorized = {RoleType.Admin})
    private Boolean displayVm;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, since = "4.3", description = "used to specify the custom parameters. 'extraconfig' is not allowed to be passed in details")
    private Map details;

    @Parameter(name = ApiConstants.DEPLOYMENT_PLANNER, type = CommandType.STRING, description = "Deployment planner to use for vm allocation. Available to ROOT admin only", since = "4.4", authorized = { RoleType.Admin })
    private String deploymentPlanner;

    @Parameter(name = ApiConstants.DHCP_OPTIONS_NETWORK_LIST, type = CommandType.MAP, description = "DHCP options which are passed to the VM on start up"
            + " Example: dhcpoptionsnetworklist[0].dhcp:114=url&dhcpoptionsetworklist[0].networkid=networkid&dhcpoptionsetworklist[0].dhcp:66=www.test.com")
    private Map dhcpOptionsNetworkList;

    @Parameter(name = ApiConstants.DATADISK_OFFERING_LIST, type = CommandType.MAP, since = "4.11", description = "datadisk template to disk-offering mapping;" +
            " an optional parameter used to create additional data disks from datadisk templates; can't be specified with diskOfferingId parameter")
    private Map dataDiskTemplateToDiskOfferingList;

    @Parameter(name = ApiConstants.EXTRA_CONFIG, type = CommandType.STRING, since = "4.12", description = "an optional URL encoded string that can be passed to the virtual machine upon successful deployment", length = 5120)
    private String extraConfig;

    @Parameter(name = ApiConstants.COPY_IMAGE_TAGS, type = CommandType.BOOLEAN, since = "4.13", description = "if true the image tags (if any) will be copied to the VM, default value is false")
    private Boolean copyImageTags;

    @Parameter(name = ApiConstants.PROPERTIES, type = CommandType.MAP, since = "4.15",
            description = "used to specify the vApp properties.")
    @LogLevel(LogLevel.Log4jLevel.Off)
    private Map vAppProperties;

    @Parameter(name = ApiConstants.NIC_NETWORK_LIST, type = CommandType.MAP, since = "4.15",
            description = "VMware only: used to specify network mapping of a vApp VMware template registered \"as-is\"." +
                    " Example nicnetworklist[0].ip=Nic-101&nicnetworklist[0].network=uuid")
    @LogLevel(LogLevel.Log4jLevel.Off)
    private Map vAppNetworks;

    @Parameter(name = ApiConstants.DYNAMIC_SCALING_ENABLED,
            type = CommandType.BOOLEAN,
            description = "true if virtual machine needs to be dynamically scalable")
    protected Boolean dynamicScalingEnabled;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        if (accountName == null) {
            return CallContext.current().getCallingAccount().getAccountName();
        }
        return accountName;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public String getDeploymentPlanner() {
        return deploymentPlanner;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getDomainId() {
        if (domainId == null) {
            return CallContext.current().getCallingAccount().getDomainId();
        }
        return domainId;
    }

    public ApiConstants.BootType  getBootType() {
        if (StringUtils.isNotBlank(bootType)) {
            try {
                String type = bootType.trim().toUpperCase();
                return ApiConstants.BootType.valueOf(type);
            } catch (IllegalArgumentException e) {
                String errMesg = "Invalid bootType " + bootType + "Specified for vm " + getName()
                        + " Valid values are: " + Arrays.toString(ApiConstants.BootType.values());
                s_logger.warn(errMesg);
                throw new InvalidParameterValueException(errMesg);
            }
        }
        return null;
    }

    public Map<String, String> getDetails() {
        Map<String, String> customparameterMap = new HashMap<String, String>();
        if (details != null && details.size() != 0) {
            Collection parameterCollection = details.values();
            Iterator iter = parameterCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> value = (HashMap<String, String>)iter.next();
                for (Map.Entry<String,String> entry: value.entrySet()) {
                    customparameterMap.put(entry.getKey(),entry.getValue());
                }
            }
        }
        if (ApiConstants.BootType.UEFI.equals(getBootType())) {
            customparameterMap.put(getBootType().toString(), getBootMode().toString());
        }

        if (rootdisksize != null && !customparameterMap.containsKey("rootdisksize")) {
            customparameterMap.put("rootdisksize", rootdisksize.toString());
        }

        return customparameterMap;
    }


    public ApiConstants.BootMode getBootMode() {
        if (StringUtils.isNotBlank(bootMode)) {
            try {
                String mode = bootMode.trim().toUpperCase();
                return ApiConstants.BootMode.valueOf(mode);
            } catch (IllegalArgumentException e) {
                String errMesg = "Invalid bootMode " + bootMode + "Specified for vm " + getName()
                        + " Valid values are:  "+ Arrays.toString(ApiConstants.BootMode.values());
                s_logger.warn(errMesg);
                throw new InvalidParameterValueException(errMesg);
                }
        }
        return null;
    }

    public Map<String, String> getVmProperties() {
        Map<String, String> map = new HashMap<>();
        if (MapUtils.isNotEmpty(vAppProperties)) {
            Collection parameterCollection = vAppProperties.values();
            Iterator iterator = parameterCollection.iterator();
            while (iterator.hasNext()) {
                HashMap<String, String> entry = (HashMap<String, String>)iterator.next();
                map.put(entry.get("key"), entry.get("value"));
            }
        }
        return map;
    }

    public Map<Integer, Long> getVmNetworkMap() {
        Map<Integer, Long> map = new HashMap<>();
        if (MapUtils.isNotEmpty(vAppNetworks)) {
            Collection parameterCollection = vAppNetworks.values();
            Iterator iterator = parameterCollection.iterator();
            while (iterator.hasNext()) {
                HashMap<String, String> entry = (HashMap<String, String>) iterator.next();
                Integer nic;
                try {
                    nic = Integer.valueOf(entry.get(VmDetailConstants.NIC));
                } catch (NumberFormatException nfe) {
                    nic = null;
                }
                String networkUuid = entry.get(VmDetailConstants.NETWORK);
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace(String.format("nic, '%s', goes on net, '%s'", nic, networkUuid));
                }
                if (nic == null || Strings.isNullOrEmpty(networkUuid) || _entityMgr.findByUuid(Network.class, networkUuid) == null) {
                    throw new InvalidParameterValueException(String.format("Network ID: %s for NIC ID: %s is invalid", networkUuid, nic));
                }
                map.put(nic, _entityMgr.findByUuid(Network.class, networkUuid).getId());
            }
        }
        return map;
    }

    public String getGroup() {
        return group;
    }

    public HypervisorType getHypervisor() {
        return HypervisorType.getType(hypervisor);
    }

    public Boolean isDisplayVm() {
        return displayVm;
    }

    @Override
    public boolean isDisplay() {
        if(displayVm == null)
            return true;
        else
            return displayVm;
    }

    public List<String> getSecurityGroupNameList() {
        return securityGroupNameList;
    }

    public List<Long> getSecurityGroupIdList() {
        return securityGroupIdList;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getSize() {
        return size;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getUserData() {
        return userData;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public List<Long> getNetworkIds() {
        if (MapUtils.isNotEmpty(vAppNetworks)) {
            if (CollectionUtils.isNotEmpty(networkIds) || ipAddress != null || getIp6Address() != null || MapUtils.isNotEmpty(ipToNetworkList)) {
                throw new InvalidParameterValueException(String.format("%s can't be specified along with %s, %s, %s", ApiConstants.NIC_NETWORK_LIST, ApiConstants.NETWORK_IDS, ApiConstants.IP_ADDRESS, ApiConstants.IP_NETWORK_LIST));
            } else {
                return new ArrayList<>();
            }
        }
       if (ipToNetworkList != null && !ipToNetworkList.isEmpty()) {
           if ((networkIds != null && !networkIds.isEmpty()) || ipAddress != null || getIp6Address() != null) {
               throw new InvalidParameterValueException("ipToNetworkMap can't be specified along with networkIds or ipAddress");
           } else {
               List<Long> networks = new ArrayList<Long>();
               networks.addAll(getIpToNetworkMap().keySet());
               return networks;
           }
       }
        return networkIds;
    }

    public String getName() {
        return name;
    }

    public String getSSHKeyPairName() {
        return sshKeyPairName;
    }

    public Long getHostId() {
        return hostId;
    }

    public boolean getStartVm() {
        return startVm == null ? true : startVm;
    }

    public Map<Long, IpAddresses> getIpToNetworkMap() {
        if ((networkIds != null || ipAddress != null || getIp6Address() != null) && ipToNetworkList != null) {
            throw new InvalidParameterValueException("NetworkIds and ipAddress can't be specified along with ipToNetworkMap parameter");
        }
        LinkedHashMap<Long, IpAddresses> ipToNetworkMap = null;
        if (ipToNetworkList != null && !ipToNetworkList.isEmpty()) {
            ipToNetworkMap = new LinkedHashMap<Long, IpAddresses>();
            Collection ipsCollection = ipToNetworkList.values();
            Iterator iter = ipsCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> ips = (HashMap<String, String>)iter.next();
                Long networkId = getNetworkIdFomIpMap(ips);
                IpAddresses addrs = getIpAddressesFromIpMap(ips);
                ipToNetworkMap.put(networkId, addrs);
            }
        }

        return ipToNetworkMap;
    }

    @Nonnull
    private IpAddresses getIpAddressesFromIpMap(HashMap<String, String> ips) {
        String requestedIp = ips.get("ip");
        String requestedIpv6 = ips.get("ipv6");
        String requestedMac = ips.get("mac");
        if (requestedIpv6 != null) {
            requestedIpv6 = NetUtils.standardizeIp6Address(requestedIpv6);
        }
        if (requestedMac != null) {
            if(!NetUtils.isValidMac(requestedMac)) {
                throw new InvalidParameterValueException("Mac address is not valid: " + requestedMac);
            } else if(!NetUtils.isUnicastMac(requestedMac)) {
                throw new InvalidParameterValueException("Mac address is not unicast: " + requestedMac);
            }
            requestedMac = NetUtils.standardizeMacAddress(requestedMac);
        }
        return new IpAddresses(requestedIp, requestedIpv6, requestedMac);
    }

    @Nonnull
    private Long getNetworkIdFomIpMap(HashMap<String, String> ips) {
        Long networkId;
        final String networkid = ips.get("networkid");
        Network network = _networkService.getNetwork(networkid);
        if (network != null) {
            networkId = network.getId();
        } else {
            try {
                networkId = Long.parseLong(networkid);
            } catch (NumberFormatException e) {
                throw new InvalidParameterValueException("Unable to translate and find entity with networkId: " + networkid);
            }
        }
        return networkId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getIp6Address() {
        if (ip6Address == null) {
            return null;
        }
        return NetUtils.standardizeIp6Address(ip6Address);
    }


    public String getMacAddress() {
        if (macAddress == null) {
            return null;
        }
        if(!NetUtils.isValidMac(macAddress)) {
            throw new InvalidParameterValueException("Mac address is not valid: " + macAddress);
        } else if(!NetUtils.isUnicastMac(macAddress)) {
            throw new InvalidParameterValueException("Mac address is not unicast: " + macAddress);
        }
        return NetUtils.standardizeMacAddress(macAddress);
    }

    public List<Long> getAffinityGroupIdList() {
        if (affinityGroupNameList != null && affinityGroupIdList != null) {
            throw new InvalidParameterValueException("affinitygroupids parameter is mutually exclusive with affinitygroupnames parameter");
        }

        // transform group names to ids here
        if (affinityGroupNameList != null) {
            List<Long> affinityGroupIds = new ArrayList<Long>();
            for (String groupName : affinityGroupNameList) {
                Long groupId = _responseGenerator.getAffinityGroupId(groupName, getEntityOwnerId());
                if (groupId == null) {
                    throw new InvalidParameterValueException("Unable to find affinity group by name " + groupName);
                } else {
                    affinityGroupIds.add(groupId);
                }
            }
            return affinityGroupIds;
        } else {
            return affinityGroupIdList;
        }
    }

    public String getKeyboard() {
        // TODO Auto-generated method stub
        return keyboard;
    }

    public Map<String, Map<Integer, String>> getDhcpOptionsMap() {
        Map<String, Map<Integer, String>> dhcpOptionsMap = new HashMap<>();
        if (dhcpOptionsNetworkList != null && !dhcpOptionsNetworkList.isEmpty()) {

            Collection<Map<String, String>> paramsCollection = this.dhcpOptionsNetworkList.values();
            for (Map<String, String> dhcpNetworkOptions : paramsCollection) {
                String networkId = dhcpNetworkOptions.get(ApiConstants.NETWORK_ID);

                if (networkId == null) {
                    throw new IllegalArgumentException("No networkid specified when providing extra dhcp options.");
                }

                Map<Integer, String> dhcpOptionsForNetwork = new HashMap<>();
                dhcpOptionsMap.put(networkId, dhcpOptionsForNetwork);

                for (String key : dhcpNetworkOptions.keySet()) {
                    if (key.startsWith(ApiConstants.DHCP_PREFIX)) {
                        int dhcpOptionValue = Integer.parseInt(key.replaceFirst(ApiConstants.DHCP_PREFIX, ""));
                        dhcpOptionsForNetwork.put(dhcpOptionValue, dhcpNetworkOptions.get(key));
                    } else if (!key.equals(ApiConstants.NETWORK_ID)) {
                        Dhcp.DhcpOptionCode dhcpOptionEnum = Dhcp.DhcpOptionCode.valueOfString(key);
                        dhcpOptionsForNetwork.put(dhcpOptionEnum.getCode(), dhcpNetworkOptions.get(key));
                    }
                }

            }
        }

        return dhcpOptionsMap;
    }

    public Map<Long, DiskOffering> getDataDiskTemplateToDiskOfferingMap() {
        if (diskOfferingId != null && dataDiskTemplateToDiskOfferingList != null) {
            throw new InvalidParameterValueException("diskofferingid paramter can't be specified along with datadisktemplatetodiskofferinglist parameter");
        }
        if (MapUtils.isEmpty(dataDiskTemplateToDiskOfferingList)) {
            return new HashMap<Long, DiskOffering>();
        }

        HashMap<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap = new HashMap<Long, DiskOffering>();
        for (Object objDataDiskTemplates : dataDiskTemplateToDiskOfferingList.values()) {
            HashMap<String, String> dataDiskTemplates = (HashMap<String, String>) objDataDiskTemplates;
            Long dataDiskTemplateId;
            DiskOffering dataDiskOffering = null;
            VirtualMachineTemplate dataDiskTemplate= _entityMgr.findByUuid(VirtualMachineTemplate.class, dataDiskTemplates.get("datadisktemplateid"));
            if (dataDiskTemplate == null) {
                dataDiskTemplate = _entityMgr.findById(VirtualMachineTemplate.class, dataDiskTemplates.get("datadisktemplateid"));
                if (dataDiskTemplate == null)
                    throw new InvalidParameterValueException("Unable to translate and find entity with datadisktemplateid " + dataDiskTemplates.get("datadisktemplateid"));
            }
            dataDiskTemplateId = dataDiskTemplate.getId();
            dataDiskOffering = _entityMgr.findByUuid(DiskOffering.class, dataDiskTemplates.get("diskofferingid"));
            if (dataDiskOffering == null) {
                dataDiskOffering = _entityMgr.findById(DiskOffering.class, dataDiskTemplates.get("diskofferingid"));
                if (dataDiskOffering == null)
                    throw new InvalidParameterValueException("Unable to translate and find entity with diskofferingId " + dataDiskTemplates.get("diskofferingid"));
            }
            dataDiskTemplateToDiskOfferingMap.put(dataDiskTemplateId, dataDiskOffering);
        }
        return dataDiskTemplateToDiskOfferingMap;
    }

    public String getExtraConfig() {
        return extraConfig;
    }

    public boolean getCopyImageTags() {
        return copyImageTags == null ? false : copyImageTags;
    }

    public Boolean getBootIntoSetup() {
        return bootIntoSetup;
    }

    public Boolean isDynamicScalingEnabled() {
        return dynamicScalingEnabled == null ? Boolean.TRUE : dynamicScalingEnabled;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "virtualmachine";
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_CREATE;
    }

    @Override
    public String getCreateEventType() {
        return EventTypes.EVENT_VM_CREATE;
    }

    @Override
    public String getCreateEventDescription() {
        return "creating Vm";
    }

    @Override
    public String getEventDescription() {
        return "starting Vm. Vm Id: " + getEntityUuid();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.VirtualMachine;
    }

    @Override
    public void execute() {
        UserVm result;

        if (getStartVm()) {
            try {
                CallContext.current().setEventDetails("Vm Id: " + getEntityUuid());
                result = _userVmService.startVirtualMachine(this);
            } catch (ResourceUnavailableException ex) {
                s_logger.warn("Exception: ", ex);
                throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
            } catch (ResourceAllocationException ex) {
                s_logger.warn("Exception: ", ex);
                throw new ServerApiException(ApiErrorCode.RESOURCE_ALLOCATION_ERROR, ex.getMessage());
            } catch (ConcurrentOperationException ex) {
                s_logger.warn("Exception: ", ex);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
            } catch (InsufficientCapacityException ex) {
                StringBuilder message = new StringBuilder(ex.getMessage());
                if (ex instanceof InsufficientServerCapacityException) {
                    if (((InsufficientServerCapacityException)ex).isAffinityApplied()) {
                        message.append(", Please check the affinity groups provided, there may not be sufficient capacity to follow them");
                    }
                }
                s_logger.info(ex);
                s_logger.info(message.toString(), ex);
                throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, message.toString());
            }
        } else {
            result = _userVmService.getUserVm(getEntityId());
        }

        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to deploy vm uuid:"+getEntityUuid());
        }
    }


    @Override
    public void create() throws ResourceAllocationException {
        try {
            UserVm vm = _userVmService.createVirtualMachine(this);

            if (vm != null) {
                setEntityId(vm.getId());
                setEntityUuid(vm.getUuid());
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to deploy vm");
            }
        } catch (InsufficientCapacityException ex) {
            s_logger.info(ex);
            s_logger.trace(ex.getMessage(), ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        }  catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (ResourceAllocationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_ALLOCATION_ERROR, ex.getMessage());
        }
    }
}
