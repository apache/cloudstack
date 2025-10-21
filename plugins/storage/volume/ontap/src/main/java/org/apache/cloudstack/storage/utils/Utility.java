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

package org.apache.cloudstack.storage.utils;

import com.cloud.utils.StringUtils;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import javax.inject.Inject;
import java.net.URI;

@Component
public class Utility {
    @Inject
    OntapStorage ontapStorage;

    private static final String BASIC = "Basic";
    private static final String AUTH_HEADER_COLON = ":";
    /**
     * Method generates authentication headers using storage backend credentials passed as normal string
     * @param username  -->> username of the storage backend
     * @param password  -->> normal decoded password of the storage backend
     * @return
     */
    public String generateAuthHeader(String username, String password) {
        byte[] encodedBytes = Base64Utils.encode((username + AUTH_HEADER_COLON + password).getBytes());
        return BASIC + StringUtils.SPACE + new String(encodedBytes);
    }

    public URI generateURI(String path) {
        String uriString = Constants.HTTPS + ontapStorage.getManagementLIF() + path;
        return URI.create(uriString);
    }
}
