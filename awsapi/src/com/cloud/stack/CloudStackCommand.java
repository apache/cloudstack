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
package com.cloud.stack;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * CloudStackCommand wraps command properties that are being sent to CloudStack
 *
 */
public class CloudStackCommand {
    Map<String, String> _params = new HashMap<String, String>();

    public CloudStackCommand(String cmdName) {
        this(cmdName, "json");
    }

    public CloudStackCommand(String cmdName, String responseType) {
        _params.put("command", cmdName);
        if (responseType != null)
            _params.put("response", responseType);
    }

    public CloudStackCommand setParam(String paramName, String paramValue) {
        assert (paramName != null);
        assert (paramValue != null);

        _params.put(paramName, paramValue);
        return this;
    }

    public String signCommand(String apiKey, String secretKey) throws SignatureException {
        assert (_params.get("command") != null);

        List<String> paramNames = new ArrayList<String>();
        for (String paramName : _params.keySet())
            paramNames.add(paramName);

        paramNames.add("apikey");
        Collections.sort(paramNames);

        StringBuffer sb = new StringBuffer();
        for (String name : paramNames) {
            String value;
            if ("apikey".equals(name))
                value = apiKey;
            else
                value = _params.get(name);

            assert (value != null);

            value = urlSafe(value);

            if (sb.length() == 0) {
                sb.append(name).append("=").append(value);
            } else {
                sb.append("&").append(name).append("=").append(value);
            }
        }

        String signature = calculateRFC2104HMAC(sb.toString().toLowerCase(), secretKey);
        return composeQueryString(apiKey, signature);
    }

    private String composeQueryString(String apiKey, String signature) {
        StringBuffer sb = new StringBuffer();
        String name;
        String value;

        // treat command specially (although not really necessary )
        name = "command";
        value = _params.get(name);
        if (value != null) {
            value = urlSafe(value);
            sb.append(name).append("=").append(value);
        }

        for (Map.Entry<String, String> entry : _params.entrySet()) {
            name = entry.getKey();

            if (!"command".equals(name)) {
                value = urlSafe(entry.getValue());

                if (sb.length() == 0)
                    sb.append(name).append("=").append(value);
                else
                    sb.append("&").append(name).append("=").append(value);
            }
        }

        sb.append("&apikey=").append(urlSafe(apiKey));
        sb.append("&signature=").append(urlSafe(signature));

        return sb.toString();
    }

    private String calculateRFC2104HMAC(String signIt, String secretKey) throws SignatureException {
        String result = null;
        try {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA1");
            Mac hmacSha1 = Mac.getInstance("HmacSHA1");
            hmacSha1.init(key);
            byte[] rawHmac = hmacSha1.doFinal(signIt.getBytes());
            result = new String(Base64.encodeBase64(rawHmac));
        } catch (Exception e) {
            throw new SignatureException("Failed to generate keyed HMAC on soap request: " + e.getMessage());
        }
        return result.trim();
    }

    private String urlSafe(String value) {
        try {
            if (value != null)
                return URLEncoder.encode(value, "UTF-8").replaceAll("\\+", "%20");
            else
                return null;
        } catch (UnsupportedEncodingException e) {
            assert (false);
        }

        return value;
    }
}
