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

package com.cloud.info;

public class ConsoleProxyInfo {

	private boolean sslEnabled;
	private String proxyAddress;
	private int proxyPort;
	private String proxyImageUrl;
	private int proxyUrlPort = 8000;

	public ConsoleProxyInfo(int proxyUrlPort) {
		this.proxyUrlPort = proxyUrlPort;
	}
	
	public ConsoleProxyInfo(boolean sslEnabled, String proxyIpAddress, int port, int proxyUrlPort) {
		this.sslEnabled = sslEnabled;
		
		if(sslEnabled) {
			StringBuffer sb = new StringBuffer(proxyIpAddress);
			for(int i = 0; i < sb.length(); i++)
				if(sb.charAt(i) == '.')
					sb.setCharAt(i, '-');
			sb.append(".realhostip.com");
			proxyAddress = sb.toString();
			proxyPort = port;
			this.proxyUrlPort = proxyUrlPort;
			
			proxyImageUrl = "https://" + proxyAddress;
			if(proxyUrlPort != 443)
				proxyImageUrl += ":" + this.proxyUrlPort;
		} else {
			proxyAddress = proxyIpAddress;
			proxyPort = port;
			this.proxyUrlPort = proxyUrlPort;
			
			proxyImageUrl = "http://" + proxyAddress;
			if(proxyUrlPort != 80)
				proxyImageUrl += ":" + proxyUrlPort;
		}
	}
	
	public String getProxyAddress() {
		return proxyAddress;
	}
	
	public void setProxyAddress(String proxyAddress) {
		this.proxyAddress = proxyAddress;
	}
	
	public int getProxyPort() {
		return proxyPort;
	}
	
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}
	
	public String getProxyImageUrl() {
		return proxyImageUrl;
	}
	
	public void setProxyImageUrl(String proxyImageUrl) {
		this.proxyImageUrl = proxyImageUrl;
	}
	
	public boolean isSslEnabled() {
		return sslEnabled;
	}

	public void setSslEnabled(boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}
}
