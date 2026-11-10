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
package com.cloud.consoleproxy.vnc;

import com.cloud.utils.exception.CloudRuntimeException;
import org.junit.Assert;
import org.junit.Test;

public class NoVncClientTest {

    @Test(timeout = 10000)
    public void waitForNoVNCReplyTimesOutWhenNoReplyArrives() {
        NoVncClient client = new NoVncClient();
        client.setWaitForNoVnc(true);
        long start = System.nanoTime();
        Assert.assertThrows(CloudRuntimeException.class, () -> client.waitForNoVNCReply(200));
        long elapsedMs = (System.nanoTime() - start) / 1000000;
        Assert.assertTrue("Gave up after " + elapsedMs + " ms", elapsedMs >= 150);
    }

    @Test(timeout = 10000, expected = CloudRuntimeException.class)
    public void waitForNoVNCReplyFailsFastWhenAlreadyClosed() {
        NoVncClient client = new NoVncClient();
        client.close();
        client.setWaitForNoVnc(true);
        client.waitForNoVNCReply();
    }

    @Test
    public void waitForNoVNCReplyReturnsImmediatelyWhenNotWaiting() {
        NoVncClient client = new NoVncClient();
        client.waitForNoVNCReply();
    }

    @Test(timeout = 10000)
    public void waitForNoVNCReplyReturnsWhenReplyArrives() throws Exception {
        NoVncClient client = new NoVncClient();
        client.setWaitForNoVnc(true);
        Thread replier = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            client.setWaitForNoVnc(false);
        });
        replier.start();
        client.waitForNoVNCReply();
        replier.join();
        Assert.assertFalse(client.isWaitForNoVnc());
    }

    @Test(timeout = 10000, expected = CloudRuntimeException.class)
    public void waitForNoVNCReplyFailsWhenClientIsClosed() {
        NoVncClient client = new NoVncClient();
        client.setWaitForNoVnc(true);
        Thread closer = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            client.close();
        });
        closer.start();
        client.waitForNoVNCReply();
    }

    @Test(timeout = 10000, expected = CloudRuntimeException.class)
    public void waitForNoVNCReplyFailsWhenInterrupted() {
        NoVncClient client = new NoVncClient();
        client.setWaitForNoVnc(true);
        Thread.currentThread().interrupt();
        try {
            client.waitForNoVNCReply();
        } finally {
            Thread.interrupted();
        }
    }
}
