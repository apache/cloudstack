/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.commands;

import java.util.Date;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.serializer.Param;
import com.cloud.vm.UserVmVO;

@Implementation(method="upgradeVirtualMachine", manager=Manager.UserVmManager)
public class UpgradeVMCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpgradeVMCmd.class.getName());
    private static final String s_name = "changeserviceforvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="serviceofferingid", type=CommandType.LONG, required=true)
    private Long serviceOfferingId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    private UserVmVO responseObject = null;
    
    public String getName() {
        return s_name;
    }

    public static String getResultObjectName() {
    	return "virtualmachine";
    }    
    
    @Override
    public String getResponse() 
    {
        UpgradeVmResponse response = new UpgradeVmResponse();
        return null;//TODO -- construct response
//        if (responseObject != null) {
//            response.set
//            
//    		Account acct = ms.findAccountById(Long.valueOf(vm.getAccountId()));
//    		resultObj.setAccount(acct.getAccountName());
//    		
//    		ServiceOfferingVO offering = ms.findServiceOfferingById(vm.getServiceOfferingId());
//    		resultObj.setCpuSpeed(offering.getSpeed());
//    		resultObj.setMemory(offering.getRamSize());
//    		if(offering.getDisplayText()!=null)
//    			resultObj.setServiceOfferingName(offering.getDisplayText());
//    		else
//    			resultObj.setServiceOfferingName(offering.getName());
//    		resultObj.setServiceOfferingId(vm.getServiceOfferingId());
//    		
//    		VmStats vmStats = ms.getVmStatistics(vm.getId());
//    		if(vmStats != null)
//    		{
//    			resultObj.setCpuUsed((long) vmStats.getCPUUtilization());
//    			resultObj.setNetworkKbsRead((long) vmStats.getNetworkReadKBs());
//    			resultObj.setNetworkKbsWrite((long) vmStats.getNetworkWriteKBs());
//    		}
//    		
//    		resultObj.setCreated(vm.getCreated());
//    		resultObj.setDisplayName(vm.getDisplayName());
//    		resultObj.setDomain(ms.findDomainIdById(acct.getDomainId()).getName());
//    		resultObj.setDomainId(acct.getDomainId());
//    		resultObj.setHaEnable(vm.isHaEnabled());
//    		if(vm.getHostId() != null)
//    		{
//    			resultObj.setHostId(vm.getHostId());
//    			resultObj.setHostName(ms.getHostBy(vm.getHostId()).getName());
//    		}
//    		resultObj.setIpAddress(vm.getPrivateIpAddress());
//    		resultObj.setName(vm.getName());
//    		resultObj.setState(vm.getState().toString());
//    		resultObj.setZoneId(vm.getDataCenterId());
//    		resultObj.setZoneName(ms.findDataCenterById(vm.getDataCenterId()).getName());
//    		
//    		VMTemplateVO template = ms.findTemplateById(vm.getTemplateId());
//    		resultObj.setPasswordEnabled(template.getEnablePassword());
//    		resultObj.setTemplateDisplayText(template.getDisplayText());
//    		resultObj.setTemplateId(template.getId());
//    		resultObj.setTemplateName(template.getName());
//        } 
//        else 
//        {
//        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update zone; internal error.");
//        }
//        
//        return SerializerHelper.toSerializedString(responseObject);
    }
    
    public void setResponseObject(UserVmVO userVm) {
        responseObject = userVm;
    }
    
    // helper class for the response object
    private class UpgradeVmResponse 
    {
    	@Param(name="id")
    	private long id;
    	
    	public String getName() {
    		return name;
    	}

    	public void setName(String name) {
    		this.name = name;
    	}

    	public Date getCreated() {
    		return created;
    	}

    	public void setCreated(Date created) {
    		this.created = created;
    	}

    	public String getIpAddress() {
    		return ipAddress;
    	}

    	public void setIpAddress(String ipAddress) {
    		this.ipAddress = ipAddress;
    	}

    	public String getState() {
    		return state;
    	}

    	public void setState(String state) {
    		this.state = state;
    	}

    	public String getAccount() {
    		return account;
    	}

    	public void setAccount(String account) {
    		this.account = account;
    	}

    	public long getDomainId() {
    		return domainId;
    	}

    	public void setDomainId(long domainId) {
    		this.domainId = domainId;
    	}

    	public String getDomain() {
    		return domain;
    	}

    	public void setDomain(String domain) {
    		this.domain = domain;
    	}

    	public boolean isHaEnable() {
    		return haEnable;
    	}

    	public void setHaEnable(boolean haEnable) {
    		this.haEnable = haEnable;
    	}

    	public long getZoneId() {
    		return zoneId;
    	}

    	public void setZoneId(long zoneId) {
    		this.zoneId = zoneId;
    	}

    	public String getDisplayName() {
    		return displayName;
    	}

    	public void setDisplayName(String displayName) {
    		this.displayName = displayName;
    	}

    	public String getZoneName() {
    		return zoneName;
    	}

    	public void setZoneName(String zoneName) {
    		this.zoneName = zoneName;
    	}

    	public long getHostId() {
    		return hostId;
    	}

    	public void setHostId(long hostId) {
    		this.hostId = hostId;
    	}

    	public String getHostName() {
    		return hostName;
    	}

    	public void setHostName(String hostName) {
    		this.hostName = hostName;
    	}

    	public long getTemplateId() {
    		return templateId;
    	}

    	public void setTemplateId(long templateId) {
    		this.templateId = templateId;
    	}

    	public String getTemplateName() {
    		return templateName;
    	}

    	public void setTemplateName(String templateName) {
    		this.templateName = templateName;
    	}

    	public String getTemplateDisplayText() {
    		return templateDisplayText;
    	}

    	public void setTemplateDisplayText(String templateDisplayText) {
    		this.templateDisplayText = templateDisplayText;
    	}

    	public boolean isPasswordEnabled() {
    		return passwordEnabled;
    	}

    	public void setPasswordEnabled(boolean passwordEnabled) {
    		this.passwordEnabled = passwordEnabled;
    	}

    	public long getServiceOfferingId() {
    		return serviceOfferingId;
    	}

    	public void setServiceOfferingId(long serviceOfferingId) {
    		this.serviceOfferingId = serviceOfferingId;
    	}

    	public String getServiceOfferingName() {
    		return serviceOfferingName;
    	}

    	public void setServiceOfferingName(String serviceOfferingName) {
    		this.serviceOfferingName = serviceOfferingName;
    	}

    	public long getCpuSpeed() {
    		return cpuSpeed;
    	}

    	public void setCpuSpeed(long cpuSpeed) {
    		this.cpuSpeed = cpuSpeed;
    	}

    	public long getMemory() {
    		return memory;
    	}

    	public void setMemory(long memory) {
    		this.memory = memory;
    	}

    	public long getCpuUsed() {
    		return cpuUsed;
    	}

    	public void setCpuUsed(long cpuUsed) {
    		this.cpuUsed = cpuUsed;
    	}

    	public long getNetworkKbsRead() {
    		return networkKbsRead;
    	}

    	public void setNetworkKbsRead(long networkKbsRead) {
    		this.networkKbsRead = networkKbsRead;
    	}

    	public long getNetworkKbsWrite() {
    		return networkKbsWrite;
    	}

    	public void setNetworkKbsWrite(long networkKbsWrite) {
    		this.networkKbsWrite = networkKbsWrite;
    	}

    	public long isId()
    	{
    		return id;
    	}
    	
    	@Param(name="name")
    	private String name;

        @Param(name="created")
        private Date created;

    	@Param(name="ipaddress")
    	private String ipAddress;

        @Param(name="state")
        private String state;
        
        @Param(name="account")
        private String account;
        
    	@Param(name="domainid")
    	private long domainId;
    	
    	@Param(name="domain")
    	private String domain;
        
    	@Param(name="haenable")
    	private boolean haEnable;
    	
    	@Param(name="zoneid")
    	private long zoneId;
    	
    	@Param(name="displayname")
    	private String displayName;

    	@Param(name="zonename")
    	private String zoneName;
    	
    	@Param(name="hostid")
    	private long hostId;

    	@Param(name="hostname")
    	private String hostName;

    	@Param(name="templateid")
    	private long templateId;
    	
    	@Param(name="templatename")
    	private String templateName;

    	@Param(name="templatedisplaytext")
    	private String templateDisplayText;
    	
    	@Param(name="passwordenabled")
    	private boolean passwordEnabled;

    	@Param(name="serviceofferingid")
    	private long serviceOfferingId;
    	
    	@Param(name="serviceofferingname")
    	private String serviceOfferingName;
    	
    	@Param(name="cpunumber")
    	private long cpuSpeed;

    	@Param(name="memory")
    	private long memory;

    	@Param(name="cpuused")
    	private long cpuUsed;
    	
    	@Param(name="networkkbsread")
    	private long networkKbsRead;
    	
    	@Param(name="networkkbswrite")
    	private long networkKbsWrite;
    }
    
}
