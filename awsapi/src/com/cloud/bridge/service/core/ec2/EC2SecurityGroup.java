package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.List;

public class EC2SecurityGroup {

	private String id;
	private String name;
	private String description;
	private String accountName;
	private String domainId;
	private List<EC2IpPermission> permissionSet = new ArrayList<EC2IpPermission>();    

	public EC2SecurityGroup() {
		id          = null;
		name        = null;
		description = null;
		accountName = null;
		domainId	= null;
	}

	public void setId( String id ) {
		this.id = id;
	}
	
	public String getId() {
		return this.id;
	}

	public void setName( String name ) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}

	public void setDescription( String description ) {
		this.description = description;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public void setAccount( String account ) {
		this.accountName = account;
	}
	
	public String getAccount() {
		return this.accountName;
	}
	
	public void addIpPermission( EC2IpPermission param ) {
		permissionSet.add( param );
	}
	
	public EC2IpPermission[] getIpPermissionSet() {
		return permissionSet.toArray(new EC2IpPermission[0]);
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getDomainId() {
		return domainId;
	}

	public void setDomainId(String domainId) {
		this.domainId = domainId;
	}
	
}
