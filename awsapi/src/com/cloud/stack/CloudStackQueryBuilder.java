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
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.cloud.utils.StringUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * CloudStackQueryBuilder wraps command properties that are being sent to CloudStack
 */
public class CloudStackQueryBuilder {
    private static final String HMAC_SHA_1 = "HmacSHA1";
    private static final String COMMAND_KEY = "command";
    private static final String APIKEY_KEY = "apikey";

    private final Collection<NameValuePair> params = new ArrayList<NameValuePair>();

    public CloudStackQueryBuilder(final String commandName) {
        params.add(new NameValuePair("command", commandName));
        params.add(new NameValuePair("response", "json"));
    }

    public static CloudStackQueryBuilder create(final String commandName) {
        return new CloudStackQueryBuilder(commandName);
    }

    /**
     * parameter will be stored to query only if paramValue != null. This assumption is very useful
     * for optional parameters
     * @param paramName - not null parameter name
     * @param paramValue - nullable parameter value
     */
    public CloudStackQueryBuilder addParam(final String paramName, final Object paramValue) {
        if (paramName == null) {
            throw new NullPointerException();
        }
        if (paramValue != null) {
            params.add(new NameValuePair(paramName, String.valueOf(paramValue)));
        }
        return this;
    }

    public HttpMethod buildRequest(final String apiKey, final String secretKey) throws SignatureException {
        final PostMethod postMethod = new PostMethod();
        for (final NameValuePair param : params) {
            postMethod.addParameter(param);
        }
        postMethod.addParameter(APIKEY_KEY, apiKey);
        postMethod.addParameter("signature", calculateSignature(secretKey, apiKey));
        return postMethod;
    }

    private String calculateSignature(final String secretKey, final String apiKey) throws SignatureException {
        final List<NameValuePair> paramsCopy = new ArrayList<NameValuePair>(params.size() + 1);
        paramsCopy.addAll(params);
        paramsCopy.add(new NameValuePair(APIKEY_KEY, urlSafe(apiKey)));
        Collections.sort(paramsCopy, new Comparator<NameValuePair>() {
            @Override
            public int compare(final NameValuePair nameValuePair, final NameValuePair nameValuePair2) {
                return nameValuePair.getName().compareTo(nameValuePair2.getName());
            }
        });

        final List<String> serializedParameters = new ArrayList<String>(paramsCopy.size());
        for (final NameValuePair pair : paramsCopy) {
            serializedParameters.add(pair.getName() + "=" + urlSafe(pair.getValue()));
        }
        final String toSign = StringUtils.join(serializedParameters, "&").toLowerCase();
        return calculateRFC2104HMAC(toSign, secretKey);
    }

    private static String calculateRFC2104HMAC(final String signIt, final String secretKey) throws SignatureException {
        try {
            final Mac hmacSha1 = Mac.getInstance(HMAC_SHA_1);
            hmacSha1.init(new SecretKeySpec(secretKey.getBytes(), HMAC_SHA_1));
            final byte[] rawHmac = hmacSha1.doFinal(signIt.getBytes());
            return new String(Base64.encodeBase64(rawHmac)).trim();
        } catch (final Exception e) {
            throw new SignatureException("Failed to generate keyed HMAC on soap request: " + e.getMessage());
        }
    }

    private static String urlSafe(final String value) {
        try {
            return value == null ? null : URLEncoder.encode(value, "UTF-8").replaceAll("\\+", "%20");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
