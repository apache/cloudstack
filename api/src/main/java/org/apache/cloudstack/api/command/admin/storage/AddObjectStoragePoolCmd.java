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

import org.apache.cloudstack.storage.object.ObjectStore;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ObjectStoreResponse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@APICommand(name = "addObjectStoragePool", description = "Adds a object storage pool", responseObject = ObjectStoreResponse.class, since = "4.19.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AddObjectStoragePoolCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name for the object store")
    private String name;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, length = 2048, required = true, description = "the URL for the object store")
    private String url;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, required = true, description = "the object store provider name")
    private String providerName;

    @Parameter(name = ApiConstants.DETAILS,
               type = CommandType.MAP,
               description = "the details for the object store. Example: details[0].key=accesskey&details[0].value=s389ddssaa&details[1].key=secretkey&details[1].value=8dshfsss")
    private Map details;

    @Parameter(name = ApiConstants.TAGS, type = CommandType.STRING, description = "the tags for the storage pool")
    private String tags;

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
                HashMap<String, String> detail = (HashMap<String, String>)iter.next();
                String key = detail.get(ApiConstants.KEY);
                String value = detail.get(ApiConstants.VALUE);
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
            ObjectStore result = _storageService.discoverObjectStore(getName(), getUrl(), getProviderName(), getDetails());
            ObjectStoreResponse storeResponse = null;
            if (result != null) {
                storeResponse = _responseGenerator.createObjectStoreResponse(result);
                storeResponse.setResponseName(getCommandName());
                storeResponse.setObjectName("objectstore");
                setResponseObject(storeResponse);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add object storage");
            }
        } catch (Exception ex) {
            logger.error("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
