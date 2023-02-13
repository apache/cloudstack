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
package org.apache.cloudstack.api.command.admin.storage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ImageStoreResponse;

import com.cloud.exception.DiscoveryException;
import com.cloud.storage.ImageStore;
import com.cloud.user.Account;

@APICommand(name = "updateCloudToUseObjectStore", description = "Migrate current NFS secondary storages to use object store.", responseObject = ImageStoreResponse.class, since = "4.3.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateCloudToUseObjectStoreCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateCloudToUseObjectStoreCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the name for the image store")
    private String name;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, description="the URL for the image store")
    private String url;

    @Parameter(name=ApiConstants.PROVIDER, type=CommandType.STRING,
            required=true, description="the image store provider name")
    private String providerName;

    @Parameter(name=ApiConstants.DETAILS, type=CommandType.MAP, description="the details for the image store. Example: details[0].key=accesskey&details[0].value=s389ddssaa&details[1].key=secretkey&details[1].value=8dshfsss")
    private Map details;



    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getDetails() {
        Map<String, String> detailsMap = null;
        if (details != null && !details.isEmpty()) {
            detailsMap = new HashMap<String, String>();
            Collection<?> props = details.values();
            Iterator<?> iter = props.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> detail = (HashMap<String, String>) iter.next();
                String key = detail.get("key");
                String value = detail.get("value");
                detailsMap.put(key, value);
            }
        }
        return detailsMap;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////



    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        try{
            ImageStore result = _storageService.migrateToObjectStore(getName(), getUrl(), getProviderName(), getDetails());
            ImageStoreResponse storeResponse = null;
            if (result != null ) {
                storeResponse = _responseGenerator.createImageStoreResponse(result);
                storeResponse.setResponseName(getCommandName());
                storeResponse.setObjectName("imagestore");
                setResponseObject(storeResponse);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add secondary storage");
            }
        } catch (DiscoveryException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
