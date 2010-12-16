package com.cloud.async.executor;

import java.util.List;

import com.cloud.network.security.SecurityGroupVO;
import com.cloud.user.AccountVO;

public class SecurityGroupIngressParam {
	private AccountVO account;
	private String groupName;
	private String protocol;
	private int startPort;
	private int endPort;
	private String[] cidrList;
	private List<SecurityGroupVO> authorizedGroups;

	protected SecurityGroupIngressParam() { }

	public SecurityGroupIngressParam(AccountVO account, String groupName, String protocol, int startPort, int endPort, String[] cidrList, List<SecurityGroupVO> authorizedGroups) {
		this.account = account;
		this.groupName = groupName;
		this.protocol = protocol;
		this.startPort = startPort;
		this.endPort = endPort;
		this.cidrList = cidrList;
		this.authorizedGroups = authorizedGroups;
	}

	public AccountVO getAccount() {
		return account;
	}

	public String getGroupName() {
		return groupName;
	}

	public String getProtocol() {
		return protocol;
	}

	public int getStartPort() {
		return startPort;
	}

	public int getEndPort() {
		return endPort;
	}

	public String[] getCidrList() {
		return cidrList;
	}

	public List<SecurityGroupVO> getAuthorizedGroups() {
		return authorizedGroups;
	}
}
