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

package com.cloud.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.configuration.dao.ConfigurationDao;

import com.cloud.dc.dao.VlanDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value=IpAddrAllocator.class)
public class ExteralIpAddressAllocator implements IpAddrAllocator{
	private static final Logger s_logger = Logger.getLogger(ExteralIpAddressAllocator.class);
	String _name;
    @Inject ConfigurationDao _configDao = null;
    @Inject IPAddressDao _ipAddressDao = null;
    @Inject VlanDao _vlanDao;
	private boolean _isExternalIpAllocatorEnabled = false;
	private String _externalIpAllocatorUrl;

	
	@Override
	public IpAddr getPrivateIpAddress(String macAddr, long dcId, long podId) {
		if (this._externalIpAllocatorUrl.equalsIgnoreCase("")) {
			return new IpAddr();
		}
		String urlString = this._externalIpAllocatorUrl + "?command=getIpAddr&mac=" + macAddr + "&dc=" + dcId + "&pod=" + podId;
		 s_logger.debug("getIP:" + urlString);
		
		BufferedReader in = null;
		try {
			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();
			conn.setReadTimeout(30000);
			
	        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String inputLine;
	        while ((inputLine = in.readLine()) != null) {
	        	s_logger.debug(inputLine);
	        	String[] tokens = inputLine.split(",");
	        	if (tokens.length != 3) {
	        		s_logger.debug("the return value should be: mac,netmask,gateway");
	        		return new IpAddr();
	        	}
	    		return new IpAddr(tokens[0], tokens[1], tokens[2]);
	        }
	        
	        return new IpAddr();
		} catch (MalformedURLException e) {
			throw new CloudRuntimeException("URL is malformed " + urlString, e);
		} catch (IOException e) {
			return new IpAddr();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
		
	}
	
	@Override
	public IpAddr getPublicIpAddress(String macAddr, long dcId, long podId) {
		/*TODO: call API to get  ip address from external DHCP server*/
		return getPrivateIpAddress(macAddr, dcId, podId);
	}
	
	@Override
	public boolean releasePrivateIpAddress(String ip, long dcId, long podId) {
		/*TODO: call API to release the ip address from external DHCP server*/
		String urlString = this._externalIpAllocatorUrl + "?command=releaseIpAddr&ip=" + ip + "&dc=" + dcId + "&pod=" + podId;
		if (this._externalIpAllocatorUrl.equalsIgnoreCase("")) {
			return false;
		}
		 s_logger.debug("releaseIP:" + urlString);
		BufferedReader in = null;
		try {
			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();
			conn.setReadTimeout(30000);
			
	        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        
	        return true;
		} catch (MalformedURLException e) {
			throw new CloudRuntimeException("URL is malformed " + urlString, e);
		} catch (IOException e) {
			return false;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	@Override
	public boolean releasePublicIpAddress(String ip, long dcId, long podId) {
		/*TODO: call API to release the ip address from external DHCP server*/
		return releasePrivateIpAddress(ip, dcId, podId);
	}
	
	public boolean exteralIpAddressAllocatorEnabled() {
		return _isExternalIpAllocatorEnabled;
	}
	
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		ComponentLocator locator = ComponentLocator.getCurrentLocator();
		_configDao = locator.getDao(ConfigurationDao.class);
		_isExternalIpAllocatorEnabled = Boolean.parseBoolean(_configDao.getValue("direct.attach.network.externalIpAllocator.enabled"));
		_externalIpAllocatorUrl = _configDao.getValue("direct.attach.network.externalIpAllocator.url");
		_name = name;
		
		return true;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}
}
