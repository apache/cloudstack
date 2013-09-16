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
package org.apache.cloudstack.storage.command;

import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import com.cloud.agent.api.storage.AbstractDownloadCommand;
import com.cloud.agent.api.storage.PasswordAuth;
import com.cloud.agent.api.storage.Proxy;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Volume;


public class DownloadCommand extends AbstractDownloadCommand implements InternalIdentity {

    public static enum ResourceType {
        VOLUME, TEMPLATE
    }

	private boolean hvm;
	private String description;
	private String checksum;
	private PasswordAuth auth;
	private Proxy _proxy;
	private Long maxDownloadSizeInBytes = null;
	private long id;
	private ResourceType resourceType = ResourceType.TEMPLATE;
	private String installPath;
	private DataStoreTO _store;
    private DataStoreTO cacheStore;

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
	    this.resourceType = that.resourceType;
	    this.installPath = that.installPath;
	    this._store = that._store;
	}

	public DownloadCommand(TemplateObjectTO template, Long maxDownloadSizeInBytes) {

	    super(template.getName(), template.getOrigUrl(), template.getFormat(), template.getAccountId());
	    this._store = template.getDataStore();
	    this.installPath = template.getPath();
	    this.hvm = template.isRequiresHvm();
	    this.checksum = template.getChecksum();
	    this.id = template.getId();
	    this.description = template.getDescription();
        if (_store instanceof NfsTO) {
            this.setSecUrl(((NfsTO) _store).getUrl());
        }
	    this.maxDownloadSizeInBytes = maxDownloadSizeInBytes;
	}

	public DownloadCommand(TemplateObjectTO template, String user, String passwd, Long maxDownloadSizeInBytes) {
	    this(template, maxDownloadSizeInBytes);
		auth = new PasswordAuth(user, passwd);
	}

    public DownloadCommand(VolumeObjectTO volume, Long maxDownloadSizeInBytes, String checkSum, String url, ImageFormat format) {
        super(volume.getName(), url, format, volume.getAccountId());
        this.checksum = checkSum;
        this.id = volume.getVolumeId();
        this.installPath = volume.getPath();
        this._store = volume.getDataStore();
        this.maxDownloadSizeInBytes = maxDownloadSizeInBytes;
        this.resourceType = ResourceType.VOLUME;
    }
	@Override
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


	public ResourceType getResourceType() {
		return resourceType;
	}


	public void setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
	}


    public DataStoreTO getDataStore() {
        return _store;
    }


    public void setDataStore(DataStoreTO _store) {
        this._store = _store;
    }


    public String getInstallPath() {
        return installPath;
    }


    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public void setCacheStore(DataStoreTO cacheStore) {
        this.cacheStore = cacheStore;
    }

    public DataStoreTO getCacheStore() {
        return this.cacheStore;
    }
}
