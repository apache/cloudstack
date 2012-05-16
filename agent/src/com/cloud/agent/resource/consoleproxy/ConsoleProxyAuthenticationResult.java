package com.cloud.agent.resource.consoleproxy;

public class ConsoleProxyAuthenticationResult {
	private boolean success;
	private boolean isReauthentication;
	private String host;
	private int port;
	private String tunnelUrl;
	private String tunnelSession;
	
	public ConsoleProxyAuthenticationResult() {
		success = false;
		isReauthentication = false;
		port = 0;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isReauthentication() {
		return isReauthentication;
	}

	public void setReauthentication(boolean isReauthentication) {
		this.isReauthentication = isReauthentication;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getTunnelUrl() {
		return tunnelUrl;
	}

	public void setTunnelUrl(String tunnelUrl) {
		this.tunnelUrl = tunnelUrl;
	}

	public String getTunnelSession() {
		return tunnelSession;
	}

	public void setTunnelSession(String tunnelSession) {
		this.tunnelSession = tunnelSession;
	}
}
