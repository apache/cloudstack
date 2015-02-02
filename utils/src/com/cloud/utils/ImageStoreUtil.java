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
package com.cloud.utils;

import org.apache.log4j.Logger;

public class ImageStoreUtil {
    public static final Logger s_logger = Logger.getLogger(ImageStoreUtil.class.getName());

    public static String generatePostUploadUrl(String ssvmUrlDomain, String ipAddress, String uuid) {
        String hostname = ipAddress;

        //if ssvm url domain is present, use it to construct hostname in the format 1-2-3-4.domain
        // if the domain name is not present, ssl validation fails and has to be ignored
        if(StringUtils.isNotBlank(ssvmUrlDomain)) {
            hostname = ipAddress.replace(".", "-");
            hostname = hostname + ssvmUrlDomain.substring(1);
        }

        //only https works with postupload and url format is fixed
        return "https://" + hostname + "/upload/" + uuid;
    }
}
