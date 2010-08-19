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

package com.cloud.consoleproxy;

import com.cloud.utils.events.EventArgs;
import com.cloud.vm.ConsoleProxyVO;

public class ConsoleProxyAlertEventArgs extends EventArgs {
	
	private static final long serialVersionUID = 23773987551479885L;
	
	public static final int PROXY_CREATED = 1;
	public static final int PROXY_UP = 2; 
	public static final int PROXY_DOWN = 3; 
	public static final int PROXY_CREATE_FAILURE = 4;
	public static final int PROXY_START_FAILURE = 5;
	public static final int PROXY_FIREWALL_ALERT = 6;
	public static final int PROXY_STORAGE_ALERT = 7;
	public static final int PROXY_REBOOTED = 8;
	
	private int type;
	private long zoneId;
	private long proxyId;
	private ConsoleProxyVO proxy;
	private String message;
	
	public ConsoleProxyAlertEventArgs(int type, long zoneId, 
		long proxyId, ConsoleProxyVO proxy, String message) {
		
		super(ConsoleProxyManager.ALERT_SUBJECT);
		this.type = type;
		this.zoneId = zoneId;
		this.proxyId = proxyId;
		this.proxy = proxy;
		this.message = message;
	}
	
	public int getType() {
		return type;
	}

	public long getZoneId() {
		return zoneId;
	}

	public long getProxyId() {
		return proxyId;
	}

	public ConsoleProxyVO getProxy() {
		return proxy;
	}

	public String getMessage() {
		return message;
	}
}
