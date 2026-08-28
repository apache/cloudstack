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

package org.apache.cloudstack.mom.webhook;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.rest.HttpConstants;

public final class WebhookUrlValidator {

    private WebhookUrlValidator() {
    }

    public static URI validateWebhookDestinationUrl(final String payloadUrl, final boolean allowHttp,
            final String blocklist, final boolean blockLocalAddresses) {
        return validateWebhookDestinationURI(URI.create(payloadUrl), allowHttp, blocklist, blockLocalAddresses);
    }

    public static URI validateWebhookDestinationURI(final URI uri, final boolean allowHttp,
                                                    final String blocklist, final boolean blockLocalAddresses) {
        validateScheme(uri, allowHttp);
        InetAddress[] resolved = validateResolvedAddresses(uri, blocklist, blockLocalAddresses);
        return buildResolvedUri(uri, resolved[0]);
    }

    private static URI buildResolvedUri(final URI uri, final InetAddress address) {
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), address.getHostAddress(), uri.getPort(),
                    uri.getPath(), uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new InvalidParameterValueException(
                    String.format("Failed to build resolved webhook payload URL from [%s]", uri));
        }
    }

    public static void validateScheme(final URI uri, final boolean allowHttp) {
        final String scheme = uri.getScheme();
        if (HttpConstants.HTTPS.equalsIgnoreCase(scheme)) {
            return;
        }
        if (allowHttp && "http".equalsIgnoreCase(scheme)) {
            return;
        }
        if (allowHttp) {
            throw new InvalidParameterValueException(
                    String.format("Unsupported URL scheme [%s], only HTTP/HTTPS are supported", scheme));
        }
        throw new InvalidParameterValueException(
                String.format("Only HTTPS webhook payload URLs are allowed, got: %s", uri));
    }

    public static InetAddress[] validateResolvedAddresses(final URI uri, final String blocklist,
            final boolean blockLocalAddresses) {
        final String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            throw new InvalidParameterValueException(
                    String.format("Invalid webhook payload URL host in [%s]", uri));
        }

        final InetAddress[] resolved;
        try {
            resolved = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new InvalidParameterValueException(
                    String.format("Failed to resolve webhook payload URL host [%s]", host));
        }

        if (resolved.length == 0) {
            throw new InvalidParameterValueException(
                    String.format("Failed to resolve webhook payload URL host [%s]", host));
        }

        final String[] blockedCidrs = getNormalizedBlocklist(blocklist);
        for (InetAddress address : resolved) {
            if ((blockLocalAddresses && isLocalManagementServerAddress(address)) ||
                    NetUtils.isIpInCidrList(address, blockedCidrs)) {
                throw new InvalidParameterValueException(
                        String.format("Webhook payload URL [%s] resolves to a blocked IP address",  uri));
            }
        }
        return resolved;
    }

    static boolean isLocalManagementServerAddress(final InetAddress address) {
        return address != null
                && (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || NetUtils.isLocalAddress(address));
    }

    public static String[] getNormalizedBlocklist(final String blocklist) {
        if (StringUtils.isBlank(blocklist)) {
            return new String[0];
        }
        return Arrays.stream(blocklist.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
    }
}
