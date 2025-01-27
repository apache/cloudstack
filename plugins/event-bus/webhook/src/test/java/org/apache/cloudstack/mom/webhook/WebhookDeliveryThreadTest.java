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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.DecoderException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class WebhookDeliveryThreadTest {
    @InjectMocks
    WebhookDeliveryThread webhookDeliveryThread;

    @Test
    public void testIsValidJson() {
        Assert.assertFalse(webhookDeliveryThread.isValidJson("text"));
        Assert.assertTrue(webhookDeliveryThread.isValidJson("{ \"CloudStack\": \"works!\" }"));
        Assert.assertTrue(webhookDeliveryThread.isValidJson("[{ \"CloudStack\": \"works!\" }]"));
    }

    @Test
    public void testGenerateHMACSignature() {
        String data = "CloudStack works!";
        String key = "Pj4pnwSUBZ4wQFXw2zWdVY1k5Ku9bIy70wCNG1DmS8keO7QapCLw2Axtgc2nEPYzfFCfB38ATNLt6caDqU2dSw";
        String result = "HYLWSII5Ap23WeSaykNsIo6mOhmV3d18s5p2cq2ebCA=";
        try {
            String sign = WebhookDeliveryThread.generateHMACSignature(data, key);
            Assert.assertEquals(result, sign);
        } catch (InvalidKeyException | NoSuchAlgorithmException | DecoderException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testSetDeliveryTries() {
        int tries = 2;
        webhookDeliveryThread.setDeliveryTries(tries);
        Assert.assertEquals(tries, ReflectionTestUtils.getField(webhookDeliveryThread, "deliveryTries"));
    }
}
