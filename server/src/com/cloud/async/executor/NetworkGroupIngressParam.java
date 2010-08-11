package com.cloud.async.executor;

import java.util.List;

import com.cloud.network.security.NetworkGroupVO;
import com.cloud.user.AccountVO;

public class NetworkGroupIngressParam {
	private AccountVO account;
	private String groupName;
	private String protocol;
	private int startPort;
	private int endPort;
	private String[] cidrList;
	private List<NetworkGroupVO> authorizedGroups;

	protected NetworkGroupIngressParam() { }

	public NetworkGroupIngressParam(AccountVO account, String groupName, String protocol, int startPort, int endPort, String[] cidrList, List<NetworkGroupVO> authorizedGroups) {
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

	public List<NetworkGroupVO> getAuthorizedGroups() {
		return authorizedGroups;
	}
}
