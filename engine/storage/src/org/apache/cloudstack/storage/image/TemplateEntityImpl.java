/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.image;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.storage.image.datastore.ImageDataStoreInfo;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;

public class TemplateEntityImpl implements TemplateEntity {
    protected TemplateInfo templateInfo;

    public TemplateEntityImpl(TemplateInfo templateInfo) {
        this.templateInfo = templateInfo;
    }

    public ImageDataStoreInfo getImageDataStore() {
        return (ImageDataStoreInfo)templateInfo.getDataStore();
    }

    public long getImageDataStoreId() {
        return getImageDataStore().getImageDataStoreId();
    }

    public TemplateInfo getTemplateInfo() {
        return this.templateInfo;
    }

    @Override
    public String getUuid() {
        return this.templateInfo.getUuid();
    }

    @Override
    public long getId() {
       return this.templateInfo.getId();
    }

    public String getExternalId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCurrentState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDesiredState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getCreatedTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getLastUpdatedTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOwner() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getDetails() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void addDetail(String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delDetail(String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateDetail(String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Method> getApplicableActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isFeatured() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isPublicTemplate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isExtractable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ImageFormat getFormat() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isRequiresHvm() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getDisplayText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getEnablePassword() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getEnableSshKey() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCrossZones() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Date getCreated() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getGuestOSId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isBootable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public TemplateType getTemplateType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HypervisorType getHypervisorType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getBits() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getUniqueName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getChecksum() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long getSourceTemplateId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTemplateTag() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getAccountId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getDomainId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getPhysicalSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getVirtualSize() {
        // TODO Auto-generated method stub
        return 0;
    }

}
