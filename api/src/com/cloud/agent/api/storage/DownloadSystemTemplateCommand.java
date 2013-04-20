// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.agent.api.storage;

import java.net.URI;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.DownloadCommand.Proxy;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.template.VirtualMachineTemplate;


public class DownloadSystemTemplateCommand extends Command {
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


	private PasswordAuth auth;
	private Proxy _proxy;
	private DataStoreTO _store;
    private Long resourceId;
    private Long accountId;
    private String url;
    private Long maxDownloadSizeInBytes;

	protected DownloadSystemTemplateCommand() {
	}



	public DownloadSystemTemplateCommand(DataStoreTO store, String secUrl, VirtualMachineTemplate template, Long maxDownloadSizeInBytes) {
	    this._store = store;
	    this.accountId = template.getAccountId();
	    this.url = secUrl;
	    this.maxDownloadSizeInBytes = maxDownloadSizeInBytes;
	    this.resourceId = template.getId();
	}


	public DownloadSystemTemplateCommand(DataStoreTO store, String secUrl, String url, VirtualMachineTemplate template, String user, String passwd, Long maxDownloadSizeInBytes) {
        this._store = store;
        this.accountId = template.getAccountId();
        this.url = secUrl;
        this.maxDownloadSizeInBytes = maxDownloadSizeInBytes;
        this.resourceId = template.getId();
		auth = new PasswordAuth(user, passwd);
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


    public DataStoreTO getDataStore() {
        return _store;
    }


    public void setDataStore(DataStoreTO _store) {
        this._store = _store;
    }


    public Long getResourceId() {
        return resourceId;
    }


    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }



    public Long getAccountId() {
        return accountId;
    }



    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }



    public String getUrl() {
        return url;
    }



    public void setUrl(String url) {
        this.url = url;
    }



    @Override
    public boolean executeInSequence() {
        // TODO Auto-generated method stub
        return false;
    }


}
