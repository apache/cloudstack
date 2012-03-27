package com.cloud.consoleproxy;

/**
 * @author Kelven Yang
 * ConsoleProxyClient defines an standard interface that a console client should implement,
 * example of such console clients could be a VNC client or a RDP client
 * 
 * ConsoleProxyClient maintains a session towards the target host, it glues the session
 * to a AJAX frontend viewer 
 */
public interface ConsoleProxyClient {
	int getClientId();
	
	long getClientCreateTime();
	long getClientLastFrontEndActivityTime();
	
	String getClientHostAddress();
	int getClientHostPort();
	String getClientHostPassword();
	String getClientTag();

	void initClient(String clientHostAddress, int clientHostPort, 
		String clientHostPassword, String clientTag);
	void closeClient();
}
