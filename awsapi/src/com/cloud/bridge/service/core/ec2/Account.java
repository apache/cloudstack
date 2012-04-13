package com.cloud.bridge.service.core.ec2;

public class Account {

	private String id;
	private String accountName;
	private String domainId;
	private String domainName;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
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
	
	public String getDomainName() {
		return domainName;
	}
	
	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

}
