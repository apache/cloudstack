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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Implementation;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;

import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import com.cloud.async.AsyncJob;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.security.SecurityGroup;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.uservm.UserVm;


@Implementation(description="Creates and automatically starts a virtual machine based on a service offering, disk offering, and template.", responseObject=UserVmResponse.class)
public class DeployVMCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(DeployVMCmd.class.getName());

    private static final String s_name = "deployvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    //@IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="availability zone for the virtual machine", entityType=ZoneResponse.class)
    private Long zoneId;

    @ACL
    //@IdentityMapper(entityTableName="disk_offering")
    @Parameter(name=ApiConstants.SERVICE_OFFERING_ID, type=CommandType.LONG, required=true, description="the ID of the service offering for the virtual machine", entityType=ServiceOfferingResponse.class)
    private Long serviceOfferingId;

    @ACL
    //@IdentityMapper(entityTableName="vm_template")
    @Parameter(name=ApiConstants.TEMPLATE_ID, type=CommandType.LONG, required=true, description="the ID of the template for the virtual machine", entityType=TemplateResponse.class)
    private Long templateId;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="host name for the virtual machine")
    private String name;

    @Parameter(name=ApiConstants.DISPLAY_NAME, type=CommandType.STRING, description="an optional user generated name for the virtual machine")
    private String displayName;

    //Owner information
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account for the virtual machine. Must be used with domainId.")
    private String accountName;

    //@IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="an optional domainId for the virtual machine. If the account parameter is used, domainId must also be used.", entityType=DomainResponse.class)
    private Long domainId;

    //Network information
    @ACL
    //@IdentityMapper(entityTableName="networks")
    @Parameter(name=ApiConstants.NETWORK_IDS, type=CommandType.LIST, collectionType=CommandType.LONG, description="list of network ids used by virtual machine. Can't be specified with ipToNetworkList parameter", entityType=NetworkResponse.class)
    private List<Long> networkIds;

    //DataDisk information
    @ACL
    //@IdentityMapper(entityTableName="disk_offering")
    @Parameter(name=ApiConstants.DISK_OFFERING_ID, type=CommandType.LONG, description="the ID of the disk offering for the virtual machine. If the template is of ISO format, the diskOfferingId is for the root disk volume. Otherwise this parameter is used to indicate the offering for the data disk volume. If the templateId parameter passed is from a Template object, the diskOfferingId refers to a DATA Disk Volume created. If the templateId parameter passed is from an ISO object, the diskOfferingId refers to a ROOT Disk Volume created.", entityType=DiskOfferingResponse.class)
    private Long diskOfferingId;

    @Parameter(name=ApiConstants.SIZE, type=CommandType.LONG, description="the arbitrary size for the DATADISK volume. Mutually exclusive with diskOfferingId")
    private Long size;

    @Parameter(name=ApiConstants.GROUP, type=CommandType.STRING, description="an optional group for the virtual machine")
    private String group;

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, description="the hypervisor on which to deploy the virtual machine")
    private String hypervisor;

    @Parameter(name=ApiConstants.USER_DATA, type=CommandType.STRING, description="an optional binary data that can be sent to the virtual machine upon a successful deployment. This binary data must be base64 encoded before adding it to the request. Currently only HTTP GET is supported. Using HTTP GET (via querystring), you can send up to 2KB of data after base64 encoding.", length=2048)
    private String userData;

    @Parameter(name=ApiConstants.SSH_KEYPAIR, type=CommandType.STRING, description="name of the ssh key pair used to login to the virtual machine")
    private String sshKeyPairName;


    //@IdentityMapper(entityTableName="host")
    @Parameter(name=ApiConstants.HOST_ID, type=CommandType.LONG, description="destination Host ID to deploy the VM to - parameter available for root admin only", entityType=HostResponse.class)
    private Long hostId;

    @ACL
    //@IdentityMapper(entityTableName="security_group")
    @Parameter(name=ApiConstants.SECURITY_GROUP_IDS, type=CommandType.LIST, collectionType=CommandType.LONG, description="comma separated list of security groups id that going to be applied to the virtual machine. Should be passed only when vm is created from a zone with Basic Network support. Mutually exclusive with securitygroupnames parameter", entityType=SecurityGroupResponse.class)
    private List<Long> securityGroupIdList;

    @ACL
    @Parameter(name=ApiConstants.SECURITY_GROUP_NAMES, type=CommandType.LIST, collectionType=CommandType.STRING, description="comma separated list of security groups names that going to be applied to the virtual machine. Should be passed only when vm is created from a zone with Basic Network support. Mutually exclusive with securitygroupids parameter", entityType=SecurityGroupResponse.class)
    private List<String> securityGroupNameList;

    @ACL(checkKeyAccess=true)
    @Parameter(name = ApiConstants.IP_NETWORK_LIST, type = CommandType.MAP, description = "ip to network mapping. Can't be specified with networkIds parameter. Example: iptonetworklist[0].ip=10.10.10.11&iptonetworklist[0].networkid=204 - requests to use ip 10.10.10.11 in network id=204",entityType={Network.class,IpAddress.class})
    private Map ipToNetworkList;

    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING, description="the ip address for default vm's network")
    private String ipAddress;

    @Parameter(name=ApiConstants.KEYBOARD, type=CommandType.STRING, description="an optional keyboard device type for the virtual machine. valid value can be one of de,de-ch,es,fi,fr,fr-be,fr-ch,is,it,jp,nl-be,no,pt,uk,us")
    private String keyboard;

    //@IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="Deploy vm for the project", entityType=ProjectAccountResponse.class)
    private Long projectId;

    @Parameter(name=ApiConstants.START_VM, type=CommandType.BOOLEAN, description="true if network offering supports specifying ip ranges; defaulted to true if not specified")
    private Boolean startVm;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getEntityTable() {
        return "vm_instance";
    }

    public String getAccountName() {
        if (accountName == null) {
            return UserContext.current().getCaller().getAccountName();
        }
        return accountName;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getDomainId() {
        if (domainId == null) {
            return UserContext.current().getCaller().getDomainId();
        }
        return domainId;
    }

    public String getGroup() {
        return group;
    }

    public HypervisorType getHypervisor() {
        return HypervisorType.getType(hypervisor);
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
                    throw new InvalidParameterValueException("Unable to find group by name " + groupName + " for account " + getEntityOwnerId());
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
       if (ipToNetworkList != null) {
           if (networkIds != null || ipAddress != null) {
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

    private Map<Long, String> getIpToNetworkMap() {
        if ((networkIds != null || ipAddress != null) && ipToNetworkList != null) {
            throw new InvalidParameterValueException("NetworkIds and ipAddress can't be specified along with ipToNetworkMap parameter");
        }
        LinkedHashMap<Long, String> ipToNetworkMap = null;
        if (ipToNetworkList != null && !ipToNetworkList.isEmpty()) {
            ipToNetworkMap = new LinkedHashMap<Long, String>();
            Collection ipsCollection = ipToNetworkList.values();
            Iterator iter = ipsCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> ips = (HashMap<String, String>) iter.next();
                Long networkId = Long.valueOf(_responseGenerator.getIdentiyId("networks", ips.get("networkid")));
                String requestedIp = (String) ips.get("ip");
                ipToNetworkMap.put(networkId, requestedIp);
            }
        }

        return ipToNetworkMap;
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
        Long accountId = finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return UserContext.current().getCaller().getId();
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
        return  "starting Vm. Vm Id: "+getEntityId();
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.VirtualMachine;
    }

    @Override
    public void execute(){
        UserVm result;

        if (getStartVm()) {
            try {
                UserContext.current().setEventDetails("Vm Id: "+getEntityId());
                if (getHypervisor() == HypervisorType.BareMetal) {
                    result = _bareMetalVmService.startVirtualMachine(this);
                } else {
                    result = _userVmService.startVirtualMachine(this);
                }
            } catch (ResourceUnavailableException ex) {
                s_logger.warn("Exception: ", ex);
                throw new ServerApiException(BaseCmd.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
            } catch (ConcurrentOperationException ex) {
                s_logger.warn("Exception: ", ex);
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
            } catch (InsufficientCapacityException ex) {
                s_logger.info(ex);
                s_logger.trace(ex);
                throw new ServerApiException(BaseCmd.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
            }
        } else {
            result = _userVmService.getUserVm(getEntityId());
        }

        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse("virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to deploy vm");
        }
    }

    @Override
    public void create() throws ResourceAllocationException{
        try {
            //Verify that all objects exist before passing them to the service
            Account owner = _accountService.getActiveAccountById(getEntityOwnerId());

            DataCenter zone = _configService.getZone(zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Unable to find zone by id=" + zoneId);
            }

            ServiceOffering serviceOffering = _configService.getServiceOffering(serviceOfferingId);
            if (serviceOffering == null) {
                throw new InvalidParameterValueException("Unable to find service offering: " + serviceOfferingId);
            }

            VirtualMachineTemplate template = _templateService.getTemplate(templateId);
            // Make sure a valid template ID was specified
            if (template == null) {
                throw new InvalidParameterValueException("Unable to use template " + templateId);
            }

            DiskOffering diskOffering = null;
            if (diskOfferingId != null) {
                diskOffering = _configService.getDiskOffering(diskOfferingId);
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
            if (getHypervisor() == HypervisorType.BareMetal) {
                vm = _bareMetalVmService.createVirtualMachine(this);
            } else {
                if (zone.getNetworkType() == NetworkType.Basic) {
                    if (getNetworkIds() != null) {
                        throw new InvalidParameterValueException("Can't specify network Ids in Basic zone");
                    } else {
                        vm = _userVmService.createBasicSecurityGroupVirtualMachine(zone, serviceOffering, template, getSecurityGroupIdList(), owner, name,
                                displayName, diskOfferingId, size, group, getHypervisor(), userData, sshKeyPairName, getIpToNetworkMap(), ipAddress, keyboard);
                    }
                } else {
                    if (zone.isSecurityGroupEnabled())  {
                        vm = _userVmService.createAdvancedSecurityGroupVirtualMachine(zone, serviceOffering, template, getNetworkIds(), getSecurityGroupIdList(),
                                owner, name, displayName, diskOfferingId, size, group, getHypervisor(), userData, sshKeyPairName, getIpToNetworkMap(), ipAddress, keyboard);
                    } else {
                        if (getSecurityGroupIdList() != null && !getSecurityGroupIdList().isEmpty()) {
                            throw new InvalidParameterValueException("Can't create vm with security groups; security group feature is not enabled per zone");
                        }
                        vm = _userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, getNetworkIds(), owner, name, displayName,
                                diskOfferingId, size, group, getHypervisor(), userData, sshKeyPairName, getIpToNetworkMap(), ipAddress, keyboard);
                    }
                }
            }

            if (vm != null) {
                setEntityId(vm.getId());
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to deploy vm");
            }
        } catch (InsufficientCapacityException ex) {
            s_logger.info(ex);
            s_logger.trace(ex);
            throw new ServerApiException(BaseCmd.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(BaseCmd.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        }  catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        }
    }

}
