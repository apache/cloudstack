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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.net.NetUtils;

public class WebhookUrlValidatorTest {

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateWebhookDestinationUrlRejectsHttpWhenDisallowed() {
        WebhookUrlValidator.validateWebhookDestinationUrl("http://8.8.8.8/hook", false, "", true);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateWebhookDestinationUrlRejectsBlockedAddress() {
        WebhookUrlValidator.validateWebhookDestinationUrl("https://127.0.0.1/hook", true, "127.0.0.0/8", true);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateWebhookDestinationUrlRejectsLoopbackWhenNotInBlocklist() {
        WebhookUrlValidator.validateWebhookDestinationUrl("https://127.0.0.1/hook", true, "", true);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateWebhookDestinationUrlFailsClosedOnResolutionFailure() {
        WebhookUrlValidator.validateWebhookDestinationUrl("https://nonexistent.invalid/hook", true, "", true);
    }

    @Test
    public void testValidateWebhookDestinationUrlRejectsLocalManagementServerAddressWhenNotInBlocklist() throws Exception {
        InetAddress address = InetAddress.getByName("8.8.8.8");

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class, Mockito.CALLS_REAL_METHODS)) {
            netUtilsMock.when(() -> NetUtils.isLocalAddress(address)).thenReturn(true);

            try {
                WebhookUrlValidator.validateWebhookDestinationUrl("https://8.8.8.8/hook", false, "", true);
                Assert.fail("Expected InvalidParameterValueException");
            } catch (InvalidParameterValueException e) {
                Assert.assertTrue(e.getMessage().contains("blocked IP address"));
                Assert.assertTrue(e.getMessage().contains("8.8.8.8"));
            }
        }
    }

    @Test
    public void testValidateWebhookDestinationUrlAllowsLocalManagementServerAddressWhenDisabled() throws Exception {
        InetAddress address = InetAddress.getByName("8.8.8.8");

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class, Mockito.CALLS_REAL_METHODS)) {
            netUtilsMock.when(() -> NetUtils.isLocalAddress(address)).thenReturn(true);

            URI resolvedUri = WebhookUrlValidator.validateWebhookDestinationUrl("https://8.8.8.8/hook", false, "", false);
            Assert.assertEquals(URI.create("https://8.8.8.8/hook"), resolvedUri);
        }
    }

    @Test
    public void testValidateWebhookDestinationUrlAcceptsAllowedHttpsIpLiteral() {
        URI resolvedUri = WebhookUrlValidator.validateWebhookDestinationUrl("https://8.8.8.8/hook", false,
                "127.0.0.0/8,::1/128", true);
        Assert.assertEquals(URI.create("https://8.8.8.8/hook"), resolvedUri);
    }

    @Test
    public void testValidateWebhookDestinationUrlAllowsLoopbackWhenLocalAddressBlockingDisabled() {
        URI resolvedUri = WebhookUrlValidator.validateWebhookDestinationUrl("https://127.0.0.1/hook", true, "", false);
        Assert.assertEquals(URI.create("https://127.0.0.1/hook"), resolvedUri);
    }

    @Test
    public void testGetNormalizedBlocklist() {
        String[] cidrs = WebhookUrlValidator.getNormalizedBlocklist(" 127.0.0.0/8, ::1/128 ,, 192.168.0.0/16 ");
        Assert.assertArrayEquals(new String[] {"127.0.0.0/8", "::1/128", "192.168.0.0/16"}, cidrs);
    }
}
