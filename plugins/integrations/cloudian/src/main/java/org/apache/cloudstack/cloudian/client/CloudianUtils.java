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

package org.apache.cloudstack.cloudian.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.utils.HttpUtils;
import org.apache.commons.lang3.StringUtils;

public class CloudianUtils {

    protected static Logger LOGGER = LogManager.getLogger(CloudianUtils.class);
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /**
     * Generates RFC-2104 compliant HMAC signature
     * @param data
     * @param key
     * @return returns the generated signature or null on error
     */
    public static String generateHMACSignature(final String data, final String key) {
        if (StringUtils.isAnyEmpty(data, key)) {
            return null;
        }
        try {
            final SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
            final Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(data.getBytes());
            return Base64.encodeBase64String(rawHmac);
        } catch (final Exception e) {
            LOGGER.error("Failed to generate HMAC signature from provided data and key, due to: ", e);
        }
        return null;
    }

    /**
     * Generates URL parameters for single-sign on URL
     * @param user
     * @param group
     * @param ssoKey
     * @return returns SSO URL parameters or null on error
     */
    public static String generateSSOUrl(final String cmcUrlPath, final String user, final String group, final String ssoKey) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("user=").append(user);
        stringBuilder.append("&group=").append(group);
        stringBuilder.append("&timestamp=").append(System.currentTimeMillis());

        final String signature = generateHMACSignature(stringBuilder.toString(), ssoKey);
        if (StringUtils.isEmpty(signature)) {
            return null;
        }

        try {
            stringBuilder.append("&signature=").append(URLEncoder.encode(signature, HttpUtils.UTF_8));
        } catch (final UnsupportedEncodingException e) {
            return null;
        }

        // Redirects to dashboard for admin users or the bucket browser for regular users
        stringBuilder.append("&redirect=/");

        return cmcUrlPath + "ssosecurelogin.htm?" + stringBuilder.toString();
    }
}
