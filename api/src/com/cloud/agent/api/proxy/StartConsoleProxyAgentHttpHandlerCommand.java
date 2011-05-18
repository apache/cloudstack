package com.cloud.agent.api.proxy;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.agent.api.LogLevel;


public class StartConsoleProxyAgentHttpHandlerCommand extends Command {
    @LogLevel(Log4jLevel.Off)
	private byte[] keystoreBits;
    @LogLevel(Log4jLevel.Off)
	private String keystorePassword;
	
	public StartConsoleProxyAgentHttpHandlerCommand() {
		super();
	}
	
	public StartConsoleProxyAgentHttpHandlerCommand(byte[] ksBits, String ksPassword) {
		this.keystoreBits = ksBits;
		this.keystorePassword = ksPassword;
	}
	
	@Override
	public boolean executeInSequence() {
		return true;
	}
	
	public byte[] getKeystoreBits() {
		return keystoreBits;
	}

	public void setKeystoreBits(byte[] keystoreBits) {
		this.keystoreBits = keystoreBits;
	}

	public String getKeystorePassword() {
		return keystorePassword;
	}

	public void setKeystorePassword(String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}
}
