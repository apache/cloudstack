//
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
//

package org.apache.cloudstack.storage.command;

import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import com.cloud.agent.api.storage.AbstractDownloadCommand;
import com.cloud.agent.api.storage.PasswordAuth;
import com.cloud.utils.net.Proxy;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.storage.Storage.ImageFormat;

public class DownloadCommand extends AbstractDownloadCommand implements InternalIdentity {

    public static enum ResourceType {
        VOLUME, TEMPLATE, SNAPSHOT
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

    private boolean followRedirects = false;

    protected DownloadCommand() {
    }

    public DownloadCommand(DownloadCommand that) {
        super(that);
        hvm = that.hvm;
        checksum = that.checksum;
        id = that.id;
        description = that.description;
        auth = that.getAuth();
        setSecUrl(that.getSecUrl());
        maxDownloadSizeInBytes = that.getMaxDownloadSizeInBytes();
        resourceType = that.resourceType;
        installPath = that.installPath;
        _store = that._store;
        _proxy = that._proxy;
        followRedirects = that.followRedirects;
    }

    public DownloadCommand(TemplateObjectTO template, Long maxDownloadSizeInBytes) {

        super(template.getName(), template.getOrigUrl(), template.getFormat(), template.getAccountId());
        _store = template.getDataStore();
        installPath = template.getPath();
        hvm = template.isRequiresHvm();
        checksum = template.getChecksum();
        id = template.getId();
        description = template.getDescription();
        if (_store instanceof NfsTO) {
            setSecUrl(((NfsTO)_store).getUrl());
        }
        this.maxDownloadSizeInBytes = maxDownloadSizeInBytes;
        this.followRedirects = template.isFollowRedirects();
    }

    public DownloadCommand(TemplateObjectTO template, String user, String passwd, Long maxDownloadSizeInBytes) {
        this(template, maxDownloadSizeInBytes);
        auth = new PasswordAuth(user, passwd);
    }

    public DownloadCommand(VolumeObjectTO volume, Long maxDownloadSizeInBytes, String checkSum, String url, ImageFormat format) {
        super(volume.getName(), url, format, volume.getAccountId());
        checksum = checkSum;
        id = volume.getVolumeId();
        installPath = volume.getPath();
        _store = volume.getDataStore();
        this.maxDownloadSizeInBytes = maxDownloadSizeInBytes;
        resourceType = ResourceType.VOLUME;
        this.followRedirects = volume.isFollowRedirects();
    }

    public DownloadCommand(SnapshotObjectTO snapshot, Long maxDownloadSizeInBytes, String url) {
        super(snapshot.getName(), url, null, snapshot.getAccountId());
        _store = snapshot.getDataStore();
        installPath = snapshot.getPath();
        id = snapshot.getId();
        if (_store instanceof NfsTO) {
            setSecUrl(((NfsTO)_store).getUrl());
        }
        this.maxDownloadSizeInBytes = maxDownloadSizeInBytes;
        this.resourceType = ResourceType.SNAPSHOT;
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

    public void setDataStore(DataStoreTO store) {
        this._store = store;
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
        return cacheStore;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }
}
