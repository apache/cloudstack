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
package com.cloud.agent.api.storage;

import java.net.URI;

import com.cloud.storage.Storage.ImageFormat;
import com.cloud.template.VirtualMachineTemplate;


/**
 * @author chiradeep
 *
 */
public class DownloadCommand extends AbstractDownloadCommand {
	public static class PasswordAuth {
		String userName;
		String password;
		public PasswordAuth() {
			
		}
		public PasswordAuth(String user, String password) {
			this.userName = user;
			this.password = password;
		}
		public String getUserName() {
			return userName;
		}
		public String getPassword() {
			return password;
		}
	}
	
	public static class Proxy {
		private String _host;
		private int _port;
		private String _userName;
		private String _password;
		
		public Proxy() {
			
		}
		
		public Proxy(String host, int port, String userName, String password) {
			this._host = host;
			this._port = port;
			this._userName = userName;
			this._password = password;
		}
		
		public Proxy(URI uri) {
			this._host = uri.getHost();
			this._port = uri.getPort() == -1 ? 3128 : uri.getPort();
			String userInfo = uri.getUserInfo();
			if (userInfo != null) {
				String[] tokens = userInfo.split(":");
				if (tokens.length == 1) {
					this._userName = userInfo;
					this._password = "";
				} else if (tokens.length == 2) {
					this._userName = tokens[0];
					this._password = tokens[1];
				}
			}
		}
		
		public String getHost() {
			return _host;
		}
		
		public int getPort() {
			return _port;
		}
		
		public String getUserName() {
			return _userName;
		}
		
		public String getPassword() {
			return _password;
		}
	}
	private boolean hvm;
	private String description;
	private String checksum;
	private PasswordAuth auth;
	private Proxy _proxy;
	private Long maxDownloadSizeInBytes = null;
	private long id;
	
	protected DownloadCommand() {
	}
	
	
	public DownloadCommand(DownloadCommand that) {
	    super(that);
	    this.hvm = that.hvm;
	    this.checksum = that.checksum;
	    this.id = that.id;
	    this.description = that.description;
	    this.auth = that.getAuth();
	    this.setSecUrl(that.getSecUrl());
	    this.maxDownloadSizeInBytes = that.getMaxDownloadSizeInBytes();
	}
	
	public DownloadCommand(String secUrl, VirtualMachineTemplate template, Long maxDownloadSizeInBytes) {
	    super(template.getUniqueName(), template.getUrl(), template.getFormat(), template.getAccountId());
	    this.hvm = template.isRequiresHvm();
	    this.checksum = template.getChecksum();
	    this.id = template.getId();
	    this.description = template.getDisplayText();
	    this.setSecUrl(secUrl);
	    this.maxDownloadSizeInBytes = maxDownloadSizeInBytes;
	}

	public DownloadCommand(String secUrl, String url, VirtualMachineTemplate template, String user, String passwd, Long maxDownloadSizeInBytes) {
	    super(template.getUniqueName(), url, template.getFormat(), template.getAccountId());
        this.hvm = template.isRequiresHvm();
        this.checksum = template.getChecksum();
        this.id = template.getId();
        this.description = template.getDisplayText();
        this.setSecUrl(secUrl);
        this.maxDownloadSizeInBytes = maxDownloadSizeInBytes;
		auth = new PasswordAuth(user, passwd);
	}
	
	public long getId() {
	    return id;
	}
	
	public void setHvm(boolean hvm) {
		this.hvm = hvm;
	}

	public boolean isHvm() {
		return hvm;
	}

	public String getDescription() {
		return description;
	}

	public String getChecksum() {
		return checksum;
	}

    public void setDescription(String description) {
		this.description = description;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

    @Override
    public boolean executeInSequence() {
        return false;
    }


	public PasswordAuth getAuth() {
		return auth;
	}
	
	public void setCreds(String userName, String passwd) {
		auth = new PasswordAuth(userName, passwd);
	}
	
	public Proxy getProxy() {
		return _proxy;
	}
	
	public void setProxy(Proxy proxy) {
		_proxy = proxy;
	}
	
	public Long getMaxDownloadSizeInBytes() {
		return maxDownloadSizeInBytes;
	}
}
