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
package org.apache.cloudstack.userdata;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class UserDataManagerImpl extends ManagerBase implements UserDataManager {


    private static final int MAX_USER_DATA_LENGTH_BYTES = 2048;
    private static final int MAX_HTTP_GET_LENGTH = 2 * MAX_USER_DATA_LENGTH_BYTES;
    private static final int NUM_OF_2K_BLOCKS = 512;
    private static final int MAX_HTTP_POST_LENGTH = NUM_OF_2K_BLOCKS * MAX_USER_DATA_LENGTH_BYTES;
    private List<UserDataProvider> userDataProviders;
    private static Map<String, UserDataProvider> userDataProvidersMap = new HashMap<>();

    public void setUserDataProviders(final List<UserDataProvider> userDataProviders) {
        this.userDataProviders = userDataProviders;
    }

    private void initializeUserdataProvidersMap() {
        if (userDataProviders != null) {
            for (final UserDataProvider provider : userDataProviders) {
                userDataProvidersMap.put(provider.getName().toLowerCase(), provider);
            }
        }
    }

    @Override
    public boolean start() {
        initializeUserdataProvidersMap();
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return UserDataManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {};
    }

    protected UserDataProvider getUserdataProvider(String name) {
        if (StringUtils.isEmpty(name)) {
            // Use cloud-init as the default userdata provider
            name = "cloud-init";
        }
        if (!userDataProvidersMap.containsKey(name)) {
            throw new CloudRuntimeException("Failed to find userdata provider by the name: " + name);
        }
        return userDataProvidersMap.get(name);
    }

    @Override
    public String concatenateUserData(String userdata1, String userdata2, String userdataProvider) {
        UserDataProvider provider = getUserdataProvider(userdataProvider);
        String appendUserData = provider.appendUserData(userdata1, userdata2);
        return Base64.encodeBase64String(appendUserData.getBytes());
    }

    @Override
    public String validateUserData(String userData, BaseCmd.HTTPMethod httpmethod) {
        byte[] decodedUserData = null;
        if (userData != null) {

            if (userData.contains("%")) {
                try {
                    userData = URLDecoder.decode(userData, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new InvalidParameterValueException("Url decoding of userdata failed.");
                }
            }

            if (!Base64.isBase64(userData)) {
                throw new InvalidParameterValueException("User data is not base64 encoded");
            }
            // If GET, use 4K. If POST, support up to 1M.
            if (httpmethod.equals(BaseCmd.HTTPMethod.GET)) {
                decodedUserData = validateAndDecodeByHTTPMethod(userData, MAX_HTTP_GET_LENGTH, BaseCmd.HTTPMethod.GET);
            } else if (httpmethod.equals(BaseCmd.HTTPMethod.POST)) {
                decodedUserData = validateAndDecodeByHTTPMethod(userData, MAX_HTTP_POST_LENGTH, BaseCmd.HTTPMethod.POST);
            }

            if (decodedUserData == null || decodedUserData.length < 1) {
                throw new InvalidParameterValueException("User data is too short");
            }
            // Re-encode so that the '=' paddings are added if necessary since 'isBase64' does not require it, but python does on the VR.
            return Base64.encodeBase64String(decodedUserData);
        }
        return null;
    }

    private byte[] validateAndDecodeByHTTPMethod(String userData, int maxHTTPLength, BaseCmd.HTTPMethod httpMethod) {
        byte[] decodedUserData = null;

        if (userData.length() >= maxHTTPLength) {
            throw new InvalidParameterValueException(String.format("User data is too long for an http %s request", httpMethod.toString()));
        }
        if (userData.length() > ConfigurationManager.VM_USERDATA_MAX_LENGTH.value()) {
            throw new InvalidParameterValueException("User data has exceeded configurable max length : " + ConfigurationManager.VM_USERDATA_MAX_LENGTH.value());
        }
        decodedUserData = Base64.decodeBase64(userData.getBytes());
        if (decodedUserData.length > maxHTTPLength) {
            throw new InvalidParameterValueException(String.format("User data is too long for http %s request", httpMethod.toString()));
        }
        return decodedUserData;
    }
}
