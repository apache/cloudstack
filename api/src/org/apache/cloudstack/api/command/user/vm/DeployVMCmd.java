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

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
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
import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VirtualMachine;
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
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@APICommand(name = "deployVirtualMachine", description = "Creates and automatically starts a virtual machine based on a service offering, disk offering, and template.", responseObject = UserVmResponse.class, responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class DeployVMCmd extends BaseAsyncCreateCustomIdCmd {
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
            + " Example: iptonetworklist[0].ip=10.10.10.11&iptonetworklist[0].ipv6=fc00:1234:5678::abcd&iptonetworklist[0].networkid=uuid - requests to use ip 10.10.10.11 in network id=uuid")
    private Map ipToNetworkList;

    @Parameter(name = ApiConstants.IP_ADDRESS, type = CommandType.STRING, description = "the ip address for default vm's network")
    private String ipAddress;

    @Parameter(name = ApiConstants.IP6_ADDRESS, type = CommandType.STRING, description = "the ipv6 address for default vm's network")
    private String ip6Address;

    @Parameter(name = ApiConstants.KEYBOARD, type = CommandType.STRING, description = "an optional keyboard device type for the virtual machine. valid value can be one of de,de-ch,es,fi,fr,fr-be,fr-ch,is,it,jp,nl-be,no,pt,uk,us")
    private String keyboard;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "Deploy vm for the project")
    private Long projectId;

    @Parameter(name = ApiConstants.START_VM, type = CommandType.BOOLEAN, description = "true if network offering supports specifying ip ranges; defaulted to true if not specified")
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

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, since = "4.3", description = "used to specify the custom parameters.")
    private Map details;

    @Parameter(name = ApiConstants.DEPLOYMENT_PLANNER, type = CommandType.STRING, description = "Deployment planner to use for vm allocation. Available to ROOT admin only", since = "4.4", authorized = { RoleType.Admin })
    private String deploymentPlanner;

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
        if (rootdisksize != null && !customparameterMap.containsKey("rootdisksize")) {
            customparameterMap.put("rootdisksize", rootdisksize.toString());
        }
        return customparameterMap;
    }

    public String getGroup() {
        return group;
    }

    public HypervisorType getHypervisor() {
        return HypervisorType.getType(hypervisor);
    }

    public Boolean getDisplayVm() {
        return displayVm;
    }

    @Override
    public boolean isDisplay() {
        if(displayVm == null)
            return true;
        else
            return displayVm;
    }

    public List<Long> getSecurityGroupIdList() {
        if (securityGroupNameList != null && securityGroupIdList != null) {
            throw new InvalidParameterValueException("securitygroupids parameter is mutually exclusive with securitygroupnames parameter");
        }

       //transform group names to ids here
       if (securityGroupNameList != null) {
            List<Long> securityGroupIds = new ArrayList<Long>();
            for (String groupName : securityGroupNameList) {
                Long groupId = _responseGenerator.getSecurityGroupId(groupName, getEntityOwnerId());
                if (groupId == null) {
                    throw new InvalidParameterValueException("Unable to find group by name " + groupName);
                } else {
                    securityGroupIds.add(groupId);
                }
            }
            return securityGroupIds;
        } else {
            return securityGroupIdList;
        }
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

    private Map<Long, IpAddresses> getIpToNetworkMap() {
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
                Long networkId;
                Network network = _networkService.getNetwork(ips.get("networkid"));
                if (network != null) {
                    networkId = network.getId();
                } else {
                    try {
                        networkId = Long.parseLong(ips.get("networkid"));
                    } catch (NumberFormatException e) {
                        throw new InvalidParameterValueException("Unable to translate and find entity with networkId: " + ips.get("networkid"));
                    }
                }
                String requestedIp = ips.get("ip");
                String requestedIpv6 = ips.get("ipv6");
                if (requestedIpv6 != null) {
                    requestedIpv6 = NetUtils.standardizeIp6Address(requestedIpv6);
                }
                IpAddresses addrs = new IpAddresses(requestedIp, requestedIpv6);
                ipToNetworkMap.put(networkId, addrs);
            }
        }

        return ipToNetworkMap;
    }

    public String getIp6Address() {
        if (ip6Address == null) {
            return null;
        }
        return NetUtils.standardizeIp6Address(ip6Address);
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
        return "starting Vm. Vm Id: " + getEntityId();
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
                CallContext.current().setEventDetails("Vm Id: " + getEntityId());
                result = _userVmService.startVirtualMachine(this);
            } catch (ResourceUnavailableException ex) {
                s_logger.warn("Exception: ", ex);
                throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
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
            UserVmResponse response = _responseGenerator.createUserVmResponse(ResponseView.Restricted, "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to deploy vm");
        }
    }

    // this is an opportunity to verify that parameters that came in via the Details Map are OK
    // for example, minIops and maxIops should either both be specified or neither be specified and,
    // if specified, minIops should be <= maxIops
    private void verifyDetails() {
        Map<String, String> map = getDetails();

        if (map != null) {
            String minIops = (String)map.get("minIops");
            String maxIops = (String)map.get("maxIops");

            verifyMinAndMaxIops(minIops, maxIops);

            minIops = (String)map.get("minIopsDo");
            maxIops = (String)map.get("maxIopsDo");

            verifyMinAndMaxIops(minIops, maxIops);
        }
    }

    private void verifyMinAndMaxIops(String minIops, String maxIops) {
        if ((minIops != null && maxIops == null) || (minIops == null && maxIops != null)) {
            throw new InvalidParameterValueException("Either 'Min IOPS' and 'Max IOPS' must both be specified or neither be specified.");
        }

        long lMinIops;

        try {
            if (minIops != null) {
                lMinIops = Long.parseLong(minIops);
            }
            else {
                lMinIops = 0;
            }
        }
        catch (NumberFormatException ex) {
            throw new InvalidParameterValueException("'Min IOPS' must be a whole number.");
        }

        long lMaxIops;

        try {
            if (maxIops != null) {
                lMaxIops = Long.parseLong(maxIops);
            }
            else {
                lMaxIops = 0;
            }
        }
        catch (NumberFormatException ex) {
            throw new InvalidParameterValueException("'Max IOPS' must be a whole number.");
        }

        if (lMinIops > lMaxIops) {
            throw new InvalidParameterValueException("'Min IOPS' must be less than or equal to 'Max IOPS'.");
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        try {
            //Verify that all objects exist before passing them to the service
            Account owner = _accountService.getActiveAccountById(getEntityOwnerId());

            verifyDetails();

            DataCenter zone = _entityMgr.findById(DataCenter.class, zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Unable to find zone by id=" + zoneId);
            }

            ServiceOffering serviceOffering = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
            if (serviceOffering == null) {
                throw new InvalidParameterValueException("Unable to find service offering: " + serviceOfferingId);
            }

            VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, templateId);
            // Make sure a valid template ID was specified
            if (template == null) {
                throw new InvalidParameterValueException("Unable to use template " + templateId);
            }

            DiskOffering diskOffering = null;
            if (diskOfferingId != null) {
                diskOffering = _entityMgr.findById(DiskOffering.class, diskOfferingId);
                if (diskOffering == null) {
                    throw new InvalidParameterValueException("Unable to find disk offering " + diskOfferingId);
                }
            }

            if (!zone.isLocalStorageEnabled()) {
                if (serviceOffering.getUseLocalStorage()) {
                    throw new InvalidParameterValueException("Zone is not configured to use local storage but service offering " + serviceOffering.getName() + " uses it");
                }
                if (diskOffering != null && diskOffering.getUseLocalStorage()) {
                    throw new InvalidParameterValueException("Zone is not configured to use local storage but disk offering " + diskOffering.getName() + " uses it");
                }
            }

            UserVm vm = null;
            IpAddresses addrs = new IpAddresses(ipAddress, getIp6Address());
            if (zone.getNetworkType() == NetworkType.Basic) {
                if (getNetworkIds() != null) {
                    throw new InvalidParameterValueException("Can't specify network Ids in Basic zone");
                } else {
                    vm = _userVmService.createBasicSecurityGroupVirtualMachine(zone, serviceOffering, template, getSecurityGroupIdList(), owner, name, displayName, diskOfferingId,
                            size, group, getHypervisor(), getHttpMethod(), userData, sshKeyPairName, getIpToNetworkMap(), addrs, displayVm, keyboard, getAffinityGroupIdList(),
                            getDetails(), getCustomId());
                }
            } else {
                if (zone.isSecurityGroupEnabled())  {
                    vm = _userVmService.createAdvancedSecurityGroupVirtualMachine(zone, serviceOffering, template, getNetworkIds(), getSecurityGroupIdList(), owner, name,
                            displayName, diskOfferingId, size, group, getHypervisor(), getHttpMethod(), userData, sshKeyPairName, getIpToNetworkMap(), addrs, displayVm, keyboard,
                            getAffinityGroupIdList(), getDetails(), getCustomId());

                } else {
                    if (getSecurityGroupIdList() != null && !getSecurityGroupIdList().isEmpty()) {
                        throw new InvalidParameterValueException("Can't create vm with security groups; security group feature is not enabled per zone");
                    }
                    vm = _userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, getNetworkIds(), owner, name, displayName, diskOfferingId, size, group,
                            getHypervisor(), getHttpMethod(), userData, sshKeyPairName, getIpToNetworkMap(), addrs, displayVm, keyboard, getAffinityGroupIdList(), getDetails(),
                            getCustomId());
                }
            }

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
