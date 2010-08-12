package com.cloud.async.executor;

import java.util.Date;

import com.cloud.serializer.Param;

public class VmResultObject 
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
